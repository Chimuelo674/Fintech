package cl.duocuc.pagoflex.repository

import cl.duocuc.pagoflex.data.local.PagoFlexDatabase
import cl.duocuc.pagoflex.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class PagoFlexRepository(private val db: PagoFlexDatabase) {

    private val usuarioDao = db.usuarioDao()
    private val empresaDao = db.empresaDao()
    private val compromisoDao = db.compromisoDao()
    private val pagoDao = db.pagoDao()
    private val reporteDao = db.reporteDao()
    private val recomendacionDao = db.recomendacionDao()
    private val reconocimientoDao = db.reconocimientoDao()

    // --- Usuarios & Sesión ---
    fun getAllUsuarios(): Flow<List<UsuarioEntity>> = usuarioDao.getAllUsuarios()
    suspend fun getUsuarioById(id: String): UsuarioEntity? = usuarioDao.getUsuarioById(id)
    suspend fun updateUsuario(usuario: UsuarioEntity) = usuarioDao.updateUsuario(usuario)

    // --- Empresas ---
    fun getAllEmpresas(): Flow<List<EmpresaEntity>> = empresaDao.getAllEmpresas()
    suspend fun getEmpresaById(id: String): EmpresaEntity? = empresaDao.getEmpresaById(id)

    // --- Compromisos ---
    fun getCompromisosByUsuario(idUsuario: String): Flow<List<CompromisoEntity>> = compromisoDao.getCompromisosByUsuario(idUsuario)
    fun getCompromisosByEmpresa(idEmpresa: String): Flow<List<CompromisoEntity>> = compromisoDao.getCompromisosByEmpresa(idEmpresa)
    suspend fun getCompromisoById(id: String): CompromisoEntity? = compromisoDao.getCompromisoById(id)

    // --- Validaciones y Procesamiento de Pago (RN-04, RN-05, RN-06, RN-08, RN-09) ---
    suspend fun validarOrdenPago(idUsuario: String, compromisoTarget: CompromisoEntity): Result<Unit> {
        if (compromisoTarget.estado == "En revisión") {
            return Result.failure(Exception("El compromiso está 'En revisión' por un reporte abierto (RN-08)."))
        }
        if (compromisoTarget.estado == "Anulado") {
            return Result.failure(Exception("El compromiso está 'Anulado' y no puede pagarse (RN-09)."))
        }
        if (compromisoTarget.estado == "Pagado") {
            return Result.failure(Exception("Este compromiso ya fue pagado."))
        }

        // RN-04: Pagar en orden el más antiguo primero para la misma empresa
        val compromisosUsuario = compromisoDao.getCompromisosByUsuario(idUsuario).first()
        val impagosMismaEmpresa = compromisosUsuario.filter {
            it.idEmpresa == compromisoTarget.idEmpresa && (it.estado == "Pendiente" || it.estado == "Vencido")
        }.sortedBy { it.fechaVencimiento }

        if (impagosMismaEmpresa.isNotEmpty() && impagosMismaEmpresa.first().idCompromiso != compromisoTarget.idCompromiso) {
            val masAntiguo = impagosMismaEmpresa.first()
            return Result.failure(Exception("Por regla RN-04, debes pagar primero la cuota/compromiso más antiguo con esta empresa (${masAntiguo.concepto} - Vence: ${masAntiguo.fechaVencimiento})."))
        }

        return Result.success(Unit)
    }

    suspend fun realizarPagoSimulado(
        idUsuario: String,
        compromiso: CompromisoEntity,
        medioPago: String
    ): Result<PagoEntity> {
        val validacion = validarOrdenPago(idUsuario, compromiso)
        if (validacion.isFailure) return Result.failure(validacion.exceptionOrNull()!!)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val nowStr = dateFormat.format(Date())

        val recargo = if (compromiso.estado == "Vencido") compromiso.recargoAplicado else 0.0
        val totalMonto = compromiso.monto + recargo

        val numeroComprobante = "CPR-${System.currentTimeMillis().toString().takeLast(6)}"

        val pago = PagoEntity(
            idPago = numeroComprobante,
            numeroComprobante = numeroComprobante,
            idCompromiso = compromiso.idCompromiso,
            idUsuarioFinal = idUsuario,
            idEmpresa = compromiso.idEmpresa,
            fechaHora = nowStr,
            montoPagado = totalMonto,
            recargoPagado = recargo,
            canal = "App",
            medioPago = medioPago,
            aTiempo = compromiso.estado != "Vencido"
        )

        // Actualizar compromiso a Pagado
        val compromisoPagado = compromiso.copy(estado = "Pagado")
        compromisoDao.updateCompromiso(compromisoPagado)

        // Insertar pago
        pagoDao.insertPago(pago)

        return Result.success(pago)
    }

    // --- Pagos e Historial ---
    fun getPagosByUsuario(idUsuario: String): Flow<List<PagoEntity>> = pagoDao.getPagosByUsuario(idUsuario)
    suspend fun getPagoByCompromiso(idCompromiso: String): PagoEntity? = pagoDao.getPagoByCompromiso(idCompromiso)

    // --- Reportes de Problemas ---
    fun getReportesByUsuario(idUsuario: String): Flow<List<ReporteEntity>> = reporteDao.getReportesByUsuario(idUsuario)
    fun getAllReportes(): Flow<List<ReporteEntity>> = reporteDao.getAllReportes()

    suspend fun crearReporte(idUsuario: String, idCompromiso: String, motivo: String, comentario: String): ReporteEntity {
        val dateFormat = SimpleDateFormat("yyyy-MM-DD", Locale.getDefault())
        val nowStr = dateFormat.format(Date())
        val idReporte = "REP-${System.currentTimeMillis().toString().takeLast(4)}"

        val reporte = ReporteEntity(
            idReporte = idReporte,
            idCompromiso = idCompromiso,
            idUsuarioFinal = idUsuario,
            motivo = motivo,
            comentario = comentario,
            fechaCreacion = nowStr,
            estado = "Abierto"
        )
        reporteDao.insertReporte(reporte)

        // Cambiar estado del compromiso a "En revisión"
        val comp = compromisoDao.getCompromisoById(idCompromiso)
        if (comp != null && comp.estado != "Pagado" && comp.estado != "Anulado") {
            compromisoDao.updateCompromiso(comp.copy(estado = "En revisión"))
        }

        return reporte
    }

    suspend fun resolverReporte(
        idReporte: String,
        idAgente: String,
        respuesta: String,
        nuevoEstadoCompromiso: String? = null
    ) {
        val reportes = reporteDao.getAllReportes().first()
        val reporte = reportes.find { it.idReporte == idReporte } ?: return

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val nowStr = dateFormat.format(Date())

        val reporteActualizado = reporte.copy(
            estado = "Resuelto",
            idAgente = idAgente,
            respuesta = respuesta,
            fechaResolucion = nowStr
        )
        reporteDao.updateReporte(reporteActualizado)

        // Restaurar/Anular compromiso si aplica
        if (nuevoEstadoCompromiso != null) {
            val comp = compromisoDao.getCompromisoById(reporte.idCompromiso)
            if (comp != null) {
                compromisoDao.updateCompromiso(comp.copy(estado = nuevoEstadoCompromiso))
            }
        }
    }

    // --- Recomendaciones ---
    fun getRecomendacionesByUsuario(idUsuario: String): Flow<List<RecomendacionEntity>> = recomendacionDao.getRecomendacionesByUsuario(idUsuario)
    fun getAllRecomendaciones(): Flow<List<RecomendacionEntity>> = recomendacionDao.getAllRecomendaciones()

    suspend fun crearRecomendacion(
        idUsuario: String,
        nombreOrg: String,
        rutOrg: String?,
        rubro: String?,
        comuna: String?,
        nombreContacto: String?,
        correoContacto: String?,
        telefonoContacto: String?,
        autorizaContacto: Boolean
    ): RecomendacionEntity {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val nowStr = dateFormat.format(Date())
        val idRec = "REC-${System.currentTimeMillis().toString().takeLast(4)}"

        val recomendacion = RecomendacionEntity(
            idRecomendacion = idRec,
            idUsuarioFinal = idUsuario,
            nombreOrganizacion = nombreOrg,
            rutOrganizacion = rutOrg,
            rubro = rubro,
            comuna = comuna,
            nombreContacto = nombreContacto,
            correoContacto = correoContacto,
            telefonoContacto = telefonoContacto,
            autorizaContacto = autorizaContacto,
            fecha = nowStr,
            estado = "Recibida"
        )
        recomendacionDao.insertRecomendacion(recomendacion)
        return recomendacion
    }

    suspend fun cambiarEstadoRecomendacion(
        idRecomendacion: String,
        idAgente: String,
        nuevoEstado: String,
        motivoDescarte: String? = null
    ) {
        val recomendaciones = recomendacionDao.getAllRecomendaciones().first()
        val rec = recomendaciones.find { it.idRecomendacion == idRecomendacion } ?: return

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val nowStr = dateFormat.format(Date())

        val recActualizada = rec.copy(
            estado = nuevoEstado,
            motivoDescarte = motivoDescarte,
            idAgente = idAgente,
            fechaUltimoCambio = nowStr
        )
        recomendacionDao.updateRecomendacion(recActualizada)
    }

    // --- Reconocimientos ---
    fun getReconocimientosByUsuario(idUsuario: String): Flow<List<ReconocimientoEntity>> = reconocimientoDao.getReconocimientosByUsuario(idUsuario)

    // --- Ejecutivo: Registrar y Anular Compromisos ---
    suspend fun registrarCompromiso(
        idEjecutivo: String,
        idEmpresa: String,
        rutUsuarioFinal: String,
        concepto: String,
        cuota: Int?,
        totalCuotas: Int?,
        monto: Double,
        fechaVencimiento: String
    ): Result<CompromisoEntity> {
        val usuarios = usuarioDao.getAllUsuarios().first()
        val usuarioTarget = usuarios.find { it.rut.replace(".", "").replace("-", "").lowercase() == rutUsuarioFinal.replace(".", "").replace("-", "").lowercase() }
            ?: return Result.failure(Exception("No se encontró ningún usuario con el RUT $rutUsuarioFinal"))

        val idComp = "CP-${System.currentTimeMillis().toString().takeLast(6)}"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val nowStr = dateFormat.format(Date())

        val comp = CompromisoEntity(
            idCompromiso = idComp,
            folio = idComp,
            idUsuarioFinal = usuarioTarget.idUsuario,
            idEmpresa = idEmpresa,
            concepto = concepto,
            numeroCuota = cuota,
            totalCuotas = totalCuotas,
            monto = monto,
            fechaVencimiento = fechaVencimiento,
            estado = "Pendiente",
            recargoAplicado = 0.0,
            registradoPor = idEjecutivo,
            fechaRegistro = nowStr
        )
        compromisoDao.insertCompromiso(comp)
        return Result.success(comp)
    }

    suspend fun anularCompromiso(
        idCompromiso: String,
        idEjecutivo: String,
        motivo: String,
        comentario: String
    ): Result<Unit> {
        val comp = compromisoDao.getCompromisoById(idCompromiso) ?: return Result.failure(Exception("Compromiso no encontrado"))
        if (comp.estado == "Pagado") return Result.failure(Exception("No se puede anular un compromiso que ya está Pagado."))

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val nowStr = dateFormat.format(Date())

        val compAnulado = comp.copy(
            estado = "Anulado",
            motivoAnulacion = motivo,
            comentarioAnulacion = comentario,
            ejecutivoAnulacion = idEjecutivo,
            fechaAnulacion = nowStr
        )
        compromisoDao.updateCompromiso(compAnulado)
        return Result.success(Unit)
    }
}

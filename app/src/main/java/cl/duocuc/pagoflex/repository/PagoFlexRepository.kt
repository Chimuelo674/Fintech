package cl.duocuc.pagoflex.repository

import androidx.room.withTransaction
import cl.duocuc.pagoflex.data.local.PagoFlexDatabase
import cl.duocuc.pagoflex.data.local.entities.*
import cl.duocuc.pagoflex.domain.ReglasRecomendacion
import cl.duocuc.pagoflex.domain.ReglasRolesInternos
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.util.Rut
import cl.duocuc.pagoflex.util.Reloj
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Resultado del indicador de cumplimiento (RN-10, RN-11).
 * [porcentaje] es null cuando hay menos de 3 compromisos ("historial insuficiente").
 */
data class Cumplimiento(
    val considerados: Int,
    val aTiempo: Int,
    val porcentaje: Double?,
    val categoria: String
) {
    val historialSuficiente: Boolean get() = porcentaje != null
}

class PagoFlexRepository(private val db: PagoFlexDatabase) {

    private val usuarioDao = db.usuarioDao()
    private val empresaDao = db.empresaDao()
    private val compromisoDao = db.compromisoDao()
    private val pagoDao = db.pagoDao()
    private val reporteDao = db.reporteDao()
    private val recomendacionDao = db.recomendacionDao()
    private val reconocimientoDao = db.reconocimientoDao()
    private val historialRecDao = db.historialRecomendacionDao()
    private val catalogoDao = db.catalogoDao()

    // --- Catálogos ---
    /** Descripción de cada reconocimiento (catálogo), por nombre. */
    fun getCatalogoReconocimientos(): Flow<Map<String, String>> =
        catalogoDao.getReconocimientos().map { lista -> lista.associate { it.nombre to it.descripcion } }

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

    // =====================================================================================
    // Estados por fecha (RN-01, RN-02, RN-03)
    // =====================================================================================

    /**
     * Pasa a "Vencido" todo compromiso "Pendiente" cuya fecha de vencimiento ya pasó
     * (según la fecha de referencia), copiando el recargo vigente de su empresa (RN-03:
     * se suma una sola vez, al vencer). Se llama al abrir la app y al entrar con una sesión.
     * Pagado, Anulado y En revisión no se tocan.
     */
    suspend fun sincronizarEstados() {
        db.withTransaction {
            val recargos = empresaDao.getAllEmpresasOnce().associate { it.idEmpresa to it.recargoAtraso }
            val aVencer = compromisoDao.getAllCompromisosOnce().filter {
                it.estado == "Pendiente" && Fechas.estaVencida(it.fechaVencimiento)
            }
            aVencer.forEach { c ->
                compromisoDao.updateCompromiso(
                    c.copy(estado = "Vencido", recargoAplicado = recargos[c.idEmpresa] ?: 0.0)
                )
            }
        }
    }

    /**
     * Estado que corresponde a un compromiso impago según su fecha (RN-09: al resolver un
     * reporte "vuelve a Pendiente o Vencido según la fecha"). Si queda Vencido, aplica el recargo.
     */
    private suspend fun restaurarSegunFecha(c: CompromisoEntity): CompromisoEntity =
        if (Fechas.estaVencida(c.fechaVencimiento)) {
            val recargo = empresaDao.getEmpresaById(c.idEmpresa)?.recargoAtraso ?: 0.0
            c.copy(estado = "Vencido", recargoAplicado = recargo)
        } else {
            c.copy(estado = "Pendiente", recargoAplicado = 0.0)
        }

    // =====================================================================================
    // Indicador de cumplimiento (RN-10, RN-11) — se calcula, no se guarda (MER 2.3)
    // =====================================================================================

    fun getCumplimiento(idUsuario: String): Flow<Cumplimiento> =
        combine(
            compromisoDao.getCompromisosByUsuario(idUsuario),
            pagoDao.getPagosByUsuario(idUsuario)
        ) { compromisos, pagos -> calcularCumplimiento(compromisos, pagos) }

    companion object {
        /** RN-09: motivos posibles de un reporte. */
        val MOTIVOS_REPORTE = listOf("Ya pagué por otro medio", "No reconozco este cobro", "El monto no corresponde", "Otro")

        /**
         * RN-10: % de compromisos pagados a tiempo (fecha de pago ≤ vencimiento) sobre los
         * compromisos con vencimiento en los últimos 12 meses. Se excluyen Anulados,
         * En revisión y los que aún no vencen. Con menos de 3: historial insuficiente.
         * RN-11: ≥95 Ejemplar; 80–94,9 Al día; <80 En camino.
         */
        fun calcularCumplimiento(
            compromisos: List<CompromisoEntity>,
            pagos: List<PagoEntity>,
            hoy: String = Reloj.hoy()
        ): Cumplimiento {
            val desde = Fechas.sumarMeses(hoy, -12)
            val pagoPorCompromiso = pagos.associateBy { it.idCompromiso }

            val considerados = compromisos.filter {
                it.estado != "Anulado" && it.estado != "En revisión" &&
                    it.fechaVencimiento > desde && it.fechaVencimiento < hoy
            }
            val aTiempo = considerados.count { c ->
                val pago = pagoPorCompromiso[c.idCompromiso]
                pago != null && pago.fechaHora.take(10) <= c.fechaVencimiento
            }

            if (considerados.size < 3) {
                return Cumplimiento(considerados.size, aTiempo, null, "Historial insuficiente")
            }
            // Redondeo a un decimal: 33/36 = 91,666… -> 91,7
            val porcentaje = Math.round(aTiempo * 1000.0 / considerados.size) / 10.0
            val categoria = when {
                porcentaje >= 95.0 -> "Ejemplar"
                porcentaje >= 80.0 -> "Al día"
                else -> "En camino"
            }
            return Cumplimiento(considerados.size, aTiempo, porcentaje, categoria)
        }
    }

    // =====================================================================================
    // Pago (RN-04, RN-05, RN-06, RN-08, RN-09, Aclaraciones 1, 5 y 6)
    // =====================================================================================

    suspend fun validarOrdenPago(idUsuario: String, compromisoTarget: CompromisoEntity): Result<Unit> {
        if (compromisoTarget.estado == "En revisión") {
            return Result.failure(Exception("Este compromiso tiene un reporte en revisión. Podrás pagarlo cuando se resuelva."))
        }
        if (compromisoTarget.estado == "Anulado") {
            return Result.failure(Exception("Este compromiso fue anulado por la empresa, así que no hay nada que pagar."))
        }
        if (compromisoTarget.estado == "Pagado") {
            return Result.failure(Exception("Este compromiso ya está pagado."))
        }

        // RN-04: con la misma empresa se paga primero el de vencimiento más antiguo
        val compromisosUsuario = compromisoDao.getCompromisosByUsuarioOnce(idUsuario)
        val impagosMismaEmpresa = compromisosUsuario.filter {
            it.idEmpresa == compromisoTarget.idEmpresa && (it.estado == "Pendiente" || it.estado == "Vencido")
        }.sortedBy { it.fechaVencimiento }

        if (impagosMismaEmpresa.isNotEmpty() && impagosMismaEmpresa.first().idCompromiso != compromisoTarget.idCompromiso) {
            val masAntiguo = impagosMismaEmpresa.first()
            val cuota = masAntiguo.numeroCuota?.let { " (cuota $it/${masAntiguo.totalCuotas ?: "?"})" } ?: ""
            return Result.failure(
                Exception(
                    "Primero debes pagar \"${masAntiguo.concepto}$cuota\", que vence el " +
                        "${Fechas.mostrar(masAntiguo.fechaVencimiento)}. Con una misma empresa se paga en orden."
                )
            )
        }

        return Result.success(Unit)
    }

    suspend fun realizarPagoSimulado(
        idUsuario: String,
        compromiso: CompromisoEntity,
        medioPago: String
    ): Result<PagoEntity> {
        // Aclaración nº 5: el pago y el cambio a "Pagado" ocurren en una sola transacción.
        // Si algo falla, no queda ni el comprobante ni el compromiso como Pagado.
        return try {
            db.withTransaction {
                // Se relee el compromiso dentro de la transacción para no pagar sobre un estado
                // desactualizado (ej. ya pagado por otro canal, RN-07, o pasado a En revisión).
                val actual = compromisoDao.getCompromisoById(compromiso.idCompromiso)
                    ?: throw IllegalStateException("No encontramos este compromiso.")
                if (pagoDao.getPagoByCompromiso(actual.idCompromiso) != null) {
                    throw IllegalStateException("Este compromiso ya está pagado.")
                }
                validarOrdenPago(idUsuario, actual).getOrThrow()

                val ahora = Reloj.ahora()
                val recargo = if (actual.estado == "Vencido") actual.recargoAplicado else 0.0
                val numeroComprobante = generarNumeroComprobante()

                val pago = PagoEntity(
                    idPago = numeroComprobante,
                    numeroComprobante = numeroComprobante,
                    idCompromiso = actual.idCompromiso,
                    fechaHora = ahora,
                    montoPagado = actual.monto + recargo, // RN-06: solo el total
                    canal = "App",
                    medioPago = medioPago
                )

                // Primero el comprobante, luego el estado: ambos o ninguno.
                pagoDao.insertPago(pago)
                compromisoDao.updateCompromiso(actual.copy(estado = "Pagado"))
                pago
            }.let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Correlativo CPR-000454, CPR-000455… continuando desde el último comprobante guardado. */
    private suspend fun generarNumeroComprobante(): String {
        val ultimo = pagoDao.getMaxNumeroComprobante()
            ?.removePrefix("CPR-")?.toIntOrNull() ?: 0
        return "CPR-%06d".format(ultimo + 1)
    }

    // --- Pagos e Historial ---
    fun getPagosByUsuario(idUsuario: String): Flow<List<PagoEntity>> = pagoDao.getPagosByUsuario(idUsuario)
    suspend fun getPagoByCompromiso(idCompromiso: String): PagoEntity? = pagoDao.getPagoByCompromiso(idCompromiso)
    suspend fun getPagoByNumero(numeroComprobante: String): PagoEntity? = pagoDao.getPagoByNumero(numeroComprobante)

    // --- Reportes de Problemas ---
    fun getReportesByUsuario(idUsuario: String): Flow<List<ReporteEntity>> = reporteDao.getReportesByUsuario(idUsuario)
    fun getAllReportes(): Flow<List<ReporteEntity>> = reporteDao.getAllReportes()

    /**
     * RF-09 / RN-09: la persona reporta un problema con un compromiso suyo que está Pendiente
     * o Vencido. El compromiso pasa a En revisión (y no se puede pagar). Un compromiso solo
     * puede tener un reporte abierto a la vez (HU-06 CA-2). "Otro" exige comentario.
     */
    suspend fun crearReporte(idUsuario: String, idCompromiso: String, motivo: String, comentario: String): Result<ReporteEntity> = try {
        db.withTransaction {
            val comp = compromisoDao.getCompromisoById(idCompromiso)
            if (comp == null || comp.idUsuarioFinal != idUsuario) throw IllegalStateException("No encontramos este compromiso.")
            reporteDao.getReportesByCompromisoOnce(idCompromiso).firstOrNull { it.estado != "Resuelto" }?.let {
                throw IllegalStateException("Este compromiso ya tiene un reporte en curso (${it.idReporte}).")
            }
            if (comp.estado != "Pendiente" && comp.estado != "Vencido") {
                throw IllegalStateException("Solo puedes reportar compromisos pendientes o vencidos (este está ${comp.estado}).")
            }
            if (motivo !in MOTIVOS_REPORTE) throw IllegalArgumentException("Elige un motivo.")
            if (motivo == "Otro" && comentario.isBlank()) throw IllegalArgumentException("Con el motivo \"Otro\" cuéntanos qué pasó en el comentario.")

            val ultimo = reporteDao.getMaxIdReporte()?.removePrefix("REP-")?.toIntOrNull() ?: 0
            val reporte = ReporteEntity(
                idReporte = "REP-%04d".format(ultimo + 1),
                idCompromiso = idCompromiso,
                motivo = motivo,
                comentario = comentario.trim().ifBlank { null },
                fechaCreacion = Reloj.hoy(),
                estado = "Abierto"
            )
            reporteDao.insertReporte(reporte)
            compromisoDao.updateCompromiso(comp.copy(estado = "En revisión"))
            reporte
        }.let { Result.success(it) }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getReporteById(id: String): ReporteEntity? = reporteDao.getReporteById(id)

    /** RN-23: el agente toma un reporte Abierto; pasa a En gestión y queda como responsable. */
    suspend fun tomarReporte(idReporte: String, idAgente: String): Result<Unit> = try {
        db.withTransaction {
            val r = reporteDao.getReporteById(idReporte) ?: throw IllegalStateException("No encontramos el reporte.")
            if (r.estado != "Abierto") {
                throw IllegalStateException("Este reporte ya fue tomado por ${r.idAgente ?: "otro agente"}.")
            }
            reporteDao.updateReporte(r.copy(estado = "En gestión", idAgente = idAgente, fechaToma = Reloj.hoy()))
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * RN-23: solo el agente responsable puede resolver un reporte En gestión, y debe escribir
     * la respuesta para el usuario. El agente no anula: si la empresa ya anuló el compromiso
     * (RN-21) queda Anulado; si no, el compromiso vuelve a Pendiente o Vencido según la fecha (RN-09).
     */
    suspend fun resolverReporte(idReporte: String, idAgente: String, respuesta: String): Result<Unit> = try {
        db.withTransaction {
            val r = reporteDao.getReporteById(idReporte) ?: throw IllegalStateException("No encontramos el reporte.")
            when {
                r.estado != "En gestión" -> throw IllegalStateException("Primero debes tomar el reporte.")
                r.idAgente != idAgente -> throw IllegalStateException("Este reporte lo está gestionando ${r.idAgente}.")
                respuesta.isBlank() -> throw IllegalStateException("Escribe la respuesta para el usuario antes de resolver.")
            }
            reporteDao.updateReporte(
                r.copy(estado = "Resuelto", respuesta = respuesta.trim(), fechaResolucion = Reloj.hoy())
            )
            val comp = compromisoDao.getCompromisoById(r.idCompromiso)
            if (comp != null && comp.estado == "En revisión") {
                compromisoDao.updateCompromiso(restaurarSegunFecha(comp))
            }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // --- Recomendaciones ---
    fun getRecomendacionesByUsuario(idUsuario: String): Flow<List<RecomendacionEntity>> = recomendacionDao.getRecomendacionesByUsuario(idUsuario)
    fun getAllRecomendaciones(): Flow<List<RecomendacionEntity>> = recomendacionDao.getAllRecomendaciones()

    /**
     * RF-12 / RN-12: registra una recomendación en estado Recibida. Valida los datos, que la
     * persona no haya recomendado antes la misma organización (HU-08 CA-3) y el máximo de 5
     * por mes. Los datos de contacto solo se guardan si la persona declaró la autorización.
     */
    suspend fun crearRecomendacion(idUsuario: String, datos: ReglasRecomendacion.Datos): Result<RecomendacionEntity> = try {
        ReglasRecomendacion.validar(datos)?.let { throw IllegalArgumentException(it) }
        db.withTransaction {
            val existentes = recomendacionDao.getRecomendacionesByUsuarioOnce(idUsuario)
            ReglasRecomendacion.buscarDuplicada(existentes, datos.nombreOrganizacion)?.let {
                throw IllegalStateException(
                    "Ya recomendaste ${it.nombreOrganizacion} el ${Fechas.mostrar(it.fecha)}. Su estado es: ${it.estado}."
                )
            }
            val hoy = Reloj.hoy()
            if (ReglasRecomendacion.usadasEnElMes(existentes, hoy.take(7)) >= ReglasRecomendacion.MAXIMO_POR_MES) {
                throw IllegalStateException("Ya hiciste ${ReglasRecomendacion.MAXIMO_POR_MES} recomendaciones este mes. Podrás hacer más el próximo mes.")
            }
            val ultimo = recomendacionDao.getMaxIdRecomendacion()?.removePrefix("REC-")?.toIntOrNull() ?: 0
            val conContacto = datos.autorizaContacto && datos.tieneDatosDeContacto
            val rec = RecomendacionEntity(
                idRecomendacion = "REC-%04d".format(ultimo + 1),
                idUsuarioFinal = idUsuario,
                nombreOrganizacion = datos.nombreOrganizacion.trim(),
                rutOrganizacion = datos.rutOrganizacion.takeIf { it.isNotBlank() }?.let { Rut.formatear(it) },
                rubro = datos.rubro.trim(),
                comuna = datos.comuna.trim(),
                nombreContacto = datos.nombreContacto.trim().takeIf { conContacto && it.isNotBlank() },
                correoContacto = datos.correoContacto.trim().takeIf { conContacto && it.isNotBlank() },
                telefonoContacto = datos.telefonoContacto.trim().takeIf { conContacto && it.isNotBlank() },
                autorizaContacto = conContacto,
                fecha = hoy,
                estado = "Recibida"
            )
            // Rubro y comuna son catálogos (FK): si la persona escribió uno nuevo, se agrega al catálogo
            catalogoDao.insertRubros(listOf(RubroEntity(rec.rubro!!)))
            catalogoDao.insertComunas(listOf(ComunaEntity(rec.comuna!!)))
            recomendacionDao.insertRecomendacion(rec)
            rec
        }.let { Result.success(it) }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getHistorialRecomendaciones(): Flow<List<HistorialRecomendacionEntity>> = historialRecDao.getAll()

    /**
     * RN-13: solo un agente cambia el estado, sin saltarse etapas, y cada cambio queda en el
     * historial con agente y fecha (RNF-15). Descartar exige motivo.
     * RN-14: al pasar a "Se unió", quien recomendó recibe un reconocimiento simbólico.
     */
    suspend fun cambiarEstadoRecomendacion(
        idRecomendacion: String,
        idAgente: String,
        nuevoEstado: String,
        motivoDescarte: String? = null
    ): Result<Unit> = try {
        db.withTransaction {
            val rec = recomendacionDao.getRecomendacionById(idRecomendacion)
                ?: throw IllegalStateException("No encontramos la recomendación.")
            if (nuevoEstado !in ReglasRolesInternos.siguientesEstados(rec.estado)) {
                throw IllegalStateException("Desde \"${rec.estado}\" no se puede pasar a \"$nuevoEstado\" (no se pueden saltar etapas).")
            }
            if (nuevoEstado == "Descartada" && motivoDescarte.isNullOrBlank()) {
                throw IllegalStateException("Indica el motivo del descarte.")
            }
            val hoy = Reloj.hoy()
            val motivo = motivoDescarte?.trim()?.takeIf { nuevoEstado == "Descartada" }
            recomendacionDao.updateRecomendacion(
                rec.copy(estado = nuevoEstado, motivoDescarte = motivo, idAgente = idAgente, fechaUltimoCambio = hoy)
            )
            historialRecDao.insert(
                HistorialRecomendacionEntity(
                    idRecomendacion = idRecomendacion, estado = nuevoEstado, idAgente = idAgente, fecha = hoy, motivo = motivo
                )
            )
            if (nuevoEstado == "Se unió") {
                reconocimientoDao.insertReconocimiento(
                    ReconocimientoEntity(
                        idUsuarioFinal = rec.idUsuarioFinal,
                        nombreReconocimiento = "Embajador PagoFlex",
                        fechaObtencion = hoy,
                        motivo = "${rec.idRecomendacion} · ${rec.nombreOrganizacion} se unió"
                    )
                )
            }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // --- Reconocimientos ---
    fun getReconocimientosByUsuario(idUsuario: String): Flow<List<ReconocimientoEntity>> = reconocimientoDao.getReconocimientosByUsuario(idUsuario)

    // =====================================================================================
    // Ejecutivo de empresa cliente (RF-17 a RF-20, RN-20 a RN-22, RN-24)
    // =====================================================================================

    fun getAllCompromisos(): Flow<List<CompromisoEntity>> = compromisoDao.getAllCompromisos()
    fun getPagosByEmpresa(idEmpresa: String): Flow<List<PagoEntity>> = pagoDao.getPagosByEmpresa(idEmpresa)

    suspend fun buscarUsuarioFinalPorRut(rut: String): UsuarioEntity? =
        usuarioDao.getAllUsuariosOnce().firstOrNull { it.idRol == "USUARIO_FINAL" && Rut.mismoRut(it.rut, rut) }

    /** Datos básicos para registrar a una persona que aún no existe en PagoFlex (RN-20). */
    data class PersonaNueva(val nombres: String, val apellidos: String, val correo: String, val telefono: String)

    /**
     * RF-18 / RN-20. [fechaVencimiento] en formato interno yyyy-MM-dd (o null si es inválida).
     * Si la persona no existe, se crea con [personaNueva]. El compromiso queda Pendiente, con
     * folio correlativo (CP-2026-0113…) y el ejecutivo que lo registró.
     */
    suspend fun registrarCompromiso(
        idEjecutivo: String,
        idEmpresa: String,
        rutUsuarioFinal: String,
        concepto: String,
        cuota: Int?,
        totalCuotas: Int?,
        monto: Long?,
        fechaVencimiento: String?,
        personaNueva: PersonaNueva? = null
    ): Result<CompromisoEntity> = try {
        ReglasRolesInternos.validarDatosCompromiso(
            rutUsuarioFinal, concepto, cuota, totalCuotas, monto, fechaVencimiento
        )?.let { throw IllegalArgumentException(it) }

        db.withTransaction {
            val usuario = buscarUsuarioFinalPorRut(rutUsuarioFinal) ?: crearUsuarioFinal(rutUsuarioFinal, personaNueva)

            val existentes = compromisoDao.getCompromisosByUsuarioOnce(usuario.idUsuario)
            if (ReglasRolesInternos.esDuplicado(existentes, usuario.idUsuario, idEmpresa, concepto, cuota)) {
                throw IllegalArgumentException("Ya existe este compromiso para ${usuario.nombreCompleto} (misma empresa, concepto y cuota).")
            }

            val hoy = Reloj.hoy()
            val prefijo = "CP-${hoy.take(4)}-"
            val ultimo = compromisoDao.getMaxFolio(prefijo)?.removePrefix(prefijo)?.toIntOrNull() ?: 0
            val folio = prefijo + "%04d".format(ultimo + 1)

            val comp = CompromisoEntity(
                idCompromiso = folio,
                folio = folio,
                idUsuarioFinal = usuario.idUsuario,
                idEmpresa = idEmpresa,
                concepto = concepto.trim(),
                numeroCuota = cuota,
                totalCuotas = totalCuotas,
                monto = monto!!.toDouble(),
                fechaVencimiento = fechaVencimiento!!,
                estado = "Pendiente",
                recargoAplicado = 0.0,
                registradoPor = idEjecutivo,
                fechaRegistro = hoy
            )
            compromisoDao.insertCompromiso(comp)
            comp
        }.let { Result.success(it) }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun crearUsuarioFinal(rut: String, p: PersonaNueva?): UsuarioEntity {
        if (p == null || p.nombres.isBlank() || p.apellidos.isBlank() || p.correo.isBlank() || p.telefono.isBlank()) {
            throw IllegalArgumentException("Esta persona no está en PagoFlex: completa nombre, apellidos, correo y teléfono.")
        }
        if (!p.correo.contains("@")) throw IllegalArgumentException("El correo no es válido.")
        val todos = usuarioDao.getAllUsuariosOnce()
        // Correo y RUT son únicos en la base (MER): se avisa con un mensaje claro antes de insertar
        if (todos.any { it.correo.equals(p.correo.trim(), ignoreCase = true) }) {
            throw IllegalArgumentException("Ese correo ya está registrado en PagoFlex.")
        }
        if (todos.any { Rut.mismoRut(it.rut, rut) }) {
            throw IllegalArgumentException("Ese RUT ya está registrado en PagoFlex con otro rol.")
        }
        val siguiente = (todos.mapNotNull { it.idUsuario.removePrefix("UF-").toIntOrNull() }.maxOrNull() ?: 0) + 1
        val nuevo = UsuarioEntity(
            idUsuario = "UF-%02d".format(siguiente),
            idRol = "USUARIO_FINAL",
            rut = Rut.formatear(rut),
            nombres = p.nombres.trim(),
            apellidos = p.apellidos.trim(),
            correo = p.correo.trim(),
            telefono = p.telefono.trim(),
            fechaIncorporacion = Reloj.hoy()
        )
        usuarioDao.insertUsuario(nuevo)
        return nuevo
    }

    /** RF-19 / RN-21: solo compromisos Pendientes, Vencidos o En revisión de la propia empresa. */
    suspend fun anularCompromiso(
        idCompromiso: String,
        idEjecutivo: String,
        idEmpresaEjecutivo: String,
        motivo: String,
        comentario: String
    ): Result<Unit> = try {
        db.withTransaction {
            val comp = compromisoDao.getCompromisoById(idCompromiso)
                ?: throw IllegalStateException("No encontramos el compromiso.")
            ReglasRolesInternos.validarAnulacion(comp, idEmpresaEjecutivo, motivo, comentario)
                ?.let { throw IllegalArgumentException(it) }
            compromisoDao.updateCompromiso(
                comp.copy(
                    estado = "Anulado",
                    motivoAnulacion = motivo,
                    comentarioAnulacion = comentario.trim().ifBlank { null },
                    ejecutivoAnulacion = idEjecutivo,
                    fechaAnulacion = Reloj.hoy()
                )
            )
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

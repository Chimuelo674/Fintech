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

    fun getCatalogoReconocimientos(): Flow<Map<String, String>> =
        catalogoDao.getReconocimientos().map { lista -> lista.associate { it.nombre to it.descripcion } }

    fun getAllUsuarios(): Flow<List<UsuarioEntity>> = usuarioDao.getAllUsuarios()
    suspend fun getUsuarioById(id: String): UsuarioEntity? = usuarioDao.getUsuarioById(id)
    suspend fun updateUsuario(usuario: UsuarioEntity) = usuarioDao.updateUsuario(usuario)

    fun getAllEmpresas(): Flow<List<EmpresaEntity>> = empresaDao.getAllEmpresas()
    suspend fun getEmpresaById(id: String): EmpresaEntity? = empresaDao.getEmpresaById(id)

    fun getCompromisosByUsuario(idUsuario: String): Flow<List<CompromisoEntity>> = compromisoDao.getCompromisosByUsuario(idUsuario)
    fun getCompromisosByEmpresa(idEmpresa: String): Flow<List<CompromisoEntity>> = compromisoDao.getCompromisosByEmpresa(idEmpresa)
    suspend fun getCompromisoById(id: String): CompromisoEntity? = compromisoDao.getCompromisoById(id)

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

    private suspend fun restaurarSegunFecha(c: CompromisoEntity): CompromisoEntity =
        if (Fechas.estaVencida(c.fechaVencimiento)) {
            val recargo = empresaDao.getEmpresaById(c.idEmpresa)?.recargoAtraso ?: 0.0
            c.copy(estado = "Vencido", recargoAplicado = recargo)
        } else {
            c.copy(estado = "Pendiente", recargoAplicado = 0.0)
        }

    fun getCumplimiento(idUsuario: String): Flow<Cumplimiento> =
        combine(
            compromisoDao.getCompromisosByUsuario(idUsuario),
            pagoDao.getPagosByUsuario(idUsuario)
        ) { compromisos, pagos -> calcularCumplimiento(compromisos, pagos) }

    companion object {
        val MOTIVOS_REPORTE = listOf("Ya pagué por otro medio", "No reconozco este cobro", "El monto no corresponde", "Otro")

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
            val porcentaje = Math.round(aTiempo * 1000.0 / considerados.size) / 10.0
            val categoria = when {
                porcentaje >= 95.0 -> "Ejemplar"
                porcentaje >= 80.0 -> "Al día"
                else -> "En camino"
            }
            return Cumplimiento(considerados.size, aTiempo, porcentaje, categoria)
        }
    }

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
        return try {
            db.withTransaction {
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
                    montoPagado = actual.monto + recargo,
                    canal = "App",
                    medioPago = medioPago
                )

                pagoDao.insertPago(pago)
                compromisoDao.updateCompromiso(actual.copy(estado = "Pagado"))
                pago
            }.let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun generarNumeroComprobante(): String {
        val ultimo = pagoDao.getMaxNumeroComprobante()
            ?.removePrefix("CPR-")?.toIntOrNull() ?: 0
        return "CPR-%06d".format(ultimo + 1)
    }

    fun getPagosByUsuario(idUsuario: String): Flow<List<PagoEntity>> = pagoDao.getPagosByUsuario(idUsuario)
    suspend fun getPagoByCompromiso(idCompromiso: String): PagoEntity? = pagoDao.getPagoByCompromiso(idCompromiso)
    suspend fun getPagoByNumero(numeroComprobante: String): PagoEntity? = pagoDao.getPagoByNumero(numeroComprobante)

    fun getReportesByUsuario(idUsuario: String): Flow<List<ReporteEntity>> = reporteDao.getReportesByUsuario(idUsuario)
    fun getAllReportes(): Flow<List<ReporteEntity>> = reporteDao.getAllReportes()

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

    fun getRecomendacionesByUsuario(idUsuario: String): Flow<List<RecomendacionEntity>> = recomendacionDao.getRecomendacionesByUsuario(idUsuario)
    fun getAllRecomendaciones(): Flow<List<RecomendacionEntity>> = recomendacionDao.getAllRecomendaciones()

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
            catalogoDao.insertRubros(listOf(RubroEntity(rec.rubro!!)))
            catalogoDao.insertComunas(listOf(ComunaEntity(rec.comuna!!)))
            recomendacionDao.insertRecomendacion(rec)
            rec
        }.let { Result.success(it) }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getHistorialRecomendaciones(): Flow<List<HistorialRecomendacionEntity>> = historialRecDao.getAll()

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

    fun getReconocimientosByUsuario(idUsuario: String): Flow<List<ReconocimientoEntity>> = reconocimientoDao.getReconocimientosByUsuario(idUsuario)

    fun getAllCompromisos(): Flow<List<CompromisoEntity>> = compromisoDao.getAllCompromisos()
    fun getPagosByEmpresa(idEmpresa: String): Flow<List<PagoEntity>> = pagoDao.getPagosByEmpresa(idEmpresa)

    suspend fun buscarUsuarioFinalPorRut(rut: String): UsuarioEntity? =
        usuarioDao.getAllUsuariosOnce().firstOrNull { it.idRol == "USUARIO_FINAL" && Rut.mismoRut(it.rut, rut) }

    data class PersonaNueva(val nombres: String, val apellidos: String, val correo: String, val telefono: String)

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

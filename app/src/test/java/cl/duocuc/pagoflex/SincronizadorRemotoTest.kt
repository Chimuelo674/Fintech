package cl.duocuc.pagoflex

import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.EmpresaEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.local.entities.ReporteEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.data.remote.*
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** Pruebas de la conexión con los microservicios, usando APIs falsas en memoria. */
class SincronizadorRemotoTest {

    // ---------- Datos de prueba ----------

    private val usuario = UsuarioEntity("UF-01", "USUARIO_FINAL", "15.834.207-3", "Camila Andrea", "Rojas Peña", "crojas@correo.ejemplo")
    private val empresa = EmpresaEntity("EC-01", "Altos de Maipo SpA", "Altos de Maipo", "76.000.000-1", "Inmobiliaria", "a@b.cl", "+56 2", 8500.0)
    private val compromiso = CompromisoEntity(
        idCompromiso = "CP-1", folio = "CP-1", idUsuarioFinal = "UF-01", idEmpresa = "EC-01",
        concepto = "Cuota pie", monto = 310000.0, fechaVencimiento = "2026-10-31", estado = "Pendiente"
    )
    private val pago = PagoEntity("CPR-000001", "CPR-000001", "CP-1", "2026-10-06 10:00", 310000.0)
    private val reporte = ReporteEntity("REP-0001", "CP-1", "Otro", "Hola", "2026-10-06")

    private fun json(texto: String) = texto.toResponseBody("application/json".toMediaType())
    private fun error(codigo: Int, texto: String = "{}") = HttpException(Response.error<Any>(codigo, json(texto)))

    // ---------- Fakes ----------

    private class FakeAlmacen(
        val usuarios: List<UsuarioEntity>,
        val empresas: List<EmpresaEntity>,
        val compromisos: List<CompromisoEntity>,
        val pagos: List<PagoEntity>,
        val reportes: List<ReporteEntity>
    ) : AlmacenLocal {
        var guardado: DatosServidor? = null
        override suspend fun usuarios() = usuarios
        override suspend fun empresas() = empresas
        override suspend fun compromisos() = compromisos
        override suspend fun pagos() = pagos
        override suspend fun reportes() = reportes
        override suspend fun guardarDatosServidor(datos: DatosServidor) { guardado = datos }
    }

    private class FakeUsuariosApi : UsuariosApi {
        val usuarios = mutableListOf<UsuarioDto>()
        val empresas = mutableListOf<EmpresaDto>()
        val llamadas = mutableListOf<String>()
        var falla: Exception? = null
        var respuestaLogin: Response<UsuarioDto>? = null

        private fun revisar(nombre: String) { llamadas += nombre; falla?.let { throw it } }

        override suspend fun salud(): Map<String, Any> { revisar("salud"); return mapOf("estado" to "OK") }
        override suspend fun login(request: LoginRequest): Response<UsuarioDto> { revisar("login"); return respuestaLogin!! }
        override suspend fun listarUsuarios(): List<UsuarioDto> { revisar("listarUsuarios"); return usuarios.toList() }
        override suspend fun crearUsuario(usuario: UsuarioDto): UsuarioDto { revisar("crearUsuario"); usuarios += usuario; return usuario }
        override suspend fun crearUsuariosLote(usuarios: List<UsuarioDto>): LoteRespuesta {
            revisar("crearUsuariosLote"); this.usuarios += usuarios; return LoteRespuesta(usuarios.size)
        }
        override suspend fun actualizarUsuario(id: String, usuario: UsuarioDto): UsuarioDto { revisar("actualizarUsuario:$id"); return usuario }
        override suspend fun listarEmpresas(): List<EmpresaDto> { revisar("listarEmpresas"); return empresas.toList() }
        override suspend fun crearEmpresasLote(empresas: List<EmpresaDto>): LoteRespuesta {
            revisar("crearEmpresasLote"); this.empresas += empresas; return LoteRespuesta(empresas.size)
        }
    }

    private class FakePagosApi : PagosApi {
        val compromisos = mutableListOf<CompromisoDto>()
        val pagos = mutableListOf<PagoDto>()
        val reportes = mutableListOf<ReporteDto>()
        val llamadas = mutableListOf<String>()
        var falla: Exception? = null
        var fallaAlCrearCompromiso: Exception? = null
        var codigoEliminar = 204

        private fun revisar(nombre: String) { llamadas += nombre; falla?.let { throw it } }

        override suspend fun salud(): Map<String, Any> { revisar("salud"); return mapOf("estado" to "OK") }
        override suspend fun listarCompromisos(): List<CompromisoDto> { revisar("listarCompromisos"); return compromisos.toList() }
        override suspend fun crearCompromiso(compromiso: CompromisoDto): CompromisoDto {
            revisar("crearCompromiso"); fallaAlCrearCompromiso?.let { throw it }; compromisos += compromiso; return compromiso
        }
        override suspend fun crearCompromisosLote(compromisos: List<CompromisoDto>): LoteRespuesta {
            revisar("crearCompromisosLote"); this.compromisos += compromisos; return LoteRespuesta(compromisos.size)
        }
        override suspend fun actualizarCompromiso(id: String, compromiso: CompromisoDto): CompromisoDto {
            revisar("actualizarCompromiso:$id"); return compromiso
        }
        override suspend fun listarPagos(): List<PagoDto> { revisar("listarPagos"); return pagos.toList() }
        override suspend fun listarPagosDeUsuario(idUsuario: String): List<PagoDto> { revisar("listarPagosDeUsuario"); return pagos.toList() }
        override suspend fun crearPago(pago: PagoDto): PagoDto { revisar("crearPago"); pagos += pago; return pago }
        override suspend fun crearPagosLote(pagos: List<PagoDto>): LoteRespuesta {
            revisar("crearPagosLote"); this.pagos += pagos; return LoteRespuesta(pagos.size)
        }
        override suspend fun listarReportes(): List<ReporteDto> { revisar("listarReportes"); return reportes.toList() }
        override suspend fun crearReporte(reporte: ReporteDto): ReporteDto { revisar("crearReporte"); reportes += reporte; return reporte }
        override suspend fun crearReportesLote(reportes: List<ReporteDto>): LoteRespuesta {
            revisar("crearReportesLote"); this.reportes += reportes; return LoteRespuesta(reportes.size)
        }
        override suspend fun actualizarReporte(id: String, reporte: ReporteDto): ReporteDto { revisar("actualizarReporte:$id"); return reporte }
        override suspend fun eliminarReporte(id: String): Response<Unit> {
            revisar("eliminarReporte:$id")
            return if (codigoEliminar in 200..299) Response.success(Unit)
            else Response.error(codigoEliminar, "{}".toResponseBody("application/json".toMediaType()))
        }
    }

    private val uApi = FakeUsuariosApi()
    private val pApi = FakePagosApi()
    private val almacen = FakeAlmacen(listOf(usuario), listOf(empresa), listOf(compromiso), listOf(pago), listOf(reporte))
    private val sync = SincronizadorRemoto({ uApi }, { pApi }, almacen)

    // ---------- sincronizar() ----------

    @Test
    fun servidorVacioRecibeLosDatosDePruebaYLuegoSeDescargan() = runBlocking {
        val estado = sync.sincronizar()

        assertTrue(estado is EstadoConexion.Conectado)
        assertTrue((estado as EstadoConexion.Conectado).detalle.contains("5 registros iniciales subidos"))
        assertTrue("crearEmpresasLote" in uApi.llamadas)
        assertTrue("crearUsuariosLote" in uApi.llamadas)
        assertTrue("crearCompromisosLote" in pApi.llamadas)
        val guardado = almacen.guardado!!
        assertEquals(listOf(usuario), guardado.usuarios)
        assertEquals(listOf(compromiso), guardado.compromisos)
        assertEquals(listOf(pago), guardado.pagos)
        assertEquals(listOf(reporte), guardado.reportes)
        assertEquals(estado, sync.estado.value)
    }

    @Test
    fun servidorConDatosNoRecibeLaCargaInicialYReemplazaLoLocal() = runBlocking {
        val pagadoEnOtroTelefono = compromiso.copy(estado = "Pagado")
        uApi.usuarios += usuario.aDto()
        uApi.empresas += empresa.aDto()
        pApi.compromisos += pagadoEnOtroTelefono.aDto()

        sync.sincronizar()

        assertFalse("crearUsuariosLote" in uApi.llamadas)
        assertFalse("crearCompromisosLote" in pApi.llamadas)
        assertEquals("Pagado", almacen.guardado!!.compromisos.single().estado)
        assertTrue(almacen.guardado!!.pagos.isEmpty())
    }

    @Test
    fun sinServidorQuedaSinConexionYNoTocaLosDatosLocales() = runBlocking {
        uApi.falla = IOException("Connection refused")

        val estado = sync.sincronizar()

        assertTrue(estado is EstadoConexion.SinConexion)
        assertTrue((estado as EstadoConexion.SinConexion).motivo.contains("No se pudo conectar"))
        assertNull(almacen.guardado)
    }

    @Test
    fun errorHttpDelServidorSeInformaConSuCodigo() = runBlocking {
        pApi.falla = error(500)
        val estado = sync.sincronizar() as EstadoConexion.SinConexion
        assertEquals("El servidor respondió con el código 500.", estado.motivo)
    }

    // ---------- publicar() ----------

    @Test
    fun publicarUnPagoLoEnviaAPagosService() = runBlocking {
        assertTrue(sync.publicar(Cambio.PagoNuevo(pago)))
        assertEquals(listOf(pago.aDto()), pApi.pagos)
        assertTrue(sync.estado.value is EstadoConexion.Conectado)
    }

    @Test
    fun publicarVariosCambiosLosEnviaEnOrden() = runBlocking {
        val enRevision = compromiso.copy(estado = "En revisión")
        sync.publicar(Cambio.ReporteNuevo(reporte), Cambio.CompromisoActualizado(enRevision))
        assertEquals(listOf("crearReporte", "actualizarCompromiso:CP-1"), pApi.llamadas)
    }

    @Test
    fun siElCompromisoYaExisteEnElServidorSeActualiza() = runBlocking {
        pApi.fallaAlCrearCompromiso = error(409)
        assertTrue(sync.publicar(Cambio.CompromisoNuevo(compromiso)))
        assertEquals(listOf("crearCompromiso", "actualizarCompromiso:CP-1"), pApi.llamadas)
    }

    @Test
    fun unErrorDistintoDe409AlCrearNoSeReintentaComoActualizacion() = runBlocking {
        pApi.fallaAlCrearCompromiso = error(400)
        assertFalse(sync.publicar(Cambio.CompromisoNuevo(compromiso)))
        assertEquals(listOf("crearCompromiso"), pApi.llamadas)
    }

    @Test
    fun usuariosNuevosYActualizadosVanAUsuariosService() = runBlocking {
        sync.publicar(Cambio.UsuarioNuevo(usuario), Cambio.UsuarioActualizado(usuario.copy(diasAnticipacion = 5)))
        assertEquals(listOf("crearUsuario", "actualizarUsuario:UF-01"), uApi.llamadas)
    }

    @Test
    fun eliminarUnReporteLlamaAlEndpointDelete() = runBlocking {
        assertTrue(sync.publicar(Cambio.ReporteEliminado("REP-0001")))
        assertEquals(listOf("eliminarReporte:REP-0001"), pApi.llamadas)
    }

    @Test
    fun eliminarUnReporteQueYaNoExisteNoEsError() = runBlocking {
        pApi.codigoEliminar = 404
        assertTrue(sync.publicar(Cambio.ReporteEliminado("REP-9999")))
    }

    @Test
    fun eliminarConErrorDelServidorDevuelveFalse() = runBlocking {
        pApi.codigoEliminar = 500
        assertFalse(sync.publicar(Cambio.ReporteEliminado("REP-0001")))
    }

    @Test
    fun siNoSePuedePublicarQuedaSinConexion() = runBlocking {
        pApi.falla = IOException("timeout")
        assertFalse(sync.publicar(Cambio.ReporteActualizado(reporte.copy(estado = "En gestión"))))
        assertTrue(sync.estado.value is EstadoConexion.SinConexion)
    }

    @Test
    fun sinConexionLosCambiosQuedanEnColaSinLlamarAlServidor() = runBlocking {
        pApi.falla = IOException("down")
        sync.publicar(Cambio.PagoNuevo(pago))            // falla: queda en la cola y el estado pasa a sin conexión
        val llamadasAntes = pApi.llamadas.size

        assertFalse(sync.publicar(Cambio.ReporteNuevo(reporte)))  // ya sin conexión: ni siquiera intenta
        assertEquals(llamadasAntes, pApi.llamadas.size)
        assertEquals(2, sync.cambiosPendientes)
    }

    @Test
    fun alVolverLaConexionSeEnvianLosPendientesAntesDeDescargar() = runBlocking {
        pApi.falla = IOException("down")
        sync.publicar(Cambio.PagoNuevo(pago))
        pApi.falla = null
        uApi.usuarios += usuario.aDto()
        uApi.empresas += empresa.aDto()
        pApi.compromisos += compromiso.aDto()

        sync.sincronizar()

        assertEquals(0, sync.cambiosPendientes)
        assertTrue(pApi.llamadas.indexOf("crearPago") < pApi.llamadas.indexOf("listarPagos"))
        assertEquals(listOf(pago), almacen.guardado!!.pagos)
    }

    @Test
    fun unCambioRechazadoPorElServidorNoBloqueaLosSiguientes() = runBlocking {
        pApi.fallaAlCrearCompromiso = error(400)
        assertFalse(sync.publicar(Cambio.CompromisoNuevo(compromiso), Cambio.PagoNuevo(pago)))
        assertEquals(listOf(pago.aDto()), pApi.pagos)
        assertEquals(0, sync.cambiosPendientes)
    }

    // ---------- login() ----------

    @Test
    fun loginCorrectoDevuelveElUsuarioDelServidor() = runBlocking {
        uApi.respuestaLogin = Response.success(usuario.aDto().copy(avisosActivos = null, diasAnticipacion = null))
        val resultado = sync.login("15.834.207-3", "1234")
        val u = (resultado as ResultadoLogin.Exitoso).usuario
        assertEquals("UF-01", u.idUsuario)
        assertTrue(u.avisosActivos)
        assertEquals(3, u.diasAnticipacion)
    }

    @Test
    fun loginRechazadoMuestraElMensajeDelServidor() = runBlocking {
        uApi.respuestaLogin = Response.error(401, json("{\"mensaje\":\"RUT o clave incorrectos.\"}"))
        val resultado = sync.login("15.834.207-3", "9999")
        assertEquals(ResultadoLogin.Rechazado("RUT o clave incorrectos."), resultado)
    }

    @Test
    fun loginRechazadoSinMensajeUsaUnoPorDefecto() = runBlocking {
        uApi.respuestaLogin = Response.error(403, json("{}"))
        assertEquals(ResultadoLogin.Rechazado("RUT o clave incorrectos."), sync.login("1-9", "1234"))
    }

    @Test
    fun loginConErrorDelServidorNoEsRechazo() = runBlocking {
        uApi.respuestaLogin = Response.error(500, json("{}"))
        assertTrue(sync.login("1-9", "1234") is ResultadoLogin.SinConexion)
    }

    @Test
    fun loginSinServidorPermiteUsarElModoSinConexion() = runBlocking {
        uApi.falla = IOException("Connection refused")
        assertTrue(sync.login("15.834.207-3", "1234") is ResultadoLogin.SinConexion)
        assertTrue(sync.estado.value is EstadoConexion.SinConexion)
    }

    // ---------- probarConexion() y utilidades ----------

    @Test
    fun probarConexionRevisaLosDosMicroservicios() = runBlocking {
        assertTrue(sync.probarConexion() is EstadoConexion.Conectado)
        pApi.falla = IOException("down")
        assertTrue(sync.probarConexion() is EstadoConexion.SinConexion)
    }

    @Test
    fun leeElMensajeDeErrorDeLosMicroservicios() {
        assertEquals("Tu cuenta está desactivada.", SincronizadorRemoto.leerMensaje("{\"mensaje\": \"Tu cuenta está desactivada.\"}"))
        assertEquals("Dice \\\"hola\\\"", SincronizadorRemoto.leerMensaje("{\"mensaje\":\"Dice \\\"hola\\\"\"}"))
        assertNull(SincronizadorRemoto.leerMensaje("{}"))
        assertNull(SincronizadorRemoto.leerMensaje(null))
        assertNull(SincronizadorRemoto.leerMensaje("{\"mensaje\":\"\"}"))
    }

    @Test
    fun describeErroresDesconocidos() {
        assertEquals("algo raro", SincronizadorRemoto.describirError(IllegalStateException("algo raro")))
    }

    // ---------- Conversión DTO <-> entidad ----------

    @Test
    fun conversionIdaYVueltaNoPierdeDatos() {
        assertEquals(usuario, usuario.aDto().aEntidad())
        assertEquals(empresa, empresa.aDto().aEntidad())
        assertEquals(compromiso, compromiso.aDto().aEntidad())
        assertEquals(pago, pago.aDto().aEntidad())
        assertEquals(reporte, reporte.aDto().aEntidad())
    }

    @Test
    fun camposQueFaltanEnElJsonTomanValoresPorDefecto() {
        val e = EmpresaDto("EC-9", "R", "N", "1-9", "Otro", null, null, null, null).aEntidad()
        assertEquals("", e.correoAtencion)
        assertEquals(0.0, e.recargoAtraso, 0.0)
        assertTrue(e.activa)

        val c = compromiso.aDto().copy(recargoAplicado = null, registradoPor = null, fechaRegistro = null).aEntidad()
        assertEquals(0.0, c.recargoAplicado, 0.0)
        assertEquals("PagoFlex", c.registradoPor)
        assertEquals("2026-10-31", c.fechaRegistro)

        val p = pago.aDto().copy(canal = null, medioPago = null).aEntidad()
        assertEquals("App", p.canal)
        assertEquals("Pago simulado", p.medioPago)
    }
}

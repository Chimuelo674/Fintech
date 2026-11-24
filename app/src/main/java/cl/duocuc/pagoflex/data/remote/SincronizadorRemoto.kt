package cl.duocuc.pagoflex.data.remote

import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.EmpresaEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.local.entities.ReporteEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException

/** Lo que la sincronización necesita de la base de datos del teléfono (Room). */
interface AlmacenLocal {
    suspend fun usuarios(): List<UsuarioEntity>
    suspend fun empresas(): List<EmpresaEntity>
    suspend fun compromisos(): List<CompromisoEntity>
    suspend fun pagos(): List<PagoEntity>
    suspend fun reportes(): List<ReporteEntity>

    /** Reemplaza la copia local con lo que hay en el servidor. */
    suspend fun guardarDatosServidor(datos: DatosServidor)
}

data class DatosServidor(
    val usuarios: List<UsuarioEntity>,
    val empresas: List<EmpresaEntity>,
    val compromisos: List<CompromisoEntity>,
    val pagos: List<PagoEntity>,
    val reportes: List<ReporteEntity>
)

/** Un cambio hecho en la app que hay que enviar al microservicio que corresponde. */
sealed interface Cambio {
    data class UsuarioNuevo(val usuario: UsuarioEntity) : Cambio
    data class UsuarioActualizado(val usuario: UsuarioEntity) : Cambio
    data class CompromisoNuevo(val compromiso: CompromisoEntity) : Cambio
    data class CompromisoActualizado(val compromiso: CompromisoEntity) : Cambio
    data class PagoNuevo(val pago: PagoEntity) : Cambio
    data class ReporteNuevo(val reporte: ReporteEntity) : Cambio
    data class ReporteActualizado(val reporte: ReporteEntity) : Cambio
    data class ReporteEliminado(val idReporte: String) : Cambio
}

sealed interface EstadoConexion {
    data object Desconocido : EstadoConexion
    data object Sincronizando : EstadoConexion
    data class Conectado(val detalle: String) : EstadoConexion
    data class SinConexion(val motivo: String) : EstadoConexion
}

sealed interface ResultadoLogin {
    data class Exitoso(val usuario: UsuarioEntity) : ResultadoLogin
    data class Rechazado(val mensaje: String) : ResultadoLogin
    data class SinConexion(val mensaje: String) : ResultadoLogin
}

/**
 * Conecta la app con los microservicios:
 * - [sincronizar]: si el servidor está vacío le sube los datos de prueba, y luego descarga todo a Room.
 * - [publicar]: envía al servidor cada cambio hecho en la app (crear, actualizar, eliminar).
 *   Si no hay conexión, el cambio queda en una cola y se envía en la próxima sincronización.
 * - [login]: valida RUT y clave en usuarios-service.
 *
 * Las APIs se reciben como funciones para poder cambiar la IP del servidor sin reiniciar la app,
 * y para reemplazarlas por versiones falsas en las pruebas unitarias.
 */
class SincronizadorRemoto(
    private val usuariosApi: () -> UsuariosApi,
    private val pagosApi: () -> PagosApi,
    private val local: AlmacenLocal
) {
    private val _estado = MutableStateFlow<EstadoConexion>(EstadoConexion.Desconocido)
    val estado: StateFlow<EstadoConexion> = _estado.asStateFlow()

    private val mutex = Mutex()
    private val envio = Mutex()

    /** Cambios hechos en la app que todavía no llegan al servidor. */
    private val pendientes = ArrayDeque<Cambio>()

    val cambiosPendientes: Int
        get() = synchronized(pendientes) { pendientes.size }

    suspend fun sincronizar(): EstadoConexion = mutex.withLock {
        _estado.value = EstadoConexion.Sincronizando
        val resultado = try {
            val uApi = usuariosApi()
            val pApi = pagosApi()

            // 0) Primero se envían los cambios hechos sin conexión, para no pisarlos al descargar.
            vaciarCola()

            // 1) Primera vez: el servidor está vacío, así que le subimos los datos de prueba de la app.
            var subidos = 0
            if (uApi.listarEmpresas().isEmpty()) {
                subidos += uApi.crearEmpresasLote(local.empresas().map { it.aDto() }).insertados ?: 0
            }
            if (uApi.listarUsuarios().isEmpty()) {
                subidos += uApi.crearUsuariosLote(local.usuarios().map { it.aDto() }).insertados ?: 0
            }
            if (pApi.listarCompromisos().isEmpty()) {
                subidos += pApi.crearCompromisosLote(local.compromisos().map { it.aDto() }).insertados ?: 0
                subidos += pApi.crearPagosLote(local.pagos().map { it.aDto() }).insertados ?: 0
                subidos += pApi.crearReportesLote(local.reportes().map { it.aDto() }).insertados ?: 0
            }

            // 2) Descargamos lo que hay en el servidor (incluye cambios hechos desde otros teléfonos).
            val datos = DatosServidor(
                usuarios = uApi.listarUsuarios().map { it.aEntidad() },
                empresas = uApi.listarEmpresas().map { it.aEntidad() },
                compromisos = pApi.listarCompromisos().map { it.aEntidad() },
                pagos = pApi.listarPagos().map { it.aEntidad() },
                reportes = pApi.listarReportes().map { it.aEntidad() }
            )
            local.guardarDatosServidor(datos)

            val detalle = buildString {
                append("${datos.compromisos.size} compromisos y ${datos.pagos.size} pagos sincronizados")
                if (subidos > 0) append(" ($subidos registros iniciales subidos)")
            }
            EstadoConexion.Conectado(detalle)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            EstadoConexion.SinConexion(describirError(e))
        }
        _estado.value = resultado
        resultado
    }

    /**
     * Envía los cambios en orden. Devuelve false si alguno no se pudo enviar.
     * El cambio ya quedó guardado en el teléfono: si faltó la conexión, queda en la cola
     * para la próxima sincronización; si el servidor lo rechazó, se descarta.
     */
    suspend fun publicar(vararg cambios: Cambio): Boolean {
        synchronized(pendientes) { pendientes.addAll(cambios) }
        // Sin conexión no esperamos a que la red falle: el cambio se envía al sincronizar.
        if (_estado.value is EstadoConexion.SinConexion) return false
        return try {
            val rechazados = vaciarCola()
            if (rechazados > 0) {
                _estado.value = EstadoConexion.Conectado("El servidor rechazó $rechazados cambio(s).")
                false
            } else {
                if (_estado.value !is EstadoConexion.Conectado) {
                    _estado.value = EstadoConexion.Conectado("Cambios enviados al servidor")
                }
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _estado.value = EstadoConexion.SinConexion(describirError(e))
            false
        }
    }

    /**
     * Envía la cola de cambios en orden y devuelve cuántos rechazó el servidor.
     * Si falla la conexión lanza la excepción y el cambio sigue en la cola.
     */
    private suspend fun vaciarCola(): Int = envio.withLock {
        var rechazados = 0
        while (true) {
            val siguiente = synchronized(pendientes) { pendientes.firstOrNull() } ?: break
            try {
                enviar(siguiente)
            } catch (e: HttpException) {
                rechazados++ // el servidor respondió con un error: reintentarlo no lo arreglaría
            }
            synchronized(pendientes) { pendientes.removeFirstOrNull() }
        }
        rechazados
    }

    private suspend fun enviar(cambio: Cambio) {
        val uApi = usuariosApi()
        val pApi = pagosApi()
        when (cambio) {
            is Cambio.UsuarioNuevo -> crearOActualizar(
                crear = { uApi.crearUsuario(cambio.usuario.aDto()) },
                actualizar = { uApi.actualizarUsuario(cambio.usuario.idUsuario, cambio.usuario.aDto()) }
            )
            is Cambio.UsuarioActualizado ->
                uApi.actualizarUsuario(cambio.usuario.idUsuario, cambio.usuario.aDto())
            is Cambio.CompromisoNuevo -> crearOActualizar(
                crear = { pApi.crearCompromiso(cambio.compromiso.aDto()) },
                actualizar = { pApi.actualizarCompromiso(cambio.compromiso.idCompromiso, cambio.compromiso.aDto()) }
            )
            is Cambio.CompromisoActualizado ->
                pApi.actualizarCompromiso(cambio.compromiso.idCompromiso, cambio.compromiso.aDto())
            is Cambio.PagoNuevo -> pApi.crearPago(cambio.pago.aDto())
            is Cambio.ReporteNuevo -> crearOActualizar(
                crear = { pApi.crearReporte(cambio.reporte.aDto()) },
                actualizar = { pApi.actualizarReporte(cambio.reporte.idReporte, cambio.reporte.aDto()) }
            )
            is Cambio.ReporteActualizado ->
                pApi.actualizarReporte(cambio.reporte.idReporte, cambio.reporte.aDto())
            is Cambio.ReporteEliminado -> {
                val respuesta = pApi.eliminarReporte(cambio.idReporte)
                // 404: ya no existía en el servidor, que es lo que queríamos.
                if (!respuesta.isSuccessful && respuesta.code() != 404) throw HttpException(respuesta)
            }
        }
    }

    /** Si el registro ya existe en el servidor (409), lo actualiza en vez de crearlo. */
    private suspend fun crearOActualizar(crear: suspend () -> Any, actualizar: suspend () -> Any) {
        try {
            crear()
        } catch (e: HttpException) {
            if (e.code() == 409) actualizar() else throw e
        }
    }

    suspend fun login(rut: String, clave: String): ResultadoLogin = try {
        val respuesta = usuariosApi().login(LoginRequest(rut, clave))
        val usuario = respuesta.body()
        when {
            respuesta.isSuccessful && usuario != null -> {
                if (_estado.value !is EstadoConexion.Conectado) {
                    _estado.value = EstadoConexion.Conectado("Sesión validada en el servidor")
                }
                ResultadoLogin.Exitoso(usuario.aEntidad())
            }
            respuesta.code() == 401 || respuesta.code() == 403 ->
                ResultadoLogin.Rechazado(
                    leerMensaje(respuesta.errorBody()?.string()) ?: "RUT o clave incorrectos."
                )
            else -> ResultadoLogin.SinConexion("El servidor respondió con el código ${respuesta.code()}.")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val motivo = describirError(e)
        _estado.value = EstadoConexion.SinConexion(motivo)
        ResultadoLogin.SinConexion(motivo)
    }

    /** Comprueba que los dos microservicios respondan. */
    suspend fun probarConexion(): EstadoConexion = try {
        usuariosApi().salud()
        pagosApi().salud()
        EstadoConexion.Conectado("usuarios-service y pagos-service responden")
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        EstadoConexion.SinConexion(describirError(e))
    }

    companion object {
        private val REGEX_MENSAJE = Regex("\"mensaje\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")

        /** Saca el texto de {"mensaje": "..."}, el formato de error de los microservicios. */
        fun leerMensaje(json: String?): String? =
            json?.let { REGEX_MENSAJE.find(it)?.groupValues?.get(1) }?.takeIf { it.isNotBlank() }

        fun describirError(e: Exception): String = when (e) {
            is HttpException -> "El servidor respondió con el código ${e.code()}."
            is IOException -> "No se pudo conectar con el servidor. Revisa que los microservicios estén encendidos y la IP configurada."
            else -> e.message ?: "Error desconocido al conectar con el servidor."
        }
    }
}

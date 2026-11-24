package cl.duocuc.pagoflex.ui.session

import androidx.lifecycle.viewModelScope
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.data.remote.EstadoConexion
import cl.duocuc.pagoflex.data.remote.ResultadoLogin
import cl.duocuc.pagoflex.domain.ReglasAcceso
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.common.BaseViewModel
import cl.duocuc.pagoflex.util.Rut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class LoginViewModel(
    private val repo: PagoFlexRepository,
    rutRecordado: String?
) : BaseViewModel() {

    data class Estado(
        val rut: String = "",
        val clave: String = "",
        val error: String? = null,
        val cargando: Boolean = false,
        val segundosBloqueo: Int = 0,
        /** Usuario cuyo RUT quedó recordado en este teléfono ("Hola de nuevo, ..."). */
        val recordado: UsuarioEntity? = null
    )

    private val control = ReglasAcceso.ControlIntentos()
    private val _estado = MutableStateFlow(Estado(rut = rutRecordado.orEmpty()))
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    /** Cuentas de la base de prueba, para que el profesor o el equipo puedan probar cada rol. */
    val usuariosPrueba: StateFlow<List<UsuarioEntity>> =
        repo.getAllUsuarios().map { lista -> lista.filter { it.activo } }.comoEstado(emptyList())

    init {
        if (rutRecordado != null) {
            viewModelScope.launch {
                val u = buscar(rutRecordado)
                _estado.update { it.copy(recordado = u?.takeIf { usuario -> usuario.activo }) }
            }
        }
    }

    private suspend fun buscar(rut: String): UsuarioEntity? {
        // En la primera apertura la base se llena en segundo plano: esperamos a que haya datos.
        val todos = withTimeoutOrNull(5_000) { repo.getAllUsuarios().first { it.isNotEmpty() } } ?: emptyList()
        return todos.firstOrNull { Rut.mismoRut(it.rut, rut) }
    }

    fun cambiarRut(texto: String) {
        _estado.update { it.copy(rut = ReglasAcceso.formatearMientrasEscribe(texto), error = null) }
    }

    fun cambiarClave(texto: String) {
        _estado.update {
            it.copy(clave = texto.filter(Char::isDigit).take(ReglasAcceso.LARGO_CLAVE), error = null)
        }
    }

    fun usarOtroRut() {
        _estado.update { it.copy(recordado = null, rut = "", clave = "", error = null) }
    }

    fun actualizarBloqueo(ahoraMs: Long) {
        _estado.update { it.copy(segundosBloqueo = control.segundosRestantes(ahoraMs)) }
    }

    fun ingresar(ahoraMs: Long, onListo: (UsuarioEntity) -> Unit) {
        val e = _estado.value
        if (e.cargando) return
        if (control.estaBloqueado(ahoraMs)) {
            actualizarBloqueo(ahoraMs)
            return
        }
        val rut = e.recordado?.rut ?: e.rut
        ReglasAcceso.validarFormulario(rut, e.clave)?.let { mensaje ->
            _estado.update { it.copy(error = mensaje) }
            return
        }

        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            // 1) Se valida en el microservicio usuarios-service (POST /api/auth/login).
            when (val remoto = repo.loginRemoto(rut, e.clave)) {
                is ResultadoLogin.Exitoso -> entrar(remoto.usuario, onListo)
                is ResultadoLogin.Rechazado -> {
                    if (remoto.mensaje.contains("desactivada")) {
                        _estado.update { it.copy(cargando = false, clave = "", error = remoto.mensaje) }
                    } else {
                        registrarFallo(ahoraMs)
                    }
                }
                // 2) Sin servidor (o sin conexión): se valida con la copia del teléfono.
                is ResultadoLogin.SinConexion, null -> ingresarSinConexion(rut, e.clave, ahoraMs, onListo)
            }
        }
    }

    private suspend fun ingresarSinConexion(rut: String, clave: String, ahoraMs: Long, onListo: (UsuarioEntity) -> Unit) {
        val usuario = buscar(rut)
        when {
            // Mismo mensaje si el RUT no existe o la clave está mal, como hacen los bancos.
            usuario == null || !ReglasAcceso.claveCorrecta(clave) -> registrarFallo(ahoraMs)
            !usuario.activo -> _estado.update {
                it.copy(cargando = false, clave = "", error = "Tu cuenta está desactivada. Contacta a soporte.")
            }
            else -> entrar(usuario, onListo)
        }
    }

    private fun registrarFallo(ahoraMs: Long) {
        val quedan = control.registrarFallo(ahoraMs)
        val mensaje = if (quedan == 0) {
            "Superaste los intentos permitidos. Espera ${ReglasAcceso.SEGUNDOS_BLOQUEO} segundos para volver a intentar."
        } else {
            "RUT o clave incorrectos. Te ${if (quedan == 1) "queda 1 intento" else "quedan $quedan intentos"}."
        }
        _estado.update {
            it.copy(
                cargando = false,
                clave = "",
                error = mensaje,
                segundosBloqueo = control.segundosRestantes(ahoraMs)
            )
        }
    }

    // ---------- Configuración del servidor (IP de los microservicios) ----------

    val estadoConexion: StateFlow<EstadoConexion> = repo.estadoConexion

    private val _pruebaServidor = MutableStateFlow<EstadoConexion?>(null)
    val pruebaServidor: StateFlow<EstadoConexion?> = _pruebaServidor.asStateFlow()

    /** Prueba la conexión con la IP indicada y, si responde, descarga los datos. */
    fun probarServidor() {
        _pruebaServidor.value = EstadoConexion.Sincronizando
        viewModelScope.launch {
            val prueba = repo.probarConexion() ?: EstadoConexion.SinConexion("La app no tiene servidor configurado.")
            _pruebaServidor.value = prueba
            if (prueba is EstadoConexion.Conectado) repo.sincronizarConServidor()
        }
    }

    fun limpiarPruebaServidor() {
        _pruebaServidor.value = null
    }

    fun ingresarConHuella(onListo: (UsuarioEntity) -> Unit) {
        val usuario = _estado.value.recordado ?: return
        if (_estado.value.cargando) return
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch { entrar(usuario, onListo) }
    }

    fun mostrarError(mensaje: String) {
        _estado.update { it.copy(error = mensaje) }
    }

    private suspend fun entrar(usuario: UsuarioEntity, onListo: (UsuarioEntity) -> Unit) {
        control.reiniciar()
        repo.sincronizarEstados()
        UserSession.setSession(usuario)
        _estado.update { it.copy(cargando = false, clave = "", segundosBloqueo = 0) }
        onListo(usuario)
    }
}

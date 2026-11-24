package cl.duocuc.pagoflex

import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.data.remote.EstadoConexion
import cl.duocuc.pagoflex.data.remote.ResultadoLogin
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.session.LoginViewModel
import cl.duocuc.pagoflex.ui.session.UserSession
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pruebas del LoginViewModel con MockK: el repositorio es un objeto simulado. */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val camila = UsuarioEntity("UF-01", "USUARIO_FINAL", "15.834.207-3", "Camila Andrea", "Rojas Peña", "crojas@correo.ejemplo")
    private val repo = mockk<PagoFlexRepository>()
    private var ingresado: UsuarioEntity? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        UserSession.clearSession()
        every { repo.getAllUsuarios() } returns flowOf(listOf(camila))
        every { repo.estadoConexion } returns MutableStateFlow(EstadoConexion.Desconocido)
        coEvery { repo.sincronizarEstados() } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelConDatos(rut: String, clave: String): LoginViewModel =
        LoginViewModel(repo, rutRecordado = null).apply {
            cambiarRut(rut)
            cambiarClave(clave)
        }

    @Test
    fun loginAprobadoPorElServidorIniciaSesion() = runTest {
        coEvery { repo.loginRemoto(any(), any()) } returns ResultadoLogin.Exitoso(camila)
        val vm = viewModelConDatos("158342073", "1234")

        vm.ingresar(0L) { ingresado = it }

        assertEquals(camila, ingresado)
        assertEquals(camila, UserSession.currentUser.value)
        assertEquals("", vm.estado.value.clave)
        coVerify { repo.loginRemoto("15.834.207-3", "1234") }
    }

    @Test
    fun loginRechazadoPorElServidorCuentaUnIntento() = runTest {
        coEvery { repo.loginRemoto(any(), any()) } returns ResultadoLogin.Rechazado("RUT o clave incorrectos.")
        val vm = viewModelConDatos("15.834.207-3", "9999")

        vm.ingresar(0L) { ingresado = it }

        assertNull(ingresado)
        assertEquals("RUT o clave incorrectos. Te quedan 2 intentos.", vm.estado.value.error)
        assertEquals("", vm.estado.value.clave)
    }

    @Test
    fun cuentaDesactivadaMuestraElMensajeDelServidor() = runTest {
        coEvery { repo.loginRemoto(any(), any()) } returns
            ResultadoLogin.Rechazado("Tu cuenta está desactivada. Contacta a soporte.")
        val vm = viewModelConDatos("15.834.207-3", "1234")

        vm.ingresar(0L) { ingresado = it }

        assertEquals("Tu cuenta está desactivada. Contacta a soporte.", vm.estado.value.error)
    }

    @Test
    fun sinServidorValidaConLosDatosDelTelefono() = runTest {
        coEvery { repo.loginRemoto(any(), any()) } returns ResultadoLogin.SinConexion("No se pudo conectar")
        val vm = viewModelConDatos("15.834.207-3", "1234")

        vm.ingresar(0L) { ingresado = it }

        assertEquals(camila, ingresado)
    }

    @Test
    fun sinServidorYClaveIncorrectaNoEntra() = runTest {
        coEvery { repo.loginRemoto(any(), any()) } returns null
        val vm = viewModelConDatos("15.834.207-3", "0000")

        vm.ingresar(0L) { ingresado = it }

        assertNull(ingresado)
        assertTrue(vm.estado.value.error!!.startsWith("RUT o clave incorrectos"))
    }

    @Test
    fun formularioInvalidoNoLlamaAlServidor() = runTest {
        val vm = viewModelConDatos("15.834.207-4", "1234")

        vm.ingresar(0L) { ingresado = it }

        assertEquals("El RUT ingresado no es válido.", vm.estado.value.error)
        coVerify(exactly = 0) { repo.loginRemoto(any(), any()) }
    }

    @Test
    fun tresIntentosFallidosBloqueanElIngreso() = runTest {
        coEvery { repo.loginRemoto(any(), any()) } returns ResultadoLogin.Rechazado("RUT o clave incorrectos.")
        val vm = viewModelConDatos("15.834.207-3", "9999")

        repeat(3) {
            vm.cambiarClave("9999")
            vm.ingresar(1_000L) { ingresado = it }
        }

        assertEquals(30, vm.estado.value.segundosBloqueo)
        // Bloqueado: aunque la clave sea correcta, no consulta al servidor.
        vm.cambiarClave("1234")
        vm.ingresar(2_000L) { ingresado = it }
        coVerify(exactly = 3) { repo.loginRemoto(any(), any()) }
        assertNull(ingresado)
    }
}

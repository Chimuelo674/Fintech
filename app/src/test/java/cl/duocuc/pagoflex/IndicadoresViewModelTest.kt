package cl.duocuc.pagoflex

import cl.duocuc.pagoflex.data.remote.Indicador
import cl.duocuc.pagoflex.data.remote.IndicadoresRepository
import cl.duocuc.pagoflex.ui.usuario.IndicadoresViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Pruebas del ViewModel de la API externa (mindicador.cl) con MockK. */
@OptIn(ExperimentalCoroutinesApi::class)
class IndicadoresViewModelTest {

    private val repo = mockk<IndicadoresRepository>()
    private val uf = Indicador("uf", "UF", 39500.12, "2026-11-24")

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun alCrearseCargaLosIndicadores() = runTest {
        coEvery { repo.obtener(false) } returns Result.success(listOf(uf))

        val vm = IndicadoresViewModel(repo)

        assertFalse(vm.estado.value.cargando)
        assertEquals(listOf(uf), vm.estado.value.indicadores)
        assertNull(vm.estado.value.error)
    }

    @Test
    fun siLaApiFallaMuestraElError() = runTest {
        coEvery { repo.obtener(false) } returns Result.failure(Exception("Sin internet"))

        val vm = IndicadoresViewModel(repo)

        assertFalse(vm.estado.value.cargando)
        assertEquals("Sin internet", vm.estado.value.error)
    }

    @Test
    fun reintentarPideDatosNuevos() = runTest {
        coEvery { repo.obtener(false) } returns Result.failure(Exception("Sin internet"))
        coEvery { repo.obtener(true) } returns Result.success(listOf(uf))
        val vm = IndicadoresViewModel(repo)

        vm.recargar()

        coVerify { repo.obtener(true) }
        assertEquals(listOf(uf), vm.estado.value.indicadores)
        assertNull(vm.estado.value.error)
    }
}

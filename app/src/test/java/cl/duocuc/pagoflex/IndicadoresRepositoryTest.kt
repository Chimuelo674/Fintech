package cl.duocuc.pagoflex

import cl.duocuc.pagoflex.data.remote.Indicador
import cl.duocuc.pagoflex.data.remote.IndicadorDto
import cl.duocuc.pagoflex.data.remote.IndicadoresApi
import cl.duocuc.pagoflex.data.remote.IndicadoresDto
import cl.duocuc.pagoflex.data.remote.IndicadoresRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Pruebas del consumo de la API externa mindicador.cl, con una API falsa. */
class IndicadoresRepositoryTest {

    private class FakeIndicadoresApi(var respuesta: IndicadoresDto) : IndicadoresApi {
        var llamadas = 0
        var falla: Exception? = null
        override suspend fun obtener(): IndicadoresDto {
            llamadas++
            falla?.let { throw it }
            return respuesta
        }
    }

    private fun ind(codigo: String, valor: Double?) =
        IndicadorDto(codigo, "Nombre $codigo", "Pesos", "2026-11-24T03:00:00.000Z", valor)

    private val completo = IndicadoresDto(
        uf = ind("uf", 39500.12),
        dolar = ind("dolar", 950.5),
        euro = ind("euro", 1030.0),
        utm = ind("utm", 69000.0)
    )

    private var ahora = 1_000_000L
    private val api = FakeIndicadoresApi(completo)
    private val repo = IndicadoresRepository({ api }, { ahora })

    @Test
    fun convierteLaRespuestaEnOrdenYConNombresCortos() = runBlocking {
        val lista = repo.obtener().getOrThrow()
        assertEquals(listOf("UF", "Dólar", "Euro", "UTM"), lista.map { it.nombre })
        assertEquals("2026-11-24", lista.first().fecha)
        assertEquals(39500.12, lista.first().valor, 0.0)
    }

    @Test
    fun omiteLosIndicadoresQueVienenSinValor() {
        val lista = Indicador.desde(IndicadoresDto(uf = ind("uf", 1.0), dolar = ind("dolar", null), euro = null, utm = null))
        assertEquals(listOf("uf"), lista.map { it.codigo })
    }

    @Test
    fun usaLaCacheDuranteDiezMinutos() = runBlocking {
        repo.obtener()
        ahora += IndicadoresRepository.DURACION_CACHE_MS - 1
        repo.obtener()
        assertEquals(1, api.llamadas)

        ahora += 2
        repo.obtener()
        assertEquals(2, api.llamadas)
    }

    @Test
    fun forzarIgnoraLaCache() = runBlocking {
        repo.obtener()
        repo.obtener(forzar = true)
        assertEquals(2, api.llamadas)
    }

    @Test
    fun sinInternetYSinDatosPreviosDevuelveUnError() = runBlocking {
        api.falla = IOException("sin red")
        val resultado = repo.obtener()
        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull()!!.message!!.contains("indicadores económicos"))
    }

    @Test
    fun sinInternetPeroConDatosPreviosMuestraLosAnteriores() = runBlocking {
        repo.obtener()
        api.falla = IOException("sin red")
        val resultado = repo.obtener(forzar = true)
        assertEquals(4, resultado.getOrThrow().size)
    }

    @Test
    fun respuestaVaciaEsUnError() = runBlocking {
        api.respuesta = IndicadoresDto(null, null, null, null)
        assertTrue(repo.obtener().isFailure)
    }

    @Test
    fun formateaElValorEnPesosChilenos() {
        assertEquals("$ 39.500,12", Indicador("uf", "UF", 39500.12, "2026-11-24").valorTexto)
        assertEquals("$ 950,50", Indicador("dolar", "Dólar", 950.5, "2026-11-24").valorTexto)
    }
}

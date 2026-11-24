package cl.duocuc.pagoflex.data.remote

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import retrofit2.http.GET
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/*
 * API externa: mindicador.cl (indicadores económicos de Chile, gratis y sin clave).
 * Ejemplo de respuesta de GET https://mindicador.cl/api :
 * { "uf": { "codigo": "uf", "nombre": "Unidad de fomento (UF)", "unidad_medida": "Pesos",
 *           "fecha": "2026-11-24T03:00:00.000Z", "valor": 39500.12 }, "dolar": {...}, ... }
 */

interface IndicadoresApi {
    @GET("api")
    suspend fun obtener(): IndicadoresDto
}

data class IndicadorDto(
    val codigo: String?,
    val nombre: String?,
    @SerializedName("unidad_medida") val unidadMedida: String?,
    val fecha: String?,
    val valor: Double?
)

data class IndicadoresDto(
    val uf: IndicadorDto?,
    val dolar: IndicadorDto?,
    val euro: IndicadorDto?,
    val utm: IndicadorDto?
)

/** Indicador listo para mostrar en la app. */
data class Indicador(
    val codigo: String,
    val nombre: String,
    val valor: Double,
    val fecha: String
) {
    /** "$ 39.500,12" */
    val valorTexto: String
        get() {
            val simbolos = DecimalFormatSymbols().apply {
                groupingSeparator = '.'
                decimalSeparator = ','
            }
            return "$ " + DecimalFormat("#,##0.00", simbolos).format(valor)
        }

    companion object {
        private val NOMBRES_CORTOS = linkedMapOf("uf" to "UF", "dolar" to "Dólar", "euro" to "Euro", "utm" to "UTM")

        /** Convierte la respuesta de mindicador.cl, en orden UF, Dólar, Euro, UTM, omitiendo los que vengan sin valor. */
        fun desde(dto: IndicadoresDto): List<Indicador> =
            listOf("uf" to dto.uf, "dolar" to dto.dolar, "euro" to dto.euro, "utm" to dto.utm)
                .mapNotNull { (codigo, ind) ->
                    val valor = ind?.valor ?: return@mapNotNull null
                    Indicador(
                        codigo = codigo,
                        nombre = NOMBRES_CORTOS.getValue(codigo),
                        valor = valor,
                        fecha = ind.fecha?.take(10).orEmpty()
                    )
                }
    }
}

/**
 * Repositorio de la API externa. Guarda la última respuesta 10 minutos para no
 * llamar a mindicador.cl cada vez que se abre el inicio.
 */
class IndicadoresRepository(
    private val api: () -> IndicadoresApi,
    private val reloj: () -> Long = System::currentTimeMillis
) {
    private var cache: List<Indicador>? = null
    private var cacheEn: Long = 0L

    suspend fun obtener(forzar: Boolean = false): Result<List<Indicador>> {
        val ahora = reloj()
        val guardado = cache
        if (!forzar && guardado != null && ahora - cacheEn < DURACION_CACHE_MS) {
            return Result.success(guardado)
        }
        return try {
            val lista = Indicador.desde(api().obtener())
            if (lista.isEmpty()) {
                Result.failure(IllegalStateException("mindicador.cl no devolvió indicadores."))
            } else {
                cache = lista
                cacheEn = ahora
                Result.success(lista)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin internet: si teníamos datos anteriores, los mostramos igual.
            guardado?.let { Result.success(it) }
                ?: Result.failure(Exception("No pudimos obtener los indicadores económicos. Revisa tu conexión a internet."))
        }
    }

    companion object {
        const val DURACION_CACHE_MS = 10 * 60 * 1000L
    }
}

package cl.duocuc.pagoflex.data.remote

import android.content.Context
import cl.duocuc.pagoflex.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Dirección del servidor donde corren los microservicios.
 * - Emulador de Android Studio: 10.0.2.2 (así el emulador ve el "localhost" del PC).
 * - Celular físico: la IP del PC en la red WiFi (en Windows: comando ipconfig).
 * Se puede cambiar desde la pantalla de ingreso, sin volver a compilar.
 */
object ConfigServidor {
    const val PUERTO_USUARIOS = 8081
    const val PUERTO_PAGOS = 8082

    private const val PREFS = "pagoflex_servidor"
    private const val KEY_HOST = "host"

    @Volatile
    var host: String = BuildConfig.API_HOST
        private set

    fun init(context: Context) {
        host = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_HOST, null) ?: BuildConfig.API_HOST
    }

    fun cambiar(context: Context, nuevoHost: String) {
        val limpio = limpiar(nuevoHost)
        host = limpio
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_HOST, limpio).apply()
    }

    /** Acepta "192.168.1.5", "http://192.168.1.5:8081/" o similares y deja solo el host. */
    fun limpiar(texto: String): String =
        texto.trim()
            .removePrefix("http://").removePrefix("https://")
            .substringBefore("/")
            .substringBefore(":")
            .ifBlank { BuildConfig.API_HOST }

    fun urlUsuarios(): String = "http://$host:$PUERTO_USUARIOS/"
    fun urlPagos(): String = "http://$host:$PUERTO_PAGOS/"
}

/** Crea los clientes Retrofit. Si cambia la IP del servidor, los vuelve a crear. */
object ClienteHttp {
    private const val URL_MINDICADOR = "https://mindicador.cl/"

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    private fun retrofit(url: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(url)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    private var usuariosHost: String? = null
    private var usuariosCache: UsuariosApi? = null
    private var pagosHost: String? = null
    private var pagosCache: PagosApi? = null

    @Synchronized
    fun usuariosApi(): UsuariosApi {
        val host = ConfigServidor.host
        if (usuariosCache == null || usuariosHost != host) {
            usuariosCache = retrofit(ConfigServidor.urlUsuarios()).create(UsuariosApi::class.java)
            usuariosHost = host
        }
        return usuariosCache!!
    }

    @Synchronized
    fun pagosApi(): PagosApi {
        val host = ConfigServidor.host
        if (pagosCache == null || pagosHost != host) {
            pagosCache = retrofit(ConfigServidor.urlPagos()).create(PagosApi::class.java)
            pagosHost = host
        }
        return pagosCache!!
    }

    /** API externa (mindicador.cl). */
    val indicadoresApi: IndicadoresApi by lazy {
        retrofit(URL_MINDICADOR).create(IndicadoresApi::class.java)
    }

    /** Repositorio único de indicadores, para compartir la caché entre pantallas. */
    val indicadoresRepository: IndicadoresRepository by lazy {
        IndicadoresRepository({ indicadoresApi })
    }
}

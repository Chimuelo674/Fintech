package cl.duocuc.pagoflex

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import cl.duocuc.pagoflex.avisos.ServicioAvisos
import cl.duocuc.pagoflex.data.local.PagoFlexDatabase
import cl.duocuc.pagoflex.data.remote.ClienteHttp
import cl.duocuc.pagoflex.data.remote.ConfigServidor
import cl.duocuc.pagoflex.data.remote.RoomAlmacenLocal
import cl.duocuc.pagoflex.data.remote.SincronizadorRemoto
import cl.duocuc.pagoflex.data.seed.InitialDataSeeder
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.navigation.PagoFlexNavigation
import cl.duocuc.pagoflex.ui.theme.PagoFlexTheme
import cl.duocuc.pagoflex.ui.theme.TemaPreferencia
import kotlinx.coroutines.launch

// FragmentActivity (en vez de ComponentActivity) es necesaria para el ingreso con huella.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaPreferencia.init(applicationContext)
        enableEdgeToEdge()

        ConfigServidor.init(applicationContext)

        val database = PagoFlexDatabase.getDatabase(applicationContext)
        // Conexión con los microservicios usuarios-service y pagos-service (Retrofit).
        val sincronizador = SincronizadorRemoto(
            usuariosApi = { ClienteHttp.usuariosApi() },
            pagosApi = { ClienteHttp.pagosApi() },
            local = RoomAlmacenLocal(database)
        )
        val repository = PagoFlexRepository(database, sincronizador)

        // Al abrir la app: datos de prueba locales (si es la primera vez) y luego sincronización con el servidor.
        lifecycleScope.launch {
            InitialDataSeeder.seedDatabase(database)
            repository.sincronizarConServidor()
        }

        ServicioAvisos.programar(applicationContext)

        setContent {
            val oscuro = TemaPreferencia.esOscuro()

            LaunchedEffect(oscuro) {
                val transparente = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(transparente),
                    navigationBarStyle = if (oscuro) {
                        SystemBarStyle.dark(transparente)
                    } else {
                        SystemBarStyle.light(transparente, transparente)
                    }
                )
            }

            PagoFlexTheme(darkTheme = oscuro) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PagoFlexNavigation(repository = repository)
                }
            }
        }
    }
}

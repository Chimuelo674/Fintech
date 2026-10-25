package cl.duocuc.pagoflex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import cl.duocuc.pagoflex.avisos.ServicioAvisos
import cl.duocuc.pagoflex.data.local.PagoFlexDatabase
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.navigation.PagoFlexNavigation
import cl.duocuc.pagoflex.ui.theme.PagoFlexTheme
import cl.duocuc.pagoflex.ui.theme.TemaPreferencia

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaPreferencia.init(applicationContext)
        enableEdgeToEdge() // ajuste inicial; los colores de íconos se afinan abajo según el tema

        val database = PagoFlexDatabase.getDatabase(applicationContext)
        val repository = PagoFlexRepository(database)

        // RF-11: revisión periódica de avisos de vencimiento (respeta RN-15)
        ServicioAvisos.programar(applicationContext)

        setContent {
            val oscuro = TemaPreferencia.esOscuro()

            // Íconos de las barras del sistema según el tema elegido:
            // - Arriba siempre hay una barra verde oscuro -> íconos claros.
            // - Abajo el fondo es claro u oscuro según el tema -> íconos oscuros o claros.
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

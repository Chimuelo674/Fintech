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
        enableEdgeToEdge()

        val database = PagoFlexDatabase.getDatabase(applicationContext)
        val repository = PagoFlexRepository(database)

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

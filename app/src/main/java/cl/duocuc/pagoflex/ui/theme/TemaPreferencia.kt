package cl.duocuc.pagoflex.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object TemaPreferencia {
    private const val PREFS = "pagoflex_prefs"
    private const val KEY = "tema_oscuro"

    private var elegido by mutableStateOf<Boolean?>(null)
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        elegido = if (prefs.contains(KEY)) prefs.getBoolean(KEY, false) else null
    }

    @Composable
    fun esOscuro(): Boolean = elegido ?: isSystemInDarkTheme()

    fun cambiar(oscuroActual: Boolean) {
        val nuevo = !oscuroActual
        elegido = nuevo
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()?.putBoolean(KEY, nuevo)?.apply()
    }
}

@Composable
fun BotonTema(tint: Color = Color.White) {
    val oscuro = TemaPreferencia.esOscuro()
    IconButton(onClick = { TemaPreferencia.cambiar(oscuro) }) {
        Icon(
            imageVector = if (oscuro) Icons.Default.LightMode else Icons.Default.DarkMode,
            contentDescription = if (oscuro) "Cambiar a tema claro" else "Cambiar a tema oscuro",
            tint = tint
        )
    }
}

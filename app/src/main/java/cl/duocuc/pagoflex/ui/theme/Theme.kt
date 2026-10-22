package cl.duocuc.pagoflex.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PagoFlexDarkGreen,
    onPrimary = Color.White,
    primaryContainer = PagoFlexDarkGreen,
    onPrimaryContainer = Color.White,
    secondary = PagoFlexMint,
    onSecondary = Color.White,
    secondaryContainer = PagoFlexLightMint,
    onSecondaryContainer = PagoFlexDarkGreen,
    tertiary = PagoFlexTeal,
    onTertiary = Color.White,
    tertiaryContainer = PagoFlexLightMint,
    onTertiaryContainer = PagoFlexDarkGreen,
    background = PagoFlexBackground,
    onBackground = PagoFlexTextDark,
    surface = PagoFlexSurface,
    onSurface = PagoFlexTextDark,
    surfaceVariant = PagoFlexLightMint,
    onSurfaceVariant = PagoFlexDarkGreen,
    outline = PagoFlexTextMuted
)

private val DarkColorScheme = darkColorScheme(
    primary = PagoFlexMint,
    onPrimary = PagoFlexDarkGreen,
    primaryContainer = PagoFlexDarkGreen,
    onPrimaryContainer = Color.White,
    secondary = PagoFlexTeal,
    onSecondary = Color.White,
    background = Color(0xFF0D1B1E),
    surface = Color(0xFF132529)
)

@Composable
fun PagoFlexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use custom PagoFlex palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
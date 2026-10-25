package cl.duocuc.pagoflex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PagoFlexDarkGreen,
    onPrimary = Color.White,
    primaryContainer = PagoFlexLightMint,
    onPrimaryContainer = PagoFlexDarkGreen,
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
    onSurfaceVariant = Color(0xFF3F4F4C),
    outline = PagoFlexTextMuted,
    outlineVariant = Color(0xFFCFE3DE),
    // Contenedores que usan Card, diálogos y menús por defecto (sin esto salen lilas)
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7FBFA),
    surfaceContainer = Color(0xFFF0F7F5),
    surfaceContainerHigh = Color(0xFFEAF3F0),
    surfaceContainerHighest = Color(0xFFE4EFEC)
)

private val DarkColorScheme = darkColorScheme(
    primary = PagoFlexMint,
    onPrimary = Color(0xFF00241D),
    primaryContainer = Color(0xFF1E3833),
    onPrimaryContainer = Color(0xFFBFE9DD),
    secondary = PagoFlexMint,
    onSecondary = Color(0xFF00241D),
    secondaryContainer = Color(0xFF1E3833),
    onSecondaryContainer = Color(0xFFBFE9DD),
    tertiary = Color(0xFF4FD1B5),
    onTertiary = Color(0xFF00241D),
    tertiaryContainer = Color(0xFF1E3833),
    onTertiaryContainer = Color(0xFFE3F2EE),
    background = Color(0xFF0F1A1C),
    onBackground = Color(0xFFE3F2EE),
    surface = Color(0xFF172629),
    onSurface = Color(0xFFE3F2EE),
    surfaceVariant = Color(0xFF1E3833),
    onSurfaceVariant = Color(0xFFB9CCC8),
    outline = Color(0xFF8A9E9A),
    outlineVariant = Color(0xFF2E4743),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5C1A1B),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF0B1416),
    surfaceContainerLow = Color(0xFF132124),
    surfaceContainer = Color(0xFF172629),
    surfaceContainerHigh = Color(0xFF1C2D30),
    surfaceContainerHighest = Color(0xFF22363A)
)

/**
 * Tema de la app. Por defecto usa la preferencia guardada (botón sol/luna en las barras
 * superiores); si la persona nunca eligió, sigue el modo del teléfono.
 */
@Composable
fun PagoFlexTheme(
    darkTheme: Boolean = TemaPreferencia.esOscuro(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalPagoFlexColors provides if (darkTheme) DarkPagoFlexColors else LightPagoFlexColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}

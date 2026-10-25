package cl.duocuc.pagoflex.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colores con significado (no "verde" o "blanco", sino "tarjeta", "texto fuerte", "estado vencido").
 * Cada pantalla usa estos nombres; el tema decide el valor real en claro u oscuro.
 * Así no quedan colores fijos que se rompen al cambiar de tema.
 */
@Immutable
data class PagoFlexColors(
    val header: Color,        // barras superiores y tarjeta resumen (verde oscuro en ambos temas)
    val onHeader: Color,
    val onHeaderMuted: Color,
    val card: Color,          // fondo de tarjetas
    val chip: Color,          // fondo de etiquetas suaves / íconos circulares
    val onChip: Color,
    val textStrong: Color,    // títulos y montos
    val textMuted: Color,     // textos secundarios
    val divider: Color,
    val accent: Color,        // botón principal (Pagar)
    val onAccent: Color,
    val pendienteBg: Color, val pendienteFg: Color,
    val vencidoBg: Color, val vencidoFg: Color,
    val pagadoBg: Color, val pagadoFg: Color,
    val revisionBg: Color, val revisionFg: Color,
    val anuladoBg: Color, val anuladoFg: Color,
    val infoBg: Color, val infoFg: Color,
    val headerAlert: Color    // dato destacado sobre la tarjeta resumen (ej. compromisos vencidos)
)

val LightPagoFlexColors = PagoFlexColors(
    header = PagoFlexDarkGreen,
    onHeader = Color.White,
    onHeaderMuted = Color.White.copy(alpha = 0.75f),
    card = Color.White,
    chip = PagoFlexLightMint,
    onChip = PagoFlexDarkGreen,
    textStrong = PagoFlexDarkGreen,
    textMuted = Color(0xFF5F6F6C),
    divider = Color(0x1F063B34),
    accent = PagoFlexMint,
    onAccent = Color.White,
    pendienteBg = PagoFlexLightMint, pendienteFg = PagoFlexDarkGreen,
    vencidoBg = Color(0xFFFFEBEE), vencidoFg = Color(0xFFB3261E),
    pagadoBg = Color(0xFFE8F5E9), pagadoFg = Color(0xFF2E7D32),
    revisionBg = Color(0xFFFFF3E0), revisionFg = Color(0xFFB45309),
    anuladoBg = Color(0xFFECEFF1), anuladoFg = Color(0xFF546E7A),
    infoBg = Color(0xFFE3F2FD), infoFg = Color(0xFF1565C0),
    headerAlert = Color(0xFFFFC9A8)
)

val DarkPagoFlexColors = PagoFlexColors(
    header = Color(0xFF0A2F2A),
    onHeader = Color(0xFFF1FAF7),
    onHeaderMuted = Color(0xFFF1FAF7).copy(alpha = 0.7f),
    card = Color(0xFF172629),
    chip = Color(0xFF1E3833),
    onChip = Color(0xFFBFE9DD),
    textStrong = Color(0xFFE3F2EE),
    textMuted = Color(0xFF9DB2AE),
    divider = Color(0x29E3F2EE),
    accent = PagoFlexMint,
    onAccent = Color(0xFF00241D),
    pendienteBg = Color(0xFF1E3833), pendienteFg = Color(0xFF8FE3C8),
    vencidoBg = Color(0xFF3B1D1F), vencidoFg = Color(0xFFFF9E9E),
    pagadoBg = Color(0xFF1B3320), pagadoFg = Color(0xFF8BD69A),
    revisionBg = Color(0xFF3A2A14), revisionFg = Color(0xFFFFB866),
    anuladoBg = Color(0xFF2A3133), anuladoFg = Color(0xFFB0BCBF),
    infoBg = Color(0xFF16283A), infoFg = Color(0xFF8CC4FF),
    headerAlert = Color(0xFFFFC9A8)
)

val LocalPagoFlexColors = staticCompositionLocalOf { LightPagoFlexColors }

/** Acceso corto desde las pantallas: PF.colors.card, PF.colors.textStrong, etc. */
object PF {
    val colors: PagoFlexColors
        @Composable @ReadOnlyComposable get() = LocalPagoFlexColors.current

    /** Barra superior de todas las pantallas: verde oscuro con texto e íconos claros, en ambos temas. */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun barra(): TopAppBarColors {
        val c = colors
        return TopAppBarDefaults.topAppBarColors(
            containerColor = c.header,
            titleContentColor = c.onHeader,
            navigationIconContentColor = c.onHeader,
            actionIconContentColor = c.onHeader
        )
    }

    /** Colores (fondo, texto) de la etiqueta de un estado de compromiso, reporte o recomendación. */
    @Composable
    @ReadOnlyComposable
    fun estado(estado: String): Pair<Color, Color> {
        val c = colors
        return when (estado) {
            "Pendiente", "Recibida", "Abierto" -> c.pendienteBg to c.pendienteFg
            "Vencido", "Descartada" -> c.vencidoBg to c.vencidoFg
            "Pagado", "Resuelto", "Se unió" -> c.pagadoBg to c.pagadoFg
            "En revisión", "En gestión", "En evaluación" -> c.revisionBg to c.revisionFg
            "Contactada" -> c.infoBg to c.infoFg
            else -> c.anuladoBg to c.anuladoFg // Anulado y otros
        }
    }
}

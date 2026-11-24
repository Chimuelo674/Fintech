package cl.duocuc.pagoflex.ui.usuario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.domain.SimuladorCostos
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import java.util.Locale

private fun porcentaje(tasa: Double): String = String.format(Locale("es", "CL"), "%.2f%%", tasa * 100)

private fun textoMonto(valor: Double): TextFieldValue {
    val texto = CurrencyFormatter.formatCLP(valor)
    return TextFieldValue(texto, selection = TextRange(texto.length))
}

/**
 * Simulador de costos, como la herramienta de pagoflex.com.ar:
 * compara el 3% fijo de PagoFlex con lo que cobraría un banco tradicional.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimuladorCostosScreen(onBack: () -> Unit) {
    val c = PF.colors
    val parametros = remember { SimuladorCostos.Parametros() }
    var ventas by remember { mutableDoubleStateOf(1_000_000.0) }
    var campo by remember { mutableStateOf(textoMonto(1_000_000.0)) }
    val r = remember(ventas) { SimuladorCostos.calcular(ventas, parametros) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simulador de costos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = PF.barra()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Descubre cuánto pagarías por cobrar con PagoFlex frente a un banco tradicional.",
                color = c.textMuted,
                fontSize = 14.sp
            )

            OutlinedTextField(
                value = campo,
                onValueChange = { nuevo ->
                    val monto = SimuladorCostos.leerMonto(nuevo.text) ?: 0.0
                    ventas = monto
                    campo = if (nuevo.text.any { it.isDigit() }) textoMonto(monto) else TextFieldValue("")
                },
                label = { Text("¿Cuánto vendes al mes?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(500_000.0, 2_000_000.0, 10_000_000.0).forEach { sugerido ->
                    AssistChip(
                        onClick = {
                            ventas = sugerido
                            campo = textoMonto(sugerido)
                        },
                        label = { Text(CurrencyFormatter.formatCLP(sugerido), fontSize = 12.sp) }
                    )
                }
            }

            // ---------- Ahorro destacado ----------
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = c.header),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = c.accent, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        if (r.pagoFlexConviene) {
                            Text("Con PagoFlex ahorras", color = c.onHeaderMuted, fontSize = 13.sp)
                            Text(
                                "${CurrencyFormatter.formatCLP(r.ahorroMensual)} al mes",
                                color = c.onHeader,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp
                            )
                            Text(
                                "${CurrencyFormatter.formatCLP(r.ahorroAnual)} al año",
                                color = c.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Text("Ingresa tus ventas mensuales", color = c.onHeader, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("para ver la comparación.", color = c.onHeaderMuted, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ---------- Comparación con barras ----------
            val maximo = maxOf(r.costoBanco, r.costoPagoFlex, 1.0)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = c.card),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Costo mensual", fontWeight = FontWeight.Bold, color = c.textStrong, fontSize = 16.sp)
                    Spacer(Modifier.height(14.dp))
                    BarraCosto(
                        titulo = "PagoFlex",
                        detalle = "${porcentaje(parametros.tasaPagoFlex)} fijo",
                        monto = r.costoPagoFlex,
                        fraccion = (r.costoPagoFlex / maximo).toFloat(),
                        color = c.accent
                    )
                    Spacer(Modifier.height(14.dp))
                    BarraCosto(
                        titulo = "Banco tradicional",
                        detalle = "${porcentaje(r.tasaEfectivaBanco)} efectivo",
                        monto = r.costoBanco,
                        fraccion = (r.costoBanco / maximo).toFloat(),
                        color = c.vencidoFg
                    )
                }
            }

            // ---------- Detalle del banco ----------
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = c.card),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("¿Qué cobra el banco?", fontWeight = FontWeight.Bold, color = c.textStrong, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    FilaDetalle("Comisión (${porcentaje(parametros.tasaBanco)})", r.comisionBanco)
                    FilaDetalle("IVA sobre la comisión (${porcentaje(parametros.iva)})", r.ivaBanco)
                    FilaDetalle("Mantención mensual", r.mantencionBanco)
                    HorizontalDivider(color = c.divider, modifier = Modifier.padding(vertical = 8.dp))
                    FilaDetalle("Total banco", r.costoBanco, destacado = true)
                    FilaDetalle("Total PagoFlex (sin cargos ocultos)", r.costoPagoFlex, destacado = true)
                }
            }

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Los costos del banco son referenciales y varían según la institución y el plan contratado.",
                    color = c.textMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun BarraCosto(titulo: String, detalle: String, monto: Double, fraccion: Float, color: Color) {
    val c = PF.colors
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, color = c.textStrong, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(detalle, color = c.textMuted, fontSize = 12.sp)
            }
            Text(CurrencyFormatter.formatCLP(monto), color = c.textStrong, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(c.divider, RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraccion.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(6.dp))
            )
        }
    }
}

@Composable
private fun FilaDetalle(etiqueta: String, monto: Double, destacado: Boolean = false) {
    val c = PF.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            etiqueta,
            color = if (destacado) c.textStrong else c.textMuted,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            CurrencyFormatter.formatCLP(monto),
            color = c.textStrong,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

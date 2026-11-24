package cl.duocuc.pagoflex.ui.usuario

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.EmpresaEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.domain.ReglasAcceso
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.common.vm
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import cl.duocuc.pagoflex.util.Fechas

/**
 * Comprobante estilo banco: check animado, monto destacado, número de operación
 * y un "ticket" con el detalle, listo para compartir.
 */
@Composable
fun ComprobanteScreen(
    idPago: String,
    usuario: UsuarioEntity,
    repository: PagoFlexRepository,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = vm(key = "comprobante-$idPago") { ComprobanteViewModel(repository, usuario.idUsuario, idPago) }
    val ui by viewModel.estado.collectAsState()
    val pago = ui.pago
    val compromiso = ui.compromiso
    val empresa = ui.empresa
    val c = PF.colors

    if (pago == null || compromiso == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!ui.cargando) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No encontramos este comprobante.", fontWeight = FontWeight.Bold, color = c.textStrong)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBackToHome) { Text("Volver al inicio") }
                }
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    val rutOculto = ReglasAcceso.enmascararRut(usuario.rut)
    val recargo = (pago.montoPagado - compromiso.monto).coerceAtLeast(0.0)
    val nombreEmpresa = empresa?.nombreFantasia ?: compromiso.idEmpresa

    // Animación del check al abrir el comprobante.
    var mostrado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mostrado = true }
    val escala by animateFloatAsState(
        targetValue = if (mostrado) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "check"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ---------- Encabezado de éxito ----------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.header, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = c.accent,
                modifier = Modifier.size(76.dp).scale(escala)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(46.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("¡Pago exitoso!", color = c.onHeader, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            Text(
                "Tu pago a $nombreEmpresa fue recibido",
                color = c.onHeaderMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                CurrencyFormatter.formatCLP(pago.montoPagado),
                color = c.onHeader,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp
            )
        }

        // ---------- Ticket con el detalle ----------
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = c.card),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-44).dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("N° de operación", color = c.textMuted, fontSize = 12.sp)
                Text(pago.numeroComprobante, color = c.textStrong, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(Fechas.mostrar(pago.fechaHora), color = c.textMuted, fontSize = 13.sp)

                LineaPunteada(Modifier.padding(vertical = 16.dp))

                ReceiptRow("Pagado a", nombreEmpresa)
                empresa?.razonSocial?.let { ReceiptRow("Razón social", it) }
                ReceiptRow(
                    "Concepto",
                    compromiso.concepto + (compromiso.numeroCuota?.let { " (cuota $it/${compromiso.totalCuotas ?: "?"})" } ?: "")
                )
                ReceiptRow("Folio", compromiso.folio)
                ReceiptRow("Vencimiento", Fechas.mostrar(compromiso.fechaVencimiento))

                LineaPunteada(Modifier.padding(vertical = 16.dp))

                ReceiptRow("Monto de la cuota", CurrencyFormatter.formatCLP(compromiso.monto))
                if (recargo > 0) ReceiptRow("Recargo por atraso", CurrencyFormatter.formatCLP(recargo))
                ReceiptRow("Total pagado", CurrencyFormatter.formatCLP(pago.montoPagado), isBold = true)

                LineaPunteada(Modifier.padding(vertical = 16.dp))

                ReceiptRow("Medio de pago", pago.medioPago)
                ReceiptRow("Canal", pago.canal)
                ReceiptRow("Pagado por", usuario.nombreCompleto)
                ReceiptRow("RUT", rutOculto)

                Spacer(Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = c.pagadoBg, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = c.pagadoFg, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Comprobante válido. Puedes volver a verlo en Movimientos.",
                            color = c.pagadoFg,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // ---------- Acciones ----------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-28).dp)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    compartirComprobante(
                        context,
                        textoComprobante(pago, compromiso, empresa, "${usuario.nombreCompleto} · RUT $rutOculto")
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = c.textStrong)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir comprobante", color = c.textStrong, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onBackToHome,
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Volver al inicio", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

/** Línea punteada que separa las secciones del ticket. */
@Composable
private fun LineaPunteada(modifier: Modifier = Modifier) {
    val color = PF.colors.divider
    Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        )
    }
}

@Composable
fun ReceiptRow(label: String, value: String, isBold: Boolean = false) {
    val c = PF.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(text = label, fontSize = 13.sp, color = c.textMuted, modifier = Modifier.padding(end = 12.dp))
        Text(
            text = value,
            fontSize = if (isBold) 15.sp else 13.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Medium,
            color = c.textStrong,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun textoComprobante(p: PagoEntity, c: CompromisoEntity, e: EmpresaEntity?, pagadoPor: String): String {
    val recargo = (p.montoPagado - c.monto).coerceAtLeast(0.0)
    return buildString {
        appendLine("Comprobante de pago N° ${p.numeroComprobante}")
        appendLine("Fecha y hora: ${Fechas.mostrar(p.fechaHora)}")
        appendLine("Pagado por: $pagadoPor")
        appendLine("Empresa: ${e?.let { "${it.nombreFantasia} (${it.razonSocial})" } ?: c.idEmpresa}")
        appendLine("Folio del compromiso: ${c.folio}")
        appendLine("Concepto: ${c.concepto}${c.numeroCuota?.let { " · Cuota $it/${c.totalCuotas}" } ?: ""}")
        appendLine("Vencimiento: ${Fechas.mostrar(c.fechaVencimiento)}")
        appendLine("Monto de la cuota: ${CurrencyFormatter.formatCLP(c.monto)}")
        appendLine("Recargo por atraso: ${CurrencyFormatter.formatCLP(recargo)}")
        appendLine("Total pagado: ${CurrencyFormatter.formatCLP(p.montoPagado)}")
        appendLine("Medio de pago: ${p.medioPago}")
        appendLine("Canal: ${p.canal}")
        append("Estado: Pago confirmado")
    }
}

private fun compartirComprobante(context: Context, texto: String) {
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, texto.lineSequence().first())
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    context.startActivity(Intent.createChooser(envio, "Compartir comprobante"))
}

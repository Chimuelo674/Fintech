package cl.duocuc.pagoflex.ui.usuario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.ui.common.MensajesEnSnackbar
import cl.duocuc.pagoflex.ui.common.vm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleCompromisoScreen(
    idCompromiso: String,
    usuario: UsuarioEntity,
    repository: PagoFlexRepository,
    onPagar: (String) -> Unit,
    onReportar: (String) -> Unit,
    onVerComprobante: (String) -> Unit,
    onBack: () -> Unit
) {
    val c = PF.colors
    val viewModel = vm(key = "detalle-$idCompromiso") { DetalleCompromisoViewModel(repository, usuario.idUsuario, idCompromiso) }
    val ui by viewModel.estado.collectAsState()
    val comp = ui.compromiso
    val empresa = ui.empresa
    val pago = ui.pago
    val reporte = ui.reporte

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del compromiso", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                colors = PF.barra()
            )
        }
    ) { padding ->
        if (comp == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (!ui.cargado) CircularProgressIndicator() else Text("No encontramos este compromiso.", color = c.textMuted)
            }
            return@Scaffold
        }

        val vencido = comp.estado == "Vencido"
        val recargo = if (vencido) comp.recargoAplicado else 0.0
        val total = comp.monto + recargo
        val (bg, fg) = PF.estado(comp.estado)

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = c.card), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            empresa?.nombreFantasia ?: comp.idEmpresa,
                            fontWeight = FontWeight.Bold, fontSize = 18.sp, color = c.textStrong, modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(20.dp), color = bg) {
                            Text(comp.estado, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                    Text(comp.concepto, color = c.textStrong)
                    comp.numeroCuota?.let { Text("Cuota $it de ${comp.totalCuotas}", fontSize = 13.sp, color = c.textMuted) }
                    Text("Folio ${comp.folio} (cítalo si necesitas ayuda)", fontSize = 12.sp, color = c.textMuted)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = c.card), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Linea("Vence el", Fechas.mostrar(comp.fechaVencimiento))
                    if (vencido) {
                        val dias = Fechas.diasAtraso(comp.fechaVencimiento)
                        Linea("Días desde el vencimiento", "$dias ${if (dias == 1) "día" else "días"}", color = c.vencidoFg)
                    }
                    Linea("Monto", CurrencyFormatter.formatCLP(comp.monto))
                    if (vencido) {
                        Linea(
                            "Recargo por atraso",
                            if (recargo > 0) CurrencyFormatter.formatCLP(recargo) else "Sin recargo",
                            color = c.vencidoFg
                        )
                    }
                    HorizontalDivider(color = c.divider)
                    Linea(
                        if (comp.estado == "Pagado") "Total pagado" else "Total a pagar",
                        CurrencyFormatter.formatCLP(pago?.montoPagado ?: total),
                        destacado = true
                    )
                    Text(
                        when (comp.estado) {
                            "Pendiente" -> "Si pagas hasta el ${Fechas.mostrar(comp.fechaVencimiento)} no tiene recargo."
                            "Vencido" -> "Te falta este pago para quedar al día con ${empresa?.nombreFantasia ?: "la empresa"}."
                            "En revisión" -> "Mientras revisamos tu reporte, este cobro no se puede pagar."
                            "Anulado" -> "La empresa anuló este cobro: no tienes que pagarlo."
                            else -> ""
                        },
                        fontSize = 12.sp, color = c.textMuted
                    )
                }
            }

            pago?.let { p ->
                Card(colors = CardDefaults.cardColors(containerColor = c.card), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Pago", fontWeight = FontWeight.Bold, color = c.textStrong)
                        Linea("Fecha", Fechas.mostrar(p.fechaHora))
                        Linea("Canal", p.canal)
                        Linea("¿A tiempo?", if (p.fechaHora.take(10) <= comp.fechaVencimiento) "Sí" else "Con atraso")
                        Linea("Comprobante", p.numeroComprobante)
                    }
                }
            }

            reporte?.let { r ->
                Card(colors = CardDefaults.cardColors(containerColor = c.card), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tu reporte ${r.idReporte} · ${r.estado}", fontWeight = FontWeight.Bold, color = c.textStrong)
                        Text("Motivo: ${r.motivo}", fontSize = 13.sp, color = c.textStrong)
                        if (!r.respuesta.isNullOrBlank()) Text("Respuesta: ${r.respuesta}", fontSize = 13.sp, color = c.textMuted)
                        TextButton(onClick = { onReportar(comp.idCompromiso) }) { Text("Ver en Mis reportes") }
                    }
                }
            }

            empresa?.let { e ->
                Card(colors = CardDefaults.cardColors(containerColor = c.chip), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Contacto de ${e.nombreFantasia}", fontWeight = FontWeight.Bold, color = c.onChip)
                        Text(e.correoAtencion, fontSize = 13.sp, color = c.onChip)
                        Text(e.telefonoAtencion, fontSize = 13.sp, color = c.onChip)
                    }
                }
            }

            when (comp.estado) {
                "Pendiente", "Vencido" -> {
                    Button(
                        onClick = { onPagar(comp.idCompromiso) },
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pagar ${CurrencyFormatter.formatCLP(total)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    OutlinedButton(onClick = { onReportar(comp.idCompromiso) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Reportar un problema")
                    }
                }
                "Pagado" -> OutlinedButton(onClick = { onVerComprobante(comp.idCompromiso) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Receipt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ver comprobante")
                }
            }
        }
    }
}

@Composable
private fun Linea(
    etiqueta: String,
    valor: String,
    color: androidx.compose.ui.graphics.Color = PF.colors.textStrong,
    destacado: Boolean = false
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, color = PF.colors.textMuted, fontSize = if (destacado) 15.sp else 14.sp, modifier = Modifier.weight(1f))
        Text(
            valor, color = color,
            fontSize = if (destacado) 20.sp else 14.sp,
            fontWeight = if (destacado) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

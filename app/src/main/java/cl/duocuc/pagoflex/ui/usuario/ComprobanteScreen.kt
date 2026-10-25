package cl.duocuc.pagoflex.ui.usuario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.EmpresaEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import cl.duocuc.pagoflex.ui.theme.BotonTema
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.ui.common.MensajesEnSnackbar
import cl.duocuc.pagoflex.ui.common.vm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprobanteScreen(
    idPago: String,
    usuario: UsuarioEntity,
    repository: PagoFlexRepository,
    onBackToHome: () -> Unit
) {
    // MVVM. El parámetro puede ser un N° de comprobante (al venir desde el pago, "CPR-…")
    // o un id de compromiso (al venir desde el historial, "CP-…"). Se aceptan ambos.
    val context = LocalContext.current
    val viewModel = vm(key = "comprobante-$idPago") { ComprobanteViewModel(repository, usuario.idUsuario, idPago) }
    val ui by viewModel.estado.collectAsState()
    val pago = ui.pago
    val compromiso = ui.compromiso
    val empresa = ui.empresa
    val noEncontrado = !ui.cargando && (pago == null || compromiso == null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comprobante de Pago", fontWeight = FontWeight.Bold) },
                colors = PF.barra()
            )
        }
    ) { paddingValues ->
        val currentPago = pago
        val currentCompromiso = compromiso

        if (currentPago == null || currentCompromiso == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (noEncontrado) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No encontramos este comprobante.", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBackToHome) { Text("Volver al inicio") }
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
        } else {
            // Ocultar RUT parcialmente (RNF-07): 15.834.207-3 -> 15.*.*-3 ; 6.893.541-5 -> 6.*.*-5
            val rutPartes = usuario.rut.split("-")
            val rutOculto = if (rutPartes.size == 2) {
                "${rutPartes[0].substringBefore(".")}.*.*-${rutPartes[1]}"
            } else usuario.rut

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        // Header Status Success
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PF.colors.pagadoFg,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Pago Confirmado", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PF.colors.pagadoFg)
                                Text("N° ${currentPago.numeroComprobante}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Receipt Fields Table
                        ReceiptRow("Fecha y hora:", Fechas.mostrar(currentPago.fechaHora))
                        ReceiptRow("Pagado por:", "${usuario.nombreCompleto} · RUT $rutOculto")
                        ReceiptRow(
                            "Empresa:",
                            empresa?.let { "${it.nombreFantasia} (${it.razonSocial})" } ?: currentCompromiso.idEmpresa
                        )
                        ReceiptRow("Folio compromiso:", currentCompromiso.folio)
                        ReceiptRow("Concepto:", "${currentCompromiso.concepto}${currentCompromiso.numeroCuota?.let { " (Cuota $it/${currentCompromiso.totalCuotas ?: "?"})" } ?: ""}")
                        ReceiptRow("Vencimiento:", Fechas.mostrar(currentCompromiso.fechaVencimiento))
                        ReceiptRow("Monto cuota:", CurrencyFormatter.formatCLP(currentCompromiso.monto))
                        // El recargo no se guarda: es lo pagado menos el monto del compromiso (MER 2.3)
                        ReceiptRow("Recargo por atraso:", CurrencyFormatter.formatCLP((currentPago.montoPagado - currentCompromiso.monto).coerceAtLeast(0.0)))

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        ReceiptRow("Total pagado:", CurrencyFormatter.formatCLP(currentPago.montoPagado), isBold = true)
                        ReceiptRow("Medio de pago:", currentPago.medioPago)
                        ReceiptRow("Canal:", currentPago.canal)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        // RF-22: enviar el comprobante a otra persona (arrendador, colegio…) con el menú de Android
                        onClick = {
                            compartirComprobante(
                                context,
                                textoComprobante(currentPago, currentCompromiso, empresa, "${usuario.nombreCompleto} · RUT $rutOculto")
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir")
                    }

                    Button(
                        onClick = onBackToHome,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ir al Inicio", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Texto del comprobante para compartir: mismos datos que en pantalla, con el RUT parcialmente oculto (RNF-07). */
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

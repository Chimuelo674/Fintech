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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprobanteScreen(
    idPago: String,
    usuario: UsuarioEntity,
    repository: PagoFlexRepository,
    onBackToHome: () -> Unit
) {
    var pago by remember { mutableStateOf<PagoEntity?>(null) }
    var compromiso by remember { mutableStateOf<CompromisoEntity?>(null) }
    var empresa by remember { mutableStateOf<EmpresaEntity?>(null) }

    LaunchedEffect(idPago) {
        val p = repository.getPagoByCompromiso(idPago) ?: run {
            // Alternatively query by idPago directly if passed receipt number
            val pagos = repository.getPagosByUsuario(usuario.idUsuario)
            // Or search in repository
            null
        }
        if (p != null) {
            pago = p
            val c = repository.getCompromisoById(p.idCompromiso)
            compromiso = c
            if (c != null) {
                empresa = repository.getEmpresaById(c.idEmpresa)
            }
        } else {
            // Query compromise by idPago
            val c = repository.getCompromisoById(idPago)
            if (c != null) {
                compromiso = c
                empresa = repository.getEmpresaById(c.idEmpresa)
                pago = repository.getPagoByCompromiso(c.idCompromiso)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comprobante de Pago", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { paddingValues ->
        val currentPago = pago
        val currentCompromiso = compromiso

        if (currentPago == null || currentCompromiso == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Ocultar RUT parcialmente (RNF-07): e.g. 15.*.*-3
            val rutPartes = usuario.rut.split("-")
            val rutOculto = if (rutPartes.size == 2) {
                "${rutPartes[0].take(2)}.*.*-${rutPartes[1]}"
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
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Pago Confirmado", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF2E7D32))
                                Text("N° ${currentPago.numeroComprobante}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Receipt Fields Table
                        ReceiptRow("Fecha y hora:", currentPago.fechaHora)
                        ReceiptRow("Pagado por:", "${usuario.nombreCompleto} · RUT $rutOculto")
                        ReceiptRow("Empresa:", empresa?.razonSocial ?: currentPago.idEmpresa)
                        ReceiptRow("Folio compromiso:", currentCompromiso.folio)
                        ReceiptRow("Concepto:", "${currentCompromiso.concepto}${currentCompromiso.numeroCuota?.let { " (Cuota $it/${currentCompromiso.totalCuotas ?: "?"})" } ?: ""}")
                        ReceiptRow("Vencimiento:", currentCompromiso.fechaVencimiento)
                        ReceiptRow("Monto cuota:", CurrencyFormatter.formatCLP(currentCompromiso.monto))
                        ReceiptRow("Recargo mora:", CurrencyFormatter.formatCLP(currentPago.recargoPagado))

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
                        onClick = { /* Share simulated */ },
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

package cl.duocuc.pagoflex.ui.usuario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
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
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import kotlinx.coroutines.launch
import cl.duocuc.pagoflex.ui.theme.BotonTema
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.ui.common.MensajesEnSnackbar
import cl.duocuc.pagoflex.ui.common.vm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagoScreen(
    idCompromiso: String,
    usuario: UsuarioEntity,
    repository: PagoFlexRepository,
    onPagoConfirmado: (String) -> Unit,
    onBack: () -> Unit
) {
    val viewModel = vm(key = "pago-$idCompromiso") { PagoViewModel(repository, usuario.idUsuario, idCompromiso) }
    val ui by viewModel.estado.collectAsState()
    val compromiso = ui.compromiso
    val empresa = ui.empresa
    val errorMessage = ui.error
    val isLoading = ui.procesando

    val mediosPago = listOf("Tarjeta de débito", "Tarjeta de crédito", "Transferencia")
    var selectedMedioPago by remember { mutableStateOf(mediosPago[0]) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proceso de Pago Simulado", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = PF.barra()
            )
        }
    ) { paddingValues ->
        val currentCompromiso = compromiso
        if (currentCompromiso == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val recargo = if (currentCompromiso.estado == "Vencido") currentCompromiso.recargoAplicado else 0.0
            val totalMonto = currentCompromiso.monto + recargo

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = empresa?.nombreFantasia ?: currentCompromiso.idEmpresa,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentCompromiso.concepto,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Folio: ${currentCompromiso.folio}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Monto cuota:")
                                Text(CurrencyFormatter.formatCLP(currentCompromiso.monto), fontWeight = FontWeight.SemiBold)
                            }

                            if (recargo > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Recargo por atraso:", color = MaterialTheme.colorScheme.error)
                                    Text(CurrencyFormatter.formatCLP(recargo), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total a pagar:", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(
                                    CurrencyFormatter.formatCLP(totalMonto),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Selecciona Medio de Pago Simulado",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            mediosPago.forEach { medio ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = (medio == selectedMedioPago),
                                            onClick = { selectedMedioPago = medio }
                                        )
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (medio == selectedMedioPago),
                                        onClick = { selectedMedioPago = medio }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = medio, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pago 100% simulado. No se cobran datos reales ni dinero.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.pagar(selectedMedioPago, onPagoConfirmado) },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = LocalContentColor.current, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Payment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirmar Pago de ${CurrencyFormatter.formatCLP(totalMonto)}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

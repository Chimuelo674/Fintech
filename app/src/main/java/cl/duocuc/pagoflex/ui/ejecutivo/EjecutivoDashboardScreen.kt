package cl.duocuc.pagoflex.ui.ejecutivo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjecutivoDashboardScreen(
    ejecutivo: UsuarioEntity,
    repository: PagoFlexRepository,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val idEmpresaEjecutivo = ejecutivo.idEmpresa ?: "EC-01"

    val empresa by produceState<EmpresaEntity?>(initialValue = null) {
        value = repository.getEmpresaById(idEmpresaEjecutivo)
    }

    val compromisos by repository.getCompromisosByEmpresa(idEmpresaEjecutivo).collectAsState(initial = emptyList())

    // Cálculos Tasa de Cobranza (RN-24)
    val totalCompromisos = compromisos.size
    val anulados = compromisos.count { it.estado == "Anulado" }
    val pagados = compromisos.count { it.estado == "Pagado" }
    val vencidos = compromisos.count { it.estado == "Vencido" }

    val baseCobranza = (totalCompromisos - anulados).coerceAtLeast(1)
    val tasaCobranza = (pagados.toDouble() / baseCobranza.toDouble()) * 100.0
    val montoRecaudado = compromisos.filter { it.estado == "Pagado" }.sumOf { it.monto }

    var showRegistrarDialog by remember { mutableStateOf(false) }
    var selectedCompromisoToAnular by remember { mutableStateOf<CompromisoEntity?>(null) }

    // Campos formulario registrar
    var rutUsuarioText by remember { mutableStateOf("15.834.207-3") }
    var conceptoText by remember { mutableStateOf("") }
    var cuotaText by remember { mutableStateOf("1") }
    var totalCuotasText by remember { mutableStateOf("12") }
    var montoText by remember { mutableStateOf("50000") }
    var fechaVencimientoText by remember { mutableStateOf("2026-11-10") }
    var registrarErrorMessage by remember { mutableStateOf<String?>(null) }

    // Campos formulario anular
    val motivosAnulacion = listOf("Repactación", "Error de digitación", "Cobro duplicado", "Anulación por cliente")
    var selectedMotivoAnulacion by remember { mutableStateOf(motivosAnulacion[0]) }
    var comentarioAnulacionText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PagoFlex · Panel Ejecutivo (R-03)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${ejecutivo.nombreCompleto} (${empresa?.nombreFantasia ?: idEmpresaEjecutivo})", fontSize = 12.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showRegistrarDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar Compromiso (RF-17)") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Resumen Métricas y Tasa de Cobranza (RF-20, RN-24)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Tasa de Cobranza Mensual (RN-24)", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", tasaCobranza),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Monto Recaudado", style = MaterialTheme.typography.labelSmall)
                                Text(CurrencyFormatter.formatCLP(montoRecaudado), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Column {
                                Text("Pagados", style = MaterialTheme.typography.labelSmall)
                                Text("$pagados de ${totalCompromisos - anulados}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Column {
                                Text("Vencidos", style = MaterialTheme.typography.labelSmall)
                                Text("$vencidos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Compromisos de ${empresa?.nombreFantasia ?: idEmpresaEjecutivo}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            if (compromisos.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No hay compromisos registrados para esta empresa.")
                    }
                }
            } else {
                items(compromisos) { comp ->
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(comp.concepto, fontWeight = FontWeight.Bold)
                                    Text("Folio: ${comp.folio} • Usuario: ${comp.idUsuarioFinal}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (comp.estado) {
                                        "Pagado" -> Color(0xFFE8F5E9)
                                        "Vencido" -> MaterialTheme.colorScheme.errorContainer
                                        "Anulado" -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    }
                                ) {
                                    Text(
                                        text = comp.estado,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vence: ${comp.fechaVencimiento}", fontSize = 12.sp)
                                Text(CurrencyFormatter.formatCLP(comp.monto), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            if (comp.estado != "Pagado" && comp.estado != "Anulado") {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { selectedCompromisoToAnular = comp },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Anular Compromiso (RF-18)")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog Registrar Compromiso (RF-17)
        if (showRegistrarDialog) {
            AlertDialog(
                onDismissRequest = { showRegistrarDialog = false },
                title = { Text("Registrar Nuevo Compromiso (RF-17)", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rutUsuarioText,
                            onValueChange = { rutUsuarioText = it },
                            label = { Text("RUT Usuario Final *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = conceptoText,
                            onValueChange = { conceptoText = it },
                            label = { Text("Concepto (ej. Cuota arriendo) *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cuotaText,
                                onValueChange = { cuotaText = it },
                                label = { Text("N° Cuota") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = totalCuotasText,
                                onValueChange = { totalCuotasText = it },
                                label = { Text("Total Cuotas") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = montoText,
                            onValueChange = { montoText = it },
                            label = { Text("Monto *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = fechaVencimientoText,
                            onValueChange = { fechaVencimientoText = it },
                            label = { Text("Fecha Vencimiento (YYYY-MM-DD) *") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (registrarErrorMessage != null) {
                            Text(registrarErrorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (rutUsuarioText.isNotBlank() && conceptoText.isNotBlank() && montoText.isNotBlank()) {
                                scope.launch {
                                    val result = repository.registrarCompromiso(
                                        idEjecutivo = ejecutivo.idUsuario,
                                        idEmpresa = idEmpresaEjecutivo,
                                        rutUsuarioFinal = rutUsuarioText,
                                        concepto = conceptoText,
                                        cuota = cuotaText.toIntOrNull(),
                                        totalCuotas = totalCuotasText.toIntOrNull(),
                                        monto = montoText.toDoubleOrNull() ?: 0.0,
                                        fechaVencimiento = fechaVencimientoText
                                    )
                                    result.onSuccess {
                                        showRegistrarDialog = false
                                        conceptoText = ""
                                        registrarErrorMessage = null
                                    }.onFailure { err ->
                                        registrarErrorMessage = err.message
                                    }
                                }
                            }
                        }
                    ) { Text("Guardar Compromiso") }
                },
                dismissButton = {
                    TextButton(onClick = { showRegistrarDialog = false }) { Text("Cancelar") }
                }
            )
        }

        // Dialog Anular Compromiso (RF-18, RN-21)
        val compToAnular = selectedCompromisoToAnular
        if (compToAnular != null) {
            AlertDialog(
                onDismissRequest = { selectedCompromisoToAnular = null },
                title = { Text("Anular Compromiso ${compToAnular.folio}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Selecciona el motivo de anulación (RN-21):", fontWeight = FontWeight.SemiBold)

                        motivosAnulacion.forEach { mot ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = (mot == selectedMotivoAnulacion),
                                    onClick = { selectedMotivoAnulacion = mot }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(mot)
                            }
                        }

                        OutlinedTextField(
                            value = comentarioAnulacionText,
                            onValueChange = { comentarioAnulacionText = it },
                            label = { Text("Comentario explicativo") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                repository.anularCompromiso(
                                    idCompromiso = compToAnular.idCompromiso,
                                    idEjecutivo = ejecutivo.idUsuario,
                                    motivo = selectedMotivoAnulacion,
                                    comentario = comentarioAnulacionText
                                )
                                selectedCompromisoToAnular = null
                                comentarioAnulacionText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Confirmar Anulación") }
                },
                dismissButton = {
                    TextButton(onClick = { selectedCompromisoToAnular = null }) { Text("Cancelar") }
                }
            )
        }
    }
}

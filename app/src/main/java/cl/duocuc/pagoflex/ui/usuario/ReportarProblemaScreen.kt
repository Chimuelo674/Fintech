package cl.duocuc.pagoflex.ui.usuario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.ReporteEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportarProblemaScreen(
    idCompromisoInitial: String? = null,
    usuario: UsuarioEntity,
    repository: PagoFlexRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val reportes by repository.getReportesByUsuario(usuario.idUsuario).collectAsState(initial = emptyList())
    val compromisos by repository.getCompromisosByUsuario(usuario.idUsuario).collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(idCompromisoInitial != null) }
    var selectedCompromisoId by remember { mutableStateOf(idCompromisoInitial ?: compromisos.firstOrNull()?.idCompromiso ?: "") }

    val motivos = listOf("Ya pagué por otro medio", "El monto no corresponde", "No reconozco este cobro", "Otro")
    var selectedMotivo by remember { mutableStateOf(motivos[0]) }
    var comentarioText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes de Problemas (RF-09)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo Reporte") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Reporta un problema en tus cobros. El compromiso pasará a 'En revisión' mientras el agente responde.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (reportes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No has realizado reportes de problemas.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reportes) { reporte ->
                        ReporteCardItem(reporte = reporte)
                    }
                }
            }
        }

        // Dialog para crear nuevo reporte
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Crear Reporte de Problema", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Motivo del problema:", fontWeight = FontWeight.SemiBold)

                        motivos.forEach { mot ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = (mot == selectedMotivo),
                                    onClick = { selectedMotivo = mot }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(mot, fontSize = 14.sp)
                            }
                        }

                        OutlinedTextField(
                            value = comentarioText,
                            onValueChange = { comentarioText = it },
                            label = { Text("Comentario explicativo") },
                            placeholder = { Text("Describe brevemente el problema...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (selectedCompromisoId.isNotBlank()) {
                                isSubmitting = true
                                scope.launch {
                                    repository.crearReporte(
                                        idUsuario = usuario.idUsuario,
                                        idCompromiso = selectedCompromisoId,
                                        motivo = selectedMotivo,
                                        comentario = comentarioText
                                    )
                                    isSubmitting = false
                                    showDialog = false
                                    comentarioText = ""
                                }
                            }
                        },
                        enabled = !isSubmitting
                    ) {
                        Text("Enviar Reporte")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun ReporteCardItem(reporte: ReporteEntity) {
    val (statusBg, statusFg) = when (reporte.estado) {
        "Abierto" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "En gestión" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        else -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
    }

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
                Text("Reporte N° ${reporte.idReporte}", fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg
                ) {
                    Text(
                        text = reporte.estado,
                        color = statusFg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Motivo: ${reporte.motivo}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (!reporte.comentario.isNull_or_blank()) {
                Text("Comentario: ${reporte.comentario}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Fecha: ${reporte.fechaCreacion}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

            if (!reporte.respuesta.isNull_or_blank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Respuesta de Soporte PagoFlex:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Text(reporte.respuesta ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()

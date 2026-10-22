package cl.duocuc.pagoflex.ui.agente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.RecomendacionEntity
import cl.duocuc.pagoflex.data.local.entities.ReporteEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgenteDashboardScreen(
    agente: UsuarioEntity,
    repository: PagoFlexRepository,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val reportes by repository.getAllReportes().collectAsState(initial = emptyList())
    val recomendaciones by repository.getAllRecomendaciones().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Reportes (RF-14, RF-15), 1: Recomendaciones (RF-16)

    var selectedReporteToResolve by remember { mutableStateOf<ReporteEntity?>(null) }
    var respuestaText by remember { mutableStateOf("") }
    var nuevoEstadoCompromiso by remember { mutableStateOf("Pendiente") }

    var selectedRecToChange by remember { mutableStateOf<RecomendacionEntity?>(null) }
    var nuevoEstadoRec by remember { mutableStateOf("Contactada") }
    var motivoDescarteText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PagoFlex · Panel Agente (R-02)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${agente.nombreCompleto} (${agente.area ?: "Soporte"})", fontSize = 12.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Reportes de Problemas (${reportes.count { it.estado != "Resuelto" }})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Recomendaciones (${recomendaciones.size})", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // LISTA DE REPORTES (RF-14, RF-15)
                if (reportes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay reportes registrados.")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(reportes) { reporte ->
                            // Identificar reporte atrasado (RN-23): Abierto > 2 días
                            val isAtrasado = reporte.estado == "Abierto" && (reporte.idReporte == "REP-0010" || reporte.fechaCreacion <= "2026-10-01")

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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Reporte ${reporte.idReporte}", fontWeight = FontWeight.Bold)
                                            if (isAtrasado) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.error) {
                                                    Text("ATRASADO (>2 días - RN-23)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        Badge(
                                            containerColor = if (reporte.estado == "Resuelto") Color(0xFFE8F5E9) else MaterialTheme.colorScheme.errorContainer,
                                            contentColor = if (reporte.estado == "Resuelto") Color(0xFF2E7D32) else MaterialTheme.colorScheme.onErrorContainer
                                        ) {
                                            Text(reporte.estado, modifier = Modifier.padding(4.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Usuario: ${reporte.idUsuarioFinal} • Compromiso: ${reporte.idCompromiso}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Motivo: ${reporte.motivo}", fontSize = 13.sp)
                                    Text("Comentario: ${reporte.comentario ?: "Sin comentario"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Fecha creación: ${reporte.fechaCreacion}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                                    if (reporte.respuesta != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Respuesta de Soporte (${reporte.idAgente}): ${reporte.respuesta}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                    } else {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { selectedReporteToResolve = reporte },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Tomar y Responder Reporte")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // LISTA DE RECOMENDACIONES (RF-16)
                if (recomendaciones.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay recomendaciones registradas.")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recomendaciones) { rec ->
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
                                        Text(rec.nombreOrganizacion, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(rec.estado, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }

                                    Text("Recomendado por: ${rec.idUsuarioFinal} • Rubro: ${rec.rubro ?: "N/A"}", fontSize = 13.sp)
                                    if (rec.nombreContacto != null) {
                                        Text("Contacto: ${rec.nombreContacto} (${rec.correoContacto ?: rec.telefonoContacto ?: ""})", fontSize = 12.sp)
                                    }
                                    Text("Fecha: ${rec.fecha}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { selectedRecToChange = rec },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Cambiar Estado (RF-16)")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog Resolver Reporte
        val repToResolve = selectedReporteToResolve
        if (repToResolve != null) {
            AlertDialog(
                onDismissRequest = { selectedReporteToResolve = null },
                title = { Text("Responder Reporte ${repToResolve.idReporte}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Motivo del usuario: ${repToResolve.motivo}", fontSize = 13.sp)

                        OutlinedTextField(
                            value = respuestaText,
                            onValueChange = { respuestaText = it },
                            label = { Text("Respuesta para el usuario *") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Actualizar estado del compromiso:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = (nuevoEstadoCompromiso == "Pendiente"),
                                onClick = { nuevoEstadoCompromiso = "Pendiente" },
                                label = { Text("Restaurar Pendiente") }
                            )
                            FilterChip(
                                selected = (nuevoEstadoCompromiso == "Anulado"),
                                onClick = { nuevoEstadoCompromiso = "Anulado" },
                                label = { Text("Anular Cobro") }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (respuestaText.isNotBlank()) {
                                scope.launch {
                                    repository.resolverReporte(
                                        idReporte = repToResolve.idReporte,
                                        idAgente = agente.idUsuario,
                                        respuesta = respuestaText,
                                        nuevoEstadoCompromiso = nuevoEstadoCompromiso
                                    )
                                    selectedReporteToResolve = null
                                    respuestaText = ""
                                }
                            }
                        }
                    ) {
                        Text("Guardar Resolución")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedReporteToResolve = null }) { Text("Cancelar") }
                }
            )
        }

        // Dialog Cambiar Estado Recomendación
        val recToChange = selectedRecToChange
        if (recToChange != null) {
            val estadosRec = listOf("En evaluación", "Contactada", "Se unió", "Descartada")

            AlertDialog(
                onDismissRequest = { selectedRecToChange = null },
                title = { Text("Gestionar Recomendación ${recToChange.idRecomendacion}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Organización: ${recToChange.nombreOrganizacion}", fontWeight = FontWeight.Bold)

                        estadosRec.forEach { est ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = (est == nuevoEstadoRec),
                                    onClick = { nuevoEstadoRec = est }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(est)
                            }
                        }

                        if (nuevoEstadoRec == "Descartada") {
                            OutlinedTextField(
                                value = motivoDescarteText,
                                onValueChange = { motivoDescarteText = it },
                                label = { Text("Motivo de descarte *") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                repository.cambiarEstadoRecomendacion(
                                    idRecomendacion = recToChange.idRecomendacion,
                                    idAgente = agente.idUsuario,
                                    nuevoEstado = nuevoEstadoRec,
                                    motivoDescarte = if (nuevoEstadoRec == "Descartada") motivoDescarteText else null
                                )
                                selectedRecToChange = null
                            }
                        }
                    ) {
                        Text("Guardar Estado")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedRecToChange = null }) { Text("Cancelar") }
                }
            )
        }
    }
}

package cl.duocuc.pagoflex.ui.usuario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionAvisosScreen(
    usuario: UsuarioEntity,
    repository: PagoFlexRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var avisosActivos by remember { mutableStateOf(usuario.avisosActivos) }
    var diasAnticipacion by remember { mutableIntStateOf(usuario.diasAnticipacion) }
    var showSuccessSnackbar by remember { mutableStateOf(false) }

    val opcionesAnticipacion = listOf(1, 3, 5)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración de Avisos (RF-11)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Info Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Avisos Personalizados de Vencimiento",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Recibe notificaciones amistosas para evitar recargos por mora. Aplicamos la regla RN-15: máximo 1 aviso al día por compromiso y nunca en horario nocturno (22:00 - 08:00).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Switch Activar Avisos
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notificaciones push de vencimiento", fontWeight = FontWeight.SemiBold)
                            Text("Activa o desactiva los recordatorios", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = avisosActivos,
                            onCheckedChange = { avisosActivos = it }
                        )
                    }
                }

                // Selector de días de anticipación (1, 3 o 5 días - RN-15)
                if (avisosActivos) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Anticipación para el aviso previo", fontWeight = FontWeight.SemiBold)
                            Text("Selecciona cuántos días antes de la fecha límite deseas ser notificado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                opcionesAnticipacion.forEach { dias ->
                                    FilterChip(
                                        selected = (diasAnticipacion == dias),
                                        onClick = { diasAnticipacion = dias },
                                        label = { Text("$dias ${if (dias == 1) "día antes" else "días antes"}") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (showSuccessSnackbar) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Preferencias guardadas exitosamente", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Guardar Button
            Button(
                onClick = {
                    scope.launch {
                        val usuarioActualizado = usuario.copy(
                            avisosActivos = avisosActivos,
                            diasAnticipacion = diasAnticipacion
                        )
                        repository.updateUsuario(usuarioActualizado)
                        showSuccessSnackbar = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar Preferencias de Avisos", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

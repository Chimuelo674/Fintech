package cl.duocuc.pagoflex.ui.usuario

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.RecomendacionEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecomendarOrganizacionScreen(
    usuario: UsuarioEntity,
    repository: PagoFlexRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val recomendaciones by repository.getRecomendacionesByUsuario(usuario.idUsuario).collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }

    var nombreOrg by remember { mutableStateOf("") }
    var rutOrg by remember { mutableStateOf("") }
    var rubro by remember { mutableStateOf("") }
    var comuna by remember { mutableStateOf("") }
    var nombreContacto by remember { mutableStateOf("") }
    var correoContacto by remember { mutableStateOf("") }
    var telefonoContacto by remember { mutableStateOf("") }
    var autorizaContacto by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recomendar Organización (RF-12)", fontWeight = FontWeight.Bold) },
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
                text = { Text("Recomendar") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Programa de Referidos de Organizaciones",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "¿Tienes otros compromisos de pago (gastos comunes, jardín infantil, club, etc.) que aún no pagan por PagoFlex? Recomiéndanos y nuestro equipo comercial los contactará.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recomendaciones.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aún no has recomendado organizaciones.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recomendaciones) { rec ->
                        RecomendacionCardItem(recomendacion = rec)
                    }
                }
            }
        }

        // Dialog Formulario
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Recomendar Organización", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = nombreOrg,
                            onValueChange = { nombreOrg = it },
                            label = { Text("Nombre de la organización *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = rubro,
                            onValueChange = { rubro = it },
                            label = { Text("Rubro (ej. Jardín infantil, Condominio)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = comuna,
                            onValueChange = { comuna = it },
                            label = { Text("Comuna") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = nombreContacto,
                            onValueChange = { nombreContacto = it },
                            label = { Text("Nombre de contacto (Opcional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = correoContacto,
                            onValueChange = { correoContacto = it },
                            label = { Text("Correo o Teléfono de contacto") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = autorizaContacto,
                                onCheckedChange = { autorizaContacto = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Autorizo contacto mencionando mi nombre (RN-12)", fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (nombreOrg.isNotBlank()) {
                                scope.launch {
                                    repository.crearRecomendacion(
                                        idUsuario = usuario.idUsuario,
                                        nombreOrg = nombreOrg,
                                        rutOrg = rutOrg.ifBlank { null },
                                        rubro = rubro.ifBlank { null },
                                        comuna = comuna.ifBlank { null },
                                        nombreContacto = nombreContacto.ifBlank { null },
                                        correoContacto = correoContacto.ifBlank { null },
                                        telefonoContacto = telefonoContacto.ifBlank { null },
                                        autorizaContacto = autorizaContacto
                                    )
                                    showDialog = false
                                    nombreOrg = ""
                                    rubro = ""
                                    comuna = ""
                                    nombreContacto = ""
                                    correoContacto = ""
                                }
                            }
                        }
                    ) {
                        Text("Enviar Recomendación")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
fun RecomendacionCardItem(recomendacion: RecomendacionEntity) {
    val (statusBg, statusFg) = when (recomendacion.estado) {
        "Recibida" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        "En evaluación" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "Contactada" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "Se unió" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
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
                Text(recomendacion.nombreOrganizacion, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                    Text(
                        text = recomendacion.estado,
                        color = statusFg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Rubro: ${recomendacion.rubro ?: "No especificado"} • Comuna: ${recomendacion.comuna ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
            Text("Fecha enviada: ${recomendacion.fecha}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

            if (!recomendacion.motivoDescarte.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Motivo: ${recomendacion.motivoDescarte}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

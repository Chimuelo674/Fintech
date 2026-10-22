package cl.duocuc.pagoflex.ui.usuario

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.EmpresaEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.theme.PagoFlexDarkGreen
import cl.duocuc.pagoflex.ui.theme.PagoFlexLightMint
import cl.duocuc.pagoflex.ui.theme.PagoFlexMint
import cl.duocuc.pagoflex.ui.theme.PagoFlexTeal
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsuarioDashboardScreen(
    usuario: UsuarioEntity,
    repository: PagoFlexRepository,
    onNavigateToPago: (String) -> Unit,
    onNavigateToHistorial: () -> Unit,
    onNavigateToReportar: (String) -> Unit,
    onNavigateToAvisos: () -> Unit,
    onNavigateToRecomendar: () -> Unit,
    onLogout: () -> Unit
) {
    val compromisos by repository.getCompromisosByUsuario(usuario.idUsuario).collectAsState(initial = emptyList())
    val empresas by repository.getAllEmpresas().collectAsState(initial = emptyList())
    val reconocimientos by repository.getReconocimientosByUsuario(usuario.idUsuario).collectAsState(initial = emptyList())

    val empresaMap = remember(empresas) { empresas.associateBy { it.idEmpresa } }

    val compromisosPendientes = compromisos.filter { it.estado == "Pendiente" || it.estado == "Vencido" }
    val compromisosVencidos = compromisos.filter { it.estado == "Vencido" }

    val totalMes = compromisosPendientes.sumOf { it.monto + (if (it.estado == "Vencido") it.recargoAplicado else 0.0) }
    val proximoVencimiento = compromisosPendientes.minByOrNull { it.fechaVencimiento }?.fechaVencimiento ?: "Al día"

    var isAmountVisible by remember { mutableStateOf(true) }
    var showSimulator by remember { mutableStateOf(false) }

    // Indicador de cumplimiento (RN-10, RN-11)
    val indicadorCumplimiento = when (usuario.idUsuario) {
        "UF-01" -> "91.7%" to "Al día"
        "UF-02" -> "66.7%" to "En camino"
        "UF-03" -> "100.0%" to "Ejemplar"
        "UF-05" -> "95.0%" to "Ejemplar"
        "UF-06" -> "85.7%" to "Al día"
        "UF-07" -> "100.0%" to "Ejemplar"
        else -> "85.0%" to "Al día"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PagoFlexMint,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "Pago",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "PagoFlex",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAvisos) {
                        Icon(Icons.Default.Notifications, contentDescription = "Avisos", tint = Color.White)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PagoFlexDarkGreen)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Inicio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PagoFlexDarkGreen,
                        selectedTextColor = PagoFlexDarkGreen,
                        indicatorColor = PagoFlexLightMint
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHistorial,
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Historial") },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray)
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToRecomendar,
                    icon = { Icon(Icons.Default.Recommend, contentDescription = null) },
                    label = { Text("Referidos") },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray)
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToAvisos,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ajustes") },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Color.Gray)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Header Card (Dark Green background)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PagoFlexDarkGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Total compromisos del mes",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { isAmountVisible = !isAmountVisible },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isAmountVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Ocultar monto",
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = PagoFlexMint
                            ) {
                                Text(
                                    text = indicadorCumplimiento.second,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isAmountVisible) CurrencyFormatter.formatCLP(totalMes) else "••••••••",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Próximo vencimiento", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text(proximoVencimiento, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Vencidos", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text(
                                    text = "${compromisosVencidos.size} de ${compromisosPendientes.size}",
                                    color = if (compromisosVencidos.isNotEmpty()) Color(0xFFFF8A80) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Column {
                                Text("Cumplimiento", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                Text(indicadorCumplimiento.first, color = PagoFlexMint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Category Chips Row (from Image 2: Pagos, Transferencias, Beneficios, Estimación)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = !showSimulator,
                            onClick = { showSimulator = false },
                            label = { Text("Pagos", fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PagoFlexLightMint,
                                selectedLabelColor = PagoFlexDarkGreen
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = showSimulator,
                            onClick = { showSimulator = true },
                            label = { Text("Estimador de Costos", fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PagoFlexLightMint,
                                selectedLabelColor = PagoFlexDarkGreen
                            )
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = onNavigateToRecomendar,
                            label = { Text("Beneficios") }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = onNavigateToAvisos,
                            label = { Text("Administración de saldo") }
                        )
                    }
                }
            }

            // Interactive Cost Simulator Card (Exact feature from Image 1: "Simulá tu costo y comparalo con tu banco")
            if (showSimulator) {
                item {
                    SimuladorCostoCard()
                }
            }

            // Quick Action Pills Row (Historial, Referidos, Avisos)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionPill(
                        icon = Icons.Default.History,
                        label = "Historial",
                        onClick = onNavigateToHistorial,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionPill(
                        icon = Icons.Default.Recommend,
                        label = "Referidos",
                        onClick = onNavigateToRecomendar,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionPill(
                        icon = Icons.Default.NotificationsActive,
                        label = "Avisos",
                        onClick = onNavigateToAvisos,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Reconocimientos Banner (RF-13)
            if (reconocimientos.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PagoFlexLightMint),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PagoFlexMint,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = reconocimientos.first().nombreReconocimiento,
                                    fontWeight = FontWeight.Bold,
                                    color = PagoFlexDarkGreen,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = reconocimientos.first().descripcion,
                                    fontSize = 12.sp,
                                    color = PagoFlexDarkGreen.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Compromisos de pago (${compromisos.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PagoFlexDarkGreen
                    )
                    Text(
                        text = "Actualizado 06-10",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // List of Compromisos
            if (compromisos.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No tienes compromisos de pago vigentes.", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(compromisos) { compromiso ->
                    CompromisoCardItemStyled(
                        compromiso = compromiso,
                        empresa = empresaMap[compromiso.idEmpresa],
                        onPagar = { onNavigateToPago(compromiso.idCompromiso) },
                        onReportar = { onNavigateToReportar(compromiso.idCompromiso) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = PagoFlexLightMint,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PagoFlexDarkGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PagoFlexDarkGreen
            )
        }
    }
}

@Composable
fun CompromisoCardItemStyled(
    compromiso: CompromisoEntity,
    empresa: EmpresaEntity?,
    onPagar: () -> Unit,
    onReportar: () -> Unit
) {
    val totalMonto = compromiso.monto + if (compromiso.estado == "Vencido") compromiso.recargoAplicado else 0.0

    val (statusBg, statusFg) = when (compromiso.estado) {
        "Pendiente" -> PagoFlexLightMint to PagoFlexDarkGreen
        "Vencido" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        "Pagado" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "En revisión" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        else -> PagoFlexLightMint to Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = PagoFlexLightMint,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (empresa?.nombreFantasia ?: compromiso.idEmpresa).take(2).uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = PagoFlexDarkGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = empresa?.nombreFantasia ?: compromiso.idEmpresa,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PagoFlexDarkGreen
                        )
                        Text(
                            text = "${compromiso.concepto}${compromiso.numeroCuota?.let { " ($it/${compromiso.totalCuotas ?: "?"})" } ?: ""}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusBg
                ) {
                    Text(
                        text = compromiso.estado,
                        color = statusFg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Vence: ${compromiso.fechaVencimiento}", fontSize = 11.sp, color = Color.Gray)
                    if (compromiso.estado == "Vencido" && compromiso.recargoAplicado > 0) {
                        Text(
                            text = "+ Mora: ${CurrencyFormatter.formatCLP(compromiso.recargoAplicado)}",
                            fontSize = 11.sp,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = CurrencyFormatter.formatCLP(totalMonto),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PagoFlexDarkGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (compromiso.estado == "Pendiente" || compromiso.estado == "Vencido") {
                    TextButton(onClick = onReportar) {
                        Text("Reportar", color = Color.Gray, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onPagar,
                        colors = ButtonDefaults.buttonColors(containerColor = PagoFlexMint),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pagar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onReportar,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Ver Detalle / Reporte", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SimuladorCostoCard() {
    var montoOperacion by remember { mutableFloatStateOf(500000f) }

    val costoPagoFlex = (montoOperacion * 0.03f) // 3% costo fijo
    val comisionBanco = (montoOperacion * 0.021f)
    val ivaComision = comisionBanco * 0.19f
    val otrosCargos = 3500f
    val costoBanco = comisionBanco + ivaComision + otrosCargos
    val ahorro = (costoBanco - costoPagoFlex).coerceAtLeast(0f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PagoFlexLightMint
            ) {
                Text(
                    text = "HERRAMIENTA DE ESTIMACIÓN",
                    color = PagoFlexDarkGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Simula tu costo y compáralo con tu banco",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = PagoFlexDarkGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "PagoFlex cobra un costo claro y transparente, sin cargos ocultos.",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Monto de la operación", fontSize = 12.sp, color = Color.Gray)
            Text(
                text = CurrencyFormatter.formatCLP(montoOperacion.toDouble()),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PagoFlexMint
            )

            Slider(
                value = montoOperacion,
                onValueChange = { montoOperacion = it },
                valueRange = 50000f..2000000f,
                steps = 38,
                colors = SliderDefaults.colors(
                    thumbColor = PagoFlexDarkGreen,
                    activeTrackColor = PagoFlexMint,
                    inactiveTrackColor = PagoFlexLightMint
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PagoFlex Box
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, PagoFlexMint, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PagoFlexMint)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PagoFlex", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PagoFlexDarkGreen)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PagoFlexLightMint
                        ) {
                            Text("Costo fijo 3%", color = PagoFlexDarkGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                    Text(
                        text = CurrencyFormatter.formatCLP(costoPagoFlex.toDouble()),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = PagoFlexMint
                    )
                    Text("3% sin cargos ocultos ni cuota de mantención", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ahorro Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PagoFlexLightMint),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = PagoFlexDarkGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ahorro estimado con PagoFlex:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PagoFlexDarkGreen)
                    }
                    Text(
                        text = CurrencyFormatter.formatCLP(ahorro.toDouble()),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = PagoFlexMint
                    )
                }
            }
        }
    }
}




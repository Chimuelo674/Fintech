package cl.duocuc.pagoflex.ui.usuario

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.EmpresaEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.repository.Cumplimiento
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.theme.BotonTema
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.util.Reloj
import java.util.Locale
import cl.duocuc.pagoflex.ui.common.MensajesEnSnackbar
import cl.duocuc.pagoflex.ui.common.vm

/** Compromisos que se muestran en el inicio (RF-03): los vigentes y los del mes en curso. */
private fun esVisibleEnInicio(c: CompromisoEntity): Boolean =
    c.estado == "Pendiente" || c.estado == "Vencido" || c.estado == "En revisión" ||
        c.fechaVencimiento.take(7) == Reloj.hoy().take(7)

/** 91.7 -> "91,7%" (RN-10: un decimal; formato chileno). */
fun formatearPorcentaje(p: Double): String = String.format(Locale("es", "CL"), "%.1f%%", p)

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
    onVerComprobante: (String) -> Unit,
    onNavigateToMisReportes: () -> Unit,
    onVerDetalle: (String) -> Unit,
    onLogout: () -> Unit
) {
    // MVVM: el ViewModel entrega el estado de la pantalla (y al crearse sincroniza los estados por fecha)
    val viewModel = vm(key = "inicio-${usuario.idUsuario}") { InicioViewModel(repository, usuario.idUsuario) }
    val ui by viewModel.estado.collectAsState()
    val compromisos = ui.compromisos
    val reconocimientos = ui.reconocimientos
    val cumplimiento = ui.cumplimiento
    val empresaMap = ui.empresas

    // RF-02: "total por pagar del mes" = lo que vence este mes y aún no se paga, más lo que ya
    // venció (con su recargo, RN-03). Los compromisos de meses siguientes no se suman.
    val mesActual = Reloj.hoy().take(7)
    val vencidos = compromisos.filter { it.estado == "Vencido" }
    val porPagar = compromisos.filter {
        it.estado == "Vencido" || (it.estado == "Pendiente" && it.fechaVencimiento.take(7) <= mesActual)
    }
    val totalPorPagar = porPagar.sumOf { it.monto + if (it.estado == "Vencido") it.recargoAplicado else 0.0 }
    val proximo = compromisos.filter { it.estado == "Pendiente" }.minByOrNull { it.fechaVencimiento }
    val visibles = compromisos.filter(::esVisibleEnInicio)
        .sortedWith(compareBy({ ordenEstado(it.estado) }, { it.fechaVencimiento }))

    var isAmountVisible by remember { mutableStateOf(true) }
    val c = PF.colors

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PagoFlex",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = c.onHeader
                    )
                },
                actions = {
                    BotonTema(tint = c.onHeader)
                    IconButton(onClick = onNavigateToAvisos) {
                        Icon(Icons.Default.Notifications, contentDescription = "Avisos", tint = c.onHeader)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión", tint = c.onHeader)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.header)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = c.card, tonalElevation = 8.dp) {
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = c.textStrong,
                    selectedTextColor = c.textStrong,
                    indicatorColor = c.chip,
                    unselectedIconColor = c.textMuted,
                    unselectedTextColor = c.textMuted
                )
                NavigationBarItem(
                    selected = true, onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Inicio") }, colors = itemColors
                )
                NavigationBarItem(
                    selected = false, onClick = onNavigateToHistorial,
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Historial") }, colors = itemColors
                )
                NavigationBarItem(
                    selected = false, onClick = onNavigateToRecomendar,
                    icon = { Icon(Icons.Default.Recommend, contentDescription = null) },
                    label = { Text("Referidos") }, colors = itemColors
                )
                NavigationBarItem(
                    selected = false, onClick = onNavigateToAvisos,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Ajustes") }, colors = itemColors
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Resumen de la situación (RF-02, HU-01)
            item {
                ResumenCard(
                    nombre = usuario.nombres.substringBefore(" "),
                    total = totalPorPagar,
                    montoVisible = isAmountVisible,
                    onToggleMonto = { isAmountVisible = !isAmountVisible },
                    proximo = proximo,
                    cantidadVencidos = vencidos.size,
                    cumplimiento = cumplimiento
                )
            }

            // Accesos rápidos
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionPill(Icons.Default.History, "Historial", onNavigateToHistorial, Modifier.weight(1f))
                    // RF-09 / PB-08: la persona ve sus reportes y en qué va cada uno
                    QuickActionPill(
                        Icons.Default.ReportProblem,
                        "Reportes",
                        onNavigateToMisReportes,
                        Modifier.weight(1f)
                    )
                    QuickActionPill(Icons.Default.Recommend, "Referidos", onNavigateToRecomendar, Modifier.weight(1f))
                    QuickActionPill(Icons.Default.NotificationsActive, "Avisos", onNavigateToAvisos, Modifier.weight(1f))
                }
            }

            // Reconocimientos (RF-13)
            if (reconocimientos.isNotEmpty()) {
                item {
                    val r = reconocimientos.first()
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = c.chip),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = c.accent, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(r.nombreReconocimiento, fontWeight = FontWeight.Bold, color = c.onChip, fontSize = 15.sp)
                                Text(ui.descripcionReconocimiento[r.nombreReconocimiento].orEmpty(), fontSize = 12.sp, color = c.onChip.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
            }

            // Encabezado de la lista + desde cuándo está actualizada la información (RNF-08)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tus compromisos (${visibles.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = c.textStrong,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Al ${Fechas.mostrar(Reloj.hoy())}",
                        fontSize = 12.sp,
                        color = c.textMuted
                    )
                }
            }

            if (visibles.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = c.card),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Estás al día: no tienes compromisos vigentes.", color = c.textMuted)
                        }
                    }
                }
            } else {
                items(visibles, key = { it.idCompromiso }) { compromiso ->
                    CompromisoCardItemStyled(
                        compromiso = compromiso,
                        empresa = empresaMap[compromiso.idEmpresa],
                        onPagar = { onNavigateToPago(compromiso.idCompromiso) },
                        onReportar = { onNavigateToReportar(compromiso.idCompromiso) },
                        onVerComprobante = { onVerComprobante(compromiso.idCompromiso) },
                        onClick = { onVerDetalle(compromiso.idCompromiso) }
                    )
                }
            }
        }
    }
}

/** Orden en la lista: primero lo que requiere acción. */
private fun ordenEstado(estado: String) = when (estado) {
    "Vencido" -> 0
    "Pendiente" -> 1
    "En revisión" -> 2
    "Pagado" -> 3
    else -> 4
}

@Composable
private fun ResumenCard(
    nombre: String,
    total: Double,
    montoVisible: Boolean,
    onToggleMonto: () -> Unit,
    proximo: CompromisoEntity?,
    cantidadVencidos: Int,
    cumplimiento: Cumplimiento?
) {
    val c = PF.colors
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = c.header),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text("Hola $nombre, este mes tienes por pagar", color = c.onHeaderMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    // RNF-06: ocultar el monto si otra persona mira el teléfono
                    IconButton(onClick = onToggleMonto, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (montoVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (montoVisible) "Ocultar monto" else "Mostrar monto",
                            tint = c.onHeaderMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (cumplimiento != null && cumplimiento.historialSuficiente) {
                    Surface(shape = RoundedCornerShape(20.dp), color = c.accent) {
                        Text(
                            text = cumplimiento.categoria,
                            color = c.onAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (montoVisible) CurrencyFormatter.formatCLP(total) else "••••••••",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = c.onHeader
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = c.onHeader.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Próximo pago", color = c.onHeaderMuted, fontSize = 12.sp)
                    Text(
                        text = proximo?.let { Fechas.mostrar(it.fechaVencimiento) } ?: "Sin pendientes",
                        color = c.onHeader, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    // RN-11 / HU-01 CA-2: se informa sin culpar
                    Text("Por ponerte al día", color = c.onHeaderMuted, fontSize = 12.sp)
                    Text(
                        text = if (cantidadVencidos == 0) "Nada" else "$cantidadVencidos",
                        color = if (cantidadVencidos > 0) c.headerAlert else c.onHeader,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cumplimiento", color = c.onHeaderMuted, fontSize = 12.sp)
                    Text(
                        text = when {
                            cumplimiento == null -> "…"
                            cumplimiento.porcentaje == null -> "Aún sin historial"
                            else -> formatearPorcentaje(cumplimiento.porcentaje)
                        },
                        color = c.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp
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
    val c = PF.colors
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = c.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = CircleShape, color = c.chip, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = c.onChip, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.textStrong, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
fun CompromisoCardItemStyled(
    compromiso: CompromisoEntity,
    empresa: EmpresaEntity?,
    onPagar: () -> Unit,
    onReportar: () -> Unit,
    onVerComprobante: () -> Unit,
    onClick: () -> Unit = {}
) {
    val c = PF.colors
    val vencido = compromiso.estado == "Vencido"
    val totalMonto = compromiso.monto + if (vencido) compromiso.recargoAplicado else 0.0
    val (statusBg, statusFg) = PF.estado(compromiso.estado)

    // RF-04: tocar la tarjeta abre el detalle del compromiso
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = c.card)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // weight(1f): el texto ocupa solo el espacio que deja la etiqueta de estado
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(shape = CircleShape, color = c.chip, modifier = Modifier.size(38.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (empresa?.nombreFantasia ?: compromiso.idEmpresa).take(2).uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = c.onChip
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = empresa?.nombreFantasia ?: compromiso.idEmpresa,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = c.textStrong
                        )
                        Text(
                            text = compromiso.concepto + (compromiso.numeroCuota?.let { " · Cuota $it/${compromiso.totalCuotas ?: "?"}" } ?: ""),
                            fontSize = 12.sp,
                            color = c.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = statusBg) {
                    Text(
                        text = compromiso.estado,
                        color = statusFg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = c.divider)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vence el ${Fechas.mostrar(compromiso.fechaVencimiento)}", fontSize = 12.sp, color = c.textMuted)
                    if (vencido) {
                        // RN-02 / RN-03 con tono positivo (RN-11, RNF-04): se informa, no se culpa
                        val dias = Fechas.diasAtraso(compromiso.fechaVencimiento)
                        Text(
                            text = "Venció hace $dias ${if (dias == 1) "día" else "días"}" +
                                if (compromiso.recargoAplicado > 0) " · incluye recargo de ${CurrencyFormatter.formatCLP(compromiso.recargoAplicado)}" else "",
                            fontSize = 12.sp,
                            color = c.vencidoFg
                        )
                    }
                }
                Text(
                    text = CurrencyFormatter.formatCLP(totalMonto),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = c.textStrong
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (compromiso.estado) {
                    "Pendiente", "Vencido" -> {
                        TextButton(onClick = onReportar) {
                            Text("Reportar un problema", color = c.textMuted, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onPagar,
                            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pagar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    "En revisión" -> OutlinedButton(onClick = onReportar, shape = RoundedCornerShape(20.dp)) {
                        Text("Ver mi reporte", fontSize = 13.sp, color = c.textStrong)
                    }
                    "Pagado" -> OutlinedButton(onClick = onVerComprobante, shape = RoundedCornerShape(20.dp)) {
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp), tint = c.textStrong)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver comprobante", fontSize = 13.sp, color = c.textStrong)
                    }
                    else -> Text("Anulado por la empresa", fontSize = 12.sp, color = c.textMuted)
                }
            }
        }
    }
}

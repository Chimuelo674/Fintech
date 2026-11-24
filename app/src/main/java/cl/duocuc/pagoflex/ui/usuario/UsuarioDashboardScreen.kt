package cl.duocuc.pagoflex.ui.usuario

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.EmpresaEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.data.remote.ClienteHttp
import cl.duocuc.pagoflex.data.remote.EstadoConexion
import cl.duocuc.pagoflex.data.remote.Indicador
import cl.duocuc.pagoflex.domain.ReglasAcceso
import cl.duocuc.pagoflex.repository.Cumplimiento
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.theme.BotonTema
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.util.Reloj
import java.util.Calendar
import java.util.Locale
import cl.duocuc.pagoflex.ui.common.vm

private fun esVisibleEnInicio(c: CompromisoEntity): Boolean =
    c.estado == "Pendiente" || c.estado == "Vencido" || c.estado == "En revisión" ||
        c.fechaVencimiento.take(7) == Reloj.hoy().take(7)

fun formatearPorcentaje(p: Double): String = String.format(Locale("es", "CL"), "%.1f%%", p)

private const val MOVIMIENTOS_EN_INICIO = 5

/**
 * Inicio estilo app de banco (inspirado en BancoEstado): encabezado con saludo,
 * tarjeta tipo "cuenta" con el total por pagar, accesos rápidos en círculos,
 * compromisos vigentes y últimos movimientos.
 */
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
    onNavigateToCobroQr: () -> Unit,
    onNavigateToSimulador: () -> Unit,
    onLogout: () -> Unit
) {
    val viewModel = vm(key = "inicio-${usuario.idUsuario}") { InicioViewModel(repository, usuario.idUsuario) }
    val ui by viewModel.estado.collectAsState()
    val conexion by viewModel.estadoConexion.collectAsState()
    val sincronizando by viewModel.sincronizando.collectAsState()
    val indicadoresVm = vm(key = "indicadores") { IndicadoresViewModel(ClienteHttp.indicadoresRepository) }
    val indicadores by indicadoresVm.estado.collectAsState()
    val compromisos = ui.compromisos
    val empresaMap = ui.empresas
    val compromisoMap = remember(compromisos) { compromisos.associateBy { it.idCompromiso } }

    val mesActual = Reloj.hoy().take(7)
    val vencidos = compromisos.filter { it.estado == "Vencido" }
    val porPagar = compromisos.filter {
        it.estado == "Vencido" || (it.estado == "Pendiente" && it.fechaVencimiento.take(7) <= mesActual)
    }
    val totalPorPagar = porPagar.sumOf { it.monto + if (it.estado == "Vencido") it.recargoAplicado else 0.0 }
    val proximo = compromisos.filter { it.estado == "Pendiente" }.minByOrNull { it.fechaVencimiento }
    // El primero que conviene pagar: lo vencido antes que lo pendiente.
    val prioritario = porPagar.sortedWith(compareBy({ ordenEstado(it.estado) }, { it.fechaVencimiento })).firstOrNull()
    val visibles = compromisos.filter(::esVisibleEnInicio)
        .sortedWith(compareBy({ ordenEstado(it.estado) }, { it.fechaVencimiento }))

    var montoVisible by remember { mutableStateOf(true) }
    val saludo = remember { ReglasAcceso.saludo(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    val c = PF.colors

    Scaffold(
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
                    selected = false, onClick = onNavigateToCobroQr,
                    icon = { Icon(Icons.Default.QrCode2, contentDescription = null) },
                    label = { Text("QR") }, colors = itemColors
                )
                NavigationBarItem(
                    selected = false, onClick = onNavigateToHistorial,
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null) },
                    label = { Text("Movimientos") }, colors = itemColors
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
                .padding(bottom = paddingValues.calculateBottomPadding()),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---------- Encabezado con saludo y tarjeta de cuenta ----------
            item {
                EncabezadoBanco(
                    usuario = usuario,
                    saludo = saludo,
                    total = totalPorPagar,
                    montoVisible = montoVisible,
                    onToggleMonto = { montoVisible = !montoVisible },
                    proximo = proximo,
                    cantidadVencidos = vencidos.size,
                    cumplimiento = ui.cumplimiento,
                    onPagarAhora = prioritario?.let { p -> { onNavigateToPago(p.idCompromiso) } },
                    onAvisos = onNavigateToAvisos,
                    onLogout = onLogout
                )
            }

            // ---------- Conexión con los microservicios ----------
            item {
                FranjaConexion(
                    conexion = conexion,
                    sincronizando = sincronizando,
                    onSincronizar = viewModel::sincronizar
                )
            }

            // ---------- Accesos rápidos ----------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AccesoRapido(Icons.Default.QrCode2, "QR y link\nde pago", onNavigateToCobroQr)
                    AccesoRapido(Icons.Default.Calculate, "Simulador\nde costos", onNavigateToSimulador)
                    AccesoRapido(Icons.Default.ReportProblem, "Mis\nreportes", onNavigateToMisReportes)
                    AccesoRapido(Icons.Default.Recommend, "Recomendar\norganización", onNavigateToRecomendar)
                }
            }

            // ---------- Indicadores económicos (API externa mindicador.cl) ----------
            item {
                TarjetaIndicadores(
                    estado = indicadores,
                    onReintentar = indicadoresVm::recargar
                )
            }

            // ---------- Reconocimiento ----------
            if (ui.reconocimientos.isNotEmpty()) {
                item {
                    val r = ui.reconocimientos.first()
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = c.chip),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
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

            // ---------- Banner promocional (como los de las apps de banco) ----------
            item {
                BannerSimulador(onClick = onNavigateToSimulador)
            }

            // ---------- Compromisos ----------
            item {
                TituloSeccion(
                    titulo = "Tus compromisos (${visibles.size})",
                    detalle = "Al ${Fechas.mostrar(Reloj.hoy())}"
                )
            }

            if (visibles.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = c.card),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Estás al día: no tienes compromisos vigentes.", color = c.textMuted)
                        }
                    }
                }
            } else {
                items(visibles, key = { it.idCompromiso }) { compromiso ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
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

            // ---------- Últimos movimientos ----------
            item {
                TituloSeccion(
                    titulo = "Últimos movimientos",
                    accion = if (ui.pagos.isNotEmpty()) "Ver todos" else null,
                    onAccion = onNavigateToHistorial
                )
            }
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = c.card),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    val ultimos = ui.pagos.take(MOVIMIENTOS_EN_INICIO)
                    if (ultimos.isEmpty()) {
                        Text(
                            "Aún no tienes pagos registrados.",
                            color = c.textMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(20.dp)
                        )
                    } else {
                        ultimos.forEachIndexed { i, pago ->
                            val compromiso = compromisoMap[pago.idCompromiso]
                            MovimientoFila(
                                pago = pago,
                                compromiso = compromiso,
                                empresa = compromiso?.let { empresaMap[it.idEmpresa] },
                                onClick = { onVerComprobante(pago.numeroComprobante) }
                            )
                            if (i < ultimos.lastIndex) {
                                HorizontalDivider(color = c.divider, modifier = Modifier.padding(start = 68.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EncabezadoBanco(
    usuario: UsuarioEntity,
    saludo: String,
    total: Double,
    montoVisible: Boolean,
    onToggleMonto: () -> Unit,
    proximo: CompromisoEntity?,
    cantidadVencidos: Int,
    cumplimiento: Cumplimiento?,
    onPagarAhora: (() -> Unit)?,
    onAvisos: () -> Unit,
    onLogout: () -> Unit
) {
    val c = PF.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.header, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 20.dp)
    ) {
        Column {
            // Saludo, avatar y acciones
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = c.accent, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            usuario.nombres.take(1) + usuario.apellidos.take(1),
                            color = c.onAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("$saludo,", color = c.onHeaderMuted, fontSize = 13.sp)
                    Text(
                        usuario.nombres.substringBefore(" "),
                        color = c.onHeader,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BotonTema(tint = c.onHeader)
                IconButton(onClick = onAvisos) {
                    Icon(Icons.Default.Notifications, contentDescription = "Avisos", tint = c.onHeader)
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión", tint = c.onHeader)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tarjeta tipo "cuenta"
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = c.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cuenta PagoFlex", color = c.textStrong, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("RUT ${ReglasAcceso.enmascararRut(usuario.rut)}", color = c.textMuted, fontSize = 12.sp)
                        }
                        if (cumplimiento != null && cumplimiento.historialSuficiente) {
                            Surface(shape = RoundedCornerShape(20.dp), color = c.chip) {
                                Text(
                                    text = cumplimiento.categoria,
                                    color = c.onChip,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Total por pagar este mes", color = c.textMuted, fontSize = 13.sp)
                        IconButton(onClick = onToggleMonto, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = if (montoVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (montoVisible) "Ocultar monto" else "Mostrar monto",
                                tint = c.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = if (montoVisible) CurrencyFormatter.formatCLP(total) else "$ ••••••",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = c.textStrong
                    )

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = c.divider)
                    Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        DatoCuenta(
                            "Próximo pago",
                            proximo?.let { Fechas.mostrar(it.fechaVencimiento) } ?: "Sin pendientes",
                            Modifier.weight(1f)
                        )
                        DatoCuenta(
                            "Atrasados",
                            if (cantidadVencidos == 0) "Ninguno" else "$cantidadVencidos",
                            Modifier.weight(1f),
                            alerta = cantidadVencidos > 0
                        )
                        DatoCuenta(
                            "Cumplimiento",
                            when {
                                cumplimiento == null -> "…"
                                cumplimiento.porcentaje == null -> "Sin historial"
                                else -> formatearPorcentaje(cumplimiento.porcentaje)
                            },
                            Modifier.weight(1f)
                        )
                    }

                    if (onPagarAhora != null) {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onPagarAhora,
                            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (cantidadVencidos > 0) "Ponerme al día" else "Pagar ahora",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Muestra si la app está conectada a los microservicios, con un botón para sincronizar. */
@Composable
private fun FranjaConexion(
    conexion: EstadoConexion,
    sincronizando: Boolean,
    onSincronizar: () -> Unit
) {
    val c = PF.colors
    val (color, texto) = when {
        sincronizando || conexion is EstadoConexion.Sincronizando -> c.textMuted to "Sincronizando con el servidor…"
        conexion is EstadoConexion.Conectado -> c.pagadoFg to "Conectado al servidor PagoFlex"
        conexion is EstadoConexion.SinConexion -> c.revisionFg to "Sin conexión: usando los datos del teléfono"
        else -> c.textMuted to "Conectando con el servidor…"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(texto, color = color, fontSize = 12.sp, modifier = Modifier.weight(1f))
        if (sincronizando || conexion is EstadoConexion.Sincronizando) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(12.dp).size(18.dp))
        } else {
            IconButton(onClick = onSincronizar) {
                Icon(Icons.Default.Sync, contentDescription = "Sincronizar con el servidor", tint = c.textMuted)
            }
        }
    }
}

/** Indicadores económicos del día desde la API externa mindicador.cl. */
@Composable
private fun TarjetaIndicadores(
    estado: IndicadoresViewModel.Estado,
    onReintentar: () -> Unit
) {
    val c = PF.colors
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = c.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Indicadores de hoy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = c.textStrong,
                    modifier = Modifier.weight(1f)
                )
                Text("mindicador.cl", fontSize = 11.sp, color = c.textMuted)
            }
            Spacer(Modifier.height(12.dp))
            when {
                estado.cargando && estado.indicadores.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
                estado.indicadores.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        estado.error ?: "No hay indicadores disponibles.",
                        fontSize = 12.sp,
                        color = c.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onReintentar) {
                        Text("Reintentar", color = c.accent, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        estado.indicadores.forEach { ind ->
                            IndicadorChip(ind, Modifier.weight(1f))
                        }
                    }
                    estado.indicadores.firstOrNull()?.fecha?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            "Valores al ${Fechas.mostrar(it)}",
                            fontSize = 11.sp,
                            color = c.textMuted,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IndicadorChip(indicador: Indicador, modifier: Modifier = Modifier) {
    val c = PF.colors
    Surface(shape = RoundedCornerShape(12.dp), color = c.chip, modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
            Text(indicador.nombre, color = c.onChip, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(
                indicador.valorTexto.substringBefore(","),
                color = c.textStrong,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DatoCuenta(titulo: String, valor: String, modifier: Modifier = Modifier, alerta: Boolean = false) {
    val c = PF.colors
    Column(modifier = modifier) {
        Text(titulo, color = c.textMuted, fontSize = 12.sp)
        Text(
            valor,
            color = if (alerta) c.vencidoFg else c.textStrong,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

/** Acceso rápido redondo, como los íconos del inicio de BancoEstado. */
@Composable
private fun AccesoRapido(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = PF.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(84.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = c.card,
            shadowElevation = 3.dp,
            modifier = Modifier.size(58.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.textStrong,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BannerSimulador(onClick: () -> Unit) {
    val c = PF.colors
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = c.header),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("¿Tienes un negocio?", color = c.onHeader, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Simula cuánto ahorras cobrando con PagoFlex: 3% fijo, sin mantención.",
                    color = c.onHeaderMuted,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Surface(shape = CircleShape, color = c.accent, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Abrir simulador", tint = c.onAccent)
                }
            }
        }
    }
}

@Composable
private fun TituloSeccion(
    titulo: String,
    detalle: String? = null,
    accion: String? = null,
    onAccion: () -> Unit = {}
) {
    val c = PF.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titulo,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = c.textStrong,
            modifier = Modifier.weight(1f)
        )
        if (detalle != null) {
            Text(detalle, fontSize = 12.sp, color = c.textMuted, modifier = Modifier.padding(end = 8.dp))
        }
        if (accion != null) {
            TextButton(onClick = onAccion) {
                Text(accion, color = c.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun MovimientoFila(
    pago: PagoEntity,
    compromiso: CompromisoEntity?,
    empresa: EmpresaEntity?,
    onClick: () -> Unit
) {
    val c = PF.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = c.chip, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.NorthEast, contentDescription = null, tint = c.onChip, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                empresa?.nombreFantasia ?: compromiso?.idEmpresa ?: "Pago",
                color = c.textStrong,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${compromiso?.concepto ?: "Pago de compromiso"} · ${Fechas.mostrar(pago.fechaHora)}",
                color = c.textMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "-" + CurrencyFormatter.formatCLP(pago.montoPagado),
            color = c.textStrong,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

private fun ordenEstado(estado: String) = when (estado) {
    "Vencido" -> 0
    "Pendiente" -> 1
    "En revisión" -> 2
    "Pagado" -> 3
    else -> 4
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

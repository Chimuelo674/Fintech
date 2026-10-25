package cl.duocuc.pagoflex.ui.usuario

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.PagoEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.util.Reloj
import cl.duocuc.pagoflex.ui.common.MensajesEnSnackbar
import cl.duocuc.pagoflex.ui.common.vm

/** Una línea del historial: un pago (de cualquier canal) o un compromiso anulado. */
private sealed class Movimiento(val fecha: String, val compromiso: CompromisoEntity?, val idEmpresa: String) {
    class Pagado(val pago: PagoEntity, c: CompromisoEntity?) : Movimiento(pago.fechaHora, c, c?.idEmpresa ?: "")
    class Anulado(c: CompromisoEntity) : Movimiento(c.fechaAnulacion ?: c.fechaVencimiento, c, c.idEmpresa)
}

private val PERIODOS = listOf("Todo", "Este mes", "Últimos 3 meses", "2026", "2025")

/**
 * Historial (RF-07, HU-04): pagos hechos por la app, el portal o el agente conversacional
 * (RN-07), con filtros por empresa y período. Los compromisos anulados aparecen marcados
 * como Anulado, no como deuda ni como pago (HU-04 CA-3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    usuario: UsuarioEntity,
    repository: PagoFlexRepository,
    onVerComprobante: (String) -> Unit,
    onBack: () -> Unit
) {
    val c = PF.colors
    // MVVM: pagos, compromisos y empresas vienen del ViewModel; los filtros son estado de la pantalla
    val viewModel = vm(key = "historial-${usuario.idUsuario}") { HistorialViewModel(repository, usuario.idUsuario) }
    val ui by viewModel.estado.collectAsState()
    val pagos = ui.pagos
    val compromisoMap = ui.compromisos
    val compromisos = compromisoMap.values.toList()
    val empresaMap = ui.empresas

    var empresaFiltro by remember { mutableStateOf<String?>(null) } // null = todas
    var periodo by remember { mutableStateOf(PERIODOS[0]) }

    val movimientos: List<Movimiento> = remember(pagos, compromisos) {
        pagos.map { Movimiento.Pagado(it, compromisoMap[it.idCompromiso]) } +
            compromisos.filter { it.estado == "Anulado" }.map { Movimiento.Anulado(it) }
    }
    val empresasConMovimientos = movimientos.map { it.idEmpresa }.distinct()
        .sortedBy { empresaMap[it]?.nombreFantasia ?: it }

    val hoy = Reloj.hoy()
    val desde3Meses = Fechas.sumarMeses(hoy, -3)
    val visibles = movimientos
        .filter { empresaFiltro == null || it.idEmpresa == empresaFiltro }
        .filter {
            val f = it.fecha.take(10)
            when (periodo) {
                "Este mes" -> f.take(7) == hoy.take(7)
                "Últimos 3 meses" -> f > desde3Meses
                "2026", "2025" -> f.take(4) == periodo
                else -> true
            }
        }
        .sortedByDescending { it.fecha }
    val porMes = visibles.groupBy { it.fecha.take(7) }

    val pagosVisibles = visibles.filterIsInstance<Movimiento.Pagado>()
    val totalPagado = pagosVisibles.sumOf { it.pago.montoPagado }
    // "¿A tiempo?" se calcula: fecha de pago ≤ fecha de vencimiento (RN-10, MER 2.3)
    val aTiempo = pagosVisibles.count { m -> m.compromiso?.let { m.pago.fechaHora.take(10) <= it.fechaVencimiento } ?: true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de pagos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                colors = PF.barra()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Filtro por empresa
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    item { FilterChip(selected = empresaFiltro == null, onClick = { empresaFiltro = null }, label = { Text("Todas") }) }
                    items(empresasConMovimientos) { id ->
                        FilterChip(
                            selected = empresaFiltro == id,
                            onClick = { empresaFiltro = id },
                            label = { Text(empresaMap[id]?.nombreFantasia ?: id) }
                        )
                    }
                }
            }
            // Filtro por período
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(PERIODOS) { p -> FilterChip(selected = periodo == p, onClick = { periodo = p }, label = { Text(p) }) }
                }
            }
            // Resumen de lo filtrado
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = c.chip),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${pagosVisibles.size} ${if (pagosVisibles.size == 1) "pago" else "pagos"}" +
                                    if (pagosVisibles.isNotEmpty()) " · $aTiempo a tiempo" else "",
                                color = c.onChip, fontWeight = FontWeight.SemiBold
                            )
                            Text("Incluye pagos por la app, el portal y el agente conversacional", fontSize = 12.sp, color = c.onChip.copy(alpha = 0.8f))
                        }
                        Text(CurrencyFormatter.formatCLP(totalPagado), color = c.onChip, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }

            if (visibles.isEmpty()) {
                item { Text("No hay movimientos con estos filtros.", color = c.textMuted, modifier = Modifier.padding(24.dp)) }
            }

            porMes.forEach { (mes, lista) ->
                item(key = "mes-$mes") {
                    Text(
                        Fechas.nombreMes(mes),
                        fontWeight = FontWeight.Bold, color = c.textStrong,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                    )
                }
                items(lista, key = { m -> if (m is Movimiento.Pagado) m.pago.idPago else "anulado-${m.compromiso?.idCompromiso}" }) { m ->
                    MovimientoItem(
                        m = m,
                        nombreEmpresa = empresaMap[m.idEmpresa]?.nombreFantasia ?: m.idEmpresa,
                        onClick = { m.compromiso?.let { if (m is Movimiento.Pagado) onVerComprobante(it.idCompromiso) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun MovimientoItem(m: Movimiento, nombreEmpresa: String, onClick: () -> Unit) {
    val c = PF.colors
    val comp = m.compromiso
    val concepto = comp?.let { it.concepto + (it.numeroCuota?.let { n -> " · Cuota $n/${it.totalCuotas}" } ?: "") } ?: ""

    Card(
        colors = CardDefaults.cardColors(containerColor = c.card),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(enabled = m is Movimiento.Pagado, onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = c.chip, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (m is Movimiento.Pagado) Icons.Default.Receipt else Icons.Default.Block,
                        contentDescription = null, tint = c.onChip, modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(nombreEmpresa, fontWeight = FontWeight.Bold, color = c.textStrong)
                if (concepto.isNotBlank()) Text(concepto, fontSize = 12.sp, color = c.textMuted)
                when (m) {
                    is Movimiento.Pagado -> Text(
                        "${Fechas.mostrar(m.pago.fechaHora.take(10))} · ${m.pago.canal}",
                        fontSize = 12.sp, color = c.textMuted
                    )
                    is Movimiento.Anulado -> Text(
                        "Anulado por la empresa el ${Fechas.mostrar(m.fecha)}",
                        fontSize = 12.sp, color = c.textMuted
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                when (m) {
                    is Movimiento.Pagado -> {
                        val aTiempo = comp?.let { m.pago.fechaHora.take(10) <= it.fechaVencimiento } ?: true
                        Text(CurrencyFormatter.formatCLP(m.pago.montoPagado), fontWeight = FontWeight.Bold, color = c.textStrong)
                        val (bg, fg) = if (aTiempo) c.pagadoBg to c.pagadoFg else c.revisionBg to c.revisionFg
                        Surface(shape = RoundedCornerShape(20.dp), color = bg) {
                            Text(
                                if (aTiempo) "A tiempo" else "Con atraso", color = fg, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    is Movimiento.Anulado -> {
                        Text(
                            CurrencyFormatter.formatCLP(comp?.monto ?: 0.0),
                            color = c.textMuted, textDecoration = TextDecoration.LineThrough
                        )
                        val (bg, fg) = PF.estado("Anulado")
                        Surface(shape = RoundedCornerShape(20.dp), color = bg) {
                            Text("Anulado", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

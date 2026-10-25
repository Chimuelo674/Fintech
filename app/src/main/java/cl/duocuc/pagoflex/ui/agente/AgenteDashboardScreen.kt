package cl.duocuc.pagoflex.ui.agente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.*
import cl.duocuc.pagoflex.domain.ReglasRolesInternos
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.theme.BotonTema
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.util.Reloj
import kotlinx.coroutines.launch
import cl.duocuc.pagoflex.ui.common.MensajesEnSnackbar
import cl.duocuc.pagoflex.ui.common.vm

/**
 * Panel del agente PagoFlex (R-02): reportes de problema (RF-14, RF-15) y
 * recomendaciones de organizaciones (RF-16).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgenteDashboardScreen(
    agente: UsuarioEntity,
    repository: PagoFlexRepository,
    onLogout: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val c = PF.colors

    // MVVM: datos y acciones en el ViewModel; pestaña, filtros y diálogos son estado de la pantalla
    val viewModel = vm(key = "agente-${agente.idUsuario}") { AgenteViewModel(repository, agente.idUsuario) }
    MensajesEnSnackbar(viewModel, snackbar)
    val ui by viewModel.estado.collectAsState()
    val reportes = ui.reportes
    val recomendaciones = ui.recomendaciones
    val usuarioMap = ui.usuarios
    val compromisoMap = ui.compromisos
    val empresaMap = ui.empresas
    val historialPorRec = ui.historialPorRec
    fun nombre(id: String?) = id?.let { usuarioMap[it]?.nombreCompleto ?: it } ?: "—"

    // El área comercial parte en Recomendaciones; soporte, en Reportes
    var tab by remember { mutableIntStateOf(if (agente.area == "Comercial") 1 else 0) }
    var reporteAResolver by remember { mutableStateOf<ReporteEntity?>(null) }
    var recADescartar by remember { mutableStateOf<RecomendacionEntity?>(null) }

    val porAtender = reportes.count { it.estado != "Resuelto" }
    val recAbiertas = recomendaciones.count { ReglasRolesInternos.siguientesEstados(it.estado).isNotEmpty() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Panel agente PagoFlex", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${agente.nombreCompleto} · ${agente.area ?: "Soporte"}", fontSize = 12.sp, color = c.onHeaderMuted)
                    }
                },
                actions = {
                    BotonTema(tint = c.onHeader)
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                },
                colors = PF.barra()
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SecondaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Reportes ($porAtender)", fontWeight = FontWeight.Bold) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Recomendaciones ($recAbiertas)", fontWeight = FontWeight.Bold) })
            }

            if (tab == 0) {
                ReportesTab(
                    reportes = reportes,
                    idAgente = agente.idUsuario,
                    nombre = ::nombre,
                    compromisoMap = compromisoMap,
                    empresaMap = empresaMap,
                    onTomar = { r -> viewModel.tomarReporte(r.idReporte) },
                    onResolver = { reporteAResolver = it }
                )
            } else {
                RecomendacionesTab(
                    recomendaciones = recomendaciones,
                    historialPorRec = historialPorRec,
                    nombre = ::nombre,
                    onAvanzar = { rec, estado -> viewModel.cambiarEstadoRecomendacion(rec, estado) },
                    onDescartar = { recADescartar = it }
                )
            }
        }
    }

    // ---- Diálogo: responder y resolver (RF-15, RN-23)
    reporteAResolver?.let { r ->
        val comp = compromisoMap[r.idCompromiso]
        var respuesta by remember(r.idReporte) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reporteAResolver = null },
            title = { Text("Resolver ${r.idReporte}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${nombre(comp?.idUsuarioFinal)} · ${r.motivo}", fontWeight = FontWeight.SemiBold)
                    if (!r.comentario.isNullOrBlank()) Text("\"${r.comentario}\"", fontSize = 13.sp, color = c.textMuted)
                    Text(
                        text = if (comp?.estado == "Anulado")
                            "La empresa ya anuló este cobro: quedará Anulado."
                        else
                            "Al resolver, el cobro vuelve a Pendiente o Vencido según su fecha. " +
                                "Si corresponde anularlo, coordínalo antes con el ejecutivo de la empresa: el agente no puede anular.",
                        fontSize = 12.sp,
                        color = c.textMuted
                    )
                    OutlinedTextField(
                        value = respuesta,
                        onValueChange = { respuesta = it },
                        label = { Text("Respuesta para el usuario *") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = respuesta.isNotBlank(), // HU-10 CA-3: no se resuelve sin respuesta
                    onClick = { viewModel.resolverReporte(r.idReporte, respuesta) { reporteAResolver = null } }
                ) { Text("Resolver") }
            },
            dismissButton = { TextButton(onClick = { reporteAResolver = null }) { Text("Cancelar") } }
        )
    }

    // ---- Diálogo: descartar recomendación con motivo (RN-13)
    recADescartar?.let { rec ->
        var motivo by remember(rec.idRecomendacion) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { recADescartar = null },
            title = { Text("Descartar ${rec.nombreOrganizacion}", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo del descarte *") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = motivo.isNotBlank(),
                    onClick = { viewModel.cambiarEstadoRecomendacion(rec, "Descartada", motivo) { recADescartar = null } }
                ) { Text("Descartar") }
            },
            dismissButton = { TextButton(onClick = { recADescartar = null }) { Text("Cancelar") } }
        )
    }
}

// =========================================================================== Reportes

private val FILTROS_REPORTE = listOf("Por atender", "Abierto", "En gestión", "Resuelto", "Todos")

@Composable
private fun ReportesTab(
    reportes: List<ReporteEntity>,
    idAgente: String,
    nombre: (String?) -> String,
    compromisoMap: Map<String, CompromisoEntity>,
    empresaMap: Map<String, EmpresaEntity>,
    onTomar: (ReporteEntity) -> Unit,
    onResolver: (ReporteEntity) -> Unit
) {
    val c = PF.colors
    var filtro by remember { mutableStateOf(FILTROS_REPORTE[0]) }
    val hoy = Reloj.hoy()

    // RNF-14: lo más urgente primero (atrasados, luego abiertos, en gestión y resueltos)
    val visibles = reportes
        .filter {
            when (filtro) {
                "Por atender" -> it.estado != "Resuelto"
                "Todos" -> true
                else -> it.estado == filtro
            }
        }
        .sortedWith(
            compareByDescending<ReporteEntity> { ReglasRolesInternos.reporteAtrasado(it, hoy) }
                .thenBy { listOf("Abierto", "En gestión", "Resuelto").indexOf(it.estado) }
                .thenBy { it.fechaCreacion }
        )
    val atrasados = reportes.count { ReglasRolesInternos.reporteAtrasado(it, hoy) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (atrasados > 0) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = c.vencidoBg), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = c.vencidoFg)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "$atrasados ${if (atrasados == 1) "reporte lleva" else "reportes llevan"} más de 2 días hábiles sin tomar.",
                            color = c.vencidoFg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FILTROS_REPORTE) { f ->
                    FilterChip(selected = filtro == f, onClick = { filtro = f }, label = { Text(f) })
                }
            }
        }
        if (visibles.isEmpty()) {
            item { Text("No hay reportes en esta vista.", color = c.textMuted, modifier = Modifier.padding(24.dp)) }
        }
        items(visibles, key = { it.idReporte }) { r ->
            val comp = compromisoMap[r.idCompromiso]
            val atrasado = ReglasRolesInternos.reporteAtrasado(r, hoy)
            val (bg, fg) = PF.estado(r.estado)
            Card(
                colors = CardDefaults.cardColors(containerColor = c.card),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.idReporte, fontWeight = FontWeight.Bold, color = c.textStrong, modifier = Modifier.weight(1f))
                        if (atrasado) {
                            Surface(shape = RoundedCornerShape(20.dp), color = c.vencidoBg) {
                                Text("Atrasado", color = c.vencidoFg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        Surface(shape = RoundedCornerShape(20.dp), color = bg) {
                            Text(r.estado, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                    Text(nombre(comp?.idUsuarioFinal), fontWeight = FontWeight.SemiBold, color = c.textStrong) // persona vía compromiso
                    if (comp != null) {
                        val total = comp.monto + if (comp.estado == "Vencido") comp.recargoAplicado else 0.0
                        Text(
                            "${empresaMap[comp.idEmpresa]?.nombreFantasia ?: comp.idEmpresa} · ${comp.concepto}" +
                                (comp.numeroCuota?.let { " · Cuota $it/${comp.totalCuotas}" } ?: "") +
                                " · ${CurrencyFormatter.formatCLP(total)} · ${comp.folio}",
                            fontSize = 12.sp, color = c.textMuted
                        )
                    }
                    Text("Motivo: ${r.motivo}", fontSize = 13.sp, color = c.textStrong)
                    if (!r.comentario.isNullOrBlank()) Text("\"${r.comentario}\"", fontSize = 13.sp, color = c.textMuted)
                    val dias = Fechas.diasHabilesEntre(r.fechaCreacion, hoy)
                    Text(
                        "Creado el ${Fechas.mostrar(r.fechaCreacion)}" +
                            if (r.estado == "Abierto") " · $dias ${if (dias == 1) "día hábil" else "días hábiles"} esperando" else "",
                        fontSize = 12.sp, color = c.textMuted
                    )
                    if (r.idAgente != null) {
                        Text(
                            "Responsable: ${nombre(r.idAgente)}" + (r.fechaToma?.let { " · tomado el ${Fechas.mostrar(it)}" } ?: ""),
                            fontSize = 12.sp, color = c.textMuted
                        )
                    }
                    if (!r.respuesta.isNullOrBlank()) {
                        HorizontalDivider(color = c.divider, modifier = Modifier.padding(vertical = 4.dp))
                        Text("Respuesta: ${r.respuesta}", fontSize = 13.sp, color = c.pagadoFg)
                    }

                    when {
                        r.estado == "Abierto" -> Button(onClick = { onTomar(r) }, modifier = Modifier.align(Alignment.End)) {
                            Text("Tomar reporte")
                        }
                        r.estado == "En gestión" && r.idAgente == idAgente ->
                            Button(onClick = { onResolver(r) }, modifier = Modifier.align(Alignment.End)) {
                                Text("Responder y resolver")
                            }
                        r.estado == "En gestión" ->
                            Text("Lo está gestionando ${nombre(r.idAgente)}.", fontSize = 12.sp, color = c.textMuted,
                                modifier = Modifier.align(Alignment.End))
                    }
                }
            }
        }
    }
}

// ==================================================================== Recomendaciones

private val FILTROS_REC = listOf("Abiertas", "Cerradas", "Todas")

@Composable
private fun RecomendacionesTab(
    recomendaciones: List<RecomendacionEntity>,
    historialPorRec: Map<String, List<HistorialRecomendacionEntity>>,
    nombre: (String?) -> String,
    onAvanzar: (RecomendacionEntity, String) -> Unit,
    onDescartar: (RecomendacionEntity) -> Unit
) {
    val c = PF.colors
    var filtro by remember { mutableStateOf(FILTROS_REC[0]) }
    val visibles = recomendaciones.filter {
        val abierta = ReglasRolesInternos.siguientesEstados(it.estado).isNotEmpty()
        when (filtro) {
            "Abiertas" -> abierta
            "Cerradas" -> !abierta
            else -> true
        }
    }.sortedBy { it.fecha }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FILTROS_REC.forEach { f -> FilterChip(selected = filtro == f, onClick = { filtro = f }, label = { Text(f) }) }
            }
        }
        if (visibles.isEmpty()) {
            item { Text("No hay recomendaciones en esta vista.", color = c.textMuted, modifier = Modifier.padding(24.dp)) }
        }
        items(visibles, key = { it.idRecomendacion }) { rec ->
            val (bg, fg) = PF.estado(rec.estado)
            Card(
                colors = CardDefaults.cardColors(containerColor = c.card),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(rec.nombreOrganizacion, fontWeight = FontWeight.Bold, color = c.textStrong, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(20.dp), color = bg) {
                            Text(rec.estado, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                    Text("${rec.idRecomendacion} · ${rec.rubro ?: "Sin rubro"} · ${rec.comuna ?: "Sin comuna"}", fontSize = 12.sp, color = c.textMuted)
                    Text("Recomendada por ${nombre(rec.idUsuarioFinal)} el ${Fechas.mostrar(rec.fecha)}", fontSize = 12.sp, color = c.textMuted)
                    // RN-12: el contacto solo existe si la persona declaró que autorizó ser contactada
                    if (rec.autorizaContacto && rec.nombreContacto != null) {
                        Text(
                            "Contacto: ${rec.nombreContacto}" +
                                listOfNotNull(rec.correoContacto, rec.telefonoContacto).joinToString(prefix = " · ", separator = " · ")
                                    .takeIf { rec.correoContacto != null || rec.telefonoContacto != null }.orEmpty(),
                            fontSize = 12.sp, color = c.textStrong
                        )
                    }

                    // Historial de cambios (RNF-15)
                    historialPorRec[rec.idRecomendacion].orEmpty().forEach { h ->
                        Text(
                            "• ${Fechas.mostrar(h.fecha)} · ${h.estado} · ${nombre(h.idAgente)}" + (h.motivo?.let { " · $it" } ?: ""),
                            fontSize = 12.sp, color = c.textMuted
                        )
                    }

                    val siguientes = ReglasRolesInternos.siguientesEstados(rec.estado)
                    if (siguientes.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            if ("Descartada" in siguientes) {
                                OutlinedButton(onClick = { onDescartar(rec) }) { Text("Descartar") }
                            }
                            siguientes.filter { it != "Descartada" }.forEach { est ->
                                Button(onClick = { onAvanzar(rec, est) }) {
                                    Text(if (est == "Se unió") "Se unió" else "Pasar a $est")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

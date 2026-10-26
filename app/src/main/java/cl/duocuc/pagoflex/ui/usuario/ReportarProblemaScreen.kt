package cl.duocuc.pagoflex.ui.usuario

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.EmpresaEntity
import cl.duocuc.pagoflex.data.local.entities.ReporteEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import cl.duocuc.pagoflex.util.Fechas
import kotlinx.coroutines.launch
import cl.duocuc.pagoflex.ui.common.MensajesEnSnackbar
import cl.duocuc.pagoflex.ui.common.vm

private val FILTROS = listOf("Todos", "En curso", "Resueltos")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportarProblemaScreen(
    idCompromisoInitial: String? = null,
    usuario: UsuarioEntity,
    repository: PagoFlexRepository,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val c = PF.colors

    val viewModel = vm(key = "reportes-${usuario.idUsuario}") { MisReportesViewModel(repository, usuario.idUsuario) }
    MensajesEnSnackbar(viewModel, snackbar)
    val ui by viewModel.estado.collectAsState()
    val reportes: List<ReporteEntity>? = if (ui.cargado) ui.reportes else null
    val compromisoMap = ui.compromisos
    val compromisos = compromisoMap.values.toList()
    val empresaMap = ui.empresas

    var filtro by remember { mutableStateOf(FILTROS[0]) }
    var aReportar by remember { mutableStateOf<CompromisoEntity?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var destacado by remember { mutableStateOf(idCompromisoInitial) }
    var yaRevisado by remember { mutableStateOf(false) }

    LaunchedEffect(reportes, compromisos) {
        val lista = reportes ?: return@LaunchedEffect
        if (yaRevisado || idCompromisoInitial == null || compromisos.isEmpty()) return@LaunchedEffect
        yaRevisado = true
        val comp = compromisoMap[idCompromisoInitial] ?: return@LaunchedEffect
        val enCurso = lista.firstOrNull { it.idCompromiso == comp.idCompromiso && it.estado != "Resuelto" }
        when {
            enCurso != null -> aviso = "Este compromiso ya tiene un reporte en curso (${enCurso.idReporte}). Te mostramos su estado aquí abajo."
            comp.estado == "Pendiente" || comp.estado == "Vencido" -> aReportar = comp
            else -> aviso = "Este compromiso está ${comp.estado}, así que no se puede reportar."
        }
    }

    val visibles = (reportes ?: emptyList())
        .filter {
            when (filtro) {
                "En curso" -> it.estado != "Resuelto"
                "Resueltos" -> it.estado == "Resuelto"
                else -> true
            }
        }
        .sortedWith(compareBy<ReporteEntity> { it.estado == "Resuelto" }.thenByDescending { it.fechaCreacion })

    val listState = rememberLazyListState()
    LaunchedEffect(destacado, visibles.size) {
        val i = visibles.indexOfFirst { it.idCompromiso == destacado }
        if (i >= 0) listState.animateScrollToItem(i + 2)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Mis reportes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = PF.barra()
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Aquí ves los problemas que nos has reportado y lo que respondió soporte. " +
                            "Para reportar uno nuevo, toca \"Reportar un problema\" en el compromiso desde el inicio.",
                        fontSize = 13.sp, color = c.textMuted
                    )
                    aviso?.let {
                        Card(colors = CardDefaults.cardColors(containerColor = c.revisionBg), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = c.revisionFg)
                                Spacer(Modifier.width(8.dp))
                                Text(it, color = c.revisionFg, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FILTROS.forEach { f -> FilterChip(selected = filtro == f, onClick = { filtro = f }, label = { Text(f) }) }
                }
            }
            if (reportes != null && visibles.isEmpty()) {
                item {
                    Text(
                        if (filtro == "Todos") "Aún no has reportado problemas." else "No tienes reportes en esta vista.",
                        color = c.textMuted, modifier = Modifier.padding(24.dp)
                    )
                }
            }
            items(visibles, key = { it.idReporte }) { r ->
                ReporteCardItem(
                    reporte = r,
                    compromiso = compromisoMap[r.idCompromiso],
                    empresa = compromisoMap[r.idCompromiso]?.let { empresaMap[it.idEmpresa] },
                    destacado = r.idCompromiso == destacado && r.estado != "Resuelto"
                )
            }
        }
    }

    aReportar?.let { comp ->
        NuevoReporteDialog(
            compromiso = comp,
            empresa = empresaMap[comp.idEmpresa],
            onEnviar = { motivo, comentario ->
                viewModel.crearReporte(comp.idCompromiso, motivo, comentario) { exito ->
                    aReportar = null
                    if (exito) destacado = comp.idCompromiso
                }
            },
            onDismiss = { aReportar = null }
        )
    }
}

@Composable
private fun NuevoReporteDialog(
    compromiso: CompromisoEntity,
    empresa: EmpresaEntity?,
    onEnviar: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val c = PF.colors
    var motivo by remember { mutableStateOf<String?>(null) }
    var comentario by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    val listo = motivo != null && (motivo != "Otro" || comentario.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportar un problema", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${empresa?.nombreFantasia ?: compromiso.idEmpresa} · ${compromiso.concepto}" +
                        (compromiso.numeroCuota?.let { " · Cuota $it/${compromiso.totalCuotas}" } ?: ""),
                    fontWeight = FontWeight.SemiBold, color = c.textStrong
                )
                Text("Folio ${compromiso.folio} · ${CurrencyFormatter.formatCLP(compromiso.monto)}", fontSize = 12.sp, color = c.textMuted)
                Spacer(Modifier.height(4.dp))
                Text("¿Qué pasó?", fontWeight = FontWeight.SemiBold)
                PagoFlexRepository.MOTIVOS_REPORTE.forEach { m ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { motivo = m }
                    ) {
                        RadioButton(selected = motivo == m, onClick = { motivo = m })
                        Text(m, fontSize = 14.sp)
                    }
                }
                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it },
                    label = { Text(if (motivo == "Otro") "Cuéntanos qué pasó *" else "Comentario (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Mientras soporte revisa tu caso, este cobro queda En revisión y no se puede pagar.",
                    fontSize = 12.sp, color = c.textMuted
                )
            }
        },
        confirmButton = {
            Button(
                enabled = listo && !enviando,
                onClick = { enviando = true; onEnviar(motivo!!, comentario) }
            ) { Text("Enviar reporte") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ReporteCardItem(
    reporte: ReporteEntity,
    compromiso: CompromisoEntity?,
    empresa: EmpresaEntity?,
    destacado: Boolean = false
) {
    val c = PF.colors
    val (statusBg, statusFg) = PF.estado(reporte.estado)

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = c.card),
        border = if (destacado) BorderStroke(2.dp, c.accent) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    empresa?.nombreFantasia ?: compromiso?.idEmpresa ?: "Compromiso ${reporte.idCompromiso}",
                    fontWeight = FontWeight.Bold, color = c.textStrong, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = statusBg) {
                    Text(
                        text = reporte.estado, color = statusFg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            compromiso?.let {
                Text(
                    it.concepto + (it.numeroCuota?.let { n -> " · Cuota $n/${it.totalCuotas}" } ?: "") +
                        " · ${CurrencyFormatter.formatCLP(it.monto)}",
                    fontSize = 12.sp, color = c.textMuted
                )
            }
            Text("${reporte.idReporte} · enviado el ${Fechas.mostrar(reporte.fechaCreacion)}", fontSize = 12.sp, color = c.textMuted)
            Spacer(Modifier.height(2.dp))
            Text("Motivo: ${reporte.motivo}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = c.textStrong)
            if (!reporte.comentario.isNullOrBlank()) {
                Text("\"${reporte.comentario}\"", fontSize = 13.sp, color = c.textMuted)
            }

            HorizontalDivider(color = c.divider, modifier = Modifier.padding(vertical = 6.dp))

            when (reporte.estado) {
                "Abierto" -> Text("Recibimos tu reporte. Un agente lo revisará pronto.", fontSize = 13.sp, color = c.textStrong)
                "En gestión" -> Text("Un agente de soporte está revisando tu caso.", fontSize = 13.sp, color = c.textStrong)
                else -> {
                    Text("Respuesta de soporte", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = c.pagadoFg)
                    Text(reporte.respuesta ?: "—", fontSize = 13.sp, color = c.textStrong)
                    compromiso?.let {
                        val resultado = when (it.estado) {
                            "Anulado" -> "La empresa anuló este cobro. No tienes que pagarlo."
                            "Pagado" -> "Este cobro ya está pagado."
                            "Vencido" -> "El cobro se mantiene y ya venció: puedes pagarlo desde el inicio."
                            "Pendiente" -> "El cobro se mantiene: puedes pagarlo desde el inicio hasta el ${Fechas.mostrar(it.fechaVencimiento)}."
                            else -> "Estado actual del cobro: ${it.estado}."
                        }
                        Text(resultado, fontSize = 12.sp, color = c.textMuted)
                    }
                    reporte.fechaResolucion?.let {
                        Text("Resuelto el ${Fechas.mostrar(it)}", fontSize = 12.sp, color = c.textMuted)
                    }
                }
            }
        }
    }
}

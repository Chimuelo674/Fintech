package cl.duocuc.pagoflex.ui.ejecutivo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.*
import cl.duocuc.pagoflex.domain.ReglasRolesInternos
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.theme.BotonTema
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.ui.usuario.formatearPorcentaje
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.util.Reloj
import cl.duocuc.pagoflex.util.Rut
import kotlinx.coroutines.launch
import cl.duocuc.pagoflex.ui.common.MensajesEnSnackbar
import cl.duocuc.pagoflex.ui.common.vm

private val FILTROS_ESTADO = listOf("Todos", "Pendiente", "Vencido", "En revisión", "Pagado", "Anulado")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjecutivoDashboardScreen(
    ejecutivo: UsuarioEntity,
    repository: PagoFlexRepository,
    onLogout: () -> Unit
) {
    val idEmpresa = ejecutivo.idEmpresa ?: return
    val snackbar = remember { SnackbarHostState() }
    val c = PF.colors

    val viewModel = vm(key = "ejecutivo-${ejecutivo.idUsuario}") {
        EjecutivoViewModel(repository, ejecutivo.idUsuario, idEmpresa)
    }
    MensajesEnSnackbar(viewModel, snackbar)
    val ui by viewModel.estado.collectAsState()
    val empresa = ui.empresa
    val compromisos = ui.compromisos
    val pagos = ui.pagos
    val usuarioMap = ui.usuarios
    val reportesPorCompromiso = ui.reportesPorCompromiso

    var mes by remember { mutableStateOf(Reloj.hoy().take(7)) }
    var filtro by remember { mutableStateOf("Todos") }
    var detalle by remember { mutableStateOf<CompromisoEntity?>(null) }
    var aAnular by remember { mutableStateOf<CompromisoEntity?>(null) }
    var registrando by remember { mutableStateOf(false) }

    val resumen = ReglasRolesInternos.resumenDelMes(compromisos, pagos, mes)
    val enRevisionTotal = compromisos.count { it.estado == "En revisión" }
    val hoy = Reloj.hoy()

    val listaMes = compromisos
        .filter { it.fechaVencimiento.take(7) == mes && (filtro == "Todos" || it.estado == filtro) }
        .let { lista ->
            if (filtro == "Vencido") lista.sortedByDescending { Fechas.diasAtraso(it.fechaVencimiento, hoy) }
            else lista.sortedBy { it.fechaVencimiento }
        }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Panel ${empresa?.nombreFantasia ?: idEmpresa}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${ejecutivo.nombreCompleto} · ${ejecutivo.cargo ?: "Ejecutivo"}", fontSize = 12.sp, color = c.onHeaderMuted)
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { registrando = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registrar compromiso") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { mes = Fechas.sumarMesesAMes(mes, -1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior", tint = c.textStrong)
                    }
                    Text(
                        Fechas.nombreMes(mes),
                        fontWeight = FontWeight.Bold, fontSize = 18.sp, color = c.textStrong,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { mes = Fechas.sumarMesesAMes(mes, 1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente", tint = c.textStrong)
                    }
                }
            }

            item { ResumenCobranzaCard(resumen) }

            if (enRevisionTotal > 0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = c.revisionBg),
                        modifier = Modifier.fillMaxWidth().clickable { filtro = "En revisión" }
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = c.revisionFg)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "$enRevisionTotal ${if (enRevisionTotal == 1) "compromiso está" else "compromisos están"} En revisión por un reporte del usuario. Revisa si corresponde anular.",
                                color = c.revisionFg, fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FILTROS_ESTADO) { f ->
                        FilterChip(selected = filtro == f, onClick = { filtro = f }, label = { Text(f) })
                    }
                }
            }

            if (listaMes.isEmpty()) {
                item {
                    Text(
                        "No hay compromisos ${if (filtro == "Todos") "" else "\"$filtro\" "}con vencimiento en ${Fechas.nombreMes(mes).lowercase()}.",
                        color = c.textMuted, modifier = Modifier.padding(24.dp)
                    )
                }
            }

            items(listaMes, key = { it.idCompromiso }) { comp ->
                CompromisoEjecutivoCard(
                    comp = comp,
                    persona = usuarioMap[comp.idUsuarioFinal],
                    onClick = { detalle = comp }
                )
            }
        }
    }

    detalle?.let { d ->
        val actual = compromisos.firstOrNull { it.idCompromiso == d.idCompromiso } ?: d
        DetalleCompromisoDialog(
            comp = actual,
            persona = usuarioMap[actual.idUsuarioFinal],
            reportes = reportesPorCompromiso[actual.idCompromiso].orEmpty(),
            pago = pagos.firstOrNull { it.idCompromiso == actual.idCompromiso },
            nombreDe = { id -> usuarioMap[id]?.nombreCompleto ?: id },
            onAnular = { detalle = null; aAnular = actual },
            onClose = { detalle = null }
        )
    }

    aAnular?.let { comp ->
        AnularDialog(
            comp = comp,
            onConfirm = { motivo, comentario -> viewModel.anular(comp, motivo, comentario) { aAnular = null } },
            onDismiss = { aAnular = null }
        )
    }

    if (registrando) {
        RegistrarCompromisoDialog(
            viewModel = viewModel,
            onRegistrado = { comp ->
                registrando = false
                mes = comp.fechaVencimiento.take(7)
                filtro = "Todos"
            },
            onDismiss = { registrando = false }
        )
    }
}

@Composable
private fun ResumenCobranzaCard(r: ReglasRolesInternos.ResumenCobranza) {
    val c = PF.colors
    Card(colors = CardDefaults.cardColors(containerColor = c.header), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Tasa de cobranza del mes", color = c.onHeaderMuted, fontSize = 12.sp)
            Text(
                r.tasa?.let { formatearPorcentaje(it) } ?: "Sin compromisos",
                color = c.onHeader, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold
            )
            Text(
                "${r.pagados} pagados de ${r.total - r.anulados}" + if (r.anulados > 0) " (sin contar ${r.anulados} anulados)" else "",
                color = c.onHeaderMuted, fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = c.onHeader.copy(alpha = 0.15f))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Dato("Recaudado", CurrencyFormatter.formatCLP(r.montoRecaudado), Modifier.weight(1.4f))
                Dato("Pendientes", "${r.pendientes}", Modifier.weight(1f))
                Dato("Vencidos", "${r.vencidos}", Modifier.weight(1f), alerta = r.vencidos > 0)
                Dato("En revisión", "${r.enRevision}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, modifier: Modifier, alerta: Boolean = false) {
    val c = PF.colors
    Column(modifier) {
        Text(etiqueta, color = c.onHeaderMuted, fontSize = 12.sp)
        Text(valor, color = if (alerta) c.headerAlert else c.onHeader, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun CompromisoEjecutivoCard(comp: CompromisoEntity, persona: UsuarioEntity?, onClick: () -> Unit) {
    val c = PF.colors
    val (bg, fg) = PF.estado(comp.estado)
    val total = comp.monto + if (comp.estado == "Vencido") comp.recargoAplicado else 0.0
    Card(
        colors = CardDefaults.cardColors(containerColor = c.card),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(persona?.nombreCompleto ?: comp.idUsuarioFinal, fontWeight = FontWeight.Bold, color = c.textStrong)
                    Text(
                        comp.concepto + (comp.numeroCuota?.let { " · Cuota $it/${comp.totalCuotas}" } ?: ""),
                        fontSize = 12.sp, color = c.textMuted
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = bg) {
                    Text(comp.estado, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${comp.folio} · vence el ${Fechas.mostrar(comp.fechaVencimiento)}", fontSize = 12.sp, color = c.textMuted)
                    if (comp.estado == "Vencido") {
                        val dias = Fechas.diasAtraso(comp.fechaVencimiento)
                        Text("$dias ${if (dias == 1) "día" else "días"} de atraso", fontSize = 12.sp, color = c.vencidoFg)
                    }
                }
                Text(CurrencyFormatter.formatCLP(total), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = c.textStrong)
            }
        }
    }
}

@Composable
private fun DetalleCompromisoDialog(
    comp: CompromisoEntity,
    persona: UsuarioEntity?,
    reportes: List<ReporteEntity>,
    pago: PagoEntity?,
    nombreDe: (String) -> String,
    onAnular: () -> Unit,
    onClose: () -> Unit
) {
    val c = PF.colors
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Compromiso ${comp.folio}", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Fila("Persona", persona?.nombreCompleto ?: comp.idUsuarioFinal)
                Fila("RUT", persona?.rut ?: "—")
                Fila("Contacto", listOfNotNull(persona?.correo, persona?.telefono).joinToString(" · ").ifBlank { "—" })
                HorizontalDivider(color = c.divider, modifier = Modifier.padding(vertical = 6.dp))
                Fila("Concepto", comp.concepto)
                comp.numeroCuota?.let { Fila("Cuota", "$it de ${comp.totalCuotas}") }
                Fila("Monto", CurrencyFormatter.formatCLP(comp.monto))
                if (comp.recargoAplicado > 0) Fila("Recargo por atraso", CurrencyFormatter.formatCLP(comp.recargoAplicado))
                Fila("Vencimiento", Fechas.mostrar(comp.fechaVencimiento))
                Fila("Estado", comp.estado)
                Fila("Registrado por", if (comp.registradoPor == "PagoFlex") "PagoFlex (planilla)" else nombreDe(comp.registradoPor))
                pago?.let {
                    Fila("Pagado el", Fechas.mostrar(it.fechaHora))
                    Fila("Canal", it.canal)
                    Fila("Total pagado", CurrencyFormatter.formatCLP(it.montoPagado))
                }
                if (comp.estado == "Anulado") {
                    Fila("Motivo anulación", comp.motivoAnulacion ?: "—")
                    comp.comentarioAnulacion?.let { Fila("Comentario", it) }
                    Fila("Anulado por", comp.ejecutivoAnulacion?.let(nombreDe) ?: "—")
                    Fila("Fecha anulación", Fechas.mostrar(comp.fechaAnulacion))
                }
                reportes.forEach { r ->
                    HorizontalDivider(color = c.divider, modifier = Modifier.padding(vertical = 6.dp))
                    Text("Reporte ${r.idReporte} · ${r.estado}", fontWeight = FontWeight.Bold, color = c.textStrong)
                    Text("Motivo: ${r.motivo}", fontSize = 13.sp, color = c.textStrong)
                    if (!r.comentario.isNullOrBlank()) Text("\"${r.comentario}\"", fontSize = 13.sp, color = c.textMuted)
                    if (!r.respuesta.isNullOrBlank()) Text("Respuesta de soporte: ${r.respuesta}", fontSize = 13.sp, color = c.textMuted)
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Cerrar") } },
        dismissButton = {
            if (ReglasRolesInternos.puedeAnularse(comp)) {
                TextButton(onClick = onAnular) { Text("Anular", color = MaterialTheme.colorScheme.error) }
            }
        }
    )
}

@Composable
private fun Fila(etiqueta: String, valor: String) {
    val c = PF.colors
    Row(Modifier.fillMaxWidth()) {
        Text(etiqueta, fontSize = 13.sp, color = c.textMuted, modifier = Modifier.weight(0.45f))
        Text(valor, fontSize = 13.sp, color = c.textStrong, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.55f))
    }
}

@Composable
private fun AnularDialog(comp: CompromisoEntity, onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var motivo by remember { mutableStateOf<String?>(null) }
    var comentario by remember { mutableStateOf("") }
    val listo = motivo != null && (motivo != "Otro" || comentario.isNotBlank())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Anular ${comp.folio}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${comp.concepto} · ${CurrencyFormatter.formatCLP(comp.monto)}", color = PF.colors.textMuted, fontSize = 13.sp)
                Text("Motivo:", fontWeight = FontWeight.SemiBold)
                ReglasRolesInternos.MOTIVOS_ANULACION.forEach { m ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { motivo = m }
                    ) {
                        RadioButton(selected = motivo == m, onClick = { motivo = m })
                        Text(m)
                    }
                }
                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it },
                    label = { Text(if (motivo == "Otro") "Comentario *" else "Comentario (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = listo,
                onClick = { onConfirm(motivo!!, comentario) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Anular") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun RegistrarCompromisoDialog(
    viewModel: EjecutivoViewModel,
    onRegistrado: (CompromisoEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var rut by remember { mutableStateOf("") }
    var concepto by remember { mutableStateOf("") }
    var cuota by remember { mutableStateOf("") }
    var totalCuotas by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    var nombres by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }

    val rutValido = Rut.esValido(rut)
    val persona by produceState<UsuarioEntity?>(initialValue = null, rut) {
        value = if (Rut.esValido(rut)) viewModel.buscarPersona(rut) else null
    }
    val esNueva = rutValido && persona == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar compromiso", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rut, onValueChange = { rut = it; error = null },
                    label = { Text("RUT de la persona *") },
                    placeholder = { Text("12.476.318-5") },
                    isError = rut.length > 3 && !rutValido,
                    supportingText = {
                        when {
                            rut.length > 3 && !rutValido -> Text("RUT no válido")
                            persona != null -> Text("${persona!!.nombreCompleto} (${persona!!.idUsuario})")
                            esNueva -> Text("Persona nueva: completa sus datos básicos")
                        }
                    },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                if (esNueva) {
                    OutlinedTextField(nombres, { nombres = it }, label = { Text("Nombres *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(apellidos, { apellidos = it }, label = { Text("Apellidos *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        correo, { correo = it }, label = { Text("Correo *") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        telefono, { telefono = it }, label = { Text("Teléfono *") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(concepto, { concepto = it; error = null }, label = { Text("Concepto *") },
                    placeholder = { Text("Cuota pie departamento") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        cuota, { cuota = it.filter(Char::isDigit); error = null }, label = { Text("N° cuota") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        totalCuotas, { totalCuotas = it.filter(Char::isDigit); error = null }, label = { Text("Total cuotas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f)
                    )
                }
                Text("Deja las cuotas vacías si no es un compromiso en cuotas.", fontSize = 12.sp, color = PF.colors.textMuted)
                OutlinedTextField(
                    monto, { monto = it.filter(Char::isDigit); error = null }, label = { Text("Monto en pesos *") },
                    prefix = { Text("$ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    fecha, { fecha = it; error = null }, label = { Text("Vencimiento (DD-MM-AAAA) *") },
                    placeholder = { Text("30-11-2026") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(
                enabled = !guardando,
                onClick = {
                    guardando = true
                    viewModel.registrar(
                        rut = rut,
                        concepto = concepto,
                        cuota = cuota.toIntOrNull(),
                        totalCuotas = totalCuotas.toIntOrNull(),
                        monto = monto.toLongOrNull(),
                        fechaVencimiento = Fechas.desdePantalla(fecha),
                        personaNueva = if (esNueva) PagoFlexRepository.PersonaNueva(nombres, apellidos, correo, telefono) else null,
                        onExito = { guardando = false; onRegistrado(it) },
                        onError = { guardando = false; error = it }
                    )
                }
            ) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

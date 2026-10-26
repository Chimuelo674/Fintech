package cl.duocuc.pagoflex.ui.usuario

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.RecomendacionEntity
import cl.duocuc.pagoflex.data.local.entities.ReconocimientoEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.domain.ReglasRecomendacion
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.util.Reloj
import kotlinx.coroutines.launch
import cl.duocuc.pagoflex.ui.common.MensajesEnSnackbar
import cl.duocuc.pagoflex.ui.common.vm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecomendarOrganizacionScreen(
    usuario: UsuarioEntity,
    repository: PagoFlexRepository,
    onBack: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val c = PF.colors

    val viewModel = vm(key = "recomendar-${usuario.idUsuario}") { RecomendarViewModel(repository, usuario.idUsuario) }
    MensajesEnSnackbar(viewModel, snackbar)
    val ui by viewModel.estado.collectAsState()
    val recomendaciones = ui.recomendaciones
    val reconocimientos = ui.reconocimientos
    var mostrarFormulario by remember { mutableStateOf(false) }

    val usadas = ReglasRecomendacion.usadasEnElMes(recomendaciones, Reloj.hoy().take(7))
    val disponibles = (ReglasRecomendacion.MAXIMO_POR_MES - usadas).coerceAtLeast(0)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Recomendar una organización", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                colors = PF.barra()
            )
        },
        floatingActionButton = {
            if (disponibles > 0) {
                ExtendedFloatingActionButton(
                    onClick = { mostrarFormulario = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Recomendar") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = c.chip), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("¿Le pagas a alguien que todavía no usa PagoFlex?", fontWeight = FontWeight.Bold, color = c.onChip)
                        Text(
                            "Tu jardín infantil, tu club o la administración de tu edificio. Recomiéndalos y nuestro equipo " +
                                "comercial los contactará. Si se unen, recibirás el reconocimiento Embajador PagoFlex.",
                            fontSize = 13.sp, color = c.onChip
                        )
                        Text(
                            if (disponibles > 0) "Este mes puedes hacer $disponibles ${if (disponibles == 1) "recomendación más" else "recomendaciones más"}."
                            else "Ya hiciste las ${ReglasRecomendacion.MAXIMO_POR_MES} recomendaciones de este mes.",
                            fontSize = 12.sp, color = c.onChip.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (recomendaciones.isEmpty()) {
                item { Text("Aún no has recomendado organizaciones.", color = c.textMuted, modifier = Modifier.padding(24.dp)) }
            } else {
                item { Text("Tus recomendaciones", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = c.textStrong) }
            }

            items(recomendaciones, key = { it.idRecomendacion }) { rec ->
                RecomendacionCardItem(
                    recomendacion = rec,
                    reconocimiento = reconocimientos.firstOrNull { it.motivo.startsWith(rec.idRecomendacion) }
                )
            }
        }
    }

    if (mostrarFormulario) {
        FormularioRecomendacion(
            onEnviar = { datos, alTerminar ->
                viewModel.recomendar(datos, onExito = { mostrarFormulario = false }, onError = alTerminar)
            },
            onDismiss = { mostrarFormulario = false }
        )
    }
}

@Composable
private fun FormularioRecomendacion(
    onEnviar: (ReglasRecomendacion.Datos, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val c = PF.colors
    var nombre by remember { mutableStateOf("") }
    var rubro by remember { mutableStateOf("") }
    var comuna by remember { mutableStateOf("") }
    var rut by remember { mutableStateOf("") }
    var nombreContacto by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var autoriza by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }

    val datos = ReglasRecomendacion.Datos(nombre, rubro, comuna, rut, nombreContacto, correo, telefono, autoriza)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recomendar organización", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nombre, { nombre = it; error = null }, label = { Text("Nombre de la organización *") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(rubro, { rubro = it; error = null }, label = { Text("Rubro *") },
                    placeholder = { Text("Jardín infantil, club, condominio…") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(comuna, { comuna = it; error = null }, label = { Text("Comuna *") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(rut, { rut = it; error = null }, label = { Text("RUT de la organización (opcional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())

                HorizontalDivider(color = c.divider, modifier = Modifier.padding(vertical = 4.dp))
                Text("Persona de contacto (opcional)", fontWeight = FontWeight.SemiBold, color = c.textStrong)
                Text(
                    "Solo agrega estos datos si esa persona te autorizó a compartirlos con PagoFlex.",
                    fontSize = 12.sp, color = c.textMuted
                )
                OutlinedTextField(nombreContacto, { nombreContacto = it; error = null }, label = { Text("Nombre") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(correo, { correo = it; error = null }, label = { Text("Correo") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(telefono, { telefono = it; error = null }, label = { Text("Teléfono") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                if (datos.tieneDatosDeContacto) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { autoriza = !autoriza; error = null }
                    ) {
                        Checkbox(checked = autoriza, onCheckedChange = { autoriza = it; error = null })
                        Text("Esta persona autorizó que PagoFlex la contacte", fontSize = 13.sp, color = c.textStrong)
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(
                enabled = !enviando,
                onClick = {
                    val problema = ReglasRecomendacion.validar(datos)
                    if (problema != null) {
                        error = problema
                    } else {
                        enviando = true
                        onEnviar(datos) { mensaje -> error = mensaje; enviando = false }
                    }
                }
            ) { Text("Enviar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun RecomendacionCardItem(recomendacion: RecomendacionEntity, reconocimiento: ReconocimientoEntity? = null) {
    val c = PF.colors
    val (statusBg, statusFg) = PF.estado(recomendacion.estado)

    Card(
        colors = CardDefaults.cardColors(containerColor = c.card),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    recomendacion.nombreOrganizacion,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp, color = c.textStrong, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = statusBg) {
                    Text(recomendacion.estado, color = statusFg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Text(
                "${recomendacion.rubro ?: "Sin rubro"} · ${recomendacion.comuna ?: "Sin comuna"} · enviada el ${Fechas.mostrar(recomendacion.fecha)}",
                fontSize = 12.sp, color = c.textMuted
            )

            if (recomendacion.estado != "Descartada") {
                Spacer(Modifier.height(6.dp))
                Etapas(actual = recomendacion.estado)
            } else if (!recomendacion.motivoDescarte.isNullOrBlank()) {
                Text("No siguió adelante: ${recomendacion.motivoDescarte}", fontSize = 12.sp, color = c.textMuted)
            }

            recomendacion.fechaUltimoCambio?.let {
                Text("Último cambio: ${Fechas.mostrar(it)}", fontSize = 12.sp, color = c.textMuted)
            }

            reconocimiento?.let { r ->
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = c.chip) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = c.onChip)
                        Spacer(Modifier.width(8.dp))
                        Text("Obtuviste el reconocimiento ${r.nombreReconocimiento} (${Fechas.mostrar(r.fechaObtencion)})",
                            fontSize = 12.sp, color = c.onChip)
                    }
                }
            }
        }
    }
}

@Composable
private fun Etapas(actual: String) {
    val c = PF.colors
    val etapas = ReglasRecomendacion.ETAPAS
    val idx = etapas.indexOf(actual)
    val pesoEtapa = 1f
    val pesoConector = 0.2f

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            etapas.forEachIndexed { i, _ ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(pesoEtapa)) {
                    Surface(
                        shape = CircleShape,
                        color = if (i <= idx) c.accent else c.divider,
                        modifier = Modifier.size(12.dp)
                    ) {}
                }
                if (i < etapas.lastIndex) {
                    HorizontalDivider(
                        color = if (i < idx) c.accent else c.divider,
                        thickness = 2.dp,
                        modifier = Modifier.weight(pesoConector)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            etapas.forEachIndexed { i, etapa ->
                Text(
                    etapa,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color = if (i <= idx) c.textStrong else c.textMuted,
                    fontWeight = if (i == idx) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(pesoEtapa)
                )
                if (i < etapas.lastIndex) Spacer(Modifier.weight(pesoConector))
            }
        }
    }
}

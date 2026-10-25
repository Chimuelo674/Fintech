package cl.duocuc.pagoflex.ui.usuario

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.avisos.Notificador
import cl.duocuc.pagoflex.avisos.ResultadoAvisos
import cl.duocuc.pagoflex.avisos.ServicioAvisos
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.domain.ReglasAvisos
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.session.UserSession
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.ui.theme.TemaPreferencia
import cl.duocuc.pagoflex.util.Fechas
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import androidx.compose.foundation.shape.RoundedCornerShape
import cl.duocuc.pagoflex.util.Reloj
import kotlinx.coroutines.launch
import java.util.Calendar
import cl.duocuc.pagoflex.ui.common.MensajesEnSnackbar
import cl.duocuc.pagoflex.ui.common.vm

/**
 * Ajustes: avisos de vencimiento personalizados (RF-11, HU-07, RN-15) y tema.
 * La persona elige 1, 3 o 5 días de anticipación o desactiva los avisos, y ve de inmediato
 * qué avisos recibiría hoy y cuándo le avisaremos de cada compromiso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionAvisosScreen(
    usuario: UsuarioEntity,
    repository: PagoFlexRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val c = PF.colors

    var avisosActivos by remember { mutableStateOf(usuario.avisosActivos) }
    var diasAnticipacion by remember { mutableIntStateOf(usuario.diasAnticipacion) }
    val hayCambios = avisosActivos != usuario.avisosActivos || diasAnticipacion != usuario.diasAnticipacion

    // MVVM (el contexto de la aplicación se usa para enviar notificaciones)
    val viewModel = vm(key = "avisos-${usuario.idUsuario}") {
        AvisosViewModel(repository, context.applicationContext, usuario.idUsuario)
    }
    MensajesEnSnackbar(viewModel, snackbar)
    val ui by viewModel.estado.collectAsState()
    val compromisos = ui.compromisos
    val nombreEmpresa = ui.nombreEmpresa

    val hoy = Reloj.hoy()
    // Vista previa con la configuración que la persona está eligiendo (aunque no la haya guardado)
    val avisosDeHoy = ReglasAvisos.avisosDelDia(avisosActivos, diasAnticipacion, compromisos, { nombreEmpresa[it] ?: it }, hoy) +
        ReglasAvisos.avisosDeNovedades(
            avisosActivos, ui.pagos, compromisos.associateBy { it.idCompromiso }, ui.reportes, { nombreEmpresa[it] ?: it }, hoy
        )
    val proximos = if (!avisosActivos) emptyList() else compromisos
        .mapNotNull { comp -> ReglasAvisos.fechaDelAviso(comp, diasAnticipacion, hoy)?.let { comp to it } }
        .filter { it.second > hoy }
        .sortedBy { it.second }

    // La sesión guarda una copia del usuario: se actualiza para ver los valores nuevos al volver
    val alGuardar: (UsuarioEntity) -> Unit = { UserSession.setSession(it) }

    fun explicar(r: ResultadoAvisos): String = when {
        r.sinSesion -> "Entra como usuario final para recibir avisos."
        r.fueraDeHorario -> {
            val ahora = Calendar.getInstance()
            "Son las %02d:%02d. Para no molestarte, no enviamos avisos entre las 22:00 y las 08:00: te llegarán después de las 08:00."
                .format(ahora.get(Calendar.HOUR_OF_DAY), ahora.get(Calendar.MINUTE))
        }
        r.sinPermiso -> "Las notificaciones de PagoFlex están desactivadas en tu teléfono."
        r.enviados.isNotEmpty() -> "Enviamos ${r.enviados.size} ${if (r.enviados.size == 1) "aviso" else "avisos"}. Revisa tus notificaciones."
        r.yaEnviadosHoy > 0 -> "Ya te enviamos hoy estos avisos. Enviamos como máximo un aviso por compromiso al día."
        else -> "Hoy no corresponde ningún aviso con tu configuración."
    }

    fun enviarAhora() = viewModel.enviarAhora(usuario, avisosActivos, diasAnticipacion, alGuardar, ::explicar)

    // Android 13+: hay que pedir permiso para mostrar notificaciones
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) enviarAhora()
        else scope.launch { snackbar.showSnackbar("Sin permiso no podemos mostrarte los avisos. Puedes activarlo en los ajustes del teléfono.") }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                colors = PF.barra()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---------------------------------------------------------------- Avisos
            Text("Avisos de vencimiento", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = c.textStrong)
            Card(colors = CardDefaults.cardColors(containerColor = c.card), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Recibir avisos", fontWeight = FontWeight.SemiBold, color = c.textStrong)
                            Text(
                                "Te recordamos tus pagos para que no se te pase ninguno.",
                                fontSize = 12.sp, color = c.textMuted
                            )
                        }
                        Switch(checked = avisosActivos, onCheckedChange = { avisosActivos = it })
                    }
                    if (avisosActivos) {
                        Text("¿Con cuánta anticipación?", fontWeight = FontWeight.SemiBold, color = c.textStrong)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReglasAvisos.OPCIONES_ANTICIPACION.forEach { d ->
                                FilterChip(
                                    selected = diasAnticipacion == d,
                                    onClick = { diasAnticipacion = d },
                                    label = { Text("$d ${if (d == 1) "día" else "días"}") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Text(
                        "También te avisamos el día que vence, si se pasa (al día siguiente), cuando " +
                            "registramos un pago y cuando respondemos un reporte. Máximo un aviso de " +
                            "vencimiento por compromiso al día y nunca entre las 22:00 y las 08:00.",
                        fontSize = 12.sp, color = c.textMuted
                    )
                    Button(
                        onClick = { viewModel.guardar(usuario, avisosActivos, diasAnticipacion, alGuardar) },
                        enabled = hayCambios,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Guardar") }
                }
            }

            // ------------------------------------------------- Vista previa (hábito: disparador)
            if (avisosActivos) {
                Card(colors = CardDefaults.cardColors(containerColor = c.chip), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Avisos de hoy (${Fechas.mostrar(hoy)})", fontWeight = FontWeight.Bold, color = c.onChip)
                        if (avisosDeHoy.isEmpty()) {
                            Text("Hoy no tienes avisos pendientes.", fontSize = 13.sp, color = c.onChip)
                        }
                        avisosDeHoy.forEach { a ->
                            Column {
                                Text(a.titulo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = c.onChip)
                                Text(a.mensaje, fontSize = 12.sp, color = c.onChip.copy(alpha = 0.85f))
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notificador.tienePermiso(context)) {
                                    pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    enviarAhora()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Enviar los avisos de hoy ahora")
                        }
                    }
                }

                if (proximos.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = c.card), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Próximos avisos", fontWeight = FontWeight.Bold, color = c.textStrong)
                            Text("Te avisaremos en estas fechas", fontSize = 12.sp, color = c.textMuted)
                            Spacer(Modifier.height(4.dp))
                            proximos.forEachIndexed { i, (comp, fecha) ->
                                if (i > 0) HorizontalDivider(color = c.divider, modifier = Modifier.padding(vertical = 6.dp))
                                ProximoAvisoItem(
                                    fecha = fecha,
                                    empresa = nombreEmpresa[comp.idEmpresa] ?: comp.idEmpresa,
                                    concepto = comp.concepto + (comp.numeroCuota?.let { " · Cuota $it/${comp.totalCuotas}" } ?: ""),
                                    monto = CurrencyFormatter.formatCLP(comp.monto),
                                    elDiaQueVence = fecha == comp.fechaVencimiento,
                                    diasAnticipacion = diasAnticipacion
                                )
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------------------- Apariencia
            Text("Apariencia", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = c.textStrong)
            Card(colors = CardDefaults.cardColors(containerColor = c.card), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tema oscuro", fontWeight = FontWeight.SemiBold, color = c.textStrong)
                        Text(
                            "También puedes usar el ícono de sol/luna de la pantalla de inicio.",
                            fontSize = 12.sp, color = c.textMuted
                        )
                    }
                    val oscuro = TemaPreferencia.esOscuro()
                    Switch(checked = oscuro, onCheckedChange = { TemaPreferencia.cambiar(oscuro) })
                }
            }
        }
    }
}

private val MESES_CORTOS = listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")

/** Un próximo aviso: recuadro con la fecha, empresa, concepto, tipo de aviso y monto. */
@Composable
private fun ProximoAvisoItem(
    fecha: String,
    empresa: String,
    concepto: String,
    monto: String,
    elDiaQueVence: Boolean,
    diasAnticipacion: Int
) {
    val c = PF.colors
    val dia = fecha.drop(8).take(2)
    val mes = fecha.drop(5).take(2).toIntOrNull()?.let { MESES_CORTOS[it - 1] } ?: ""

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        // Recuadro de fecha
        Surface(shape = RoundedCornerShape(12.dp), color = c.chip, modifier = Modifier.size(width = 52.dp, height = 56.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(dia, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = c.onChip, lineHeight = 22.sp)
                Text(mes, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = c.onChip, lineHeight = 14.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(empresa, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c.textStrong)
            Text(concepto, fontSize = 13.sp, color = c.textMuted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tipo de aviso: con anticipación (verde) o el mismo día del vencimiento (naranja)
                val (bg, fg) = if (elDiaQueVence) c.revisionBg to c.revisionFg else c.pendienteBg to c.pendienteFg
                Surface(shape = RoundedCornerShape(20.dp), color = bg) {
                    Text(
                        if (elDiaQueVence) "El día que vence"
                        else "$diasAnticipacion ${if (diasAnticipacion == 1) "día" else "días"} antes",
                        color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(monto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.textStrong)
            }
        }
    }
}

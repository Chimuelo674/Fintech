package cl.duocuc.pagoflex.ui.session

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.R
import cl.duocuc.pagoflex.avisos.AvisosPrefs
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.data.remote.ConfigServidor
import cl.duocuc.pagoflex.data.remote.EstadoConexion
import cl.duocuc.pagoflex.domain.ReglasAcceso
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.common.vm
import cl.duocuc.pagoflex.ui.theme.BotonTema
import cl.duocuc.pagoflex.ui.theme.PF
import kotlinx.coroutines.delay

/**
 * Ingreso estilo banco (inspirado en BancoEstado): RUT + clave de 4 dígitos,
 * "Recordar mi RUT" e ingreso con huella para quien ya entró antes en este teléfono.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    repository: PagoFlexRepository,
    onIngreso: (UsuarioEntity) -> Unit
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val c = PF.colors

    val rutGuardado = remember { AccesoPrefs.rutRecordado(context) }
    val viewModel = vm { LoginViewModel(repository, rutGuardado) }
    val ui by viewModel.estado.collectAsState()
    val usuariosPrueba by viewModel.usuariosPrueba.collectAsState()

    var recordarRut by remember { mutableStateOf(rutGuardado != null) }
    var claveVisible by remember { mutableStateOf(false) }
    var verUsuariosPrueba by remember { mutableStateOf(false) }
    var verAyudaClave by remember { mutableStateOf(false) }
    var ofrecerHuellaA by remember { mutableStateOf<UsuarioEntity?>(null) }
    var verServidor by remember { mutableStateOf(false) }
    val conexion by viewModel.estadoConexion.collectAsState()

    val huellaDisponible = remember { Biometria.disponible(context) }
    val puedeUsarHuella = huellaDisponible && ui.recordado != null && AccesoPrefs.huellaActiva(context)
    val bloqueado = ui.segundosBloqueo > 0

    // Cuenta regresiva del bloqueo por intentos fallidos.
    LaunchedEffect(bloqueado) {
        while (bloqueado) {
            delay(1_000)
            viewModel.actualizarBloqueo(System.currentTimeMillis())
            if (viewModel.estado.value.segundosBloqueo == 0) break
        }
    }

    fun terminarIngreso(usuario: UsuarioEntity) {
        AvisosPrefs.guardarUsuario(context, usuario.idUsuario)
        onIngreso(usuario)
    }

    fun alIngresarConClave(usuario: UsuarioEntity) {
        AccesoPrefs.recordarRut(context, if (recordarRut) usuario.rut else null)
        val yaTieneHuella = AccesoPrefs.huellaActiva(context)
        if (recordarRut && huellaDisponible && !yaTieneHuella) {
            ofrecerHuellaA = usuario
        } else {
            terminarIngreso(usuario)
        }
    }

    fun intentarHuella() {
        val activity = context.buscarFragmentActivity() ?: return
        Biometria.autenticar(
            activity = activity,
            onExito = { viewModel.ingresarConHuella(::terminarIngreso) },
            onError = { viewModel.mostrarError(it) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.header)
    ) {
        // ---------- Encabezado de marca ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Logo de la app (res/drawable/logo_pagoflex.xml), el mismo diseño del ícono.
                    Image(
                        painter = painterResource(R.drawable.logo_pagoflex),
                        contentDescription = "Logo PagoFlex",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, c.onHeader.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("PagoFlex", color = c.onHeader, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tus pagos y compromisos, seguros en un solo lugar",
                    color = c.onHeaderMuted,
                    fontSize = 13.sp
                )
            }
            Row(modifier = Modifier.align(Alignment.TopEnd), verticalAlignment = Alignment.CenterVertically) {
                // Configurar la IP de los microservicios. El punto muestra si hay conexión.
                Box {
                    IconButton(onClick = { verServidor = true }) {
                        Icon(Icons.Default.Dns, contentDescription = "Configurar servidor", tint = c.onHeader)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(9.dp)
                            .background(
                                color = when (conexion) {
                                    is EstadoConexion.Conectado -> c.accent
                                    is EstadoConexion.SinConexion -> c.headerAlert
                                    else -> c.onHeaderMuted
                                },
                                shape = CircleShape
                            )
                    )
                }
                BotonTema(tint = c.onHeader)
            }
        }

        // ---------- Hoja blanca con el formulario ----------
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                val recordado = ui.recordado
                if (recordado != null) {
                    Text("Hola de nuevo,", color = c.textMuted, fontSize = 15.sp)
                    Text(
                        recordado.nombres.substringBefore(" "),
                        color = c.textStrong,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("RUT ${ReglasAcceso.enmascararRut(recordado.rut)}", color = c.textMuted, fontSize = 13.sp)
                        TextButton(onClick = {
                            viewModel.usarOtroRut()
                            recordarRut = false
                            AccesoPrefs.recordarRut(context, null)
                        }) {
                            Text("¿No eres tú?", color = c.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    Text(
                        "Ingresa a tu cuenta",
                        color = c.textStrong,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Usa tu RUT y tu clave de 4 dígitos.", color = c.textMuted, fontSize = 14.sp)
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        // TextFieldValue con el cursor siempre al final: al agregar puntos y guion
                        // el texto se alarga, y si el cursor no se mueve los dígitos quedan desordenados.
                        value = TextFieldValue(text = ui.rut, selection = TextRange(ui.rut.length)),
                        onValueChange = { nuevo -> viewModel.cambiarRut(nuevo.text) },
                        label = { Text("RUT") },
                        placeholder = { Text("12.345.678-9") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = ui.clave,
                    onValueChange = viewModel::cambiarClave,
                    label = { Text("Clave") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { claveVisible = !claveVisible }) {
                            Icon(
                                if (claveVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (claveVisible) "Ocultar clave" else "Mostrar clave"
                            )
                        }
                    },
                    visualTransformation = if (claveVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focus.clearFocus()
                        viewModel.ingresar(System.currentTimeMillis(), ::alIngresarConClave)
                    }),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Indicador de dígitos, como en las apps de banco.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(ReglasAcceso.LARGO_CLAVE) { i ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(12.dp)
                                .background(
                                    color = if (i < ui.clave.length) c.accent else c.divider,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                if (ui.recordado == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable { recordarRut = !recordarRut }
                    ) {
                        Checkbox(
                            checked = recordarRut,
                            onCheckedChange = { recordarRut = it },
                            colors = CheckboxDefaults.colors(checkedColor = c.accent)
                        )
                        Text("Recordar mi RUT en este teléfono", color = c.textStrong, fontSize = 14.sp)
                    }
                }

                val error = ui.error
                if (error != null || bloqueado) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = c.vencidoBg,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = c.vencidoFg, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (bloqueado) "Ingreso bloqueado por seguridad. Intenta en ${ui.segundosBloqueo} s." else error.orEmpty(),
                                color = c.vencidoFg,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        focus.clearFocus()
                        viewModel.ingresar(System.currentTimeMillis(), ::alIngresarConClave)
                    },
                    enabled = !ui.cargando && !bloqueado,
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    if (ui.cargando) {
                        CircularProgressIndicator(color = c.onAccent, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Text("Ingresar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                if (puedeUsarHuella) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = ::intentarHuella,
                        enabled = !ui.cargando,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = c.textStrong)
                        Spacer(Modifier.width(8.dp))
                        Text("Ingresar con huella", color = c.textStrong, fontWeight = FontWeight.Bold)
                    }
                    // Abrimos el lector apenas aparece la pantalla, como hacen los bancos.
                    LaunchedEffect(Unit) { intentarHuella() }
                }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { verAyudaClave = true }) {
                        Text("¿Olvidaste tu clave?", color = c.textMuted, fontSize = 13.sp)
                    }
                    TextButton(onClick = { verUsuariosPrueba = true }) {
                        Text("Usuarios de prueba", color = c.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Conexión segura · Nunca te pediremos tu clave por teléfono", color = c.textMuted, fontSize = 11.sp)
                }
            }
        }
    }

    // ---------- Diálogos y hojas ----------

    ofrecerHuellaA?.let { usuario ->
        AlertDialog(
            onDismissRequest = { ofrecerHuellaA = null; terminarIngreso(usuario) },
            icon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
            title = { Text("¿Activar ingreso con huella?") },
            text = { Text("La próxima vez podrás entrar más rápido usando la huella registrada en este teléfono.") },
            confirmButton = {
                TextButton(onClick = {
                    AccesoPrefs.activarHuella(context, true)
                    ofrecerHuellaA = null
                    terminarIngreso(usuario)
                }) { Text("Activar") }
            },
            dismissButton = {
                TextButton(onClick = { ofrecerHuellaA = null; terminarIngreso(usuario) }) { Text("Ahora no") }
            }
        )
    }

    if (verServidor) {
        ServidorDialog(
            viewModel = viewModel,
            onCerrar = {
                verServidor = false
                viewModel.limpiarPruebaServidor()
            }
        )
    }

    if (verAyudaClave) {
        AlertDialog(
            onDismissRequest = { verAyudaClave = false },
            title = { Text("Recuperar clave") },
            text = {
                Text(
                    "En la app real te enviaríamos un código a tu correo registrado para crear una nueva clave.\n\n" +
                        "En esta versión de prueba, todas las cuentas usan la clave ${ReglasAcceso.CLAVE_DEMO}."
                )
            },
            confirmButton = { TextButton(onClick = { verAyudaClave = false }) { Text("Entendido") } }
        )
    }

    if (verUsuariosPrueba) {
        ModalBottomSheet(onDismissRequest = { verUsuariosPrueba = false }) {
            UsuariosPrueba(
                usuarios = usuariosPrueba,
                onElegir = { usuario ->
                    viewModel.usarOtroRut()
                    viewModel.cambiarRut(usuario.rut)
                    verUsuariosPrueba = false
                }
            )
        }
    }
}

@Composable
private fun UsuariosPrueba(usuarios: List<UsuarioEntity>, onElegir: (UsuarioEntity) -> Unit) {
    val c = PF.colors
    val grupos = listOf(
        "USUARIO_FINAL" to "Clientes",
        "AGENTE" to "Agentes PagoFlex",
        "EJECUTIVO" to "Ejecutivos de empresa"
    )
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text("Usuarios de prueba", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = c.textStrong)
            Text(
                "Toca uno para completar su RUT. La clave de todos es ${ReglasAcceso.CLAVE_DEMO}.",
                fontSize = 13.sp,
                color = c.textMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
        grupos.forEach { (rol, titulo) ->
            val delRol = usuarios.filter { it.idRol == rol }
            if (delRol.isNotEmpty()) {
                item {
                    Text(
                        titulo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = c.accent,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(delRol, key = { it.idUsuario }) { usuario ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onElegir(usuario) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = c.chip, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    usuario.nombres.take(1) + usuario.apellidos.take(1),
                                    color = c.onChip,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(usuario.nombreCompleto, color = c.textStrong, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(usuario.cargo ?: usuario.area ?: usuario.comuna ?: "", color = c.textMuted, fontSize = 12.sp)
                        }
                        Text(usuario.rut, color = c.textMuted, fontSize = 13.sp)
                    }
                    HorizontalDivider(color = c.divider)
                }
            }
        }
    }
}

/** Diálogo para indicar la IP del PC donde corren los microservicios y probar la conexión. */
@Composable
private fun ServidorDialog(viewModel: LoginViewModel, onCerrar: () -> Unit) {
    val context = LocalContext.current
    val c = PF.colors
    var host by remember { mutableStateOf(ConfigServidor.host) }
    val prueba by viewModel.pruebaServidor.collectAsState()

    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { Icon(Icons.Default.Dns, contentDescription = null) },
        title = { Text("Servidor de PagoFlex") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "IP del computador donde corren usuarios-service (puerto ${ConfigServidor.PUERTO_USUARIOS}) " +
                        "y pagos-service (puerto ${ConfigServidor.PUERTO_PAGOS}).",
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.trim() },
                    label = { Text("IP del servidor") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Emulador: 10.0.2.2\nCelular: la IP de tu PC en la misma red WiFi (en Windows, comando ipconfig).",
                    fontSize = 12.sp,
                    color = c.textMuted
                )
                when (val p = prueba) {
                    EstadoConexion.Sincronizando -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Probando conexión…", fontSize = 13.sp)
                    }
                    is EstadoConexion.Conectado -> Text("✓ Conectado: ${p.detalle}", color = c.pagadoFg, fontSize = 13.sp)
                    is EstadoConexion.SinConexion -> Text("✗ ${p.motivo}", color = c.vencidoFg, fontSize = 13.sp)
                    else -> {}
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    ConfigServidor.cambiar(context, host)
                    host = ConfigServidor.host
                    viewModel.probarServidor()
                },
                enabled = prueba != EstadoConexion.Sincronizando
            ) { Text("Guardar y probar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}

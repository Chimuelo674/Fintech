package cl.duocuc.pagoflex.ui.usuario

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.CompromisoEntity
import cl.duocuc.pagoflex.data.local.entities.EmpresaEntity
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.domain.LinkPago
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.common.BaseViewModel
import cl.duocuc.pagoflex.ui.common.vm
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.ui.utils.CurrencyFormatter
import cl.duocuc.pagoflex.util.Fechas
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

class CobroQrViewModel(repo: PagoFlexRepository, idUsuario: String) : BaseViewModel() {
    data class Estado(
        val cargado: Boolean = false,
        val pendientes: List<CompromisoEntity> = emptyList(),
        val empresas: Map<String, EmpresaEntity> = emptyMap()
    )

    val estado: StateFlow<Estado> = combine(
        repo.getCompromisosByUsuario(idUsuario),
        repo.getAllEmpresas()
    ) { compromisos, empresas ->
        Estado(
            cargado = true,
            pendientes = compromisos
                .filter { it.estado == "Pendiente" || it.estado == "Vencido" }
                .sortedBy { it.fechaVencimiento },
            empresas = empresas.associateBy { it.idEmpresa }
        )
    }.comoEstado(Estado())
}

private fun totalAPagar(c: CompromisoEntity): Double =
    c.monto + if (c.estado == "Vencido") c.recargoAplicado else 0.0

/**
 * QR y link de pago: la persona elige un compromiso pendiente y comparte
 * un QR o un link (por WhatsApp, por ejemplo) para que otra persona lo pague.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CobroQrScreen(
    usuario: UsuarioEntity,
    repository: PagoFlexRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = vm(key = "qr-${usuario.idUsuario}") { CobroQrViewModel(repository, usuario.idUsuario) }
    val ui by viewModel.estado.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val c = PF.colors

    var idElegido by rememberSaveable { mutableStateOf<String?>(null) }
    val elegido = ui.pendientes.firstOrNull { it.idCompromiso == idElegido } ?: ui.pendientes.firstOrNull()
    var copiado by remember { mutableStateOf(false) }

    LaunchedEffect(copiado) {
        if (copiado) {
            snackbar.showSnackbar("Link copiado")
            copiado = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR y link de pago", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = PF.barra()
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            Text(
                "Comparte un QR o un link para que otra persona pague tu compromiso desde su teléfono.",
                color = c.textMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (ui.cargado && elegido == null) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = c.card),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text(
                        "No tienes compromisos pendientes. Cuando tengas uno, podrás compartir su QR o link aquí.",
                        color = c.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(28.dp)
                    )
                }
                return@Column
            }
            if (elegido == null) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            Text(
                "Elige qué compromiso compartir",
                fontWeight = FontWeight.Bold,
                color = c.textStrong,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ui.pendientes, key = { it.idCompromiso }) { compromiso ->
                    val seleccionado = compromiso.idCompromiso == elegido.idCompromiso
                    FilterChip(
                        selected = seleccionado,
                        onClick = { idElegido = compromiso.idCompromiso },
                        label = {
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(
                                    ui.empresas[compromiso.idEmpresa]?.nombreFantasia ?: compromiso.idEmpresa,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(CurrencyFormatter.formatCLP(totalAPagar(compromiso)), fontSize = 12.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = c.chip,
                            selectedLabelColor = c.onChip
                        )
                    )
                }
            }

            val empresa = ui.empresas[elegido.idEmpresa]
            val monto = totalAPagar(elegido)
            val link = remember(elegido.folio, monto) { LinkPago.generar(elegido.folio, monto) }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = c.card),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        empresa?.nombreFantasia ?: elegido.idEmpresa,
                        color = c.textStrong,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        elegido.concepto + (elegido.numeroCuota?.let { " · Cuota $it/${elegido.totalCuotas ?: "?"}" } ?: ""),
                        color = c.textMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))

                    // El QR siempre va sobre fondo blanco para que se pueda escanear también en tema oscuro.
                    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
                        CodigoQr(texto = link, modifier = Modifier.padding(14.dp).size(220.dp))
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(CurrencyFormatter.formatCLP(monto), color = c.textStrong, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                    Text(
                        "Folio ${elegido.folio} · Vence el ${Fechas.mostrar(elegido.fechaVencimiento)}",
                        color = c.textMuted,
                        fontSize = 12.sp
                    )
                    if (elegido.estado == "Vencido" && elegido.recargoAplicado > 0) {
                        Text(
                            "Incluye recargo por atraso de ${CurrencyFormatter.formatCLP(elegido.recargoAplicado)}",
                            color = c.vencidoFg,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        copiarAlPortapapeles(context, link)
                        copiado = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = c.textStrong)
                    Spacer(Modifier.width(6.dp))
                    Text("Copiar link", color = c.textStrong)
                }
                Button(
                    onClick = {
                        compartirTexto(
                            context,
                            LinkPago.mensajeParaCompartir(
                                empresa = empresa?.nombreFantasia ?: elegido.idEmpresa,
                                concepto = elegido.concepto.replaceFirstChar { it.lowercase() },
                                montoTexto = CurrencyFormatter.formatCLP(monto),
                                link = link
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Compartir", fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Versión de prueba: el link y el QR muestran cómo funcionaría, pero todavía no hay un servidor que reciba el pago.",
                    color = c.textMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/** Dibuja un código QR con ZXing. */
@Composable
fun CodigoQr(texto: String, modifier: Modifier = Modifier) {
    val matriz: BitMatrix = remember(texto) {
        QRCodeWriter().encode(
            texto,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(
                EncodeHintType.MARGIN to 0,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
        )
    }
    Canvas(modifier = modifier) {
        val n = matriz.width
        val celda = size.minDimension / n
        drawRect(Color.White)
        for (y in 0 until n) {
            for (x in 0 until n) {
                if (matriz.get(x, y)) {
                    // +0.5 evita líneas finas entre celdas por redondeo.
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(x * celda, y * celda),
                        size = Size(celda + 0.5f, celda + 0.5f)
                    )
                }
            }
        }
    }
}

private fun copiarAlPortapapeles(context: Context, texto: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Link de pago PagoFlex", texto))
}

private fun compartirTexto(context: Context, texto: String) {
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    context.startActivity(Intent.createChooser(envio, "Compartir link de pago"))
}

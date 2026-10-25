package cl.duocuc.pagoflex.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duocuc.pagoflex.data.local.entities.UsuarioEntity
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.theme.BotonTema
import cl.duocuc.pagoflex.ui.theme.PF
import cl.duocuc.pagoflex.ui.common.vm
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import cl.duocuc.pagoflex.avisos.AvisosPrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    repository: PagoFlexRepository,
    onUserSelected: (UsuarioEntity) -> Unit
) {
    // MVVM
    val viewModel = vm { SesionViewModel(repository) }
    val usuarios by viewModel.usuarios.collectAsState()
    val empresas by viewModel.empresas.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val roles = listOf("USUARIO_FINAL", "AGENTE", "EJECUTIVO")
    val tabTitles = listOf("Usuario Final", "Agente PagoFlex", "Ejecutivo Empresa")

    // Solo cuentas activas (AG-03 está inactivo en el Anexo 9 y no puede entrar)
    val usuariosFiltrados = usuarios.filter { it.idRol == roles[selectedTab] }
    val context = LocalContext.current
    val c = PF.colors

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                text = "PagoFlex",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = c.onHeader
                            )
                            Text(
                                text = "Tus compromisos de pago en un solo lugar",
                                fontSize = 12.sp,
                                color = c.onHeaderMuted
                            )
                        }
                    }
                },
                actions = { BotonTema(tint = c.onHeader) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.header)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = c.chip),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Selección de Sesión Simulada",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = c.onChip
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Elige un perfil de usuario para probar el flujo de la aplicación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onChip.copy(alpha = 0.85f)
                    )
                }
            }

            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTab),
                        color = c.accent
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) c.textStrong else c.textMuted
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (usuariosFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = c.accent)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(usuariosFiltrados) { usuario ->
                        UserCardItemStyled(
                            usuario = usuario,
                            empresa = usuario.idEmpresa?.let { empresas[it] ?: it },
                            onClick = {
                                viewModel.entrar(usuario) {
                                    // Los avisos de vencimiento son para el usuario final con sesión en este teléfono
                                    AvisosPrefs.guardarUsuario(
                                        context, usuario.idUsuario.takeIf { usuario.idRol == "USUARIO_FINAL" }
                                    )
                                    onUserSelected(usuario)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserCardItemStyled(
    usuario: UsuarioEntity,
    empresa: String? = null, // solo ejecutivos: empresa cliente a la que pertenece (S-10)
    onClick: () -> Unit
) {
    val (icon: ImageVector, badgeText: String) = when (usuario.idRol) {
        "USUARIO_FINAL" -> Icons.Default.Person to (usuario.comuna ?: "Usuario Final")
        "AGENTE" -> Icons.Default.SupportAgent to (usuario.area ?: "Agente Soporte")
        else -> Icons.Default.Business to (usuario.cargo ?: "Ejecutivo Empresa")
    }
    val c = PF.colors

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = c.card),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = c.chip,
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = c.onChip,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // El nombre usa todo el ancho; el código (UF-01, AG-01…) va como etiqueta abajo,
                // así no compite por espacio con nombres largos.
                Text(
                    text = "${usuario.nombres} ${usuario.apellidos}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = c.textStrong
                )
                // Ejecutivo: a qué empresa (panel) pertenece
                if (usuario.idRol == "EJECUTIVO" && empresa != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = c.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Panel $empresa",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = c.textStrong
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "RUT: ${usuario.rut} • ${usuario.correo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = c.header
                    ) {
                        Text(
                            text = usuario.idUsuario,
                            color = c.onHeader,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = c.chip
                    ) {
                        Text(
                            text = badgeText,
                            color = c.onChip,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

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
import cl.duocuc.pagoflex.ui.theme.PagoFlexDarkGreen
import cl.duocuc.pagoflex.ui.theme.PagoFlexLightMint
import cl.duocuc.pagoflex.ui.theme.PagoFlexMint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    repository: PagoFlexRepository,
    onUserSelected: (UsuarioEntity) -> Unit
) {
    val usuarios by repository.getAllUsuarios().collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }

    val roles = listOf("USUARIO_FINAL", "AGENTE", "EJECUTIVO")
    val tabTitles = listOf("Usuario Final", "Agente PagoFlex", "Ejecutivo Empresa")

    val usuariosFiltrados = usuarios.filter { it.idRol == roles[selectedTab] }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PagoFlexMint,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "Pago",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "PagoFlex",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Una experiencia de pago para clientes y comercios",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PagoFlexDarkGreen)
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
                colors = CardDefaults.cardColors(containerColor = PagoFlexLightMint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Selección de Sesión Simulada (RF-01)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PagoFlexDarkGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Elige un perfil de usuario para probar el flujo de la aplicación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PagoFlexDarkGreen.copy(alpha = 0.8f)
                    )
                }
            }

            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedTab),
                        color = PagoFlexMint
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
                                color = if (selectedTab == index) PagoFlexDarkGreen else Color.Gray
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
                    CircularProgressIndicator(color = PagoFlexMint)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(usuariosFiltrados) { usuario ->
                        UserCardItemStyled(
                            usuario = usuario,
                            onClick = {
                                UserSession.setSession(usuario)
                                onUserSelected(usuario)
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
    onClick: () -> Unit
) {
    val (icon: ImageVector, badgeText: String) = when (usuario.idRol) {
        "USUARIO_FINAL" -> Icons.Default.Person to (usuario.comuna ?: "Usuario Final")
        "AGENTE" -> Icons.Default.SupportAgent to (usuario.area ?: "Agente Soporte")
        else -> Icons.Default.Business to (usuario.cargo ?: "Ejecutivo Empresa")
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                color = PagoFlexLightMint,
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PagoFlexDarkGreen,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${usuario.nombres} ${usuario.apellidos}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PagoFlexDarkGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${usuario.idUsuario})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "RUT: ${usuario.rut} • ${usuario.correo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PagoFlexLightMint
                ) {
                    Text(
                        text = badgeText,
                        color = PagoFlexDarkGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

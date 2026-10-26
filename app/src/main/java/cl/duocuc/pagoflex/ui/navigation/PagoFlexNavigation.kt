package cl.duocuc.pagoflex.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import cl.duocuc.pagoflex.avisos.AvisosPrefs
import cl.duocuc.pagoflex.repository.PagoFlexRepository
import cl.duocuc.pagoflex.ui.agente.AgenteDashboardScreen
import cl.duocuc.pagoflex.ui.ejecutivo.EjecutivoDashboardScreen
import cl.duocuc.pagoflex.ui.session.RoleSelectionScreen
import cl.duocuc.pagoflex.ui.session.UserSession
import cl.duocuc.pagoflex.ui.usuario.*

object Screen {
    const val RoleSelection = "role_selection"
    const val UsuarioDashboard = "usuario_dashboard"
    const val Pago = "pago"
    const val Comprobante = "comprobante"
    const val Historial = "historial"
    const val DetalleCompromiso = "detalle_compromiso"
    const val ReportarProblema = "reportar_problema"
    const val ConfiguracionAvisos = "configuracion_avisos"
    const val RecomendarOrganizacion = "recomendar_organizacion"
    const val AgenteDashboard = "agente_dashboard"
    const val EjecutivoDashboard = "ejecutivo_dashboard"
}

@Composable
fun PagoFlexNavigation(repository: PagoFlexRepository) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val currentUser by UserSession.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.RoleSelection
    ) {
        composable(Screen.RoleSelection) {
            RoleSelectionScreen(
                repository = repository,
                onUserSelected = { user ->
                    when (user.idRol) {
                        "USUARIO_FINAL" -> navController.navigate(Screen.UsuarioDashboard) {
                            popUpTo(Screen.RoleSelection) { inclusive = true }
                        }
                        "AGENTE" -> navController.navigate(Screen.AgenteDashboard) {
                            popUpTo(Screen.RoleSelection) { inclusive = true }
                        }
                        "EJECUTIVO" -> navController.navigate(Screen.EjecutivoDashboard) {
                            popUpTo(Screen.RoleSelection) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.UsuarioDashboard) {
            val user = currentUser
            if (user != null) {
                UsuarioDashboardScreen(
                    usuario = user,
                    repository = repository,
                    onNavigateToPago = { idCompromiso ->
                        navController.navigate("${Screen.Pago}/$idCompromiso")
                    },
                    onNavigateToHistorial = {
                        navController.navigate(Screen.Historial)
                    },
                    onNavigateToReportar = { idCompromiso ->
                        navController.navigate("${Screen.ReportarProblema}/$idCompromiso")
                    },
                    onNavigateToAvisos = {
                        navController.navigate(Screen.ConfiguracionAvisos)
                    },
                    onNavigateToRecomendar = {
                        navController.navigate(Screen.RecomendarOrganizacion)
                    },
                    onVerComprobante = { idCompromiso ->
                        navController.navigate("${Screen.Comprobante}/$idCompromiso")
                    },
                    onNavigateToMisReportes = {
                        navController.navigate("${Screen.ReportarProblema}/0")
                    },
                    onVerDetalle = { idCompromiso ->
                        navController.navigate("${Screen.DetalleCompromiso}/$idCompromiso")
                    },
                    onLogout = {
                        UserSession.clearSession()
                        AvisosPrefs.guardarUsuario(context, null)
                        navController.navigate(Screen.RoleSelection) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(
            route = "${Screen.DetalleCompromiso}/{idCompromiso}",
            arguments = listOf(navArgument("idCompromiso") { type = NavType.StringType })
        ) { backStackEntry ->
            val idCompromiso = backStackEntry.arguments?.getString("idCompromiso") ?: ""
            val user = currentUser
            if (user != null) {
                DetalleCompromisoScreen(
                    idCompromiso = idCompromiso,
                    usuario = user,
                    repository = repository,
                    onPagar = { navController.navigate("${Screen.Pago}/$it") },
                    onReportar = { navController.navigate("${Screen.ReportarProblema}/$it") },
                    onVerComprobante = { navController.navigate("${Screen.Comprobante}/$it") },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = "${Screen.Pago}/{idCompromiso}",
            arguments = listOf(navArgument("idCompromiso") { type = NavType.StringType })
        ) { backStackEntry ->
            val idCompromiso = backStackEntry.arguments?.getString("idCompromiso") ?: ""
            val user = currentUser
            if (user != null) {
                PagoScreen(
                    idCompromiso = idCompromiso,
                    usuario = user,
                    repository = repository,
                    onPagoConfirmado = { numeroComprobante ->
                        navController.navigate("${Screen.Comprobante}/$numeroComprobante") {
                            popUpTo(Screen.UsuarioDashboard) { inclusive = false }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = "${Screen.Comprobante}/{idPago}",
            arguments = listOf(navArgument("idPago") { type = NavType.StringType })
        ) { backStackEntry ->
            val idPago = backStackEntry.arguments?.getString("idPago") ?: ""
            val user = currentUser
            if (user != null) {
                ComprobanteScreen(
                    idPago = idPago,
                    usuario = user,
                    repository = repository,
                    onBackToHome = {
                        navController.popBackStack(Screen.UsuarioDashboard, inclusive = false)
                    }
                )
            }
        }

        composable(Screen.Historial) {
            val user = currentUser
            if (user != null) {
                HistorialScreen(
                    usuario = user,
                    repository = repository,
                    onVerComprobante = { idCompromiso ->
                        navController.navigate("${Screen.Comprobante}/$idCompromiso")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = "${Screen.ReportarProblema}/{idCompromiso}",
            arguments = listOf(navArgument("idCompromiso") { type = NavType.StringType })
        ) { backStackEntry ->
            val idCompromiso = backStackEntry.arguments?.getString("idCompromiso")
            val user = currentUser
            if (user != null) {
                ReportarProblemaScreen(
                    idCompromisoInitial = if (idCompromiso == "0") null else idCompromiso,
                    usuario = user,
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.ConfiguracionAvisos) {
            val user = currentUser
            if (user != null) {
                ConfiguracionAvisosScreen(
                    usuario = user,
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.RecomendarOrganizacion) {
            val user = currentUser
            if (user != null) {
                RecomendarOrganizacionScreen(
                    usuario = user,
                    repository = repository,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.AgenteDashboard) {
            val user = currentUser
            if (user != null) {
                AgenteDashboardScreen(
                    agente = user,
                    repository = repository,
                    onLogout = {
                        UserSession.clearSession()
                        AvisosPrefs.guardarUsuario(context, null)
                        navController.navigate(Screen.RoleSelection) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.EjecutivoDashboard) {
            val user = currentUser
            if (user != null) {
                EjecutivoDashboardScreen(
                    ejecutivo = user,
                    repository = repository,
                    onLogout = {
                        UserSession.clearSession()
                        AvisosPrefs.guardarUsuario(context, null)
                        navController.navigate(Screen.RoleSelection) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

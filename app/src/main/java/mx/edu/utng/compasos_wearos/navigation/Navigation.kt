package mx.edu.utng.compasos_wearos.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import mx.edu.utng.compasos_wearos.presentation.theme.Negro
import mx.edu.utng.compasos_wearos.ui.screens.PantallaConfirmacionVinculacion
import mx.edu.utng.compasos_wearos.ui.screens.PantallaInicio
import mx.edu.utng.compasos_wearos.ui.screens.PantallaVinculacion
import mx.edu.utng.compasos_wearos.ui.screens.components.DashboardScreen
import mx.edu.utng.compasos_wearos.viewmodel.VinculacionViewModel

@Composable
fun AppNav(
    vm: VinculacionViewModel = viewModel()
) {
    val vinculado by vm.estaVinculado.collectAsState()

    if (vinculado == null) {
        CircularProgressIndicator()
        return
    }


    AppScaffold() {
        val navController = rememberSwipeDismissableNavController()

        SwipeDismissableNavHost(
            navController = navController,
            startDestination = "splash"
        ) {

            composable("splash") {
                LaunchedEffect(vinculado) {
                    if (vinculado == true) {
                        navController.navigate("menu_principal") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("pantalla_inicio") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Negro)
                )
            }

            composable("pantalla_inicio") {
                PantallaInicio(
                    onEmpezarClick = {
                        navController.navigate("pantalla_vinculacion")
                    }
                )
            }

            composable("pantalla_vinculacion") {
                PantallaVinculacion(
                    onVinculacionExitosa = {
                        navController.navigate("pantalla_confirmacion")
                    }
                )
            }

            composable("pantalla_confirmacion") {
                PantallaConfirmacionVinculacion(
                    nombreDispositivo = "Pixel 8 Pro",
                    nombreReloj = "Este reloj",
                    onVincular = {
                        vm.confirmarVinculacion()
                        navController.navigate("menu_principal") {
                            popUpTo("pantalla_inicio") { inclusive = true }
                        }
                    },
                    onCancelar = {
                        navController.popBackStack()
                    }
                )
            }

            // ── AQUÍ INTEGRAMOS TU DASHBOARD SCREEN COMO EL MENÚ PRINCIPAL ──
            composable("menu_principal") {
                DashboardScreen(
                    onIrAInicio = {
                        // Acción al presionar el botón Inicio de tu NavBar
                        // Por ejemplo, recargar la pantalla o limpiar el stack:
                        navController.navigate("menu_principal") {
                            popUpTo("menu_principal") { inclusive = true }
                        }
                    },
                    onIrAAjustes = {
                        // Acción al pulsar el engrane de Ajustes en tu NavBar
                        navController.navigate("pantalla_vinculacion") // O tu futura pantalla de ajustes
                    },
                    onDispararSOS = {
                        // Lógica inmediata cuando el usuario pise el botón "SOS" central
                    }
                )
            }
        }
    }
}
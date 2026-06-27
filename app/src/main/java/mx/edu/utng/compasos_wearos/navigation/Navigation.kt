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
import mx.edu.utng.compasos_wearos.data.VinculacionState
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
    val estado by vm.estado.collectAsState()

    if (vinculado == null) {
        CircularProgressIndicator()
        return
    }

    AppScaffold {
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

                // Arranca el polling al entrar a esta pantalla
                LaunchedEffect(Unit) {
                    vm.iniciarEsperaBluetooth()
                }

                // Navega según el estado
                LaunchedEffect(estado) {
                    when (estado) {
                        is VinculacionState.SolicitudRecibida -> {
                            navController.navigate("pantalla_confirmacion")
                        }
                        is VinculacionState.Vinculado -> {
                            navController.navigate("menu_principal") {
                                popUpTo("pantalla_inicio") { inclusive = true }
                            }
                        }
                        else -> {}
                    }
                }

                PantallaVinculacion(
                    viewModel = vm,
                    onSolicitudRecibida = {},
                    onVinculacionExitosa = {}
                )
            }

            composable("pantalla_confirmacion") {

                // Recupera el nombre real del teléfono del estado actual
                val nombreTelefono = (estado as? VinculacionState.SolicitudRecibida)
                    ?.nombreTelefono ?: "Teléfono"

                PantallaConfirmacionVinculacion(
                    nombreDispositivo = nombreTelefono,
                    nombreReloj = "Este reloj",
                    onVincular = {
                        vm.aceptarVinculacion()
                        navController.navigate("menu_principal") {
                            popUpTo("pantalla_inicio") { inclusive = true }
                        }
                    },
                    onCancelar = {
                        vm.cancelarVinculacion()
                        navController.popBackStack()
                    }
                )
            }

            composable("menu_principal") {
                DashboardScreen(
                    onIrAInicio = {
                        navController.navigate("menu_principal") {
                            popUpTo("menu_principal") { inclusive = true }
                        }
                    },
                    onIrAAjustes = {
                        navController.navigate("pantalla_vinculacion")
                    },
                    onDispararSOS = {
                        // Lógica SOS aquí
                    }
                )
            }
        }
    }
}
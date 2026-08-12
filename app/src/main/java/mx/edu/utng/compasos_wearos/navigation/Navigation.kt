package mx.edu.utng.compasos_wearos.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import kotlinx.coroutines.delay
import mx.edu.utng.compasos_wearos.data.VinculacionState
import mx.edu.utng.compasos_wearos.presentation.theme.Negro
import mx.edu.utng.compasos_wearos.ui.screens.AlertaEnviadaScreen
import mx.edu.utng.compasos_wearos.ui.screens.MovimientoInusualScreen
import mx.edu.utng.compasos_wearos.ui.screens.PantallaAcercaDispositivo
import mx.edu.utng.compasos_wearos.ui.screens.PantallaAjustes
import mx.edu.utng.compasos_wearos.ui.screens.PantallaAjustesAlerta
import mx.edu.utng.compasos_wearos.ui.screens.PantallaIngresoCodigo
import mx.edu.utng.compasos_wearos.ui.screens.PantallaInicio
import mx.edu.utng.compasos_wearos.ui.screens.PantallaVinculacion
import mx.edu.utng.compasos_wearos.ui.screens.components.DashboardScreen
import mx.edu.utng.compasos_wearos.viewmodel.VinculacionViewModel

// TODO: cambiar a false antes de publicar

@Composable
fun AppNav(
    vm: VinculacionViewModel = viewModel()
) {
    val vinculado               by vm.estaVinculado.collectAsState()
    val estado                  by vm.estado.collectAsState()
    val mostrarAlertaMovimiento by vm.mostrarAlertaMovimiento.collectAsState()
    val alertaEnviada           by vm.alertaEnviada.collectAsState()

    if (vinculado == null) {
        CircularProgressIndicator()
        return
    }

    // ───────────────── Overlay: Alerta ya enviada ─────────────────
    if (alertaEnviada) {

        AlertaEnviadaScreen()

        LaunchedEffect(Unit) {
            delay(5000)
            vm.ocultarAlertaEnviada()
            vm.ocultarOverlayMovimiento()
        }

        return
    }

    // ───────────────── Overlay: Movimiento inusual ─────────────────
    if (mostrarAlertaMovimiento) {

        MovimientoInusualScreen(
            onEnviar = {
                vm.confirmarAlertaMovimiento()
            },
            onCancelar = {
                vm.descartarAlertaMovimiento()
            }
        )

        return
    }

    AppScaffold(timeText = {}) {
        val navController = rememberSwipeDismissableNavController()

        SwipeDismissableNavHost(
            navController    = navController,
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

            // ── Espera a que llegue la solicitud del teléfono por MQTT ──
            composable("pantalla_vinculacion") {

                LaunchedEffect(estado) {
                    when (val estadoActual = estado) {
                        is VinculacionState.SolicitudRecibida -> {
                            estadoActual.codigoEsperado?.let {
                                navController.navigate("pantalla_codigo")
                            }
                        }
                        is VinculacionState.Vinculado ->
                            navController.navigate("menu_principal") {
                                popUpTo("pantalla_inicio") { inclusive = true }
                            }
                        else -> {}
                    }
                }

                PantallaVinculacion(
                    viewModel            = vm,
                    onSolicitudConCodigo = {},
                    onVinculacionExitosa = {}
                )
            }

            // ── Teclado para el código MQTT ──────
            composable("pantalla_codigo") {

                LaunchedEffect(estado) {
                    when (estado) {
                        is VinculacionState.Vinculado ->
                            navController.navigate("menu_principal") {
                                popUpTo("pantalla_inicio") { inclusive = true }
                            }
                        is VinculacionState.Esperando ->
                            // Se canceló o expiró: regresa
                            navController.popBackStack("pantalla_vinculacion", inclusive = false)
                        else -> {}
                    }
                }

                PantallaIngresoCodigo(
                    viewModel  = vm,
                    onCancelar = {
                        vm.cancelarVinculacionMqtt()
                        navController.popBackStack()
                    }
                )
            }

            composable("menu_principal") {
                DashboardScreen(
                    onIrAInicio      = {
                        navController.navigate("menu_principal") {
                            popUpTo("menu_principal") { inclusive = true }
                        }
                    },
                    onIrAAjustes     = {
                        navController.navigate("pantalla_ajustes")
                    },
                    onDispararSOS    = {
                        vm.dispararSOS()
                    },
                    onSwipeIzquierda = {
                        navController.navigate("pantalla_ajustes") {
                            launchSingleTop = true
                        }
                    }

                )
            }

            composable("pantalla_ajustes") {
                PantallaAjustes(
                    onAjustesAlertaClick = {
                        navController.navigate("ajustes_alerta")
                    },
                    onAcercaClick = {
                        navController.navigate("acerca_dispositivo")
                    }
                )
            }

            composable("ajustes_alerta") {
                PantallaAjustesAlerta(vm = vm)
            }

            composable("acerca_dispositivo") {
                PantallaAcercaDispositivo(vm = vm)
            }
        }
    }
}
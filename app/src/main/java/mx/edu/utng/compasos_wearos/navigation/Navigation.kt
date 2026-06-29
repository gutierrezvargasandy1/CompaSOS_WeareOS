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
import mx.edu.utng.compasos_wearos.data.VinculacionState
import mx.edu.utng.compasos_wearos.presentation.theme.Negro
import mx.edu.utng.compasos_wearos.ui.screens.AlertaEnviadaScreen
import mx.edu.utng.compasos_wearos.ui.screens.MovimientoInusualScreen
import mx.edu.utng.compasos_wearos.ui.screens.PantallaAcercaDispositivo
import mx.edu.utng.compasos_wearos.ui.screens.PantallaAjustes
import mx.edu.utng.compasos_wearos.ui.screens.PantallaAjustesAlerta
import mx.edu.utng.compasos_wearos.ui.screens.PantallaConfirmacionVinculacion
import mx.edu.utng.compasos_wearos.ui.screens.PantallaInicio
import mx.edu.utng.compasos_wearos.ui.screens.PantallaVinculacion
import mx.edu.utng.compasos_wearos.ui.screens.components.DashboardScreen
import mx.edu.utng.compasos_wearos.viewmodel.VinculacionViewModel

// TODO: cambiar a false antes de publicar
private const val ES_DEBUG = true

@Composable
fun AppNav(
    vm: VinculacionViewModel = viewModel()
) {
    val vinculado               by vm.estaVinculado.collectAsState()
    val estado                  by vm.estado.collectAsState()
    val mostrarAlertaMovimiento by vm.mostrarAlertaMovimiento.collectAsState()

    // Este estado controla localmente qué pantalla del overlay mostrar
    var alertaYaEnviada by remember { mutableStateOf(false) }

    // Si desde el ViewModel se apaga por completo la alerta (ej. al cancelarla),
    // limpiamos el flag local para futuras alertas.
    LaunchedEffect(mostrarAlertaMovimiento) {
        if (!mostrarAlertaMovimiento) {
            alertaYaEnviada = false
        }
    }

    if (vinculado == null) {
        CircularProgressIndicator()
        return
    }

    // ── Overlay movimiento inusual e histórico de alertas — encima de todo ───────────
// ── Overlay movimiento inusual e histórico de alertas — encima de todo ───────────
// ───────────────── Overlay SOS ─────────────────
    if (mostrarAlertaMovimiento) {

        if (alertaYaEnviada) {

            AlertaEnviadaScreen()

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(5000)

                alertaYaEnviada = false
                vm.ocultarOverlayMovimiento()
            }

        } else {

            MovimientoInusualScreen(

                onEnviar = {
                    alertaYaEnviada = true
                    vm.confirmarAlertaMovimiento()
                },

                onCancelar = {
                    alertaYaEnviada = false
                    vm.descartarAlertaMovimiento()
                }
            )
        }

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

            composable("pantalla_vinculacion") {
                LaunchedEffect(Unit) { vm.iniciarEsperaBluetooth() }
                LaunchedEffect(estado) {
                    when (estado) {
                        is VinculacionState.SolicitudRecibida ->
                            navController.navigate("pantalla_confirmacion")
                        is VinculacionState.Vinculado ->
                            navController.navigate("menu_principal") {
                                popUpTo("pantalla_inicio") { inclusive = true }
                            }
                        else -> {}
                    }
                }
                PantallaVinculacion(
                    viewModel            = vm,
                    onSolicitudRecibida  = {},
                    onVinculacionExitosa = {}
                )
            }

            composable("pantalla_confirmacion") {
                val nombreTelefono = (estado as? VinculacionState.SolicitudRecibida)
                    ?.nombreTelefono ?: "Teléfono"

                PantallaConfirmacionVinculacion(
                    nombreDispositivo = nombreTelefono,
                    nombreReloj       = "Este reloj",
                    onVincular        = {
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
                    },
                    onTestMovimiento = if (ES_DEBUG) {
                        { vm.simularMovimiento() }
                    } else {
                        {}
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
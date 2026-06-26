package mx.edu.utng.compasos_wearos.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import mx.edu.utng.compasos_wearos.ui.screens.PantallaInicio
import mx.edu.utng.compasos_wearos.viewmodel.VinculacionViewModel

@Composable
fun AppNav(
    vm: VinculacionViewModel = viewModel()
) {
    val vinculado by vm.estaVinculado.collectAsState()

    // Mientras carga DataStore mostramos un spinner
    if (vinculado == null) {
        CircularProgressIndicator()
        return
    }

    // Destino inicial según si ya está vinculado
    val inicio = if (vinculado == true) "menu_principal" else "pantalla_inicio"

    AppScaffold {
        val navController = rememberSwipeDismissableNavController()

        SwipeDismissableNavHost(
            navController = navController,
            startDestination = inicio
        ) {

            // Solo aparece si NO está vinculado
            composable("pantalla_inicio") {
                PantallaInicio(
                    onEmpezarClick = {
                        vm.confirmarVinculacion()          // guarda el flag
                        navController.navigate("menu_principal") {
                            popUpTo("pantalla_inicio") { inclusive = true } // la saca del stack
                        }
                    }
                )
            }




        }
    }
}
package mx.edu.utng.compasos_wearos.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

@Composable

fun AppNav(){
    AppScaffold() {  //engloba
        val navController= rememberSwipeDismissableNavController() //el chofer-sabe a donde ir
        SwipeDismissableNavHost(
            navController=navController,
            startDestination= "menu_principal"
        ){

            // --- NUESTRO MAPA DE RUTAS ---

            // Ruta A: Menú Principal

            composable("menu_principal"){
                PantallaMenu(onIrASalud = {
                    navController.navigate("pantalla_salud") // Le ordenamos al chofer ir a la otra pantalla
                })
            }

            // Ruta B: Tu pantalla de Salud
            composable("pantalla_salud"){
                PantallaSalud()
            }
        }
    }
}
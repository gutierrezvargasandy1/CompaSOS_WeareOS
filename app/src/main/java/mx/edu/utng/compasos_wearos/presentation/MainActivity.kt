package mx.edu.utng.compasos_wearos.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import mx.edu.utng.compasos_wearos.R
import mx.edu.utng.compasos_wearos.navigation.AppNav
import mx.edu.utng.compasos_wearos.presentation.theme.CompaSOSTheme

/**
 * actividad principal de la aplicación wear os que actúa como el punto de entrada del ciclo de vida de android.
 * se encarga de configurar la pantalla de bienvenida (splash screen), establecer el tema visual
 * e inicializar el árbol de componentes mediante jetpack compose.
 */
class MainActivity : ComponentActivity() {

    /**
     * método llamado cuando se crea la actividad. inicializa los componentes visuales,
     * instala la pantalla de carga nativa y define la jerarquía de navegación composable.
     *
     * @param savedInstanceState contenedor opcional con el estado previamente guardado de la actividad.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContent {
            CompaSOSTheme {
                AppNav()
            }
        }
    }
}
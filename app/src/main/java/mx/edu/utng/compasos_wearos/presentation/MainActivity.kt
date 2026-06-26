package mx.edu.utng.compasos_wearos.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import mx.edu.utng.compasos_wearos.R
import mx.edu.utng.compasos_wearos.navigation.AppNav
import mx.edu.utng.compasos_wearos.presentation.theme.CompaSOSTheme

class MainActivity : ComponentActivity() {
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
package mx.edu.utng.compasos_wearos.viewmodel

//*El NavBarViewModel actúa como el cerebro y protector del estado de la barra.
// mantiene la barra oculta dentro de mutableStateOf(false) inicia con el estado falso y
// recuerda mi barra en caso de un apagon del dispositivo
// mostrarNavBar() → muestra la barra (swipe hacia arriba)
// ocultarNavBar() → esconde la barra (swipe hacia abajo)
// alternarNavBar() → sigue funcionando para el long press

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class NavBarViewModel : ViewModel() {
    // Caja privada que muta (cambia)
    private val _mostrarNavBar = mutableStateOf(false)
    // Alarma pública de solo lectura para la pantalla
    val mostrarNavBar: State<Boolean> = _mostrarNavBar

    // Swipe hacia ARRIBA → muestra la barra
    fun mostrarNavBar() {
        _mostrarNavBar.value = true
    }

    // Swipe hacia ABAJO → esconde la barra
    fun ocultarNavBar() {
        _mostrarNavBar.value = false
    }

    // Long press → alterna (se mantiene por compatibilidad)
    fun alternarNavBar() {
        _mostrarNavBar.value = !_mostrarNavBar.value
    }
}
package mx.edu.utng.compasos_wearos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.edu.utng.compasos_wearos.data.VinculacionEvent
import mx.edu.utng.compasos_wearos.data.VinculacionManager
import mx.edu.utng.compasos_wearos.data.VinculacionState
import mx.edu.utng.compasos_wearos.data.repository.VinculacionPrefs

class VinculacionViewModel(app: Application) : AndroidViewModel(app) {

    private val manager = VinculacionManager()

    val estado = manager.estado

    val estaVinculado: StateFlow<Boolean?> =
        VinculacionPrefs
            .estaVinculado(app)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    fun iniciarEsperaBluetooth() {
        viewModelScope.launch {
            while (manager.estado.value is VinculacionState.Esperando) {
                manager.detectarTelefonoConectado(getApplication())
                delay(2000L)
            }
        }
    }

    fun recibirSolicitud(nombreTelefono: String) {
        manager.procesarEvento(
            VinculacionEvent.SolicitudVinculacion(
                nombreTelefono
            )
        )
    }

    fun aceptarVinculacion() {
        manager.procesarEvento(
            VinculacionEvent.Confirmar
        )
        viewModelScope.launch {
            VinculacionPrefs.marcarVinculado(
                getApplication()
            )
            manager.vinculacionExitosa()
        }
    }

    fun cancelarVinculacion() {
        manager.procesarEvento(
            VinculacionEvent.Cancelar
        )
    }

}
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
import mx.edu.utng.compasos_wearos.data.VinculacionState
import mx.edu.utng.compasos_wearos.data.entity.ConfigReloj
import mx.edu.utng.compasos_wearos.data.repository.VinculacionPrefs
import mx.edu.utng.compasos_wearos.data.repository.VinculacionRepository

class VinculacionViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = VinculacionRepository(app)

    // ── Estado de vinculación (flujo de eventos) ─────────────
    val estado: StateFlow<VinculacionState> = repo.vinculacionManager.estado

    // ── Flag DataStore (primera vez / ya vinculado) ──────────
    val estaVinculado: StateFlow<Boolean?> = VinculacionPrefs
        .estaVinculado(app)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // ── Config de Room en tiempo real ────────────────────────
    val configReloj: StateFlow<ConfigReloj?> = repo.observarConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // ── Inicia espera de Bluetooth ───────────────────────────
    fun iniciarEsperaBluetooth() {
        viewModelScope.launch {
            while (true) {
                repo.detectarYGuardarTelefono(getApplication())
                delay(3_000)
            }
        }
    }

    // ── Acepta vinculación → guarda en DataStore y Room ─────
    fun aceptarVinculacion() {
        viewModelScope.launch {
            val estadoActual = estado.value
            if (estadoActual is VinculacionState.SolicitudRecibida) {
                repo.guardarVinculacion(
                    nodeId = estadoActual.nombreTelefono,
                    nombreTelefono = estadoActual.nombreTelefono
                )
            }
            repo.vinculacionManager.vinculacionExitosa()
            VinculacionPrefs.marcarVinculado(getApplication())
        }
    }

    // ── Cancela vinculación ──────────────────────────────────
    fun cancelarVinculacion() {
        repo.vinculacionManager.procesarEvento(VinculacionEvent.Cancelar)
    }

    // ── Confirma vinculación (DataStore) ─────────────────────
    fun confirmarVinculacion() {
        viewModelScope.launch {
            VinculacionPrefs.marcarVinculado(getApplication())
        }
    }

    fun setModoDiscreto(activo: Boolean) {
        viewModelScope.launch {
            repo.setModoDiscreto(activo)
        }
    }

    fun setTiempoPanico(segundos: Int) {
        viewModelScope.launch {
            repo.setTiempoPanico(segundos)
        }
    }
}
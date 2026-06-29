package mx.edu.utng.compasos_wearos.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mx.edu.utng.compasos_wearos.data.VinculacionEvent
import mx.edu.utng.compasos_wearos.data.VinculacionState
import mx.edu.utng.compasos_wearos.data.entity.ConfigReloj
import mx.edu.utng.compasos_wearos.data.repository.VinculacionPrefs
import mx.edu.utng.compasos_wearos.data.repository.VinculacionRepository
import mx.edu.utng.compasos_wearos.services.MovimientoDetector

class VinculacionViewModel(app: Application) : AndroidViewModel(app) {

    private val repo               = VinculacionRepository(app)
    private val movimientoDetector = MovimientoDetector(app)

    val estado: StateFlow<VinculacionState> = repo.vinculacionManager.estado

    val estaVinculado: StateFlow<Boolean?> = VinculacionPrefs
        .estaVinculado(app)
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val configReloj: StateFlow<ConfigReloj?> = repo.observarConfig()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _mostrarAlertaMovimiento = MutableStateFlow(false)
    val mostrarAlertaMovimiento: StateFlow<Boolean> = _mostrarAlertaMovimiento

    init {
        // ── Observa config → activa/desactiva detector ────────
        viewModelScope.launch {
            configReloj.collect { config ->
                // Espera a tener config real (no null)
                if (config == null) return@collect

                if (config.modoDiscreto) {
                    movimientoDetector.iniciar(
                        umbral = config.sensibilidadCaida
                            .takeIf { it >= 18f } ?: 22f
                    )
                } else {
                    movimientoDetector.detener()
                }
            }
        }

        // ── Escucha disparos del detector ─────────────────────
        viewModelScope.launch {
            movimientoDetector.movimientoErratico.collect {
                // Solo abre la pantalla si:
                // 1. No hay ya una alerta visible
                // 2. El usuario ya está en el dashboard (vinculado)
                if (!_mostrarAlertaMovimiento.value && estaVinculado.value == true) {
                    _mostrarAlertaMovimiento.value = true
                }
            }
        }
    }

    fun descartarAlertaMovimiento() {
        _mostrarAlertaMovimiento.value = false
    }

    fun confirmarAlertaMovimiento() {

        dispararSOS()

        // NO ocultar el overlay aquí.
    }

    fun dispararSOS() {
        viewModelScope.launch {

            // Aquí va el envío real de la alerta

            // Esperar unos segundos mostrando "Alerta enviada"

            _mostrarAlertaMovimiento.value = false
        }
    }

    fun iniciarEsperaBluetooth() {
        viewModelScope.launch {
            while (true) {
                repo.detectarYGuardarTelefono(getApplication())
                delay(3_000)
            }
        }
    }

    fun aceptarVinculacion() {
        viewModelScope.launch {
            val estadoActual = estado.value
            if (estadoActual is VinculacionState.SolicitudRecibida) {
                repo.guardarVinculacion(
                    nodeId         = estadoActual.nombreTelefono,
                    nombreTelefono = estadoActual.nombreTelefono
                )
            }
            repo.vinculacionManager.vinculacionExitosa()
            VinculacionPrefs.marcarVinculado(getApplication())
        }
    }

    fun cancelarVinculacion() {
        repo.vinculacionManager.procesarEvento(VinculacionEvent.Cancelar)
    }

    fun confirmarVinculacion() {
        viewModelScope.launch {
            VinculacionPrefs.marcarVinculado(getApplication())
        }
    }

    fun setModoDiscreto(activo: Boolean) {
        viewModelScope.launch { repo.setModoDiscreto(activo) }
    }

    fun setTiempoPanico(segundos: Int) {
        viewModelScope.launch { repo.setTiempoPanico(segundos) }
    }

    override fun onCleared() {
        super.onCleared()
        movimientoDetector.detener()
    }

    // En VinculacionViewModel.kt
    fun simularMovimiento() {
        _mostrarAlertaMovimiento.value = true

    }

    fun ocultarOverlayMovimiento() {
        _mostrarAlertaMovimiento.value = false
    }
}
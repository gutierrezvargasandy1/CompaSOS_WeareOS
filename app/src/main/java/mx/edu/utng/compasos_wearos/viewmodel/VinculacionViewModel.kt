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

    // ── Overlay: movimiento brusco detectado ──────────────────
    private val _mostrarAlertaMovimiento = MutableStateFlow(false)
    val mostrarAlertaMovimiento: StateFlow<Boolean> = _mostrarAlertaMovimiento

    // ── Overlay: alerta ya enviada (pantalla final) ───────────
    private val _alertaEnviada = MutableStateFlow(false)
    val alertaEnviada: StateFlow<Boolean> = _alertaEnviada

    init {
        // ── NUEVO: empieza a escuchar solicitudes de vinculación MQTT ──
        // El código llega por MQTT, se guarda en Room, y el estado pasa
        // a SolicitudRecibida(codigoEsperado = ...) para que la UI pida
        // que el usuario lo teclee.
        viewModelScope.launch {
            repo.escucharSolicitudesVinculacion()
        }

        // ── Observa config → activa/desactiva detector ────────
        viewModelScope.launch {
            configReloj.collect { config ->
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
                if (!_mostrarAlertaMovimiento.value &&
                    !_alertaEnviada.value &&
                    estaVinculado.value == true
                ) {
                    _mostrarAlertaMovimiento.value = true
                }
            }
        }
    }

    // ── Movimiento inusual: el usuario cancela manualmente ────
    fun descartarAlertaMovimiento() {
        _mostrarAlertaMovimiento.value = false
    }

    // ── Movimiento inusual: confirmado (botón o timeout) ──────
    fun confirmarAlertaMovimiento() {
        _mostrarAlertaMovimiento.value = false
        dispararSOS()
    }

    // ── SOS manual (desde el Dashboard) o desde movimiento ────
    fun dispararSOS() {
        viewModelScope.launch {
            // Aquí va el envío real de la alerta (red, bluetooth, etc.)
            _alertaEnviada.value = true
        }
    }

    // ── Oculta la pantalla de "Alerta enviada" ────────────────
    fun ocultarAlertaEnviada() {
        _alertaEnviada.value = false
    }

    // ── Flujo VIEJO: detección automática por Wearable Node API ──
    fun iniciarEsperaBluetooth() {
        viewModelScope.launch {
            while (true) {
                repo.detectarYGuardarTelefono(getApplication())
                delay(3_000)
            }
        }
    }

    // ── Flujo VIEJO: aceptar sin código (Node detectado directo) ──
    fun aceptarVinculacion() {
        viewModelScope.launch {
            val estadoActual = estado.value
            if (estadoActual is VinculacionState.SolicitudRecibida &&
                estadoActual.codigoEsperado == null
            ) {
                repo.guardarVinculacion(
                    nodeId         = estadoActual.nombreTelefono,
                    nombreTelefono = estadoActual.nombreTelefono
                )
                repo.vinculacionManager.vinculacionExitosa()
                VinculacionPrefs.marcarVinculado(getApplication())
            }
        }
    }

    // ── NUEVO: flujo MQTT — el usuario teclea el código del teléfono ──
    fun ingresarCodigo(codigo: String) {
        viewModelScope.launch {
            val exito = repo.confirmarCodigo(codigo)
            if (exito) {
                VinculacionPrefs.marcarVinculado(getApplication())
            }
            // Si no coincide, el estado ya cambió solo a CodigoIncorrecto
        }
    }

    // ── NUEVO: cancelar mientras se espera/teclea el código MQTT ──
    fun cancelarVinculacionMqtt() {
        viewModelScope.launch {
            repo.cancelarVinculacionPendiente()
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

    fun simularMovimiento() {
        _mostrarAlertaMovimiento.value = true
    }

    fun ocultarOverlayMovimiento() {
        _mostrarAlertaMovimiento.value = false
    }
}
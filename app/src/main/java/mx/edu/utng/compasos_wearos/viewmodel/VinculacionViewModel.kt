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
import mx.edu.utng.compasos_wearos.data.repository.AlertaWearRepository
import mx.edu.utng.compasos_wearos.data.repository.VinculacionPrefs
import mx.edu.utng.compasos_wearos.data.repository.VinculacionRepository
import mx.edu.utng.compasos_wearos.services.MovimientoDetector

/**
 * viewmodel principal de la aplicación wear os que gestiona la lógica de negocio,
 * el estado de vinculación, la configuración del reloj y la detección de movimientos inusuales o sos.
 *
 * @param app instancia de la aplicación [Application] requerida por [AndroidViewModel].
 */
class VinculacionViewModel(app: Application) : AndroidViewModel(app) {

    /** repositorio encargado de la gestión de vinculación y estado del dispositivo. */
    private val repo               = VinculacionRepository(app)
    /** componente detector de movimientos erráticos mediante el acelerómetro. */
    private val movimientoDetector = MovimientoDetector(app)

    /** flujo de estado expuesto que refleja el estado actual de la vinculación en el gestor. */
    val estado: StateFlow<VinculacionState> = repo.vinculacionManager.estado
    /** repositorio encargado de procesar y enviar las alertas de emergencia sos. */
    private val alertaRepo = AlertaWearRepository(app)
    /** identificador de la alerta activa actual, si existe. */
    private var alertaActivaId:      String? = null
    /** identificador del dispositivo asociado a la alerta activa actual, si existe. */
    private var alertaDispositivoId: String? = null

    /** flujo de estado que indica si el reloj se encuentra vinculado al teléfono de forma persistente. */
    val estaVinculado: StateFlow<Boolean?> = VinculacionPrefs
        .estaVinculado(app)
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /** flujo de estado que observa los cambios en la configuración local del reloj. */
    val configReloj: StateFlow<ConfigReloj?> = repo.observarConfig()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // ── Overlay: movimiento brusco detectado ──────────────────
    /** flujo mutable interno para controlar la visibilidad de la superposición de movimiento brusco. */
    private val _mostrarAlertaMovimiento = MutableStateFlow(false)
    /** flujo expuesto de sólo lectura que indica si se debe mostrar la alerta de movimiento inusual. */
    val mostrarAlertaMovimiento: StateFlow<Boolean> = _mostrarAlertaMovimiento

    // ── Overlay: alerta ya enviada (pantalla final) ───────────
    /** flujo mutable interno que indica si el estado visual corresponde a la pantalla de alerta ya enviada. */
    private val _alertaEnviada = MutableStateFlow(false)
    /** flujo expuesto de sólo lectura que señala si la alerta ha sido enviada con éxito. */
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

    /**
     * descarta manualmente la alerta de movimiento inusual detectada y oculta el overlay.
     */
    fun descartarAlertaMovimiento() {
        _mostrarAlertaMovimiento.value = false
    }

    /**
     * confirma el estado de movimiento inusual, ocultando el aviso y disparando una emergencia sos.
     */
    fun confirmarAlertaMovimiento() {
        _mostrarAlertaMovimiento.value = false
        dispararSOS()
    }

    /**
     * dispara el envío de una señal de emergencia sos (ya sea de forma manual o por detección de movimiento).
     */
    fun dispararSOS() {
        viewModelScope.launch {
            val resultado = alertaRepo.enviarSOS()
            if (resultado != null) {
                alertaActivaId      = resultado.first
                alertaDispositivoId = resultado.second
                // Sigue enviando la ubicación cada 30 s durante 5 min
                iniciarUbicacionPeriodica(resultado.first, resultado.second)
            }
            _alertaEnviada.value = true
        }
    }

    /**
     * simula un envío periódico de ubicación en segundo plano tras disparar una alerta sos.
     *
     * @param alertaId identificador único de la alerta generada.
     * @param dispositivoId identificador único del dispositivo emisor.
     */
    private fun iniciarUbicacionPeriodica(alertaId: String, dispositivoId: String) {
        viewModelScope.launch {
            repeat(10) {
                delay(30_000L)
            }
        }
    }

    /**
     * oculta la pantalla o superposición de "alerta enviada".
     */
    fun ocultarAlertaEnviada() {
        _alertaEnviada.value = false
    }

    /**
     * inicia un bucle de espera por bluetooth utilizando la api de nodos wearable para detectar el teléfono.
     */
    fun iniciarEsperaBluetooth() {
        viewModelScope.launch {
            while (true) {
                repo.detectarYGuardarTelefono(getApplication())
                delay(3_000)
            }
        }
    }

    /**
     * acepta la solicitud de vinculación de manera directa cuando se detecta automáticamente por nodo.
     */
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

    /**
     * procesa e ingresa el código de verificación proporcionado por el usuario para completar la vinculación por mqtt.
     *
     * @param codigo cadena con el código alfanumérico ingresado.
     */
    fun ingresarCodigo(codigo: String) {
        viewModelScope.launch {
            val exito = repo.confirmarCodigo(codigo)
            if (exito) {
                VinculacionPrefs.marcarVinculado(getApplication())
            }
            // Si no coincide, el estado ya cambió solo a CodigoIncorrecto
        }
    }

    /**
     * cancela el proceso de vinculación basado en mqtt y restablece el estado pendiente.
     */
    fun cancelarVinculacionMqtt() {
        viewModelScope.launch {
            repo.cancelarVinculacionPendiente()
        }
    }

    /**
     * procesa un evento general de cancelación de vinculación en el gestor.
     */
    fun cancelarVinculacion() {
        repo.vinculacionManager.procesarEvento(VinculacionEvent.Cancelar)
    }

    /**
     * marca formalmente al dispositivo como vinculado guardando el estado en las preferencias locales.
     */
    fun confirmarVinculacion() {
        viewModelScope.launch {
            VinculacionPrefs.marcarVinculado(getApplication())
        }
    }

    /**
     * actualiza la configuración del modo discreto en el repositorio local.
     *
     * @param activo valor booleano que define si el modo discreto se encuentra habilitado.
     */
    fun setModoDiscreto(activo: Boolean) {
        viewModelScope.launch { repo.setModoDiscreto(activo) }
    }

    /**
     * actualiza el tiempo de pánico o umbral de tiempo en la configuración local.
     *
     * @param segundos valor entero con los segundos de duración configurados.
     */
    fun setTiempoPanico(segundos: Int) {
        viewModelScope.launch { repo.setTiempoPanico(segundos) }
    }

    /**
     * método invocado cuando el viewmodel es destruido; libera los recursos del detector de movimiento.
     */
    override fun onCleared() {
        super.onCleared()
        movimientoDetector.detener()
    }

    /**
     * simula de manera manual la detección de un movimiento brusco para propósitos de prueba.
     */
    fun simularMovimiento() {
        _mostrarAlertaMovimiento.value = true
    }

    /**
     * oculta la superposición de movimiento brusco de forma explícita.
     */
    fun ocultarOverlayMovimiento() {
        _mostrarAlertaMovimiento.value = false
    }
}
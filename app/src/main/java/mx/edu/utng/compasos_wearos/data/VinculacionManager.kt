package mx.edu.utng.compasos_wearos.data

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * gestor de estado y lógica para administrar el ciclo de vida del proceso de vinculación
 * del dispositivo wear os utilizando flows y la api de google play services wear.
 */
class VinculacionManager {

    /** flujo mutable interno que almacena el estado actual de la vinculación. */
    private val _estado = MutableStateFlow<VinculacionState>(VinculacionState.Esperando)

    /**
     * flujo expuesto de sólo lectura que emite los cambios de estado del proceso de vinculación.
     */
    val estado: StateFlow<VinculacionState> = _estado.asStateFlow()

    /**
     * detecta si existe un teléfono vinculado y conectado mediante la api de nodos de wear os.
     *
     * @param context contexto de la aplicación necesario para acceder al cliente de nodos wearable.
     */
    suspend fun detectarTelefonoConectado(context: Context) {
        try {
            val nodos = Wearable.getNodeClient(context).connectedNodes.await()
            val telefono = nodos.firstOrNull()
            if (telefono != null) {
                procesarEvento(VinculacionEvent.SolicitudVinculacion(telefono.displayName))
            }
        } catch (e: Exception) {
            // Sin conexión todavía
        }
    }

    /**
     * procesa un evento recibido y actualiza la máquina de estados de la vinculación en consecuencia.
     *
     * @param evento objeto [VinculacionEvent] que representa la acción o suceso a procesar.
     */
    fun procesarEvento(evento: VinculacionEvent) {
        when (evento) {
            is VinculacionEvent.BuscarReloj ->
                _estado.value = VinculacionState.Esperando

            is VinculacionEvent.SolicitudVinculacion ->
                _estado.value = VinculacionState.SolicitudRecibida(evento.nombreTelefono)

            is VinculacionEvent.Confirmar ->
                _estado.value = VinculacionState.Vinculando

            is VinculacionEvent.Cancelar ->
                _estado.value = VinculacionState.Esperando

            is VinculacionEvent.Desconectar ->
                _estado.value = VinculacionState.Esperando

            // ── NUEVO ──────────────────────────────────────────
            is VinculacionEvent.SolicitudMqttRecibida ->
                _estado.value = VinculacionState.SolicitudRecibida(
                    nombreTelefono = "Teléfono",
                    codigoEsperado = evento.codigo,
                    usuarioId = evento.usuarioId
                )

            is VinculacionEvent.CodigoIncorrecto -> {
                val actual = _estado.value
                if (actual is VinculacionState.SolicitudRecibida && actual.codigoEsperado != null) {
                    _estado.value = VinculacionState.CodigoIncorrecto(actual.codigoEsperado, actual.usuarioId)
                }
            }
        }
    }

    /**
     * actualiza el estado de la vinculación indicando que el proceso concluyó de forma exitosa.
     */
    fun vinculacionExitosa() {
        _estado.value = VinculacionState.Vinculado
    }

    /**
     * actualiza el estado de la vinculación reflejando un mensaje de error específico.
     *
     * @param mensaje descripción textual del error ocurrido.
     */
    fun error(mensaje: String) {
        _estado.value = VinculacionState.Error(mensaje)
    }
}
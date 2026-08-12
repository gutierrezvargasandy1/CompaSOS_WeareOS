package mx.edu.utng.compasos_wearos.data

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class VinculacionManager {

    private val _estado = MutableStateFlow<VinculacionState>(VinculacionState.Esperando)
    val estado: StateFlow<VinculacionState> = _estado.asStateFlow()

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

    fun vinculacionExitosa() {
        _estado.value = VinculacionState.Vinculado
    }

    fun error(mensaje: String) {
        _estado.value = VinculacionState.Error(mensaje)
    }
}
package mx.edu.utng.compasos_wearos.data

import android.content.Context
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await          // para .await()
class VinculacionManager {

    private val _estado =
        MutableStateFlow<VinculacionState>(VinculacionState.Esperando)

    val estado: StateFlow<VinculacionState> = _estado.asStateFlow()

    // ── NUEVO: detecta el teléfono conectado ──────────────────────────
    suspend fun detectarTelefonoConectado(context: Context) {
        try {
            val nodos = Wearable.getNodeClient(context)
                .connectedNodes
                .await()

            val telefono = nodos.firstOrNull()

            if (telefono != null) {
                // Teléfono encontrado → disparar SolicitudRecibida con su nombre
                procesarEvento(
                    VinculacionEvent.SolicitudVinculacion(
                        telefono.displayName  // nombre real del emulador
                    )
                )
            }
            // Si la lista está vacía, seguimos en Esperando
        } catch (e: Exception) {
            // Sin conexión todavía, no hacemos nada
        }
    }
    // ─────────────────────────────────────────────────────────────────

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
        }
    }

    fun vinculacionExitosa() {
        _estado.value = VinculacionState.Vinculado
    }

    fun error(mensaje: String) {
        _estado.value = VinculacionState.Error(mensaje)
    }
}
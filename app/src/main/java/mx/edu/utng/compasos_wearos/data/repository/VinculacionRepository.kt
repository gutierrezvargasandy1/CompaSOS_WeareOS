package mx.edu.utng.compasos_wearos.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import mx.edu.utng.compasos_wearos.data.VinculacionManager
import mx.edu.utng.compasos_wearos.data.VinculacionState
import mx.edu.utng.compasos_wearos.data.dao.ConfigRelojDao
import mx.edu.utng.compasos_wearos.data.db.WearDatabase
import mx.edu.utng.compasos_wearos.data.entity.ConfigReloj

class VinculacionRepository(context: Context) {

    private val dao: ConfigRelojDao =
        WearDatabase.getInstance(context).configRelojDao()

    val vinculacionManager = VinculacionManager()

    // ── Observa la config en tiempo real ────────────────────
    fun observarConfig(): Flow<ConfigReloj?> = dao.observarConfig()

    // ── Guarda el dispositivo vinculado en Room ──────────────
    suspend fun guardarVinculacion(nodeId: String, nombreTelefono: String) {
        // Obtiene config actual o crea una nueva
        val configActual = dao.obtenerConfig() ?: ConfigReloj()
        dao.guardarConfig(
            configActual.copy(
                deviceIdVinculado = nodeId,
                nombreDispositivoVinculado = nombreTelefono,
                ultimaActualizacion = System.currentTimeMillis()
            )
        )
    }

    // ── Borra la vinculación de Room ─────────────────────────
    suspend fun borrarVinculacion() {
        val configActual = dao.obtenerConfig() ?: ConfigReloj()
        dao.guardarConfig(
            configActual.copy(
                deviceIdVinculado = "",
                nombreDispositivoVinculado = "",
                ultimaActualizacion = System.currentTimeMillis()
            )
        )
    }

    // ── Detecta teléfono y guarda en Room si lo encuentra ───
    suspend fun detectarYGuardarTelefono(context: Context) {
        vinculacionManager.detectarTelefonoConectado(context)

        // Si el estado cambió a SolicitudRecibida, guarda en Room
        val estadoActual = vinculacionManager.estado.value
        if (estadoActual is VinculacionState.SolicitudRecibida) {
            guardarVinculacion(
                nodeId = estadoActual.nombreTelefono, // TODO: pasar nodeId real cuando tengas el Node
                nombreTelefono = estadoActual.nombreTelefono
            )
        }
    }

    suspend fun setModoDiscreto(activo: Boolean) {
        // Si no existe config aún, crea una primero
        if (dao.obtenerConfig() == null) {
            dao.guardarConfig(ConfigReloj())
        }
        dao.setModoDiscreto(activo)
    }

    suspend fun setTiempoPanico(segundos: Int) {
        if (dao.obtenerConfig() == null) {
            dao.guardarConfig(ConfigReloj())
        }
        dao.setTiempoPanico(segundos * 1000) // convierte a ms
    }
}
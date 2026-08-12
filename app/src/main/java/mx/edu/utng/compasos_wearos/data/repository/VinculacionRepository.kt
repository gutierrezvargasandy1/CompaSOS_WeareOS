package mx.edu.utng.compasos_wearos.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.edu.utng.compasos_wearos.config.mqtt.MqttConfig
import mx.edu.utng.compasos_wearos.config.mqtt.MqttManager
import mx.edu.utng.compasos_wearos.data.VinculacionEvent
import mx.edu.utng.compasos_wearos.data.VinculacionManager
import mx.edu.utng.compasos_wearos.data.VinculacionState
import mx.edu.utng.compasos_wearos.data.dao.ConfigRelojDao
import mx.edu.utng.compasos_wearos.data.db.WearDatabase
import mx.edu.utng.compasos_wearos.data.entity.ConfigReloj
import org.json.JSONObject

class VinculacionRepository(private val context: Context) {

    private val dao: ConfigRelojDao =
        WearDatabase.getInstance(context).configRelojDao()

    private val mqtt = MqttManager()

    val vinculacionManager = VinculacionManager()

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observarConfig(): Flow<ConfigReloj?> = dao.observarConfig()

    suspend fun guardarVinculacion(nodeId: String, nombreTelefono: String) {
        val configActual = dao.obtenerConfig() ?: ConfigReloj()
        dao.guardarConfig(
            configActual.copy(
                deviceIdVinculado = nodeId,
                nombreDispositivoVinculado = nombreTelefono,
                codigoVinculacion = "",
                ultimaActualizacion = System.currentTimeMillis()
            )
        )
    }

    suspend fun borrarVinculacion() {
        val configActual = dao.obtenerConfig() ?: ConfigReloj()
        dao.guardarConfig(
            configActual.copy(
                deviceIdVinculado = "",
                nombreDispositivoVinculado = "",
                codigoVinculacion = "",
                ultimaActualizacion = System.currentTimeMillis()
            )
        )
    }

    suspend fun detectarYGuardarTelefono(context: Context) {
        vinculacionManager.detectarTelefonoConectado(context)
        val estadoActual = vinculacionManager.estado.value
        if (estadoActual is VinculacionState.SolicitudRecibida && estadoActual.codigoEsperado == null) {
            guardarVinculacion(
                nodeId = estadoActual.nombreTelefono,
                nombreTelefono = estadoActual.nombreTelefono
            )
        }
    }

    // ══════════════════════════════════════════════════════════
    // FLUJO MQTT CON CÓDIGO
    // ══════════════════════════════════════════════════════════

    suspend fun escucharSolicitudesVinculacion() {
        withContext(Dispatchers.IO) {
            try {
                if (!mqtt.estaConectado) mqtt.conectar()

                mqtt.suscribir("${MqttConfig.TOPIC_VINCULACION}/+/solicitud") { _, payload ->
                    repoScope.launch {
                        try {
                            val json      = JSONObject(payload)
                            val codigo    = json.getString("code")
                            val usuarioId = json.optString("usuarioId", "")

                            guardarCodigoPendiente(codigo)

                            vinculacionManager.procesarEvento(
                                VinculacionEvent.SolicitudMqttRecibida(codigo, usuarioId)
                            )
                        } catch (e: Exception) {
                            // payload inválido, se ignora
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("VinculacionRepo", "No se pudo conectar al broker: ${e.message}")
                vinculacionManager.error("Sin conexión al broker MQTT")
            }
        }
    }

    private suspend fun guardarCodigoPendiente(codigo: String) {
        if (dao.obtenerConfig() == null) dao.guardarConfig(ConfigReloj())
        dao.setCodigoVinculacion(codigo)
    }

    /**
     * Valida el código tecleado contra el guardado en Room.
     *
     * CORREGIDO: ya no depende de que el estado actual sea exactamente
     * SolicitudRecibida. Antes, si el primer intento fallaba (estado pasaba
     * a CodigoIncorrecto), cualquier reintento —aunque el código fuera
     * correcto— se rechazaba de inmediato por esta guarda, sin comparar
     * nada. Ahora solo depende de que exista un código pendiente en Room.
     */
    suspend fun confirmarCodigo(codigoIngresado: String): Boolean {
        val codigoGuardado = dao.obtenerConfig()?.codigoVinculacion.orEmpty()

        if (codigoGuardado.isBlank()) {
            // No hay ninguna solicitud pendiente
            return false
        }

        val coincide = codigoIngresado.trim().equals(codigoGuardado.trim(), ignoreCase = true)

        if (!coincide) {
            vinculacionManager.procesarEvento(VinculacionEvent.CodigoIncorrecto)
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                // Por si el cliente se desconectó (p. ej. al abrir el
                // teclado nativo en otra Activity), reconecta antes de publicar.
                if (!mqtt.estaConectado) mqtt.conectar()

                val deviceId = "wear_${Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)}"

                val respuesta = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("tipo", "reloj")
                    put("modelo", Build.MODEL)
                    put("fabricante", Build.MANUFACTURER)
                }.toString()

                mqtt.publicar("${MqttConfig.TOPIC_VINCULACION}/$codigoGuardado/respuesta", respuesta)

                guardarVinculacion(nodeId = deviceId, nombreTelefono = "Teléfono vinculado")
                vinculacionManager.vinculacionExitosa()
                true
            } catch (e: Exception) {
                Log.e("VinculacionRepo", "No se pudo enviar la respuesta: ${e.message}")
                vinculacionManager.error("Sin conexión, inténtalo de nuevo")
                false
            }
        }
    }

    suspend fun cancelarVinculacionPendiente() {
        dao.limpiarCodigoVinculacion()
        vinculacionManager.procesarEvento(VinculacionEvent.Cancelar)
    }

    suspend fun setModoDiscreto(activo: Boolean) {
        if (dao.obtenerConfig() == null) dao.guardarConfig(ConfigReloj())
        dao.setModoDiscreto(activo)
    }

    suspend fun setTiempoPanico(segundos: Int) {
        if (dao.obtenerConfig() == null) dao.guardarConfig(ConfigReloj())
        dao.setTiempoPanico(segundos * 1000)
    }
}
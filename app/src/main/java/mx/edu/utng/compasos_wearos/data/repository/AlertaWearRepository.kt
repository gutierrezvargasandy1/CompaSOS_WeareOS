package mx.edu.utng.compasos_wearos.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mx.edu.utng.compasos_wearos.config.mqtt.MqttConfig
import mx.edu.utng.compasos_wearos.config.mqtt.MqttManager
import mx.edu.utng.compasos_wearos.data.db.WearDatabase
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AlertaWearRepository(private val context: Context) {

    private val mqtt = MqttManager()
    private val dao  = WearDatabase.getInstance(context).configRelojDao()
    private val fmt  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * El reloj actúa como control remoto: publica el trigger SOS.
     * El teléfono es responsable de obtener la ubicación y crear la alerta completa.
     */
    @SuppressLint("HardwareIds")
    suspend fun enviarSOS(): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            if (!mqtt.estaConectado) mqtt.conectar()

            val config = dao.obtenerConfig() ?: run {
                Log.e("AlertaWearRepo", "Sin config en Room")
                return@withContext null
            }

            val dispositivoId = config.deviceIdVinculado.ifBlank {
                "wear_${Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ANDROID_ID
                )}"
            }
            val alertaId = UUID.randomUUID().toString()

            val payload = JSONObject().apply {
                put("alertaId",      alertaId)
                put("dispositivoId", dispositivoId)
                put("tipoAlerta",    "SOS")
                put("descripcion",   "Alerta de pánico desde el reloj")
                put("estado",        "activa")
                put("fecha",         fmt.format(Date()))
            }.toString()

            mqtt.publicar("${MqttConfig.TOPIC_ALERTA}/$dispositivoId/sos", payload)
            Log.d("AlertaWearRepo", "Trigger SOS publicado: $alertaId")

            Pair(alertaId, dispositivoId)
        } catch (e: Exception) {
            Log.e("AlertaWearRepo", "Error al enviar SOS: ${e.message}")
            null
        }
    }

    /** Opcional: audio del micrófono del reloj si se implementa. */
    suspend fun publicarChunkAudio(
        alertaId: String,
        dispositivoId: String,
        base64Chunk: String
    ) = withContext(Dispatchers.IO) {
        try {
            if (!mqtt.estaConectado) mqtt.conectar()
            val payload = JSONObject().apply {
                put("id",       UUID.randomUUID().toString())
                put("alertaId", alertaId)
                put("audio",    base64Chunk)
                put("fecha",    fmt.format(Date()))
            }.toString()
            mqtt.publicar("${MqttConfig.TOPIC_ALERTA}/$dispositivoId/audio", payload)
        } catch (e: Exception) {
            Log.e("AlertaWearRepo", "Error al publicar audio: ${e.message}")
        }
    }
}
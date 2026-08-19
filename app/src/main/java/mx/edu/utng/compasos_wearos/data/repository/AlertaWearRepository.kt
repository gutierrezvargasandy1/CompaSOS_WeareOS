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

/**
 * repositorio encargado del envío y gestión de alertas de emergencia y datos multimedia desde el reloj inteligente.
 * coordina la comunicación remota con el teléfono móvil mediante protocolo mqtt.
 *
 * @param context contexto de la aplicación para acceder a preferencias, base de datos local y servicios del sistema.
 */
class AlertaWearRepository(private val context: Context) {

    /** gestor de cliente mqtt para realizar conexiones y publicaciones de mensajes. */
    private val mqtt = MqttManager()

    /** objeto de acceso a datos para consultar la configuración local del reloj. */
    private val dao  = WearDatabase.getInstance(context).configRelojDao()

    /** formateador de fechas para la marca de tiempo de los mensajes emitidos. */
    private val fmt  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * envía un evento o detonador sos de emergencia desde el reloj hacia el broker mqtt.
     * recupera la configuración almacenada o genera un identificador alternativo para publicar la alerta.
     *
     * @return un par [Pair] de cadenas conteniendo el id de la alerta y el id del dispositivo si el envío fue exitoso, o null en caso de falla.
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

    /**
     * transmite un fragmento de audio codificado en base64 capturado desde el micrófono del reloj.
     *
     * @param alertaId identificador único de la alerta asociada a la grabación de audio.
     * @param dispositivoId identificador del dispositivo que envía el audio.
     * @param base64Chunk cadena de texto que contiene el segmento de audio codificado en base64.
     */
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
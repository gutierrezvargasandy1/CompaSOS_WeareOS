package mx.edu.utng.compasos_wearos.config.mqtt

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttManager {

    companion object {
        private const val TAG = "MqttManager"
    }

    private var client: MqttClient? = null

    val estaConectado: Boolean
        get() = client?.isConnected == true

    /** Llamar siempre desde Dispatchers.IO */
    fun conectar() {
        if (estaConectado) return
        try {
            val clientId = "compasos_movil_${System.currentTimeMillis()}"
            client = MqttClient(MqttConfig.BROKER_URL, clientId, MemoryPersistence())

            val options = MqttConnectOptions().apply {
                isCleanSession      = true
                connectionTimeout   = MqttConfig.TIMEOUT_CONEXION
                keepAliveInterval   = MqttConfig.KEEP_ALIVE
                isAutomaticReconnect = false
            }
            client!!.connect(options)
            Log.d(TAG, "Conectado a: ${MqttConfig.BROKER_URL}")
        } catch (e: MqttException) {
            Log.e(TAG, "Error al conectar: código=${e.reasonCode}, ${e.message}")
            throw e
        }
    }

    fun desconectar() {
        try {
            client?.takeIf { it.isConnected }?.disconnect()
            Log.d(TAG, "Desconectado del broker")
        } catch (e: MqttException) {
            Log.e(TAG, "Error al desconectar: ${e.message}")
        } finally {
            client = null
        }
    }

    /** Llamar desde Dispatchers.IO */
    fun publicar(topic: String, payload: String, qos: Int = MqttConfig.QOS) {
        val c = client ?: throw IllegalStateException("MQTT no conectado")
        val message = MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply { this.qos = qos }
        c.publish(topic, message)
        Log.d(TAG, "► [$topic]: $payload")
    }

    /**
     * El callback se ejecuta en el hilo interno de Paho.
     * Desde el callback usa viewModelScope.launch para actualizar estado.
     */
    fun suscribir(topic: String, onMensaje: (topic: String, payload: String) -> Unit) {
        val c = client ?: throw IllegalStateException("MQTT no conectado")
        c.subscribe(topic, MqttConfig.QOS) { t, message ->
            val payload = String(message.payload, Charsets.UTF_8)
            Log.d(TAG, "◄ [$t]: $payload")
            onMensaje(t, payload)
        }
        Log.d(TAG, "Suscrito a: $topic")
    }

    fun desuscribir(topic: String) {
        try {
            client?.unsubscribe(topic)
            Log.d(TAG, "Desuscrito de: $topic")
        } catch (e: MqttException) {
            Log.e(TAG, "Error al desuscribir de $topic: ${e.message}")
        }
    }
}
package mx.edu.utng.compasos_wearos.config.mqtt

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

/**
 * gestor de conexiones y operaciones mqtt para la aplicación wear os.
 * proporciona funciones para conectar, desconectar, publicar mensajes y gestionar suscripciones a tópicos.
 */
class MqttManager {

    /** objeto de compañía que almacena la etiqueta de identificación para los registros de depuración. */
    companion object {
        /** etiqueta para los logs de depuración de la clase mqttmanager. */
        private const val TAG = "MqttManager"
    }

    /** cliente interno de la librería paho para gestionar la comunicación con el bróker mqtt. */
    private var client: MqttClient? = null

    /**
     * indica si el cliente mqtt mantiene actualmente una conexión activa con el bróker.
     *
     * @return true si el cliente está conectado; false de lo contrario.
     */
    val estaConectado: Boolean
        get() = client?.isConnected == true

    /**
     * establece la conexión sincrónica con el bróker mqtt configurado en [MqttConfig].
     * se debe ejecutar preferentemente desde un hilo secundario (dispatchers.io).
     *
     * @throws MqttException si ocurre un fallo durante la conexión con el bróker.
     */
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

    /**
     * finaliza la sesión activa con el bróker mqtt y libera la instancia del cliente.
     */
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

    /**
     * publica un mensaje con un contenido específico en el tópico indicado.
     * debe ejecutarse en un hilo de entrada/salida (dispatchers.io).
     *
     * @param topic canal o tópico de destino del mensaje.
     * @param payload contenido en formato de cadena de texto que será transmitido.
     * @param qos nivel de calidad de servicio para la publicación; por defecto toma el valor configurado en [MqttConfig.QOS].
     * @throws IllegalStateException si se intenta publicar sin estar conectado previamente.
     */
    fun publicar(topic: String, payload: String, qos: Int = MqttConfig.QOS) {
        val c = client ?: throw IllegalStateException("MQTT no conectado")
        val message = MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply { this.qos = qos }
        c.publish(topic, message)
        Log.d(TAG, "► [$topic]: $payload")
    }

    /**
     * suscribe el cliente a un tópico específico para escuchar los mensajes entrantes.
     *
     * @param topic canal o tópico al que se desea suscribir.
     * @param onMensaje callback que procesa la recepción de mensajes pasándole el tópico y el payload recibidos.
     * @throws IllegalStateException si se intenta suscribir sin estar conectado al bróker.
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

    /**
     * cancela la suscripción del cliente a un tópico específico.
     *
     * @param topic canal o tópico del que se desea cancelar la suscripción.
     */
    fun desuscribir(topic: String) {
        try {
            client?.unsubscribe(topic)
            Log.d(TAG, "Desuscrito de: $topic")
        } catch (e: MqttException) {
            Log.e(TAG, "Error al desuscribir de $topic: ${e.message}")
        }
    }
}
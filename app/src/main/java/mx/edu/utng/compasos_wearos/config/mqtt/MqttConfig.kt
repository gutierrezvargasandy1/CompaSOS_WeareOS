package mx.edu.utng.compasos_wearos.config.mqtt

object MqttConfig {
    /**
     * Emulador Android  → 10.0.2.2  (mapea al localhost de tu máquina)
     * Dispositivo físico → IP local de tu máquina en la misma WiFi
     *                      Ejemplo: "tcp://192.168.1.100:1883"
     */
    const val BROKER_URL = "tcp://100.72.14.51:1883"

    const val TIMEOUT_CONEXION = 10   // segundos
    const val KEEP_ALIVE       = 60   // segundos
    const val QOS              = 1    // At least once

    // Topics
    const val TOPIC_VINCULACION = "compasos/vinculacion"
    const val TOPIC_DISPOSITIVO = "compasos/dispositivo"
}
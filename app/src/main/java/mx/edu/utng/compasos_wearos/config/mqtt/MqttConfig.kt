package mx.edu.utng.compasos_wearos.config.mqtt

/**
 * objeto de configuración global que define las constantes de red, parámetros de sesión
 * y tópicos base para la comunicación mqtt en la aplicación wear os.
 */
object MqttConfig {
    /**
     * dirección url del bróker mqtt al cual se conecta el dispositivo wear os.
     * en emulador se utiliza "tcp://10.0.2.2:1883" y en dispositivos físicos la ip local correspondiente.
     */
    const val BROKER_URL = "tcp://192.168.1.102:1883"

    /** tiempo de espera máximo en segundos para establecer la conexión con el bróker. */
    const val TIMEOUT_CONEXION = 10

    /** intervalo de tiempo en segundos para el envío de paquetes de verificación de conexión activa (keep alive). */
    const val KEEP_ALIVE       = 60

    /** nivel de calidad de servicio (qos) utilizado para la entrega garantizada de mensajes mqtt. */
    const val QOS              = 1

    /** tópico base para la gestión de solicitudes y respuestas durante el proceso de vinculación. */
    const val TOPIC_VINCULACION = "compasos/vinculacion"

    /** tópico base para la transmisión de estados e información general del dispositivo. */
    const val TOPIC_DISPOSITIVO = "compasos/dispositivo"

    /** tópico base para el envío y recepción de alertas de emergencia. */
    const val TOPIC_ALERTA = "compasos/alerta"
}
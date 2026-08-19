package mx.edu.utng.compasos_wearos.data

/**
 * clase sellada (sealed class) que define los diferentes estados posibles
 * por los que atraviesa el proceso de vinculación del dispositivo wear os.
 */
sealed class VinculacionState {

    /** estado inicial o de espera en el que el sistema no se encuentra vinculado ni procesando solicitudes. */
    object Esperando : VinculacionState()

    /**
     * estado que representa una solicitud de vinculación entrante.
     *
     * @property nombreTelefono nombre descriptivo o identificador del teléfono.
     * @property codigoEsperado si es diferente de null, proviene del flujo mqtt y requiere que el usuario teclee el código; si es null, proviene de la detección automática por node/wearable.
     * @property usuarioId identificador único opcional del usuario asociado a la solicitud.
     */
    data class SolicitudRecibida(
        val nombreTelefono: String,
        val codigoEsperado: String? = null,
        val usuarioId: String? = null
    ) : VinculacionState()

    /**
     * estado que indica que el código de verificación ingresado fue erróneo durante el intento de vinculación por mqtt.
     *
     * @property codigoEsperado el código correcto que se estaba esperando validar.
     * @property usuarioId identificador único opcional del usuario asociado.
     */
    data class CodigoIncorrecto(
        val codigoEsperado: String,
        val usuarioId: String?
    ) : VinculacionState()

    /** estado que indica que el proceso de vinculación se encuentra en curso o cargando. */
    object Vinculando : VinculacionState()

    /** estado que señala que la vinculación se ha completado exitosamente. */
    object Vinculado : VinculacionState()

    /**
     * estado que refleja la ocurrencia de un error durante la vinculación.
     *
     * @property mensaje descripción detallada del error ocurrido.
     */
    data class Error(val mensaje: String) : VinculacionState()
}
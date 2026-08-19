package mx.edu.utng.compasos_wearos.data

/**
 * clase sellada (sealed class) que representa los diferentes eventos o acciones posibles
 * que pueden desencadenarse durante el flujo de vinculación del dispositivo wear os.
 */
sealed class VinculacionEvent {
    /** evento para iniciar la búsqueda de un reloj o dispositivo disponible. */
    object BuscarReloj : VinculacionEvent()

    /**
     * evento que representa una solicitud de vinculación enviada o recibida con un teléfono específico.
     *
     * @property nombreTelefono nombre o identificador del teléfono involucrado en la solicitud.
     */
    data class SolicitudVinculacion(val nombreTelefono: String) : VinculacionEvent()

    /** evento para confirmar la acción de vinculación actual. */
    object Confirmar : VinculacionEvent()

    /** evento para cancelar el proceso de vinculación en curso. */
    object Cancelar : VinculacionEvent()

    /** evento para romper o eliminar la relación de vinculación existente. */
    object Desconectar : VinculacionEvent()

    // ── NUEVO: flujo MQTT con código ──────────────────────────

    /**
     * evento disparado cuando se recibe una solicitud de vinculación mediante el protocolo mqtt conteniendo un código.
     *
     * @property codigo código alfanumérico o numérico de vinculación enviado.
     * @property usuarioId identificador único del usuario asociado a la solicitud.
     */
    data class SolicitudMqttRecibida(val codigo: String, val usuarioId: String) : VinculacionEvent()

    /** evento que indica que el código ingresado por el usuario fue incorrecto. */
    object CodigoIncorrecto : VinculacionEvent()
}
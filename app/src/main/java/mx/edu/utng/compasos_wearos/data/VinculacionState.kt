package mx.edu.utng.compasos_wearos.data

sealed class VinculacionState {

    object Esperando : VinculacionState()

    /**
     * codigoEsperado != null  → viene del flujo MQTT, el usuario debe teclearlo
     * codigoEsperado == null  → viene del flujo Node/Wearable (detección automática)
     */
    data class SolicitudRecibida(
        val nombreTelefono: String,
        val codigoEsperado: String? = null,
        val usuarioId: String? = null
    ) : VinculacionState()

    data class CodigoIncorrecto(
        val codigoEsperado: String,
        val usuarioId: String?
    ) : VinculacionState()

    object Vinculando : VinculacionState()
    object Vinculado : VinculacionState()
    data class Error(val mensaje: String) : VinculacionState()
}
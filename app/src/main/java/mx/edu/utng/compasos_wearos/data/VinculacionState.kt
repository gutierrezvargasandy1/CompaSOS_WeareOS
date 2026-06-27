package mx.edu.utng.compasos_wearos.data

sealed class VinculacionState {

    /**
     * El reloj está esperando una solicitud.
     */
    object Esperando : VinculacionState()

    /**
     * El teléfono encontró el reloj y solicita vincularse.
     */
    data class SolicitudRecibida(
        val nombreTelefono: String
    ) : VinculacionState()

    /**
     * El usuario aceptó la vinculación.
     */
    object Vinculando : VinculacionState()

    /**
     * Vinculación terminada.
     */
    object Vinculado : VinculacionState()

    /**
     * Error durante la vinculación.
     */
    data class Error(
        val mensaje: String
    ) : VinculacionState()
}
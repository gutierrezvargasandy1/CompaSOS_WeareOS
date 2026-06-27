package mx.edu.utng.compasos_wearos.data

sealed class VinculacionEvent {

    object BuscarReloj : VinculacionEvent()

    data class SolicitudVinculacion(
        val nombreTelefono: String
    ) : VinculacionEvent()

    object Confirmar : VinculacionEvent()

    object Cancelar : VinculacionEvent()

    object Desconectar : VinculacionEvent()

}
package mx.edu.utng.compasos_wearos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "config_reloj")
data class ConfigReloj(
    @PrimaryKey
    val id: Int = 1,
    val tiempoPanicoMs: Int = 3000,
    val tripleTabActivado: Boolean = true,
    val deteccionCaidasActiva: Boolean = true,
    val sensibilidadCaida: Float = 2.5f,
    val umbralBpmAlerta: Int = 130,
    val intervaloMonitoreoSeg: Int = 10,
    val audioAlActivarEmergencia: Boolean = true,
    val duracionAudioSeg: Int = 30,
    val modoDiscreto: Boolean = true,
    val deviceIdVinculado: String = "",
    val nombreDispositivoVinculado: String = "",
    val codigoVinculacion: String = "",      // código MQTT pendiente de confirmar
    val ultimaActualizacion: Long = System.currentTimeMillis()
)
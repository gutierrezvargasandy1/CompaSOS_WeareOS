package mx.edu.utng.compasos_wearos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "eventos_pendientes")
data class EventoPendiente(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tipoEvento: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitud: Double? = null,
    val longitud: Double? = null,
    val bpmRegistrado: Int? = null,
    val rutaAudioLocal: String? = null,
    val sincronizado: Boolean = false,
    val intentosSincronizacion: Int = 0
)
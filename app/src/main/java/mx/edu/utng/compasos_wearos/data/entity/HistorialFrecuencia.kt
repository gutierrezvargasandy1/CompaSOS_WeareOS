package mx.edu.utng.compasos_wearos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "historial_frecuencia")
data class HistorialFrecuencia(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bpm: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val generoAlerta: Boolean = false
)
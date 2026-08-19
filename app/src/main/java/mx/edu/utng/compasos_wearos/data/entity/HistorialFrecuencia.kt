package mx.edu.utng.compasos_wearos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * entidad room que representa una lectura del historial de frecuencia cardiaca en el reloj inteligente.
 * registra las pulsaciones por minuto junto con la marca de tiempo y si desencadenó una alerta.
 *
 * @property id identificador único del registro en la base de datos local.
 * @property bpm cantidad de pulsaciones por minuto registradas en la lectura.
 * @property timestamp marca de tiempo en milisegundos en la que se realizó la medición.
 * @property generoAlerta indica si esta lectura específica de frecuencia cardiaca sobrepasó los umbrales y generó una alerta.
 */
@Entity(tableName = "historial_frecuencia")
data class HistorialFrecuencia(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bpm: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val generoAlerta: Boolean = false
)
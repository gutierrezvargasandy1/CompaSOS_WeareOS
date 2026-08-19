package mx.edu.utng.compasos_wearos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * entidad room que representa un evento o alerta pendiente de envío o sincronización.
 * almacena temporalmente los datos del suceso en el reloj cuando no hay conectividad inmediata.
 *
 * @property id identificador único del evento registrado en la base de datos local.
 * @property tipoEvento categoría o clasificación del evento (ej. pánico, caída, bpm_alto).
 * @property timestamp marca de tiempo en milisegundos en la que ocurrió el evento.
 * @property latitud coordenada opcional de latitud obtenida al momento del suceso.
 * @property longitud coordenada opcional de longitud obtenida al momento del suceso.
 * @property bpmRegistrado lectura opcional de ritmo cardiaco asociada al evento.
 * @property rutaAudioLocal ruta local del archivo de audio grabado durante la emergencia, si aplica.
 * @property sincronizado estado que indica si el evento ya se envió exitosamente al teléfono o servidor.
 * @property intentosSincronizacion número de reintentos acumulados para transmitir este evento.
 */
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
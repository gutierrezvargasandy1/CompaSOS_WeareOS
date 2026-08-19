package mx.edu.utng.compasos_wearos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * entidad room que representa la configuración general y preferencias del reloj inteligente.
 * almacena parámetros de alertas, sensibilidad de sensores, estado de vinculación y temporizadores.
 *
 * @property id identificador único de la configuración en la base de datos local (por defecto 1).
 * @property tiempoPanicoMs tiempo de pulsación en milisegundos para activar el botón de pánico.
 * @property tripleTabActivado indica si el gesto de triple toque está habilitado para emergencias.
 * @property deteccionCaidasActiva indica si la detección automática de caídas se encuentra encendida.
 * @property sensibilidadCaida umbral o factor de sensibilidad para la aceleración en detección de caídas.
 * @property umbralBpmAlerta límite máximo de pulsaciones por minuto (bpm) para activar una alerta médica.
 * @property intervaloMonitoreoSeg frecuencia o intervalo en segundos para la lectura periódica de sensores.
 * @property audioAlActivarEmergencia indica si se debe iniciar la grabación o reproducción de audio durante una emergencia.
 * @property duracionAudioSeg tiempo en segundos que dura la captura o emisión de audio de emergencia.
 * @property modoDiscreto indica si las alertas se gestionan en modo silencioso o discreto.
 * @property deviceIdVinculado identificador único del dispositivo móvil al cual está vinculado el reloj.
 * @property nombreDispositivoVinculado nombre descriptivo o modelo del dispositivo móvil vinculado.
 * @property codigoVinculacion código temporal utilizado durante el proceso de vinculación mqtt.
 * @property ultimaActualizacion marca de tiempo en milisegundos de la última modificación realizada.
 */
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
package mx.edu.utng.compasos_wearos.data.dao

import androidx.room.*
import mx.edu.utng.compasos_wearos.data.entity.HistorialFrecuencia

/**
 * interfaz dao (data access object) para gestionar el acceso a datos del historial de lecturas
 * de frecuencia cardiaca ([HistorialFrecuencia]) en la base de datos local del reloj.
 */
@Dao
interface HistorialFrecuenciaDao {

    /**
     * inserta un nuevo registro de lectura de frecuencia cardiaca en la base de datos.
     *
     * @param lectura objeto [HistorialFrecuencia] que contiene el valor en bpm y la marca de tiempo de la medición.
     */
    @Insert
    suspend fun insertar(lectura: HistorialFrecuencia)

    /**
     * obtiene el historial de lecturas registradas a partir de una marca de tiempo específica,
     * ordenadas cronológicamente en forma ascendente.
     *
     * @param desde marca de tiempo límite inicial en milisegundos.
     * @return lista de lecturas registradas desde el momento especificado.
     */
    @Query("SELECT * FROM historial_frecuencia WHERE timestamp > :desde ORDER BY timestamp ASC")
    suspend fun obtenerDesde(desde: Long): List<HistorialFrecuencia>

    /**
     * elimina de la base de datos todas las lecturas de frecuencia cardiaca anteriores a una fecha limite.
     *
     * @param antes marca de tiempo límite en milisegundos anterior a la cual se eliminarán los registros.
     */
    @Query("DELETE FROM historial_frecuencia WHERE timestamp < :antes")
    suspend fun purgarAnterioresA(antes: Long)

    /**
     * cuenta el número de lecturas que superaron un determinado umbral de pulsaciones por minuto (bpm)
     * dentro de un periodo de tiempo específico.
     *
     * @param umbral límite de ritmo cardiaco en bpm para evaluar lecturas elevadas.
     * @param desde marca de tiempo inicial en milisegundos desde la cual se realiza el conteo.
     * @return total de mediciones que excedieron el umbral establecido en el periodo indicado.
     */
    @Query("SELECT COUNT(*) FROM historial_frecuencia WHERE bpm > :umbral AND timestamp > :desde")
    suspend fun contarPicosSostenidos(umbral: Int, desde: Long): Int
}
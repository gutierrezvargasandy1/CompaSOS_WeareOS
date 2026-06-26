package mx.edu.utng.compasos_wearos.data.dao

import androidx.room.*
import mx.edu.utng.compasos_wearos.data.entity.HistorialFrecuencia

@Dao
interface HistorialFrecuenciaDao {

    @Insert
    suspend fun insertar(lectura: HistorialFrecuencia)

    @Query("SELECT * FROM historial_frecuencia WHERE timestamp > :desde ORDER BY timestamp ASC")
    suspend fun obtenerDesde(desde: Long): List<HistorialFrecuencia>

    @Query("DELETE FROM historial_frecuencia WHERE timestamp < :antes")
    suspend fun purgarAnterioresA(antes: Long)

    @Query("SELECT COUNT(*) FROM historial_frecuencia WHERE bpm > :umbral AND timestamp > :desde")
    suspend fun contarPicosSostenidos(umbral: Int, desde: Long): Int
}
package mx.edu.utng.compasos_wearos.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.edu.utng.compasos_wearos.data.entity.EventoPendiente

@Dao
interface EventoPendienteDao {

    @Insert
    suspend fun insertar(evento: EventoPendiente): Long

    @Query("SELECT * FROM eventos_pendientes WHERE sincronizado = 0 ORDER BY timestamp ASC")
    suspend fun obtenerPendientes(): List<EventoPendiente>

    @Query("SELECT * FROM eventos_pendientes ORDER BY timestamp DESC")
    fun observarTodos(): Flow<List<EventoPendiente>>

    @Query("UPDATE eventos_pendientes SET sincronizado = 1 WHERE id = :id")
    suspend fun marcarSincronizado(id: Int)

    @Query("UPDATE eventos_pendientes SET intentosSincronizacion = intentosSincronizacion + 1 WHERE id = :id")
    suspend fun incrementarIntentos(id: Int)

    @Query("DELETE FROM eventos_pendientes WHERE sincronizado = 1")
    suspend fun limpiarSincronizados()
}
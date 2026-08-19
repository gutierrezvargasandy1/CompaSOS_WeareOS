package mx.edu.utng.compasos_wearos.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.edu.utng.compasos_wearos.data.entity.EventoPendiente

/**
 * interfaz dao (data access object) para gestionar las operaciones de acceso a datos
 * de los eventos pendientes de sincronización ([EventoPendiente]) en la base de datos local del reloj.
 */
@Dao
interface EventoPendienteDao {

    /**
     * inserta un nuevo evento pendiente en la base de datos.
     *
     * @param evento objeto [EventoPendiente] que contiene la información de la alerta o evento a registrar.
     * @return el identificador autogenerado asignado al registro insertado.
     */
    @Insert
    suspend fun insertar(evento: EventoPendiente): Long

    /**
     * recupera todos los eventos que aún no han sido sincronizados con el servidor o teléfono,
     * ordenados cronológicamente desde el más antiguo al más reciente.
     *
     * @return lista de objetos [EventoPendiente] marcados como no sincronizados.
     */
    @Query("SELECT * FROM eventos_pendientes WHERE sincronizado = 0 ORDER BY timestamp ASC")
    suspend fun obtenerPendientes(): List<EventoPendiente>

    /**
     * observa todos los eventos registrados en la base de datos en tiempo real mediante un flujo de datos [Flow],
     * ordenados del más reciente al más antiguo.
     *
     * @return un flujo [Flow] que emite la lista actualizada de todos los eventos.
     */
    @Query("SELECT * FROM eventos_pendientes ORDER BY timestamp DESC")
    fun observarTodos(): Flow<List<EventoPendiente>>

    /**
     * actualiza el estado de sincronización de un evento específico marcándolo como completado.
     *
     * @param id identificador único del evento a actualizar.
     */
    @Query("UPDATE eventos_pendientes SET sincronizado = 1 WHERE id = :id")
    suspend fun marcarSincronizado(id: Int)

    /**
     * incrementa en uno el contador de intentos fallidos de retransmisión/sincronización de un evento.
     *
     * @param id identificador único del evento.
     */
    @Query("UPDATE eventos_pendientes SET intentosSincronizacion = intentosSincronizacion + 1 WHERE id = :id")
    suspend fun incrementarIntentos(id: Int)

    /**
     * elimina de la base de datos local todos aquellos registros de eventos que ya fueron sincronizados con éxito.
     */
    @Query("DELETE FROM eventos_pendientes WHERE sincronizado = 1")
    suspend fun limpiarSincronizados()
}
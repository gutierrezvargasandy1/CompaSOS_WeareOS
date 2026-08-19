package mx.edu.utng.compasos_wearos.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.edu.utng.compasos_wearos.data.entity.ConfigReloj

/**
 * interfaz dao (data access object) para gestionar las operaciones de acceso a datos
 * de la entidad [ConfigReloj] en la base de datos local room del reloj.
 */
@Dao
interface ConfigRelojDao {

    /**
     * obtiene y observa los cambios en la configuración del reloj en tiempo real mediante un flujo de datos [Flow].
     *
     * @return un flujo [Flow] que emite la entidad [ConfigReloj] actual o null si no se ha registrado ninguna configuración.
     */
    @Query("SELECT * FROM config_reloj WHERE id = 1")
    fun observarConfig(): Flow<ConfigReloj?>

    /**
     * consulta sincrónica/suspendida para obtener la configuración guardada del reloj.
     *
     * @return la entidad [ConfigReloj] registrada o null si aún no existe registro.
     */
    @Query("SELECT * FROM config_reloj WHERE id = 1")
    suspend fun obtenerConfig(): ConfigReloj?

    /**
     * inserta o reemplaza la entidad de configuración del reloj en la base de datos.
     *
     * @param config objeto [ConfigReloj] con los datos a guardar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarConfig(config: ConfigReloj)

    /**
     * actualiza el umbral límite de ritmo cardiaco (bpm) que desencadena una alerta de emergencia.
     *
     * @param bpm valor entero del umbral cardiaco en pulsaciones por minuto.
     */
    @Query("UPDATE config_reloj SET umbralBpmAlerta = :bpm WHERE id = 1")
    suspend fun setUmbralBpm(bpm: Int)

    /**
     * actualiza la sensibilidad del acelerómetro para la detección de caídas.
     *
     * @param valor valor flotante que representa el nivel de sensibilidad ajustado.
     */
    @Query("UPDATE config_reloj SET sensibilidadCaida = :valor WHERE id = 1")
    suspend fun setSensibilidadCaida(valor: Float)

    /**
     * registra la información del dispositivo móvil vinculado al reloj smartwatch.
     *
     * @param id identificador único del dispositivo móvil vinculado.
     * @param nombre nombre o modelo del dispositivo móvil vinculado.
     */
    @Query("UPDATE config_reloj SET deviceIdVinculado = :id, nombreDispositivoVinculado = :nombre WHERE id = 1")
    suspend fun setDispositivoVinculado(id: String, nombre: String)

    /**
     * activa o desactiva el modo discreto (silencioso/sin alertas sonoras) en el reloj.
     *
     * @param activo true para activar el modo discreto, false para desactivarlo.
     */
    @Query("UPDATE config_reloj SET modoDiscreto = :activo WHERE id = 1")
    suspend fun setModoDiscreto(activo: Boolean)

    /**
     * actualiza el tiempo de espera en milisegundos requerido para confirmar la activación del botón de pánico.
     *
     * @param ms duración en milisegundos para el conteo del botón de pánico.
     */
    @Query("UPDATE config_reloj SET tiempoPanicoMs = :ms WHERE id = 1")
    suspend fun setTiempoPanico(ms: Int)

    // ── NUEVO: código de vinculación MQTT pendiente ──────────

    /**
     * establece el código de vinculación mqtt generado y pendiente de confirmación.
     *
     * @param codigo código numérico/alfanumérico generado para el proceso de vinculación.
     */
    @Query("UPDATE config_reloj SET codigoVinculacion = :codigo WHERE id = 1")
    suspend fun setCodigoVinculacion(codigo: String)

    /**
     * borra o restablece a cadena vacía el código de vinculación almacenado en la configuración.
     */
    @Query("UPDATE config_reloj SET codigoVinculacion = '' WHERE id = 1")
    suspend fun limpiarCodigoVinculacion()
}
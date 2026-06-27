package mx.edu.utng.compasos_wearos.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import mx.edu.utng.compasos_wearos.data.entity.ConfigReloj

@Dao
interface ConfigRelojDao {

    @Query("SELECT * FROM config_reloj WHERE id = 1")
    fun observarConfig(): Flow<ConfigReloj?>

    @Query("SELECT * FROM config_reloj WHERE id = 1")
    suspend fun obtenerConfig(): ConfigReloj?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarConfig(config: ConfigReloj)

    @Query("UPDATE config_reloj SET umbralBpmAlerta = :bpm WHERE id = 1")
    suspend fun setUmbralBpm(bpm: Int)

    @Query("UPDATE config_reloj SET sensibilidadCaida = :valor WHERE id = 1")
    suspend fun setSensibilidadCaida(valor: Float)

    @Query("UPDATE config_reloj SET deviceIdVinculado = :id, nombreDispositivoVinculado = :nombre WHERE id = 1")
    suspend fun setDispositivoVinculado(id: String, nombre: String)

    @Query("UPDATE config_reloj SET modoDiscreto = :activo WHERE id = 1")
    suspend fun setModoDiscreto(activo: Boolean)

    @Query("UPDATE config_reloj SET tiempoPanicoMs = :ms WHERE id = 1")
    suspend fun setTiempoPanico(ms: Int)
}
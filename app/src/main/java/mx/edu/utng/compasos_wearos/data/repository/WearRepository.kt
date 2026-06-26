package mx.edu.utng.compasos_wearos.data.repository

import kotlinx.coroutines.flow.Flow
import mx.edu.utng.compasos_wearos.data.dao.ConfigRelojDao
import mx.edu.utng.compasos_wearos.data.dao.EventoPendienteDao
import mx.edu.utng.compasos_wearos.data.dao.HistorialFrecuenciaDao
import mx.edu.utng.compasos_wearos.data.entity.ConfigReloj
import mx.edu.utng.compasos_wearos.data.entity.EventoPendiente
import mx.edu.utng.compasos_wearos.data.entity.HistorialFrecuencia

class WearRepository(
    private val configDao: ConfigRelojDao,
    private val eventosDao: EventoPendienteDao,
    private val frecuenciaDao: HistorialFrecuenciaDao
) {

    // ── ConfigReloj ───────────────────────────────────────────

    val config: Flow<ConfigReloj?> = configDao.observarConfig()

    suspend fun inicializarConfigSiVacia() {
        if (configDao.obtenerConfig() == null) {
            configDao.guardarConfig(ConfigReloj())
        }
    }

    suspend fun guardarConfig(config: ConfigReloj) =
        configDao.guardarConfig(config)

    suspend fun setModoDiscreto(activo: Boolean) =
        configDao.setModoDiscreto(activo)

    suspend fun setUmbralBpm(bpm: Int) =
        configDao.setUmbralBpm(bpm)

    suspend fun setSensibilidadCaida(valor: Float) =
        configDao.setSensibilidadCaida(valor)

    suspend fun vincularDispositivo(id: String, nombre: String) =
        configDao.setDispositivoVinculado(id, nombre)

    // ── EventoPendiente ───────────────────────────────────────

    suspend fun registrarEvento(
        tipo: String,
        latitud: Double? = null,
        longitud: Double? = null,
        bpm: Int? = null,
        rutaAudio: String? = null
    ): Long = eventosDao.insertar(
        EventoPendiente(
            tipoEvento     = tipo,
            latitud        = latitud,
            longitud       = longitud,
            bpmRegistrado  = bpm,
            rutaAudioLocal = rutaAudio
        )
    )

    suspend fun obtenerEventosPendientes(): List<EventoPendiente> =
        eventosDao.obtenerPendientes()

    suspend fun marcarEventoSincronizado(id: Int) =
        eventosDao.marcarSincronizado(id)

    suspend fun limpiarEventosSincronizados() =
        eventosDao.limpiarSincronizados()

    // ── HistorialFrecuencia ───────────────────────────────────

    suspend fun registrarBpm(bpm: Int) {
        frecuenciaDao.insertar(HistorialFrecuencia(bpm = bpm))
    }

    suspend fun hayPicoSostenido(
        ventanaMs: Long = 30_000L,
        minimoLecturas: Int = 3
    ): Boolean {
        val umbral = configDao.obtenerConfig()?.umbralBpmAlerta ?: 130
        val desde  = System.currentTimeMillis() - ventanaMs
        val picos  = frecuenciaDao.contarPicosSostenidos(umbral, desde)
        return picos >= minimoLecturas
    }

    suspend fun purgarHistorialViejo() {
        val hace24h = System.currentTimeMillis() - 86_400_000L
        frecuenciaDao.purgarAnterioresA(hace24h)
    }
}
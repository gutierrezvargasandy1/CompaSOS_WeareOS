package mx.edu.utng.compasos_wearos.data.repository

import kotlinx.coroutines.flow.Flow
import mx.edu.utng.compasos_wearos.data.dao.ConfigRelojDao
import mx.edu.utng.compasos_wearos.data.dao.EventoPendienteDao
import mx.edu.utng.compasos_wearos.data.dao.HistorialFrecuenciaDao
import mx.edu.utng.compasos_wearos.data.entity.ConfigReloj
import mx.edu.utng.compasos_wearos.data.entity.EventoPendiente
import mx.edu.utng.compasos_wearos.data.entity.HistorialFrecuencia

/**
 * repositorio principal que centraliza el acceso a la base de datos local mediante los diferentes daos
 * para la gestión de configuración, eventos pendientes e historial de frecuencia cardiaca en wear os.
 *
 * @property configDao objeto dao para gestionar la configuración del reloj.
 * @property eventosDao objeto dao para manejar los eventos pendientes de sincronización.
 * @property frecuenciaDao objeto dao para administrar el historial de lecturas de ritmo cardiaco.
 */
class WearRepository(
    private val configDao: ConfigRelojDao,
    private val eventosDao: EventoPendienteDao,
    private val frecuenciaDao: HistorialFrecuenciaDao
) {

    // ── ConfigReloj ───────────────────────────────────────────

    /** flujo que observa en tiempo real los cambios en la configuración del reloj. */
    val config: Flow<ConfigReloj?> = configDao.observarConfig()

    /**
     * verifica si la tabla de configuración está vacía e inicializa un registro por defecto si es necesario.
     */
    suspend fun inicializarConfigSiVacia() {
        if (configDao.obtenerConfig() == null) {
            configDao.guardarConfig(ConfigReloj())
        }
    }

    /**
     * guarda o reemplaza una entidad de configuración en la base de datos local.
     *
     * @param config objeto [ConfigReloj] con los datos a almacenar.
     */
    suspend fun guardarConfig(config: ConfigReloj) =
        configDao.guardarConfig(config)

    /**
     * actualiza el estado del modo discreto en la configuración del reloj.
     *
     * @param activo valor booleano que indica si el modo discreto está activo.
     */
    suspend fun setModoDiscreto(activo: Boolean) =
        configDao.setModoDiscreto(activo)

    /**
     * actualiza el umbral límite de pulsaciones por minuto (bpm) para la activación de alertas.
     *
     * @param bpm valor entero del nuevo umbral cardiaco.
     */
    suspend fun setUmbralBpm(bpm: Int) =
        configDao.setUmbralBpm(bpm)

    /**
     * actualiza el nivel de sensibilidad del acelerómetro para la detección de caídas.
     *
     * @param valor valor flotante que define la sensibilidad.
     */
    suspend fun setSensibilidadCaida(valor: Float) =
        configDao.setSensibilidadCaida(valor)

    /**
     * registra los datos del dispositivo vinculado en la configuración local.
     *
     * @param id identificador único del dispositivo asociado.
     * @param nombre nombre descriptivo del dispositivo asociado.
     */
    suspend fun vincularDispositivo(id: String, nombre: String) =
        configDao.setDispositivoVinculado(id, nombre)

    // ── EventoPendiente ───────────────────────────────────────

    /**
     * registra un nuevo evento o alerta pendiente de sincronización en la base de datos local.
     *
     * @param tipo categoría o tipo del evento registrado.
     * @param latitud coordenada opcional de latitud al momento del evento.
     * @param longitud coordenada opcional de longitud al momento del evento.
     * @param bpm lectura opcional de ritmo cardiaco asociada al suceso.
     * @param rutaAudio ruta local del archivo de audio grabado, si aplica.
     * @return el identificador único autogenerado del evento insertado.
     */
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

    /**
     * obtiene la lista de todos los eventos que aún no han sido sincronizados.
     *
     * @return lista de objetos [EventoPendiente] pendientes.
     */
    suspend fun obtenerEventosPendientes(): List<EventoPendiente> =
        eventosDao.obtenerPendientes()

    /**
     * marca un evento específico como sincronizado en la base de datos.
     *
     * @param id identificador único del evento a actualizar.
     */
    suspend fun marcarEventoSincronizado(id: Int) =
        eventosDao.marcarSincronizado(id)

    /**
     * elimina de la base de datos local todos los eventos que ya fueron sincronizados con éxito.
     */
    suspend fun limpiarEventosSincronizados() =
        eventosDao.limpiarSincronizados()

    // ── HistorialFrecuencia ───────────────────────────────────

    /**
     * inserta una nueva lectura de frecuencia cardiaca en el historial local.
     *
     * @param bpm valor entero de las pulsaciones por minuto medidas.
     */
    suspend fun registrarBpm(bpm: Int) {
        frecuenciaDao.insertar(HistorialFrecuencia(bpm = bpm))
    }

    /**
     * evalúa si se han registrado suficientes lecturas que superen el umbral de alerta dentro de una ventana de tiempo.
     *
     * @param ventanaMs duración de la ventana de tiempo en milisegundos (por defecto 30 segundos).
     * @param minimoLecturas cantidad mínima de picos requeridos para considerar la condición como verdadera.
     * @return true si se cumple el criterio de picos sostenidos; false en caso contrario.
     */
    suspend fun hayPicoSostenido(
        ventanaMs: Long = 30_000L,
        minimoLecturas: Int = 3
    ): Boolean {
        val umbral = configDao.obtenerConfig()?.umbralBpmAlerta ?: 130
        val desde  = System.currentTimeMillis() - ventanaMs
        val picos  = frecuenciaDao.contarPicosSostenidos(umbral, desde)
        return picos >= minimoLecturas
    }

    /**
     * elimina del historial aquellas lecturas de frecuencia cardiaca anteriores a las últimas 24 horas.
     */
    suspend fun purgarHistorialViejo() {
        val hace24h = System.currentTimeMillis() - 86_400_000L
        frecuenciaDao.purgarAnterioresA(hace24h)
    }
}
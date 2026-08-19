package mx.edu.utng.compasos_wearos.services

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.sqrt

/**
 * clase encargada de detectar movimientos bruscos o erráticos utilizando el acelerómetro del dispositivo wear os.
 * implementa [SensorEventListener] para escuchar los eventos de los sensores físicos del hardware.
 *
 * @param context contexto de la aplicación para acceder al servicio de sensores del sistema.
 */
class MovimientoDetector(private val context: Context) : SensorEventListener {

    companion object {
        /** umbral predeterminado de aceleración en fuerza g para considerar un impacto o movimiento inusual. */
        private const val UMBRAL_G_DEFAULT = 15f
        /** ventana de tiempo en milisegundos para agrupar y contar los picos de aceleración. */
        private const val VENTANA_MS       = 3_000L
        /** número de impactos o picos necesarios dentro de la ventana de tiempo para disparar la alerta. */
        private const val HITS_NECESARIOS  = 4
        /** tiempo de enfriamiento en milisegundos entre alertas sucesivas para evitar falsos positivos repetidos. */
        private const val COOLDOWN_MS      = 30_000L
        /** tiempo de calentamiento inicial en milisegundos para estabilizar las lecturas al iniciar el sensor. */
        private const val WARMUP_MS        = 3_000L
    }

    /** gestor de sensores del sistema obtenido desde el contexto. */
    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    /** sensor de acelerometría por defecto del dispositivo. */
    private val acelerometro: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    /** flujo mutable compartido privado para emitir eventos de movimiento erratico detectado. */
    private val _movimientoErratico = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** flujo compartido de sólo lectura expuesto para observar cuando se detecta un movimiento errático. */
    val movimientoErratico = _movimientoErratico.asSharedFlow()

    /** umbral actual de aceleración configurado para las detecciones. */
    private var umbralG       = UMBRAL_G_DEFAULT
    /** bandera que indica si el detector se encuentra escuchando activamente al sensor. */
    private var escuchando    = false
    /** marca de tiempo en milisegundos de la última emisión de movimiento errático. */
    private var ultimaEmision = 0L
    /** marca de tiempo en milisegundos en la que se inició la escucha del sensor. */
    private var iniciadoEn    = 0L
    /** colección deque que almacena las marcas de tiempo de los picos detectados dentro de la ventana activa. */
    private val picos         = ArrayDeque<Long>()

    /**
     * inicia la escucha del acelerómetro registrando el listener en el sistema.
     *
     * @param umbral valor opcional de aceleración para configurar el umbral de sensibilidad (forzado a un mínimo de 18f).
     */
    fun iniciar(umbral: Float = UMBRAL_G_DEFAULT) {
        if (escuchando) return
        umbralG       = umbral.coerceAtLeast(18f)
        escuchando    = true
        ultimaEmision = 0L
        iniciadoEn    = System.currentTimeMillis()
        picos.clear()
        acelerometro?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    /**
     * detiene la escucha del sensor y anula el registro del listener en el sistema.
     */
    fun detener() {
        if (!escuchando) return
        sensorManager.unregisterListener(this)
        escuchando = false
        picos.clear()
    }

    /**
     * callback invocado cuando cambian los valores del sensor físico monitoreado.
     * calcula la magnitud de aceleración tridimensional y evalúa si supera el umbral para registrar picos.
     *
     * @param event objeto [SensorEvent] con los datos nuevos del sensor.
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val ahora = System.currentTimeMillis()
        if (ahora - iniciadoEn < WARMUP_MS) return

        val x   = event.values[0]
        val y   = event.values[1]
        val z   = event.values[2]
        val mag = sqrt(x * x + y * y + z * z)

        if (mag > umbralG) {
            if (ahora - ultimaEmision < COOLDOWN_MS) return
            picos.addLast(ahora)
            while (picos.isNotEmpty() && ahora - picos.first() > VENTANA_MS) {
                picos.removeFirst()
            }
            if (picos.size >= HITS_NECESARIOS) {
                picos.clear()
                ultimaEmision = ahora
                _movimientoErratico.tryEmit(Unit)
            }
        }
    }

    /**
     * callback invocado cuando cambia la precisión del sensor. (sin implementación requerida).
     *
     * @param sensor objeto [Sensor] cuyo estado de precisión cambió.
     * @param accuracy nuevo nivel de precisión.
     */
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
}
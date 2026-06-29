package mx.edu.utng.compasos_wearos.services

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.sqrt

class MovimientoDetector(private val context: Context) : SensorEventListener {

    companion object {
        private const val UMBRAL_G_DEFAULT = 15f
        private const val VENTANA_MS       = 3_000L
        private const val HITS_NECESARIOS  = 4
        private const val COOLDOWN_MS      = 30_000L
        private const val WARMUP_MS        = 3_000L
    }

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val acelerometro: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private val _movimientoErratico = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val movimientoErratico = _movimientoErratico.asSharedFlow()

    // ── Cambia esto a true para simular sin mover el reloj ───
    var modoSimulacion: Boolean = false
        set(value) {
            field = value
            if (value) {
                // Dispara inmediatamente al activar
                ultimaEmision = 0L
                picos.clear()
                _movimientoErratico.tryEmit(Unit)
            }
        }

    private var umbralG       = UMBRAL_G_DEFAULT
    private var escuchando    = false
    private var ultimaEmision = 0L
    private var iniciadoEn    = 0L
    private val picos         = ArrayDeque<Long>()

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

    fun detener() {
        if (!escuchando) return
        sensorManager.unregisterListener(this)
        escuchando = false
        picos.clear()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // Si está en simulación el sensor real no hace nada
        if (modoSimulacion) return

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

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
}
package mx.edu.utng.compasos_wearos.helper

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import mx.edu.utng.compasos_wearos.R

/**
 * clase auxiliar que centraliza la retroalimentación háptica y sonora para el reloj inteligente.
 * compatible con wear os (samsung galaxy watch).
 *
 * @param context contexto de la aplicación utilizado para obtener los servicios del sistema y recursos.
 */
class FeedbackHelper(context: Context) {

    /** contexto de la aplicación almacenado de forma segura para evitar fugas de memoria. */
    private val appContext = context.applicationContext

    /** servicio de vibración del sistema adaptado según la versión de android. */
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * reproductor multimedia inicializado una sola vez y reutilizado para emitir efectos de sonido cortos.
     */
    private val mediaPlayer: MediaPlayer by lazy {
        MediaPlayer.create(appContext, R.raw.tick).apply {
            isLooping = false
            setVolume(1f, 1f)
        }
    }

    /**
     * ejecuta un patrón de vibración inicial para notificar una detección de evento.
     */
    fun vibrarDeteccion() {
        vibrarPatron(longArrayOf(0, 140))
    }

    /**
     * ejecuta una vibración corta de tipo tick.
     */
    fun vibrarTick() {
        vibrarPatron(longArrayOf(0, 45))
    }

    /**
     * reproduce el archivo de sonido tick.mp3 si el modo discreto se encuentra desactivado.
     *
     * @param modoDiscreto indica si el sonido debe silenciarse por estar en modo discreto.
     */
    fun reproducirTick(modoDiscreto: Boolean) {
        if (modoDiscreto) return

        runCatching {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }

            mediaPlayer.seekTo(0)
            mediaPlayer.start()
        }
    }

    /**
     * emite un aviso inicial combinando vibración de detección y opcionalmente sonido.
     *
     * @param modoDiscreto indica si se debe omitir la reproducción de sonido.
     */
    fun avisarDeteccion(modoDiscreto: Boolean) {
        vibrarDeteccion()

        if (!modoDiscreto) {
            reproducirTick(false)
        }
    }

    /**
     * aplica un patrón de vibración de onda personalizado según la versión del sistema operativo.
     *
     * @param patron arreglo de marcas de tiempo que define el patrón de vibración.
     */
    private fun vibrarPatron(patron: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    patron,
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(patron, -1)
        }
    }

    /**
     * ejecuta una vibración continua de una sola vez por una duración determinada.
     *
     * @param duracionMs duración de la vibración en milisegundos.
     */
    fun vibrarContinua(duracionMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    duracionMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duracionMs)
        }
    }

    /**
     * detiene y libera los recursos multimedia asociados al reproductor para evitar fugas de memoria.
     */
    fun liberar() {
        runCatching {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
    }
}
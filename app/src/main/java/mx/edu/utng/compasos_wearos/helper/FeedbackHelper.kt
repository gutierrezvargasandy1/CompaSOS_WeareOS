package mx.edu.utng.compasos_wearos.helper

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import mx.edu.utng.compasos_wearos.R

/**
 * Centraliza la retroalimentación háptica y sonora.
 * Compatible con Wear OS (Samsung Galaxy Watch).
 */
class FeedbackHelper(context: Context) {

    private val appContext = context.applicationContext

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
     * Se crea una sola vez y se reutiliza.
     */
    private val mediaPlayer: MediaPlayer by lazy {
        MediaPlayer.create(appContext, R.raw.tick).apply {
            isLooping = false
            setVolume(1f, 1f)
        }
    }

    /**
     * Vibración inicial.
     */
    fun vibrarDeteccion() {
        vibrarPatron(longArrayOf(0, 140))
    }

    /**
     * Vibración corta.
     */
    fun vibrarTick() {
        vibrarPatron(longArrayOf(0, 45))
    }

    /**
     * Reproduce el audio tick.mp3.
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
     * Aviso inicial.
     */
    fun avisarDeteccion(modoDiscreto: Boolean) {
        vibrarDeteccion()

        if (!modoDiscreto) {
            reproducirTick(false)
        }
    }

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
     * Vibración continua.
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
     * Libera recursos.
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
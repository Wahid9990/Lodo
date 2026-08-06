package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.GameSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundEffectManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun playDiceRollSound(settings: GameSettings) {
        if (!settings.soundFxEnabled) return
        scope.launch {
            generateBeepSequence(
                frequencies = listOf(400, 550, 480, 620, 700),
                durationMs = 40
            )
        }
        vibrate(settings, 30L)
    }

    fun playMoveSound(settings: GameSettings) {
        if (!settings.soundFxEnabled) return
        scope.launch {
            generateTone(frequency = 523.25, durationMs = 80) // C5 tone
        }
        vibrate(settings, 15L)
    }

    fun playCaptureSound(settings: GameSettings) {
        if (!settings.soundFxEnabled) return
        scope.launch {
            generateBeepSequence(
                frequencies = listOf(800, 600, 400, 200),
                durationMs = 60
            )
        }
        vibrate(settings, 100L)
    }

    fun playWinSound(settings: GameSettings) {
        if (!settings.soundFxEnabled) return
        scope.launch {
            generateBeepSequence(
                frequencies = listOf(523, 659, 783, 1046, 783, 1046),
                durationMs = 120
            )
        }
        vibrate(settings, 200L)
    }

    private fun vibrate(settings: GameSettings, durationMs: Long) {
        if (!settings.vibrationEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateTone(frequency: Double, durationMs: Int) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val sample = DoubleArray(numSamples)
        val buffer = ByteArray(2 * numSamples)

        for (i in 0 until numSamples) {
            sample[i] = sin(2.0 * Math.PI * i.toDouble() / (sampleRate / frequency))
        }

        var idx = 0
        for (dVal in sample) {
            val valShort = (dVal * 32767).toInt().toShort()
            buffer[idx++] = (valShort.toInt() and 0x00ff).toByte()
            buffer[idx++] = (valShort.toInt() and 0xff00 shr 8).toByte()
        }

        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            
            // Release audio track resources safely
            scope.launch {
                delay(durationMs.toLong() + 20L)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateBeepSequence(frequencies: List<Int>, durationMs: Int) {
        for (freq in frequencies) {
            generateTone(freq.toDouble(), durationMs)
            try {
                Thread.sleep(20)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

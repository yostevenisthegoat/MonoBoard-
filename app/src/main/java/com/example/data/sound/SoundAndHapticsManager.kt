package com.example.data.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SoundCue {
    MOVE,
    CAPTURE,
    WIN,
    ERROR,
    TURN_PASS
}

class SoundAndHapticsManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile var soundEnabled: Boolean = true
    @Volatile var musicEnabled: Boolean = false
    @Volatile var hapticsEnabled: Boolean = true

    private var ambientJob: Job? = null

    fun playCue(cue: SoundCue) {
        if (hapticsEnabled) {
            vibrateFor(cue)
        }
        if (soundEnabled) {
            scope.launch {
                when (cue) {
                    SoundCue.MOVE -> playTone(freq = 540.0, durationMs = 36, gain = 0.25)
                    SoundCue.CAPTURE -> playTone(freq = 280.0, durationMs = 65, gain = 0.35)
                    SoundCue.TURN_PASS -> playTone(freq = 660.0, durationMs = 48, gain = 0.22)
                    SoundCue.ERROR -> playTone(freq = 175.0, durationMs = 110, gain = 0.3)
                    SoundCue.WIN -> {
                        playTone(freq = 523.25, durationMs = 80, gain = 0.26)
                        delay(70)
                        playTone(freq = 659.25, durationMs = 80, gain = 0.26)
                        delay(70)
                        playTone(freq = 783.99, durationMs = 150, gain = 0.30)
                    }
                }
            }
        }
    }

    fun updateAmbientMusic(enabled: Boolean) {
        musicEnabled = enabled
        if (enabled) {
            if (ambientJob?.isActive == true) return
            ambientJob = scope.launch {
                val notes = listOf(220.0, 261.63, 329.63, 293.66, 246.94)
                var idx = 0
                while (isActive && musicEnabled) {
                    playTone(freq = notes[idx % notes.size], durationMs = 700, gain = 0.06)
                    idx++
                    delay(2200)
                }
            }
        } else {
            ambientJob?.cancel()
            ambientJob = null
        }
    }

    private fun vibrateFor(cue: SoundCue) {
        runCatching {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                mgr?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator == null || !vibrator.hasVibrator()) return
            val ms = when (cue) {
                SoundCue.MOVE -> 14L
                SoundCue.CAPTURE -> 28L
                SoundCue.TURN_PASS -> 20L
                SoundCue.ERROR -> 45L
                SoundCue.WIN -> 60L
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ms)
            }
        }
    }

    private fun playTone(freq: Double, durationMs: Int, gain: Double) {
        runCatching {
            val sampleRate = 22050
            val count = (sampleRate * durationMs) / 1000
            if (count <= 0) return
            val samples = ShortArray(count)
            for (i in 0 until count) {
                val t = i.toDouble() / sampleRate
                val env = exp(-5.0 * i / count)
                val valSample = sin(2.0 * PI * freq * t) * env * gain
                samples[i] = (valSample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(count * 2)
            val track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, count)
            track.play()
            Thread.sleep(durationMs.toLong() + 25L)
            track.stop()
            track.release()
        }
    }
}

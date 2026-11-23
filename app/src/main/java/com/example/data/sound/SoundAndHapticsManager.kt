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
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SoundCue {
    MOVE,
    CAPTURE,
    WIN,
    ERROR,
    TURN_PASS,
    // Rich game-specific SFX cues
    WOOD_TAP,
    PIECE_SLIDE,
    DICE_ROLL,
    CARD_FLIP,
    SHUFFLE,
    TILE_MERGE,
    EXPLOSION,
    SPLASH,
    CHECK_ALERT,
    HINT,
    KEY_TICK,
    BOUNCE
}

class SoundAndHapticsManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050
    private val pcmCache = ConcurrentHashMap<SoundCue, ShortArray>()

    @Volatile var soundEnabled: Boolean = true
    @Volatile var musicEnabled: Boolean = false
    @Volatile var hapticsEnabled: Boolean = true

    private var ambientJob: Job? = null

    init {
        // Pre-warm common cues off the UI thread for zero-latency playback
        scope.launch {
            SoundCue.entries.forEach { cue ->
                pcmCache.putIfAbsent(cue, synthesizeCue(cue))
            }
        }
    }

    fun playCue(cue: SoundCue) {
        if (hapticsEnabled) {
            vibrateFor(cue)
        }
        if (soundEnabled) {
            scope.launch {
                val samples = pcmCache.getOrPut(cue) { synthesizeCue(cue) }
                playPcm(samples)
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
                    val buf = buildTone(notes[idx % notes.size], 650, 0.05, harmonics = true)
                    playPcm(buf)
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (cue) {
                    SoundCue.WIN -> vibrator.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 28, 45, 36, 45, 65), intArrayOf(0, 140, 0, 190, 0, 255), -1)
                    )
                    SoundCue.EXPLOSION -> vibrator.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 60, 25, 40), intArrayOf(0, 255, 0, 170), -1)
                    )
                    SoundCue.DICE_ROLL -> vibrator.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 12, 35, 12, 40, 14, 50, 18), intArrayOf(0, 110, 0, 130, 0, 140, 0, 160), -1)
                    )
                    SoundCue.CHECK_ALERT, SoundCue.ERROR -> vibrator.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 24, 30, 32), intArrayOf(0, 180, 0, 210), -1)
                    )
                    else -> {
                        val ms = when (cue) {
                            SoundCue.KEY_TICK, SoundCue.MOVE, SoundCue.CARD_FLIP -> 12L
                            SoundCue.WOOD_TAP, SoundCue.BOUNCE -> 18L
                            SoundCue.CAPTURE, SoundCue.TILE_MERGE -> 28L
                            SoundCue.TURN_PASS, SoundCue.HINT -> 20L
                            else -> 16L
                        }
                        val amp = when (cue) {
                            SoundCue.CAPTURE, SoundCue.TILE_MERGE -> 190
                            SoundCue.WOOD_TAP, SoundCue.BOUNCE -> 140
                            else -> 95
                        }
                        vibrator.vibrate(VibrationEffect.createOneShot(ms, amp))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (cue == SoundCue.WIN || cue == SoundCue.EXPLOSION) 55L else 16L)
            }
        }
    }

    private fun synthesizeCue(cue: SoundCue): ShortArray {
        return when (cue) {
            SoundCue.MOVE -> buildWoodClick(baseFreq = 580.0, durMs = 38, gain = 0.28)
            SoundCue.WOOD_TAP -> buildWoodClick(baseFreq = 390.0, durMs = 58, gain = 0.36)
            SoundCue.KEY_TICK -> buildWoodClick(baseFreq = 1150.0, durMs = 24, gain = 0.24)
            SoundCue.CAPTURE -> concatWithSilence(
                buildWoodClick(baseFreq = 260.0, durMs = 45, gain = 0.40),
                buildTone(freq = 165.0, durMs = 60, gain = 0.30, harmonics = true),
                gapMs = 0
            )
            SoundCue.TURN_PASS -> concatWithSilence(
                buildTone(freq = 587.33, durMs = 45, gain = 0.24, harmonics = true),
                buildTone(freq = 880.0, durMs = 70, gain = 0.26, harmonics = true),
                gapMs = 35
            )
            SoundCue.HINT -> concatWithSilence(
                buildTone(freq = 987.77, durMs = 55, gain = 0.20, harmonics = true),
                buildTone(freq = 1318.51, durMs = 95, gain = 0.22, harmonics = true),
                gapMs = 25
            )
            SoundCue.TILE_MERGE -> concatWithSilence(
                buildTone(freq = 523.25, durMs = 45, gain = 0.26, harmonics = true),
                buildTone(freq = 783.99, durMs = 80, gain = 0.30, harmonics = true),
                gapMs = 18
            )
            SoundCue.BOUNCE -> buildSweep(startFreq = 280.0, endFreq = 690.0, durMs = 85, gain = 0.30)
            SoundCue.PIECE_SLIDE -> buildFilteredNoise(durMs = 120, lowPassAlpha = 0.28, gain = 0.24, rise = true)
            SoundCue.CARD_FLIP -> buildFilteredNoise(durMs = 62, lowPassAlpha = 0.45, gain = 0.30, rise = false)
            SoundCue.SHUFFLE -> buildRiffleBurst()
            SoundCue.DICE_ROLL -> buildDiceRattle()
            SoundCue.SPLASH -> buildFilteredNoise(durMs = 190, lowPassAlpha = 0.36, gain = 0.34, rise = false)
            SoundCue.EXPLOSION -> buildExplosion()
            SoundCue.CHECK_ALERT -> concatWithSilence(
                buildTone(freq = 920.0, durMs = 55, gain = 0.30, harmonics = true),
                buildTone(freq = 1240.0, durMs = 90, gain = 0.32, harmonics = true),
                gapMs = 45
            )
            SoundCue.ERROR -> concatWithSilence(
                buildTone(freq = 210.0, durMs = 70, gain = 0.30, harmonics = true),
                buildTone(freq = 155.0, durMs = 110, gain = 0.32, harmonics = true),
                gapMs = 15
            )
            SoundCue.WIN -> buildWinFanfare()
        }
    }

    private fun buildTone(freq: Double, durMs: Int, gain: Double, harmonics: Boolean = false): ShortArray {
        val count = (sampleRate * durMs) / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val attack = (i / (sampleRate * 0.003)).coerceAtMost(1.0)
            val env = exp(-5.5 * i / count) * attack
            val fundamental = sin(2.0 * PI * freq * t)
            val raw = if (harmonics) {
                fundamental * 0.72 + sin(2.0 * PI * freq * 2.0 * t) * 0.20 + sin(2.0 * PI * freq * 3.0 * t) * 0.08
            } else fundamental
            out[i] = toShort(raw * env * gain)
        }
        return out
    }

    private fun buildWoodClick(baseFreq: Double, durMs: Int, gain: Double): ShortArray {
        val count = (sampleRate * durMs) / 1000
        val out = ShortArray(count)
        val rng = Random(baseFreq.toInt())
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val e1 = exp(-10.0 * i / count)
            val e2 = exp(-28.0 * i / count)
            val body = sin(2.0 * PI * baseFreq * t) * e1 * 0.65 +
                sin(2.0 * PI * baseFreq * 2.35 * t) * e2 * 0.25 +
                (rng.nextDouble() * 2.0 - 1.0) * e2 * 0.25
            out[i] = toShort(body * gain)
        }
        return out
    }

    private fun buildSweep(startFreq: Double, endFreq: Double, durMs: Int, gain: Double): ShortArray {
        val count = (sampleRate * durMs) / 1000
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val u = i.toDouble() / count
            val f = startFreq + (endFreq - startFreq) * u
            phase += 2.0 * PI * f / sampleRate
            val env = exp(-4.2 * u) * (i / (sampleRate * 0.004)).coerceAtMost(1.0)
            out[i] = toShort(sin(phase) * env * gain)
        }
        return out
    }

    private fun buildFilteredNoise(durMs: Int, lowPassAlpha: Double, gain: Double, rise: Boolean): ShortArray {
        val count = (sampleRate * durMs) / 1000
        val out = ShortArray(count)
        val rng = Random(durMs)
        var lp = 0.0
        for (i in 0 until count) {
            val u = i.toDouble() / count
            val raw = rng.nextDouble() * 2.0 - 1.0
            lp = lp * (1.0 - lowPassAlpha) + raw * lowPassAlpha
            val env = if (rise) sin(PI * u) else exp(-5.0 * u) * (i / (sampleRate * 0.004)).coerceAtMost(1.0)
            out[i] = toShort(lp * env * gain)
        }
        return out
    }

    private fun buildDiceRattle(): ShortArray {
        val totalMs = 280
        val total = (sampleRate * totalMs) / 1000
        val out = FloatArray(total)
        val hits = listOf(0 to 920.0, 38 to 1180.0, 82 to 860.0, 134 to 1050.0, 195 to 790.0)
        for ((offsetMs, pitch) in hits) {
            val click = buildWoodClick(pitch, 32, 0.34)
            val startIdx = (sampleRate * offsetMs) / 1000
            for (j in click.indices) {
                if (startIdx + j < total) out[startIdx + j] += click[j] / 32767f
            }
        }
        return ShortArray(total) { i -> toShort(out[i].toDouble()) }
    }

    private fun buildRiffleBurst(): ShortArray {
        val total = (sampleRate * 220) / 1000
        val out = FloatArray(total)
        for (k in 0 until 6) {
            val snap = buildFilteredNoise(28, 0.50, 0.26, false)
            val start = (sampleRate * k * 30) / 1000
            for (j in snap.indices) {
                if (start + j < total) out[start + j] += snap[j] / 32767f
            }
        }
        return ShortArray(total) { i -> toShort(out[i].toDouble()) }
    }

    private fun buildExplosion(): ShortArray {
        val count = (sampleRate * 260) / 1000
        val out = ShortArray(count)
        val rng = Random(99)
        var lp = 0.0
        var phase = 0.0
        for (i in 0 until count) {
            val u = i.toDouble() / count
            val f = 110.0 - 78.0 * u
            phase += 2.0 * PI * f / sampleRate
            val raw = rng.nextDouble() * 2.0 - 1.0
            lp = lp * 0.85 + raw * 0.15
            val env = exp(-4.5 * u)
            val sig = (sin(phase) * 0.65 + lp * 0.65) * env * 0.45
            out[i] = toShort(sig)
        }
        return out
    }

    private fun buildWinFanfare(): ShortArray {
        val a = buildTone(523.25, 75, 0.25, harmonics = true)
        val b = buildTone(659.25, 75, 0.27, harmonics = true)
        val c = buildTone(783.99, 85, 0.29, harmonics = true)
        val d = buildTone(1046.50, 175, 0.32, harmonics = true)
        val ab = concatWithSilence(a, b, 55)
        val abc = concatWithSilence(ab, c, 55)
        return concatWithSilence(abc, d, 65)
    }

    private fun concatWithSilence(first: ShortArray, second: ShortArray, gapMs: Int): ShortArray {
        val gapSamples = (sampleRate * gapMs) / 1000
        val res = ShortArray(first.size + gapSamples + second.size)
        first.copyInto(res, 0)
        second.copyInto(res, first.size + gapSamples)
        return res
    }

    private fun toShort(v: Double): Short =
        (v.coerceIn(-0.98, 0.98) * Short.MAX_VALUE).toInt().toShort()

    private fun playPcm(samples: ShortArray) {
        if (samples.isEmpty()) return
        runCatching {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(samples.size * 2)
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
            track.write(samples, 0, samples.size)
            track.play()
            val durMs = ((samples.size * 1000L) / sampleRate) + 40L
            scope.launch {
                delay(durMs)
                runCatching {
                    track.stop()
                    track.release()
                }
            }
        }
    }
}

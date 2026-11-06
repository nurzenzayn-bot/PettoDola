package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.example.model.PetSpecies
import com.example.model.TouchZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * High-speed, zero-latency procedural cartoon sound effect synthesizer.
 * Produces Talking-Tom style cartoon audio effects (tickle giggles, spring boings,
 * high-five pops, affectionate purrs, puppy chirps, capybara yawns, and bear grunts).
 */
object ExpressiveAudioSynthesizer {

    private const val TAG = "AudioSynthesizer"
    private const val SAMPLE_RATE = 24000
    private val synthScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val audioFormat = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(SAMPLE_RATE)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    private fun playBuffer(buffer: ShortArray) {
        activeJob?.cancel()
        activeJob = synthScope.launch {
            var track: AudioTrack? = null
            try {
                track = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                val durationMs = (buffer.size * 1000L) / SAMPLE_RATE
                delay(durationMs + 40L)
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cancelled by a newer sound effect
            } catch (e: Exception) {
                Log.e(TAG, "Audio playback error", e)
            } finally {
                try {
                    track?.stop()
                } catch (_: Exception) {}
                try {
                    track?.release()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Cartoon Spring "Boing!" sound effect (Talking Tom jump/surprise).
     */
    fun playSpringBoing() {
        val durationMs = 280
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        val startFreq = 220.0
        val endFreq = 950.0

        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            // Frequency sweeps upwards with exponential chirp
            val currentFreq = startFreq + (endFreq - startFreq) * progress * progress
            // Vibrato oscillation
            val vibrato = sin(2.0 * PI * 18.0 * (i.toDouble() / SAMPLE_RATE)) * 25.0
            val angle = 2.0 * PI * (currentFreq + vibrato) * (i.toDouble() / SAMPLE_RATE)

            // Dynamic decay envelope with slight bounce
            val envelope = (1.0 - progress) * (0.8 + 0.2 * sin(progress * PI * 4.0))
            buffer[i] = (sin(angle) * 32767 * envelope * 0.45).toInt().toShort()
        }
        playBuffer(buffer)
    }

    /**
     * Cartoon Giggle / Laugh sound effect (Talking Tom tickle reaction).
     */
    fun playCartoonGiggle() {
        val durationMs = 380
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        // Multiple rapid pitched chuckles
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // 3-pulse chuckle envelope
            val cycle = (t * 8.0) % 1.0
            val chuckleEnvelope = sin(cycle * PI)

            // Pitch oscillation around 650Hz - 900Hz
            val freq = 720.0 + sin(t * 22.0) * 160.0
            val angle = 2.0 * PI * freq * t

            val overallDecay = 1.0 - (i.toDouble() / numSamples) * 0.7
            buffer[i] = (sin(angle) * 32767 * chuckleEnvelope * overallDecay * 0.38).toInt().toShort()
        }
        playBuffer(buffer)
    }

    /**
     * High-Five Pop / Slap sound effect.
     */
    fun playHighFivePop() {
        val durationMs = 160
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            // Rapid pitch drop 1200Hz -> 200Hz (cartoon pop)
            val freq = 1200.0 * (1.0 - progress * 0.8)
            val angle = 2.0 * PI * freq * t

            // Sharp exponential decay
            val envelope = kotlin.math.exp(-progress * 9.0)
            buffer[i] = (sin(angle) * 32767 * envelope * 0.6).toInt().toShort()
        }
        playBuffer(buffer)
    }

    /**
     * Sweet harmonic purr & chime for head pet.
     */
    fun playHeadPetChime() {
        val durationMs = 320
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        // Chime frequencies (C6, E6, G6 musical arpeggio)
        val f1 = 1046.50
        val f2 = 1318.51
        val f3 = 1567.98

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val freq = when {
                progress < 0.3 -> f1
                progress < 0.6 -> f2
                else -> f3
            }
            val angle = 2.0 * PI * freq * t
            val envelope = 1.0 - progress
            buffer[i] = (sin(angle) * 32767 * envelope * 0.35).toInt().toShort()
        }
        playBuffer(buffer)
    }

    /**
     * Comic Bear Grunt / Roar (for Heru the Bear).
     */
    fun playBearGrunt() {
        val durationMs = 300
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            // Deep 110Hz to 85Hz growl with distorted subharmonic
            val freq = 120.0 - progress * 40.0
            val carrier = sin(2.0 * PI * freq * t)
            val subHarmonic = sin(2.0 * PI * (freq * 0.5) * t)

            val envelope = sin(progress * PI)
            val sample = ((carrier * 0.7 + subHarmonic * 0.3) * 32767 * envelope * 0.55).toInt()
            buffer[i] = sample.coerceIn(-32767, 32767).toShort()
        }
        playBuffer(buffer)
    }

    /**
     * Capybara Chill Yawn (for Jamal the Capybara).
     */
    fun playCapybaraYawn() {
        val durationMs = 450
        val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            // Slide down from 480Hz down to 260Hz like a sleepy sigh
            val freq = 480.0 - progress * 220.0
            val angle = 2.0 * PI * freq * t
            val envelope = sin(progress * PI) * 0.4
            buffer[i] = (sin(angle) * 32767 * envelope).toInt().toShort()
        }
        playBuffer(buffer)
    }

    /**
     * Plays the tailored cartoon sound effect based on the character's species and the touched zone.
     */
    fun playTouchReactionSound(species: PetSpecies, zone: TouchZone) {
        when (zone) {
            TouchZone.HEAD -> {
                when (species) {
                    PetSpecies.BEAR -> playBearGrunt()
                    PetSpecies.CAPYBARA -> playCapybaraYawn()
                    else -> playHeadPetChime()
                }
            }
            TouchZone.BELLY -> {
                playCartoonGiggle()
            }
            TouchZone.HANDS -> {
                playHighFivePop()
            }
            TouchZone.FEET -> {
                playSpringBoing()
            }
        }
    }
}

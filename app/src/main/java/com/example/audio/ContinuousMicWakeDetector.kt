package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Continuous background audio listener built on the Android Microphone API ([AudioRecord]).
 *
 * Features:
 * - Zero system beeps and zero Binder IPC restart loops (single continuous PCM stream).
 * - Adaptive Voice Activity Detection (VAD) with a 400ms pre-roll ring buffer so the wake word
 *   "Petto" is captured from the very first consonant.
 * - Real-time throttled RMS audio level callback for smooth UI wave visualization.
 * - Automatic WAV framing (16kHz, 16-bit Mono PCM) ready for Gemini 3.8 Flash Multimodal Audio.
 * - Built-in silence timeout tracking for automatic return from Gemini Live Session to Standby.
 */
class ContinuousMicWakeDetector(
    private val context: Context,
    private val onRmsChanged: (Float) -> Unit,
    private val onSpeechActivityChanged: (Boolean) -> Unit,
    private val onVoiceSegmentCaptured: (wavBytes: ByteArray, durationMs: Long, peakRms: Float) -> Unit,
    private val onLiveSessionSilenceTimeout: () -> Unit
) {
    companion object {
        private const val TAG = "ContinuousMicWake"
        private const val SAMPLE_RATE = 16000
        private const val FRAME_DURATION_MS = 100 // 100ms per frame = 1600 samples
        private const val SAMPLES_PER_FRAME = (SAMPLE_RATE * FRAME_DURATION_MS) / 1000
        private const val PRE_ROLL_FRAMES = 8 // 800ms pre-roll buffer so the start of a short wake word is not cut
        private const val MIN_SPEECH_MS = 300L
        private const val MAX_SPEECH_MS = 6500L
        private const val END_SILENCE_MS = 650L
        private const val LIVE_SESSION_SILENCE_TIMEOUT_MS = 5500L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var captureJob: Job? = null

    @Volatile
    var isRunning: Boolean = false
        private set

    @Volatile
    private var isPausedForTts: Boolean = false

    @Volatile
    private var isLiveSessionMode: Boolean = false

    @Volatile
    private var lastLiveActivityElapsedMs: Long = SystemClock.elapsedRealtime()

    val isPaused: Boolean
        get() = isPausedForTts

    fun setPaused(paused: Boolean) {
        if (isPausedForTts != paused) {
            Log.d(TAG, "setPaused(" + paused + ")")
        }
        isPausedForTts = paused
        if (!paused) {
            lastLiveActivityElapsedMs = SystemClock.elapsedRealtime()
        } else {
            onSpeechActivityChanged(false)
            onRmsChanged(0f)
        }
    }

    fun setLiveSessionMode(active: Boolean) {
        isLiveSessionMode = active
        lastLiveActivityElapsedMs = SystemClock.elapsedRealtime()
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) {
            Log.w(TAG, "RECORD_AUDIO permission not granted yet; waiting to start ContinuousMicWakeDetector")
            return
        }

        isRunning = true
        captureJob?.cancel()
        captureJob = scope.launch {
            var audioRecord: AudioRecord? = null
            try {
                val minBufSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBufSize <= 0) {
                    Log.w(TAG, "AudioRecord minBufferSize invalid: $minBufSize")
                    isRunning = false
                    return@launch
                }

                val bufferSizeInBytes = max(minBufSize * 2, SAMPLES_PER_FRAME * 2 * 4)
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeInBytes
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w(TAG, "AudioRecord failed to initialize")
                    isRunning = false
                    return@launch
                }

                audioRecord.startRecording()
                Log.d(TAG, "ContinuousMicWakeDetector started (16kHz Mono PCM)")

                val frameBuffer = ShortArray(SAMPLES_PER_FRAME)
                val preRollQueue = ArrayDeque<ShortArray>(PRE_ROLL_FRAMES)
                val capturedPcmOut = ByteArrayOutputStream()

                var adaptiveNoiseFloorRms = 220.0
                var isSpeaking = false
                var consecutiveVoiceFrames = 0
                var speechDurationMs = 0L
                var silenceAfterSpeechMs = 0L
                var peakUiRms = 0f
                var lastRmsUiEmitMs = 0L
                var lastEmittedUiRms = 0f
                var lastVadLogMs = 0L

                lastLiveActivityElapsedMs = SystemClock.elapsedRealtime()

                while (isActive && isRunning) {
                    if (isPausedForTts) {
                        if (isSpeaking) {
                            isSpeaking = false
                            consecutiveVoiceFrames = 0
                            speechDurationMs = 0L
                            silenceAfterSpeechMs = 0L
                            capturedPcmOut.reset()
                            onSpeechActivityChanged(false)
                        }
                        delay(120)
                        continue
                    }

                    val readCount = audioRecord.read(frameBuffer, 0, SAMPLES_PER_FRAME)
                    if (readCount <= 0) {
                        delay(50)
                        continue
                    }

                    // Calculate frame RMS amplitude (0..32767)
                    var sumSquares = 0.0
                    for (i in 0 until readCount) {
                        val s = frameBuffer[i].toDouble()
                        sumSquares += s * s
                    }
                    val rmsAmplitude = sqrt(sumSquares / readCount)

                    // Convert to 0..10 UI RMS scale
                    val normalizedUiRms = if (rmsAmplitude > 50.0) {
                        ((20.0 * log10(rmsAmplitude / 120.0)).coerceIn(0.0, 10.0)).toFloat()
                    } else {
                        0f
                    }

                    val now = SystemClock.elapsedRealtime()
                    if (now - lastRmsUiEmitMs >= 90L && abs(normalizedUiRms - lastEmittedUiRms) >= 0.35f) {
                        lastRmsUiEmitMs = now
                        lastEmittedUiRms = normalizedUiRms
                        onRmsChanged(normalizedUiRms)
                    }

                    // Dynamic VAD threshold above ambient noise floor
                    val voiceThreshold = max(420.0, adaptiveNoiseFloorRms * 2.2)
                    val isVoiceFrame = rmsAmplitude >= voiceThreshold

                    if (now - lastVadLogMs >= 3000L) {
                        lastVadLogMs = now
                        Log.d(TAG, "VAD rms=" + rmsAmplitude.toInt() + " threshold=" + voiceThreshold.toInt() + " floor=" + adaptiveNoiseFloorRms.toInt() + " speaking=" + isSpeaking + " live=" + isLiveSessionMode)
                    }

                    if (!isSpeaking) {
                        // Slowly adapt noise floor during silence
                        if (!isVoiceFrame) {
                            adaptiveNoiseFloorRms = (adaptiveNoiseFloorRms * 0.92 + rmsAmplitude * 0.08)
                                .coerceIn(140.0, 1600.0)
                        }

                        // Maintain pre-roll ring buffer
                        if (preRollQueue.size >= PRE_ROLL_FRAMES) {
                            preRollQueue.removeFirst()
                        }
                        preRollQueue.addLast(frameBuffer.copyOf(readCount))

                        if (isVoiceFrame) {
                            consecutiveVoiceFrames++
                            if (consecutiveVoiceFrames >= 2) {
                                // Speech started!
                                isSpeaking = true
                                speechDurationMs = (preRollQueue.size * FRAME_DURATION_MS).toLong()
                                silenceAfterSpeechMs = 0L
                                peakUiRms = normalizedUiRms
                                capturedPcmOut.reset()
                                for (preFrame in preRollQueue) {
                                    writeShortsLittleEndian(capturedPcmOut, preFrame, preFrame.size)
                                }
                                preRollQueue.clear()
                                lastLiveActivityElapsedMs = now
                                Log.d(TAG, "Speech start: rms=" + rmsAmplitude.toInt() + " threshold=" + voiceThreshold.toInt() + " floor=" + adaptiveNoiseFloorRms.toInt())
                                onSpeechActivityChanged(true)
                            }
                        } else {
                            consecutiveVoiceFrames = 0
                            // Check if active Gemini Live session has been silent past timeout
                            if (isLiveSessionMode && (now - lastLiveActivityElapsedMs) >= LIVE_SESSION_SILENCE_TIMEOUT_MS) {
                                isLiveSessionMode = false
                                lastLiveActivityElapsedMs = now
                                onLiveSessionSilenceTimeout()
                            }
                        }
                    } else {
                        // Currently recording a speech utterance
                        writeShortsLittleEndian(capturedPcmOut, frameBuffer, readCount)
                        speechDurationMs += FRAME_DURATION_MS
                        peakUiRms = max(peakUiRms, normalizedUiRms)
                        lastLiveActivityElapsedMs = now

                        if (isVoiceFrame) {
                            silenceAfterSpeechMs = 0L
                        } else {
                            silenceAfterSpeechMs += FRAME_DURATION_MS
                        }

                        val shouldFinish = silenceAfterSpeechMs >= END_SILENCE_MS || speechDurationMs >= MAX_SPEECH_MS
                        if (shouldFinish) {
                            val totalDuration = speechDurationMs
                            val pcmBytes = capturedPcmOut.toByteArray()
                            val capturedPeak = peakUiRms

                            isSpeaking = false
                            consecutiveVoiceFrames = 0
                            speechDurationMs = 0L
                            silenceAfterSpeechMs = 0L
                            capturedPcmOut.reset()
                            onSpeechActivityChanged(false)
                            onRmsChanged(0f)

                            if (totalDuration >= MIN_SPEECH_MS && pcmBytes.isNotEmpty()) {
                                val wavBytes = wrapPcmInWavHeader(pcmBytes, SAMPLE_RATE)
                                Log.d(TAG, "Segment captured: " + totalDuration + "ms, " + wavBytes.size + " bytes, peak=" + capturedPeak)
                                onVoiceSegmentCaptured(wavBytes, totalDuration, capturedPeak)
                            } else {
                                Log.d(TAG, "Segment dropped: duration=" + totalDuration + "ms, bytes=" + pcmBytes.size)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in ContinuousMicWakeDetector loop: ${e.message}", e)
            } finally {
                isRunning = false
                try {
                    audioRecord?.stop()
                } catch (_: Exception) {}
                try {
                    audioRecord?.release()
                } catch (_: Exception) {}
            }
        }
    }

    fun stop() {
        isRunning = false
        captureJob?.cancel()
        captureJob = null
    }

    private fun writeShortsLittleEndian(out: ByteArrayOutputStream, shorts: ShortArray, count: Int) {
        val bytes = ByteArray(count * 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts, 0, count)
        out.write(bytes)
    }

    private fun wrapPcmInWavHeader(pcmData: ByteArray, sampleRate: Int): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = (channels * (bitsPerSample / 8)).toShort()
        val dataSize = pcmData.size
        val chunkSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(chunkSize)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // PCM subchunk1 size
        header.putShort(1) // AudioFormat 1 = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign)
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataSize)

        val wavBytes = ByteArray(44 + dataSize)
        System.arraycopy(header.array(), 0, wavBytes, 0, 44)
        System.arraycopy(pcmData, 0, wavBytes, 44, dataSize)
        return wavBytes
    }
}

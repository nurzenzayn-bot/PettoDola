package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.model.AppLanguage
import kotlin.math.abs

class SpeechRecognitionHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onPartialResult: (String) -> Unit = {},
    private val onRmsChanged: (Float) -> Unit = {},
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onServiceUnavailable: () -> Unit = {},
    private val onErrorCallback: (Int, Long) -> Unit = { _, _ -> }
) {
    private val TAG = "SpeechRecognitionHelper"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isServiceAvailable: Boolean = true
    private var lastStartAttemptMs: Long = 0L
    private var sessionStartElapsedMs: Long = 0L
    private var lastRmsUpdateMs: Long = 0L
    private var lastRmsValue: Float = 0f

    var isListening: Boolean = false
        private set

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    private fun initRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                isServiceAvailable = true
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            mainHandler.post { onListeningStateChanged(true) }
                        }

                        override fun onBeginningOfSpeech() {
                            isListening = true
                            mainHandler.post { onListeningStateChanged(true) }
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            val now = SystemClock.elapsedRealtime()
                            if (now - lastRmsUpdateMs >= 90L && abs(rmsdB - lastRmsValue) >= 0.4f) {
                                lastRmsUpdateMs = now
                                lastRmsValue = rmsdB
                                mainHandler.post { onRmsChanged(rmsdB) }
                            }
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            isListening = false
                            mainHandler.post { onListeningStateChanged(false) }
                        }

                        override fun onError(error: Int) {
                            val durationMs = (SystemClock.elapsedRealtime() - sessionStartElapsedMs).coerceAtLeast(0L)
                            Log.w(TAG, "SpeechRecognizer error: $error (after ${durationMs}ms)")
                            isListening = false
                            mainHandler.post {
                                onListeningStateChanged(false)
                                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                                    error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                                    error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE
                                ) {
                                    onServiceUnavailable()
                                }
                                onErrorCallback(error, durationMs)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            isListening = false
                            mainHandler.post {
                                onListeningStateChanged(false)
                                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                if (!matches.isNullOrEmpty()) {
                                    onResult(matches[0])
                                } else {
                                    onErrorCallback(SpeechRecognizer.ERROR_NO_MATCH, 1000L)
                                }
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                mainHandler.post { onPartialResult(matches[0]) }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } else {
                Log.w(TAG, "SpeechRecognizer service is not available on this device")
                isServiceAvailable = false
                onServiceUnavailable()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing SpeechRecognizer", e)
            isServiceAvailable = false
            onServiceUnavailable()
        }
    }

    fun startListening(language: AppLanguage): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (isListening || (now - lastStartAttemptMs) < 350L) {
            return isListening
        }
        lastStartAttemptMs = now

        mainHandler.post {
            try {
                if (!isServiceAvailable && !SpeechRecognizer.isRecognitionAvailable(context)) {
                    onServiceUnavailable()
                    return@post
                }
                if (speechRecognizer == null) {
                    initRecognizer()
                }

                val recognizer = speechRecognizer
                if (recognizer != null) {
                    val localeTag = when (language) {
                        AppLanguage.INDONESIAN -> "id-ID"
                        AppLanguage.ENGLISH -> "en-US"
                        AppLanguage.JAPANESE -> "ja-JP"
                        AppLanguage.SPANISH -> "es-ES"
                    }
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    }
                    sessionStartElapsedMs = SystemClock.elapsedRealtime()
                    isListening = true
                    onListeningStateChanged(true)
                    recognizer.startListening(intent)
                } else {
                    onServiceUnavailable()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening", e)
                isListening = false
                onListeningStateChanged(false)
                onServiceUnavailable()
            }
        }
        return true
    }

    fun stopListening() {
        mainHandler.post {
            try {
                if (isListening) {
                    speechRecognizer?.stopListening()
                    isListening = false
                    onListeningStateChanged(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop speech recognition", e)
                isListening = false
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                // Drop any queued startListening/stopListening Runnables so a stale
                // callback cannot recreate a recognizer after destroy().
                mainHandler.removeCallbacksAndMessages(null)
                isListening = false
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to destroy speech recognizer", e)
            }
        }
    }

    companion object {
        fun createSystemSpeechIntent(language: AppLanguage): Intent {
            val localeTag = when (language) {
                AppLanguage.INDONESIAN -> "id-ID"
                AppLanguage.ENGLISH -> "en-US"
                AppLanguage.JAPANESE -> "ja-JP"
                AppLanguage.SPANISH -> "es-ES"
            }
            return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara dengan avatar...")
            }
        }
    }
}

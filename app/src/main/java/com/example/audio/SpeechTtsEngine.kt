package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.AppLanguage
import com.example.model.CharacterCatalog
import com.example.model.PetCharacter
import com.example.model.PetSpecies
import com.example.model.TouchZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Pure Gemini AI Expressive Voice Engine with an Android system TTS safety net.
 * Character speech is normally synthesized via Gemini AI Native Audio / TTS
 * (`NeuralTtsClient.speakWithGeminiTts`). When Gemini TTS is unavailable
 * (quota exhausted / HTTP 429 / offline) the pet falls back to the device's
 * built-in TTS voice so it never goes completely mute.
 */
class SpeechTtsEngine(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) {

    private val TAG = "SpeechTtsEngine"
    private var currentLanguage = AppLanguage.INDONESIAN
    private var currentPitch: Float = 1.25f
    private var currentRate: Float = 1.15f

    private val ttsScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Gemini AI & Neural TTS Client
    private val neuralClient = NeuralTtsClient(context, onSpeakingStateChanged)

    // Emergency fallback voice: Android's built-in TTS. Keeps the pet talking
    // when the Gemini TTS free-tier quota is exhausted (HTTP 429) or offline.
    private var systemTts: TextToSpeech? = null
    @Volatile
    private var systemTtsReady = false

    // The system TTS progress listener fires on a binder thread; hop back to main
    // so the ViewModel's state updates stay single-threaded.
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private fun notifySpeaking(speaking: Boolean) {
        mainHandler.post { onSpeakingStateChanged(speaking) }
    }

    init {
        try {
            systemTts = TextToSpeech(context) { status ->
                systemTtsReady = status == TextToSpeech.SUCCESS
                if (systemTtsReady) {
                    applySystemTtsSettings()
                    systemTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            notifySpeaking(true)
                        }

                        override fun onDone(utteranceId: String?) {
                            notifySpeaking(false)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            notifySpeaking(false)
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            notifySpeaking(false)
                        }
                    })
                    Log.d(TAG, "Android system TTS fallback ready")
                } else {
                    Log.w(TAG, "Android system TTS fallback unavailable (status=$status)")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize Android system TTS fallback", e)
        }
    }

    // Voice Engine Mode & API Keys
    var engineMode: VoiceEngineMode = VoiceEngineMode.GEMINI_FLASH_TTS
    var googleApiKey: String = ""
    var elevenLabsApiKey: String = ""

    fun setVoiceProfile(pitch: Float, rate: Float) {
        currentPitch = pitch
        currentRate = rate
    }

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
    }

    /**
     * Speaks the text using Gemini AI Expressive Voice (or optional Cloud Neural API if configured).
     * When every Gemini voice path fails, falls back to the device's built-in TTS so the
     * character still answers out loud instead of staying silent.
     */
    fun speak(
        text: String,
        pitch: Float = currentPitch,
        rate: Float = currentRate,
        character: PetCharacter? = null
    ) {
        stop()
        if (text.isBlank()) return

        val targetCharacter = character ?: CharacterCatalog.characters.first()

        ttsScope.launch {
            // 1. Optional ElevenLabs Neural if explicitly selected and key is configured
            if (engineMode == VoiceEngineMode.ELEVENLABS_NEURAL && elevenLabsApiKey.isNotBlank()) {
                val success = neuralClient.speakWithElevenLabs(text, elevenLabsApiKey, targetCharacter)
                if (success) return@launch
            }

            // 2. Optional Google Cloud Neural2 if explicitly selected
            if (engineMode == VoiceEngineMode.GOOGLE_CLOUD_NEURAL && googleApiKey.isNotBlank()) {
                val success = neuralClient.speakWithGoogleCloud(
                    text = text,
                    apiKey = googleApiKey,
                    character = targetCharacter,
                    language = currentLanguage,
                    pitch = pitch,
                    rate = rate
                )
                if (success) return@launch
            }

            // 3. Primary voice engine: Gemini AI Expressive Voice
            val geminiSuccess = neuralClient.speakWithGeminiTts(
                text = text,
                apiKey = googleApiKey,
                character = targetCharacter,
                language = currentLanguage
            )

            if (!geminiSuccess) {
                Log.w(TAG, "Gemini AI Voice unavailable; using Android system TTS fallback")
                speakWithSystemTts(text, pitch, rate)
            }
        }
    }

    private fun applySystemTtsSettings() {
        val locale = when (currentLanguage) {
            AppLanguage.INDONESIAN -> Locale("id", "ID")
            AppLanguage.ENGLISH -> Locale.US
            AppLanguage.JAPANESE -> Locale.JAPAN
            AppLanguage.SPANISH -> Locale("es", "ES")
        }
        try {
            val availability = systemTts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (availability == TextToSpeech.LANG_MISSING_DATA || availability == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "System TTS language $locale unavailable; using device default locale")
                systemTts?.language = Locale.getDefault()
            } else {
                systemTts?.language = locale
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to configure system TTS language", e)
        }
    }

    private fun speakWithSystemTts(text: String, pitch: Float, rate: Float) {
        val tts = systemTts
        if (tts == null || !systemTtsReady) {
            Log.w(TAG, "Android system TTS not ready; character stays silent this turn")
            onSpeakingStateChanged(false)
            return
        }
        try {
            applySystemTtsSettings()
            tts.setPitch(pitch.coerceIn(0.5f, 2.0f))
            tts.setSpeechRate(rate.coerceIn(0.7f, 1.6f))
            val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "petto_" + System.nanoTime())
            if (result != TextToSpeech.SUCCESS) {
                Log.w(TAG, "Android system TTS speak() failed with code $result")
                onSpeakingStateChanged(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Android system TTS error", e)
            onSpeakingStateChanged(false)
        }
    }

    fun stop() {
        ttsScope.coroutineContext.cancelChildren()
        neuralClient.stop()
        try {
            systemTts?.stop()
        } catch (_: Exception) {
        }
        notifySpeaking(false)
    }

    /**
     * Zero-latency cartoon sound effects on body touch events.
     */
    fun playTouchSound(species: PetSpecies, zone: TouchZone) {
        ExpressiveAudioSynthesizer.playTouchReactionSound(species, zone)
    }

    fun playHappyChime() {
        ExpressiveAudioSynthesizer.playHeadPetChime()
    }

    fun playPurrEffect() {
        ExpressiveAudioSynthesizer.playTouchReactionSound(PetSpecies.CAT, TouchZone.HEAD)
    }

    fun release() {
        try {
            stop()
            ttsScope.cancel()
            systemTts?.shutdown()
            systemTts = null
            systemTtsReady = false
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing Gemini Voice Engine", e)
        }
    }
}

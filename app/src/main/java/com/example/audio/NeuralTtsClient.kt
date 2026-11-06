package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.AppLanguage
import com.example.model.PetCharacter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.TimeUnit

enum class VoiceEngineMode(val displayName: String, val icon: String, val description: String) {
    GEMINI_FLASH_TTS(
        displayName = "Gemini AI Expressive Voice (100% AI)",
        icon = "🎙️",
        description = "Suara murni Gemini AI penuh ekspresi emosi sesuai sifat tiap karakter (Tanpa fallback TTS robotik)."
    ),
    CARTOON_MIMIC(
        displayName = "Gemini AI Expressive (Gaya Kartun)",
        icon = "🐱",
        description = "Suara Gemini AI dengan intonasi karakter animasi yang hidup dan ekspresif."
    ),
    GOOGLE_CLOUD_NEURAL(
        displayName = "Google Cloud Neural2 Voice",
        icon = "🌐",
        description = "Intonasi manusia alami dan ekspresif menggunakan Google Cloud Text-to-Speech Neural2."
    ),
    ELEVENLABS_NEURAL(
        displayName = "ElevenLabs Neural Voice",
        icon = "✨",
        description = "Suara AI hiper-realistis dengan emosi mendalam dan gaya ekspresi dinamis."
    )
}

class NeuralTtsClient(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) {
    private val TAG = "NeuralTtsClient"
    private var mediaPlayer: MediaPlayer? = null
    private var currentTempFile: File? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(22, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    // In-memory LRU audio cache so replaying or repeating greetings plays instantaneously
    private val audioCache = Collections.synchronizedMap(
        object : LinkedHashMap<String, Pair<ByteArray, String>>(24, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<ByteArray, String>>?): Boolean {
                return size > 20
            }
        }
    )

    fun stop() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                mp.release()
            }
            mediaPlayer = null
            deleteCurrentTempFile()
            onSpeakingStateChanged(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
            mediaPlayer = null
            deleteCurrentTempFile()
            onSpeakingStateChanged(false)
        }
    }

    private fun deleteCurrentTempFile() {
        currentTempFile?.let { f ->
            try {
                f.delete()
            } catch (_: Exception) {
            }
        }
        currentTempFile = null
    }

    private fun resolveEffectiveGeminiKey(providedKey: String): String {
        if (providedKey.isNotBlank()) return providedKey.trim()
        val prefs = context.getSharedPreferences("pawpal_ai_prefs", Context.MODE_PRIVATE)
        val savedCustom = prefs.getString("custom_gemini_api_key", "")?.trim().orEmpty()
        if (savedCustom.isNotBlank()) return savedCustom

        val buildConfigKey = try {
            val key = BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String
            key?.trim()?.takeIf {
                it.isNotBlank() &&
                    !it.contains("MY_GEMINI_API_KEY") &&
                    !it.contains("YOUR_API_KEY") &&
                    it != "null"
            }
        } catch (_: Exception) {
            null
        }
        return buildConfigKey.orEmpty()
    }

    /**
     * Synthesize and play expressive character speech using Gemini AI Native Audio / TTS.
     * Supports character-specific voice personas and emotional acting directions,
     * with automatic retry and zero robotic Android TTS fallback.
     */
    suspend fun speakWithGeminiTts(
        text: String,
        apiKey: String,
        character: PetCharacter,
        language: AppLanguage
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanText = sanitizeTextForSpeech(text)
        if (cleanText.isBlank()) return@withContext false

        val effectiveKey = resolveEffectiveGeminiKey(apiKey)
        if (effectiveKey.isBlank()) {
            Log.w(TAG, "Gemini API Key is not set yet; skipping Gemini TTS synthesis")
            return@withContext false
        }

        val voiceName = when (character.id) {
            "zayn" -> "Puck"     // Youthful, lively, expressive tsundere tone
            "siti" -> "Aoede"    // Warm, gentle, affectionate female voice
            "rizky" -> "Fenrir"  // Firm, disciplined, energetic voice
            "jamal" -> "Charon"  // Deep, relaxed, laid-back capybara voice
            else -> "Orus"       // Bold, resonant, expressive bear voice
        }

        val cacheKey = "${character.id}_${language.code}_${cleanText.hashCode()}"
        audioCache[cacheKey]?.let { (cachedBytes, cachedSuffix) ->
            stop()
            if (playAudioBytes(cachedBytes, cachedSuffix)) {
                return@withContext true
            }
        }

        val expressiveStylePrompt = buildExpressiveTtsPrompt(cleanText, character, language)

        // Prioritized Gemini Audio/TTS models. gemini-2.0-flash-exp was removed
        // from the API (HTTP 404), so it is no longer worth a round trip.
        val modelsToTry = listOf(
            "gemini-3.8-flash-tts",
            "gemini-3.1-flash-tts-preview",
            "gemini-2.5-flash-preview-tts"
        )

        // Free-tier TTS quota is tiny (~10 requests/model/day). When it is gone,
        // stop hammering the API and let SpeechTtsEngine use the system voice.
        val blockedForMs = remainingQuotaBlockMs()
        if (blockedForMs > 0) {
            Log.d(TAG, "Gemini TTS skipped: quota cooldown ${blockedForMs / 60000} min remaining")
            return@withContext false
        }

        // Up to 2 passes in case of transient network hiccup
        var quotaExhausted = false
        for (attempt in 1..2) {
            for (modelName in modelsToTry) {
                try {
                    val isDedicatedTtsModel = modelName.contains("tts", ignoreCase = true)
                    val audioResult = requestGeminiAudio(
                        modelName = modelName,
                        apiKey = effectiveKey,
                        voiceName = voiceName,
                        character = character,
                        rawText = cleanText,
                        expressivePrompt = expressiveStylePrompt,
                        isDedicatedTtsModel = isDedicatedTtsModel
                    )
                    if (audioResult != null) {
                        val (playableBytes, suffix) = audioResult
                        audioCache[cacheKey] = playableBytes to suffix
                        stop()
                        if (playAudioBytes(playableBytes, suffix)) {
                            Log.d(TAG, "Gemini AI Voice succeeded using $modelName ($voiceName)")
                            return@withContext true
                        }
                    }
                } catch (quota: GeminiTtsQuotaExceeded) {
                    Log.w(TAG, "Gemini TTS quota exhausted; switching to system voice")
                    quotaExhausted = true
                    break
                } catch (e: Exception) {
                    Log.d(TAG, "Gemini TTS ($modelName, attempt $attempt) error: ${e.message}")
                }
            }
            if (quotaExhausted) break
            if (attempt == 1) {
                delay(350L)
            }
        }

        return@withContext false
    }

    private class GeminiTtsQuotaExceeded : Exception()

    private fun remainingQuotaBlockMs(): Long {
        val prefs = context.getSharedPreferences("pawpal_ai_prefs", Context.MODE_PRIVATE)
        val until = prefs.getLong("gemini_tts_blocked_until", 0L)
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    private fun blockQuotaFor(ms: Long) {
        val prefs = context.getSharedPreferences("pawpal_ai_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("gemini_tts_blocked_until", System.currentTimeMillis() + ms)
            .apply()
    }

    /**
     * Reads the "Please retry in 18h30m18.1s" hint from a Gemini 429 body and
     * returns the cooldown to apply. Falls back to one hour when unparseable.
     */
    private fun parseQuotaCooldownMs(errorMessage: String): Long {
        val match = Regex("retry in (?:([0-9]+)h)?(?:([0-9]+)m)?", RegexOption.IGNORE_CASE).find(errorMessage)
        val hours = match?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val minutes = match?.groupValues?.getOrNull(2)?.toLongOrNull() ?: 0L
        val parsed = (hours * 60 + minutes) * 60_000L
        val cooldown = if (parsed > 0L) parsed else 60 * 60_000L
        // Never block longer than a day; the free-tier quota resets daily anyway.
        return cooldown.coerceIn(5 * 60_000L, 24 * 60 * 60_000L)
    }

    private fun requestGeminiAudio(
        modelName: String,
        apiKey: String,
        voiceName: String,
        character: PetCharacter,
        rawText: String,
        expressivePrompt: String,
        isDedicatedTtsModel: Boolean
    ): Pair<ByteArray, String>? {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val jsonBody = JSONObject().apply {
            if (!isDedicatedTtsModel) {
                // For multimodal models like gemini-2.0-flash-exp, use system_instruction so it reads the exact text with emotion
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put(
                            "text",
                            "Kamu adalah pengisi suara karakter 3D bernama ${character.name}. " +
                                "Tugasmu HANYA membacakan persis kalimat yang diberikan pengguna dengan ekspresi emosi, intonasi hidup, " +
                                "dan sifat ${character.name} tanpa menambah, mengurangi, atau menjawab kata-katanya."
                        ))
                    })
                })
            }

            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put(
                            "text",
                            if (isDedicatedTtsModel) expressivePrompt else "Bacakan persis kalimat ini dengan penuh ekspresi: \"$rawText\""
                        ))
                    })
                })
            })

            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().put("AUDIO"))
                put("speechConfig", JSONObject().apply {
                    put("voiceConfig", JSONObject().apply {
                        put("prebuiltVoiceConfig", JSONObject().apply {
                            put("voiceName", voiceName)
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = try {
                    response.body?.string().orEmpty()
                } catch (_: Exception) {
                    ""
                }
                Log.d(TAG, "Gemini TTS ($modelName) HTTP ${response.code}")

                if (response.code == 429) {
                    val message = try {
                        JSONObject(errorBody).optJSONObject("error")?.optString("message").orEmpty()
                    } catch (_: Exception) {
                        ""
                    }
                    val cooldown = parseQuotaCooldownMs(message)
                    blockQuotaFor(cooldown)
                    Log.w(
                        TAG,
                        "Gemini TTS quota exhausted (cooldown ${cooldown / 60_000} min): " +
                            message.take(160)
                    )
                    throw GeminiTtsQuotaExceeded()
                }

                if (response.code == 404) {
                    Log.d(TAG, "Gemini TTS model $modelName not available (removed/renamed)")
                }
                return null
            }

            val responseBody = response.body?.string() ?: return null
            val respJson = JSONObject(responseBody)
            val candidates = respJson.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val parts = candidates.getJSONObject(0)
                .optJSONObject("content")
                ?.optJSONArray("parts") ?: return null

            for (i in 0 until parts.length()) {
                val inlineData = parts.getJSONObject(i).optJSONObject("inlineData")
                if (inlineData != null) {
                    val mimeType = inlineData.optString("mimeType", "audio/pcm;rate=24000")
                    val dataBase64 = inlineData.optString("data", "")
                    if (dataBase64.isNotBlank()) {
                        val rawBytes = Base64.decode(dataBase64, Base64.DEFAULT)
                        val sampleRate = parseSampleRateFromMime(mimeType)
                        val playableBytes = if (mimeType.contains("pcm", ignoreCase = true) ||
                            mimeType.contains("L16", ignoreCase = true) ||
                            !isContainerFormat(rawBytes)
                        ) {
                            wrapPcmInWavHeader(rawBytes, sampleRate = sampleRate, channels = 1, bitsPerSample = 16)
                        } else {
                            rawBytes
                        }
                        val suffix = if (mimeType.contains("mp3", ignoreCase = true)) ".mp3" else ".wav"
                        return playableBytes to suffix
                    }
                }
            }
        }
        return null
    }

    /**
     * Builds an expressive voice-acting prompt tailored to each character's personality
     * for Gemini Native TTS (`gemini-2.5-flash-preview-tts`).
     */
    private fun buildExpressiveTtsPrompt(
        text: String,
        character: PetCharacter,
        language: AppLanguage
    ): String {
        val styleInstruction = when (character.id) {
            "zayn" -> "Ucapkan dalam bahasa ${language.displayName} dengan intonasi remaja yang sangat ekspresif, lincah, agak gengsi/tsundere tapi perhatian"
            "siti" -> "Ucapkan dalam bahasa ${language.displayName} dengan suara perempuan yang sangat lembut, hangat, manis, penuh kasih sayang, dan menenangkan"
            "rizky" -> "Ucapkan dalam bahasa ${language.displayName} dengan suara yang tegas, penuh semangat, sigap, antusias, dan berwibawa"
            "jamal" -> "Ucapkan dalam bahasa ${language.displayName} dengan suara yang santai, kalem, akrab, dan agak mengantuk khas sahabat yang santuy"
            else -> "Ucapkan dalam bahasa ${language.displayName} dengan suara beruang yang lantang, ekspresif, kocak, dan penuh energi"
        }
        return "$styleInstruction: \"$text\""
    }

    private fun sanitizeTextForSpeech(rawText: String): String {
        // Strip decorative markdown symbols while keeping punctuation for natural intonation
        return rawText
            .replace(Regex("[*_~`#>]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun parseSampleRateFromMime(mimeType: String): Int {
        val match = Regex("rate=(\\d+)", RegexOption.IGNORE_CASE).find(mimeType)
        val parsed = match?.groupValues?.getOrNull(1)?.toIntOrNull()
        return if (parsed != null && parsed in 8000..48000) parsed else 24000
    }

    private fun isContainerFormat(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val h0 = bytes[0].toInt() and 0xFF
        val h1 = bytes[1].toInt() and 0xFF
        val h2 = bytes[2].toInt() and 0xFF
        val h3 = bytes[3].toInt() and 0xFF
        if (h0 == 'R'.code && h1 == 'I'.code && h2 == 'F'.code && h3 == 'F'.code) return true
        if (h0 == 'I'.code && h1 == 'D'.code && h2 == '3'.code) return true
        if (h0 == 'O'.code && h1 == 'g'.code && h2 == 'g'.code && h3 == 'S'.code) return true
        if (h0 == 0xFF && (h1 and 0xE0) == 0xE0) return true
        return false
    }

    private fun wrapPcmInWavHeader(
        pcmData: ByteArray,
        sampleRate: Int = 24000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val dataSize = pcmData.size
        val chunkSize = 36 + dataSize

        val out = ByteArrayOutputStream(44 + dataSize)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(chunkSize)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1) // AudioFormat 1 = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign)
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataSize)

        out.write(header.array())
        out.write(pcmData)
        return out.toByteArray()
    }

    /**
     * Synthesize and play speech via Google Cloud Text-to-Speech (Neural2 / Journey).
     */
    suspend fun speakWithGoogleCloud(
        text: String,
        apiKey: String,
        character: PetCharacter,
        language: AppLanguage,
        pitch: Float,
        rate: Float
    ): Boolean = withContext(Dispatchers.IO) {
        val effectiveKey = resolveEffectiveGeminiKey(apiKey)
        if (effectiveKey.isBlank()) {
            Log.w(TAG, "Google Cloud TTS API Key is blank")
            return@withContext false
        }

        try {
            stop()

            val (langCode, voiceName) = getGoogleVoiceConfig(language, character)
            val pitchSemitones = ((pitch - 1.0f) * 8.0f).coerceIn(-12.0f, 12.0f)
            val speedRate = rate.coerceIn(0.7f, 1.6f)

            val jsonBody = JSONObject().apply {
                put("input", JSONObject().put("text", text))
                put("voice", JSONObject().apply {
                    put("languageCode", langCode)
                    put("name", voiceName)
                })
                put("audioConfig", JSONObject().apply {
                    put("audioEncoding", "MP3")
                    put("pitch", pitchSemitones)
                    put("speakingRate", speedRate)
                })
            }

            val request = Request.Builder()
                .url("https://texttospeech.googleapis.com/v1/text:synthesize?key=$effectiveKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Google Cloud TTS error ${response.code}: ${response.message}")
                    return@withContext false
                }

                val responseBody = response.body?.string() ?: return@withContext false
                val audioContentBase64 = JSONObject(responseBody).optString("audioContent")
                if (audioContentBase64.isBlank()) return@withContext false

                val audioBytes = Base64.decode(audioContentBase64, Base64.DEFAULT)
                return@withContext playAudioBytes(audioBytes, ".mp3")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Cloud TTS request failed", e)
            return@withContext false
        }
    }

    /**
     * Synthesize and play speech via ElevenLabs Neural Voice API.
     */
    suspend fun speakWithElevenLabs(
        text: String,
        apiKey: String,
        character: PetCharacter
    ): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            Log.w(TAG, "ElevenLabs API Key is blank")
            return@withContext false
        }

        try {
            stop()

            val voiceId = when (character.id) {
                "zayn" -> "TX3LPaxmHKxFdv7VOQHJ"
                "siti" -> "EXAVITQu4vr4xnSDxMaL"
                "rizky" -> "pNInz6obpgDQGcFmaJgB"
                "jamal" -> "onwK4e9ZLuTAKqWW03F9"
                else -> "VR6AewLTigWG4xSOukaG"
            }

            val jsonBody = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.40)
                    put("similarity_boost", 0.85)
                    put("style", 0.45)
                    put("use_speaker_boost", true)
                })
            }

            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
                .addHeader("xi-api-key", apiKey)
                .addHeader("Accept", "audio/mpeg")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "ElevenLabs error ${response.code}: ${response.message}")
                    return@withContext false
                }

                val audioBytes = response.body?.bytes() ?: return@withContext false
                return@withContext playAudioBytes(audioBytes, ".mp3")
            }
        } catch (e: Exception) {
            Log.e(TAG, "ElevenLabs request failed", e)
            return@withContext false
        }
    }

    private fun playAudioBytes(bytes: ByteArray, suffix: String = ".mp3"): Boolean {
        try {
            deleteCurrentTempFile()
            val tempFile = File.createTempFile("tts_audio_", suffix, context.cacheDir)
            currentTempFile = tempFile
            FileOutputStream(tempFile).use { fos ->
                fos.write(bytes)
            }

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                setOnPreparedListener { player ->
                    onSpeakingStateChanged(true)
                    player.start()
                }
                setOnCompletionListener { player ->
                    onSpeakingStateChanged(false)
                    try {
                        player.release()
                    } catch (_: Exception) {}
                    if (mediaPlayer === player) {
                        mediaPlayer = null
                    }
                    tempFile.delete()
                    if (currentTempFile === tempFile) currentTempFile = null
                }
                setOnErrorListener { player, _, _ ->
                    onSpeakingStateChanged(false)
                    try {
                        player.release()
                    } catch (_: Exception) {}
                    if (mediaPlayer === player) {
                        mediaPlayer = null
                    }
                    tempFile.delete()
                    if (currentTempFile === tempFile) currentTempFile = null
                    false
                }
                prepareAsync()
            }
            mediaPlayer = mp
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio bytes", e)
            onSpeakingStateChanged(false)
            return false
        }
    }

    private fun getGoogleVoiceConfig(language: AppLanguage, character: PetCharacter): Pair<String, String> {
        return when (language) {
            AppLanguage.INDONESIAN -> {
                val voiceName = if (character.id == "siti") "id-ID-Neural2-A" else "id-ID-Neural2-B"
                "id-ID" to voiceName
            }
            AppLanguage.ENGLISH -> {
                val voiceName = if (character.id == "siti") "en-US-Journey-F" else "en-US-Journey-D"
                "en-US" to voiceName
            }
            AppLanguage.JAPANESE -> {
                val voiceName = if (character.id == "siti") "ja-JP-Neural2-B" else "ja-JP-Neural2-C"
                "ja-JP" to voiceName
            }
            AppLanguage.SPANISH -> {
                val voiceName = if (character.id == "siti") "es-ES-Neural2-A" else "es-ES-Neural2-B"
                "es-ES" to voiceName
            }
        }
    }
}

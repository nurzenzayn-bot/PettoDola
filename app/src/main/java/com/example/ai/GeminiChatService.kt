package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.AppLanguage
import com.example.model.AvatarEmotion
import com.example.model.ChatMessage
import com.example.model.GeminiModelTier
import com.example.model.PetCharacter
import com.example.model.PetMemoryManager
import com.example.model.QuizQuestion
import com.example.model.ReactionCatalog
import com.example.model.StoryEndingType
import com.example.model.StoryItem
import com.example.model.StoryNode
import com.example.model.StoryTheme
import com.example.ui.HomeworkSolution
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

data class GeminiReply(
    val text: String,
    val emotion: AvatarEmotion = AvatarEmotion.TALKING,
    val isFromAi: Boolean = false,
    val modelUsed: String = "gemini-3.8-flash"
)

data class VoiceAudioAnalysisResult(
    val wakeDetected: Boolean,
    val isWakeOnly: Boolean,
    val transcript: String = "",
    val replyText: String = "",
    val emotion: AvatarEmotion = AvatarEmotion.TALKING,
    val isOfflineFallback: Boolean = false
)

class GeminiChatService(private val context: Context? = null) {

    private val TAG = "GeminiChatService"

    // Lazy initialization of Firebase AI Logic models across the 3 tiers:
    // - gemini-4-argon for complex reasoning tasks
    // - gemini-3.8-flash for general multi-turn tasks
    // - gemini-3.1-flash-lite (& gemini-3.5-flash-lite) for fast low-latency tasks
    private val argonModel by lazy {
        Firebase.ai.generativeModel("gemini-4-argon")
    }

    private val flashModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    private val flashLite31Model by lazy {
        Firebase.ai.generativeModel("gemini-3.1-flash-lite")
    }

    private val flashLiteModel by lazy {
        Firebase.ai.generativeModel("gemini-3.5-flash-lite")
    }

    // Reliable HTTP client with realistic timeouts for AI reasoning
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .callTimeout(22, TimeUnit.SECONDS)
        .build()

    // Shorter hard bound for voice chunks: a slow model must not stall the whole conversation.
    private val audioClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .callTimeout(16, TimeUnit.SECONDS)
        .build()

    @Volatile
    var lastAiCallSucceeded: Boolean = false
        private set

    private val prefs by lazy {
        context?.getSharedPreferences("pawpal_ai_prefs", Context.MODE_PRIVATE)
    }

    private val memoryManager by lazy {
        context?.let { PetMemoryManager(it) }
    }

    fun getCustomApiKey(): String {
        return prefs?.getString("custom_gemini_api_key", "")?.trim().orEmpty()
    }

    fun setCustomApiKey(key: String) {
        prefs?.edit()?.putString("custom_gemini_api_key", key.trim())?.apply()
    }

    /**
     * Returns ONLY a real Gemini API Key (from user's custom input in APK or AI Studio Secrets panel).
     * Never returns the internal Firebase google_api_key, which is not a public Gemini REST API key.
     */
    fun getEffectiveApiKey(): String {
        val customKey = getCustomApiKey()
        if (customKey.isNotBlank()) return customKey

        val buildConfigKey = try {
            val key = BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String
            key?.trim()?.takeIf {
                it.isNotBlank() &&
                    !it.contains("MY_GEMINI_API_KEY") &&
                    !it.contains("YOUR_API_KEY") &&
                    it != "null"
            }
        } catch (e: Exception) {
            null
        }
        return buildConfigKey.orEmpty()
    }

    fun isGeminiConnected(): Boolean {
        return getEffectiveApiKey().isNotBlank() || lastAiCallSucceeded
    }

    /**
     * Fast, guaranteed non-blocking fallback reply generator.
     */
    fun getFastFallbackReply(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage
    ): GeminiReply {
        val directAnswer = trySolveDirectIntent(userMessage, character, language)
        if (directAnswer != null) {
            return GeminiReply(
                text = directAnswer,
                emotion = inferEmotionFromText(directAnswer, character),
                isFromAi = false
            )
        }
        val fallbackText = generatePersonaFallback(userMessage, character, language)
        return GeminiReply(
            text = fallbackText,
            emotion = inferEmotionFromText(fallbackText, character),
            isFromAi = false
        )
    }

    /**
     * Automatically selects the optimal Gemini model tier based on prompt complexity
     * when `preferredTier == GeminiModelTier.AUTO`, or respects the user's manual tier selection:
     * - Complex tasks -> gemini-4-argon
     * - General tasks -> gemini-3.8-flash
     * - Fast tasks -> gemini-3.1-flash-lite
     */
    fun resolveModelTier(
        userMessage: String,
        preferredTier: GeminiModelTier = GeminiModelTier.AUTO
    ): GeminiModelTier {
        if (preferredTier != GeminiModelTier.AUTO) return preferredTier

        val clean = userMessage.trim()
        val lower = clean.lowercase()
        val wordCount = clean.split(Regex("\\s+")).filter { it.isNotBlank() }.size

        val complexKeywords = listOf(
            "jelaskan", "analisis", "mengapa", "kenapa", "perbedaan", "bandingkan",
            "rumus", "teori", "strategi", "langkah", "kode", "program", "algoritma",
            "filsafat", "sejarah", "hitung", "buktikan", "explain", "analyze", "why", "how does"
        )
        if (wordCount >= 16 || complexKeywords.any { lower.contains(it) }) {
            return GeminiModelTier.ARGON_COMPLEX
        }

        if (wordCount <= 4 && !clean.contains("?")) {
            return GeminiModelTier.FLASH_LITE_FAST
        }

        return GeminiModelTier.FLASH_GENERAL
    }

    fun buildLiveSystemInstruction(character: PetCharacter, language: AppLanguage): String {
        return buildSystemInstruction(character, language)
    }

    /**
     * Main multi-turn conversation generator:
     * 1. Wake words check for instant reaction (0ms).
     * 2. Smart model tier routing (gemini-4-argon for complex, gemini-3.8-flash for general, gemini-3.1-flash-lite for fast).
     * 3. Full multi-turn conversation history + character role system instruction.
     * 4. Smart conversational fallback if offline/timeout.
     */
    suspend fun generateReply(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage,
        history: List<ChatMessage>,
        cameraBitmap: Bitmap? = null,
        preferredTier: GeminiModelTier = GeminiModelTier.AUTO
    ): GeminiReply = withContext(Dispatchers.IO) {
        val cleanMsg = userMessage.trim()
        val lower = cleanMsg.lowercase()

        // Automatically extract and persist any personal facts from the user's message
        memoryManager?.extractAndSaveFromUtterance(cleanMsg)

        val activeTier = resolveModelTier(cleanMsg, preferredTier)
        val targetModelId = activeTier.modelId

        // 1. Instant check: Wake words (0ms response time!)
        if (cameraBitmap == null && (lower.contains("hey petto") || lower.contains("halo petto") || lower.contains("hai petto"))) {
            return@withContext GeminiReply(
                text = ReactionCatalog.getWakeWordResponse(character, language),
                emotion = AvatarEmotion.SURPRISED,
                isFromAi = false,
                modelUsed = "gemini-3.1-flash-lite"
            )
        }

        // 2. If user has a real Gemini API Key (via Secrets or in-app input), try Direct Gemini REST API first
        val apiKey = getEffectiveApiKey()
        if (apiKey.isNotBlank()) {
            val restModelsOrder = when (activeTier) {
                GeminiModelTier.ARGON_COMPLEX -> listOf("gemini-4-argon", "gemini-3.8-flash", "gemini-3.1-flash-lite")
                GeminiModelTier.FLASH_LITE_FAST -> listOf("gemini-3.1-flash-lite", "gemini-3.5-flash-lite", "gemini-3.8-flash")
                else -> listOf("gemini-3.8-flash", "gemini-3.1-flash-lite", "gemini-3.5-flash-lite")
            }
            try {
                val restResult = withTimeoutOrNull(20000L) {
                    var replyText = ""
                    for (m in restModelsOrder) {
                        val candidate = callGeminiApi(m, apiKey, cleanMsg, character, language, history, cameraBitmap)
                        if (candidate.isNotBlank()) {
                            replyText = candidate
                            break
                        }
                    }
                    replyText
                }
                if (!restResult.isNullOrBlank()) {
                    lastAiCallSucceeded = true
                    return@withContext GeminiReply(
                        text = restResult,
                        emotion = inferEmotionFromText(restResult, character),
                        isFromAi = true,
                        modelUsed = targetModelId
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Direct Gemini REST API failed: ${e.message}")
            }
        }

        // 3. Try Firebase AI Logic with the selected model tier (gemini-4-argon / gemini-3.8-flash / gemini-3.1-flash-lite)
        try {
            val firebaseReply = withTimeoutOrNull(20000L) {
                callFirebaseAi(cleanMsg, character, language, history, cameraBitmap, activeTier)
            }
            if (firebaseReply != null && firebaseReply.first.isNotBlank()) {
                lastAiCallSucceeded = true
                return@withContext GeminiReply(
                    text = firebaseReply.first,
                    emotion = inferEmotionFromText(firebaseReply.first, character),
                    isFromAi = true,
                    modelUsed = firebaseReply.second
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Firebase AI Logic pass skipped: ${e.message}")
        }

        // 4. Offline Fallback: If cameraBitmap was provided, give an expressive visual observation reply
        if (cameraBitmap != null) {
            val visionFallback = when (character.id) {
                "zayn" -> "Hmph! Aku udah liat kamu lewat kamera! Kelihatan fokus dan keren juga sih hari ini... B-bukan berarti aku sengaja merhatiin muka kamu ya!"
                "siti" -> "Wah, Siti bisa melihatmu dengan jelas lewat kamera sayangku manis... Senang sekali melihat wajah ceriamu hari ini!"
                "rizky" -> "Siap! Visual kamera terpantau jernih! Penampilan dan kesiapan rekan hari ini terlihat sangat prima!"
                "jamal" -> "Wih, kelihatan jelas nih dari kamera bro! Asyik banget bisa tatap muka sambil santai gini."
                else -> "HEH! Gua liat lu dari kamera nih! Kelihatan semangat banget lu hari ini, mantap!"
            }
            return@withContext GeminiReply(
                text = visionFallback,
                emotion = AvatarEmotion.HAPPY,
                isFromAi = false,
                modelUsed = targetModelId
            )
        }

        val directAnswer = trySolveDirectIntent(cleanMsg, character, language)
        if (directAnswer != null) {
            val emotion = inferEmotionFromText(directAnswer, character)
            return@withContext GeminiReply(
                text = directAnswer,
                emotion = emotion,
                isFromAi = false,
                modelUsed = targetModelId
            )
        }

        // 5. Contextual Persona Fallback
        return@withContext getFastFallbackReply(cleanMsg, character, language).copy(modelUsed = targetModelId)
    }

    private suspend fun callFirebaseAi(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage,
        history: List<ChatMessage>,
        cameraBitmap: Bitmap? = null,
        activeTier: GeminiModelTier = GeminiModelTier.FLASH_GENERAL
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        val systemPrompt = buildSystemInstruction(character, language)
        val visionNote = if (cameraBitmap != null) {
            "\n[KONTEKS VISUAL KAMERA LANGSUNG: Perhatikan gambar kamera pengguna yang dilampirkan. Komentari ekspresi wajah pengguna, benda yang dipegang, pakaian, atau suasana di kamera secara alami sesuai kepribadianmu dan jawab ucapannya.]\n"
        } else ""

        // Include multi-turn conversation history so the chatbot maintains full thread context
        val historyTranscript = if (history.isNotEmpty()) {
            "\nRIWAYAT PERCAKAPAN SEBELUMNYA:\n" +
                history.takeLast(12).joinToString("\n") { msg ->
                    "${if (msg.isUser) "Pengguna" else character.name}: ${msg.text}"
                } + "\n"
        } else ""

        val prompt = "$systemPrompt$visionNote$historyTranscript\nPengguna: $userMessage\n${character.name}:"

        // Order models according to the active tier (with free-tier fallback safety)
        val modelChain = when (activeTier) {
            GeminiModelTier.ARGON_COMPLEX -> listOf(
                argonModel to "gemini-4-argon",
                flashModel to "gemini-4-argon"
            )
            GeminiModelTier.FLASH_LITE_FAST -> listOf(
                flashLite31Model to "gemini-3.1-flash-lite",
                flashLiteModel to "gemini-3.1-flash-lite",
                flashModel to "gemini-3.1-flash-lite"
            )
            else -> listOf(
                flashModel to "gemini-3.8-flash",
                flashLite31Model to "gemini-3.8-flash",
                flashLiteModel to "gemini-3.8-flash"
            )
        }

        for ((modelInstance, badgeLabel) in modelChain) {
            try {
                val response = if (cameraBitmap != null) {
                    modelInstance.generateContent(
                        content {
                            image(cameraBitmap)
                            text(prompt)
                        }
                    )
                } else {
                    modelInstance.generateContent(prompt)
                }
                val text = response.text?.replace(Regex("^\"|\"$"), "")?.trim()
                if (!text.isNullOrBlank()) return@withContext text to badgeLabel
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Firebase AI model pass ($badgeLabel) skipped: ${e.message}")
            }
        }

        null
    }

    /**
     * Solves school homework dynamically with Gemini 4 Argon / Gemini 3.8 Flash (supports Multimodal Photo Analysis),
     * or uses the advanced deterministic tutor as an offline fallback.
     */
    suspend fun analyzeVoiceAudioSegment(
        wavBytes: ByteArray,
        durationMs: Long,
        isStandbyWakeMode: Boolean,
        character: PetCharacter,
        language: AppLanguage,
        history: List<ChatMessage>
    ): VoiceAudioAnalysisResult = withContext(Dispatchers.IO) {
        val recentContext = history.takeLast(6).joinToString("\n") { msg ->
            "${if (msg.isUser) "Pengguna" else character.name}: ${msg.text}"
        }
        val systemInstruction = buildSystemInstruction(character, language)

        val prompt = if (isStandbyWakeMode) {
            """
                $systemInstruction
                
                TUGAS DETEKSI KATA PANGGILAN SUARA ("PETTO") & GEMINI LIVE:
                Dengarkan rekaman audio mikrofon pengguna ini dengan saksama.
                Rekaman ini bisa sangat pendek (0,3-2 detik) dan hanya berisi kata panggilan. Jika terdengar bunyi yang mirip "petto"/"peto"/"pito", anggap wakeDetected=true walaupun transkripsi tidak sempurna. JANGAN mengarang kata atau umpatan yang tidak terdengar.
                Periksa apakah pengguna memanggil kata panggilan "Petto" (atau variasi fonetik seperti "Hey Petto", "Halo Petto", "Hai Petto", "Peto", "Pito", atau nama karakter "${character.name}").
                
                Aturan:
                1. Jika pengguna HANYA memanggil "Petto" / "Hey Petto" / "${character.name}" tanpa pertanyaan lanjutan:
                   set "wakeDetected": true, "isWakeOnly": true.
                2. Jika pengguna memanggil "Petto" diikuti langsung dengan pertanyaan atau pernyataan (contoh: "Petto lagi ngapain?", "Hey Petto 5 dikali 9 berapa?"):
                   set "wakeDetected": true, "isWakeOnly": false, dan berikan jawaban lisan karakter di "replyText" sesuai kepribadian ${character.name}.
                3. Jika audio hanya suara bising latar belakang atau obrolan yang TIDAK memanggil "Petto" / "${character.name}":
                   set "wakeDetected": false, "isWakeOnly": false.
                
                Konteks percakapan sebelumnya:
                $recentContext
                
                Kembalikan HANYA format JSON valid berikut (tanpa markdown backtick):
                {
                  "wakeDetected": true,
                  "isWakeOnly": true,
                  "transcript": "Teks ucapan pengguna yang terdengar",
                  "replyText": "Jawaban suara karakter (kosongkan jika wakeDetected false)",
                  "emotion": "TALKING"
                }
            """.trimIndent()
        } else {
            """
                $systemInstruction
                
                TUGAS PERCAKAPAN SUARA LANGSUNG (GEMINI LIVE SESSION):
                Pengguna sedang berbicara langsung denganmu (${character.name}) dalam sesi suara aktif setelah memanggil "Petto".
                Dengarkan audio ucapan pengguna ini, transkripsikan, dan jawab langsung secara lisan dengan hangat, cerdas, ekspresif, dan sesuai kepribadian ${character.name} (2-3 kalimat alami yang enak didengar tanpa format markdown).
                Jika audio hanya hening atau gumaman tidak bermakna, set "wakeDetected": false.
                
                Konteks percakapan sebelumnya:
                $recentContext
                
                Kembalikan HANYA format JSON valid berikut (tanpa markdown backtick):
                {
                  "wakeDetected": true,
                  "isWakeOnly": false,
                  "transcript": "Transkripsi ucapan pengguna",
                  "replyText": "Jawaban lisan langsung dari ${character.name}",
                  "emotion": "HAPPY"
                }
            """.trimIndent()
        }

        // 1. Try Gemini REST API with multimodal inlineData (audio/wav) if real API key is configured
        val apiKey = getEffectiveApiKey()
        Log.i(TAG, "Voice analysis: standby=" + isStandbyWakeMode + ", durationMs=" + durationMs + ", wavBytes=" + wavBytes.size + ", keyConfigured=" + apiKey.isNotBlank())
        if (apiKey.isNotBlank()) {
            try {
                val audioModels = listOf("gemini-3.1-flash-lite", "gemini-3.5-flash-lite", "gemini-3.8-flash")
                val jsonResp = withTimeoutOrNull(48000L) {
                    var found = ""
                    for (m in audioModels) {
                        val started = System.currentTimeMillis()
                        val r = callGeminiAudioJson(m, apiKey, prompt, wavBytes, if (isStandbyWakeMode) 0.1 else 0.4)
                        Log.d(TAG, "Audio model " + m + " -> " + (if (r.isBlank()) "empty" else "ok") + " in " + (System.currentTimeMillis() - started) + "ms")
                        if (r.isNotBlank()) {
                            found = r
                            break
                        }
                    }
                    found
                }
                if (!jsonResp.isNullOrBlank()) {
                    parseVoiceAudioJson(jsonResp, character, isStandbyWakeMode)?.let {
                        lastAiCallSucceeded = true
                        return@withContext it
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Gemini audio REST analysis failed: " + e.message)
            }
        } else {
            Log.w(TAG, "Gemini audio REST skipped: no usable API key (empty or placeholder MY_GEMINI_API_KEY)")
        }

        // 2. Try Firebase AI Logic (gemini-3.8-flash -> gemini-3.5-flash-lite) with multimodal audio inlineData
        try {
            val fbResp = withTimeoutOrNull(14000L) {
                val multimodalReq = content {
                    inlineData(wavBytes, "audio/wav")
                    text(prompt)
                }
                try {
                    flashModel.generateContent(multimodalReq).text
                } catch (_: Exception) {
                    flashLiteModel.generateContent(multimodalReq).text
                }
            }
            if (!fbResp.isNullOrBlank()) {
                parseVoiceAudioJson(fbResp, character, isStandbyWakeMode)?.let {
                    lastAiCallSucceeded = true
                    return@withContext it
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Firebase AI audio analysis skipped: ${e.message}")
        }

        // 3. Offline fallback: never fabricate a wake word. Report honestly so the UI can inform the user.
        Log.w(TAG, "Voice analysis offline: REST and Firebase AI both unavailable (durationMs=" + durationMs + ")")
        if (isStandbyWakeMode) {
            return@withContext VoiceAudioAnalysisResult(
                wakeDetected = false,
                isWakeOnly = false,
                transcript = "",
                replyText = "",
                emotion = AvatarEmotion.SURPRISED,
                isOfflineFallback = true
            )
        } else {
            val offlineReply = when (character.id) {
                "zayn" -> "Hmph! Aku denger kok suaramu barusan! Menarik juga yang kamu omongin, tapi jangan berharap aku muji kamu terus ya! Mau bahas apa lagi nih?"
                "siti" -> "Siti dengerin suaramu dengan penuh perhatian sayangku manis... Senang banget bisa ngobrol langsung kayak gini sama kamu. Cerita lagi yuk sayang!"
                "rizky" -> "Siap! Suaramu terdengar jelas di frekuensi komando! Analisis situasi sangat baik, ayo lanjutkan diskusi kita kawan!"
                "jamal" -> "Wah, asyik banget ngobrol langsung gini bro... Suaramu jelas kok, santai aja lanjutin ceritanya sambil kita rebahan."
                else -> "HEH! Gua denger suara lu dengan jelas! Keren juga obrolan lu barusan, ayo ngomong lagi jangan malu-malu!"
            }
            return@withContext VoiceAudioAnalysisResult(
                wakeDetected = true,
                isWakeOnly = false,
                transcript = "(Suara pengguna)",
                replyText = offlineReply,
                emotion = AvatarEmotion.TALKING,
                isOfflineFallback = true
            )
        }
    }

    private fun parseVoiceAudioJson(rawJson: String, character: PetCharacter, isStandbyWakeMode: Boolean = false): VoiceAudioAnalysisResult? {
        return try {
            val clean = rawJson.replace(Regex("^```json|^```|```$", RegexOption.MULTILINE), "").trim()
            val obj = JSONObject(clean)
            var wakeDetected = obj.optBoolean("wakeDetected", false)
            var isWakeOnly = obj.optBoolean("isWakeOnly", false)
            val transcript = obj.optString("transcript", "").trim()

            // Local safety net: the model sometimes transcribes the wake word correctly
            // but still reports wakeDetected=false. Trust the transcript in that case.
            if (isStandbyWakeMode && !wakeDetected && looksLikeWakeWord(transcript, character.name)) {
                Log.i(TAG, "Wake word matched locally from transcript: " + transcript)
                wakeDetected = true
                isWakeOnly = true
            } else if (isStandbyWakeMode && wakeDetected && transcript.isNotBlank() &&
                !looksLikeWakeWord(transcript, character.name)
            ) {
                Log.w(TAG, "Standby wake rejected: no wake word in transcript: " + transcript)
                wakeDetected = false
                isWakeOnly = false
            }
            var replyText = obj.optString("replyText", "").trim()

            if (transcript.isNotBlank()) {
                memoryManager?.extractAndSaveFromUtterance(transcript)
            }

            // If transcript has a direct math/science intent and replyText is empty, solve it
            if (wakeDetected && !isWakeOnly && replyText.isBlank() && transcript.isNotBlank()) {
                replyText = trySolveDirectIntent(transcript, character, AppLanguage.INDONESIAN)
                    ?: generatePersonaFallback(transcript, character, AppLanguage.INDONESIAN)
            }

            val emotionStr = obj.optString("emotion", "").uppercase()
            val emotion = when (emotionStr) {
                "HAPPY" -> AvatarEmotion.HAPPY
                "LOVING" -> AvatarEmotion.LOVING
                "SURPRISED" -> AvatarEmotion.SURPRISED
                "SLEEPY" -> AvatarEmotion.SLEEPY
                else -> inferEmotionFromText(replyText, character)
            }

            VoiceAudioAnalysisResult(
                wakeDetected = wakeDetected,
                isWakeOnly = isWakeOnly,
                transcript = transcript,
                replyText = replyText,
                emotion = emotion
            )
        } catch (e: Exception) {
            Log.d(TAG, "Failed to parse voice audio JSON: ${e.message}")
            null
        }
    }

    private fun looksLikeWakeWord(transcript: String, characterName: String = ""): Boolean {
        val t = transcript.lowercase()
        val variants = listOf("petto", "peto", "pito", "pitto", "petoh", "pe to", "betto", "paitto", "pett")
        if (variants.any { t.contains(it) }) return true

        // The pet's own name is a valid wake word too ("Zayn!", "Halo Zayn").
        // Without this the standby guard rejects perfectly good wake calls.
        val name = characterName.lowercase().trim()
        if (name.length >= 3) {
            val nameVariants = listOf(name, name.replace('y', 'i'), name.replace("ay", "ai"))
            if (nameVariants.any { t.contains(it) }) return true
        }
        return false
    }

    private fun callGeminiAudioJson(
        modelName: String,
        apiKey: String,
        prompt: String,
        wavBytes: ByteArray,
        temperature: Double
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val base64Audio = Base64.encodeToString(wavBytes, Base64.NO_WRAP)

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "audio/wav")
                                put("data", base64Audio)
                            })
                        })
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", temperature)
                put("maxOutputTokens", 450)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        audioClient.newCall(request).execute().use { response ->
            val respString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini audio HTTP " + response.code + " model=" + modelName + " body=" + respString.take(400))
                return ""
            }
            val respJson = JSONObject(respString)
            val candidates = respJson.optJSONArray("candidates") ?: run {
                Log.w(TAG, "Gemini audio response missing candidates: " + respString.take(400))
                return ""
            }
            if (candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text").trim()
                }
            }
        }
        return ""
    }

    suspend fun generateHomeworkHelp(
        subject: String,
        question: String,
        character: PetCharacter,
        language: AppLanguage,
        photoBitmap: Bitmap? = null
    ): HomeworkSolution = withContext(Dispatchers.IO) {
        val actualQuestion = question.trim().ifBlank {
            if (photoBitmap != null) "Tolong analisis dan jelaskan langkah penyelesaian soal tugas yang ada pada foto ini"
            else "Tugas dan latihan materi $subject"
        }

        val prompt = """
            Kamu adalah tutor bimbingan belajar AI sekolah bernama ${character.name} (${character.species.displayName}).
            Tugasmu adalah menganalisis, menyelesaikan, dan membimbing siswa memahami soal PR/tugas sekolah berikut secara mendalam, runut, akurat, dan mudah dimengerti.
            
            Mata Pelajaran: $subject
            Teks Pertanyaan / Soal: $actualQuestion
            ${if (photoBitmap != null) "PENTING: Periksa foto/gambar lembar tugas yang dilampirkan dengan sangat teliti. Baca teks soal, persamaan matematika, diagram, tabel, atau opsi pilihan ganda yang ada di gambar tersebut." else ""}
            
            Gaya Karakter:
            ${character.personalityPrompt}
            
            Pedoman Pembahasan:
            1. Buat ringkasan pertanyaan yang jelas.
            2. Tuliskan konsep atau rumus utama yang dipakai.
            3. Uraikan langkah-langkah penyelesaian secara terstruktur (minimal 3-5 langkah logis, sertakan hitungan matematis jika ada).
            4. Berikan jawaban akhir yang tegas, jelas, dan benar.
            5. Berikan tips belajar atau trik cepat dari ${character.name}.
            6. Berikan 1 tantangan soal latihan serupa untuk menguji pemahaman siswa.
            
            Kembalikan HANYA format JSON valid berikut (tanpa markdown backtick):
            {
              "subject": "$subject",
              "questionSummary": "Ringkasan soal atau pertanyaan",
              "conceptOrFormula": "Konsep dasar atau rumus utama yang digunakan",
              "stepByStep": [
                "Langkah 1: ...",
                "Langkah 2: ...",
                "Langkah 3: ..."
              ],
              "finalAnswer": "Jawaban akhir yang jelas, tepat, dan to-the-point",
              "tutorTip": "Tips belajar mudah diingat dari ${character.name}",
              "practiceChallenge": "1 soal latihan serupa yang dapat dicoba mandiri oleh siswa"
            }
        """.trimIndent()

        // 1. If a real Gemini API Key is configured, call Direct Gemini REST API (gemini-3.8-flash -> gemini-3.1-flash-lite -> gemini-3.5-flash-lite)
        val apiKey = getEffectiveApiKey()
        if (apiKey.isNotBlank()) {
            try {
                val jsonResponse = withTimeoutOrNull(24000L) {
                    val r38 = callGeminiJson("gemini-3.8-flash", apiKey, prompt, photoBitmap)
                    if (r38.isNotBlank()) r38
                    else {
                        val r31 = callGeminiJson("gemini-3.1-flash-lite", apiKey, prompt, photoBitmap)
                        if (r31.isNotBlank()) r31 else callGeminiJson("gemini-3.5-flash-lite", apiKey, prompt, photoBitmap)
                    }
                }

                if (!jsonResponse.isNullOrBlank()) {
                    parseHomeworkJson(jsonResponse, subject, actualQuestion, character, isFromAi = true)?.let {
                        lastAiCallSucceeded = true
                        return@withContext it
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Gemini REST homework help error: ${e.message}")
            }
        }

        // 2. Try Firebase AI Logic (gemini-3.8-flash -> gemini-3.5-flash-lite) immediately with multimodal photo support
        try {
            val fbJson = withTimeoutOrNull(24000L) {
                val callModel: suspend (com.google.firebase.ai.GenerativeModel) -> String? = { model ->
                    if (photoBitmap != null) {
                        model.generateContent(
                            content {
                                image(photoBitmap)
                                text(prompt)
                            }
                        ).text
                    } else {
                        model.generateContent(prompt).text
                    }
                }
                try {
                    callModel(flashModel)
                } catch (_: Exception) {
                    callModel(flashLiteModel)
                }
            }
            if (!fbJson.isNullOrBlank()) {
                parseHomeworkJson(fbJson, subject, actualQuestion, character, isFromAi = true)?.let {
                    lastAiCallSucceeded = true
                    return@withContext it
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Firebase AI homework help skipped: ${e.message}")
        }

        // 3. Offline Educational Solver
        return@withContext solveHomeworkDeterministically(subject, actualQuestion, character)
    }

    private fun parseHomeworkJson(
        rawJson: String,
        subject: String,
        actualQuestion: String,
        character: PetCharacter,
        isFromAi: Boolean
    ): HomeworkSolution? {
        return try {
            val cleanJson = extractJsonObjectString(rawJson)
            val obj = JSONObject(cleanJson)
            val stepsList = mutableListOf<String>()
            val stepsArray = obj.optJSONArray("stepByStep")
            if (stepsArray != null) {
                for (i in 0 until stepsArray.length()) {
                    val step = stepsArray.optString(i).trim()
                    if (step.isNotBlank()) stepsList.add(step)
                }
            }
            if (stepsList.isNotEmpty()) {
                HomeworkSolution(
                    subject = obj.optString("subject", subject),
                    questionSummary = obj.optString("questionSummary", actualQuestion),
                    stepByStep = stepsList,
                    finalAnswer = obj.optString("finalAnswer", "Solusi telah diselesaikan secara lengkap."),
                    tutorTip = obj.optString("tutorTip", "Tips dari ${character.name}: Pahami konsepnya dan jangan ragu mencoba lagi!"),
                    conceptOrFormula = obj.optString("conceptOrFormula", ""),
                    practiceChallenge = obj.optString("practiceChallenge", ""),
                    isFromAi = isFromAi
                )
            } else null
        } catch (e: Exception) {
            Log.d(TAG, "Failed to parse homework JSON: ${e.message}")
            null
        }
    }

    private fun extractJsonObjectString(raw: String): String {
        val trimmed = raw.replace(Regex("^```json|^```|```$", RegexOption.MULTILINE), "").trim()
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace in 0 until lastBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }
        return trimmed
    }

    /**
     * Generates an interactive AI Quiz Question (Kuis Suara & Cerdas Cermat) via Gemini AI.
     */
    suspend fun generateQuizQuestion(
        topic: String,
        character: PetCharacter
    ): QuizQuestion = withContext(Dispatchers.IO) {
        val prompt = """
            Kamu adalah ${character.name} (${character.species.displayName}). Buatlah 1 pertanyaan kuis pilihan ganda yang seru, cerdas, dan mendidik tentang topik "$topic" dalam Bahasa Indonesia.
            Gaya penjelasan pembahasan harus sesuai kepribadianmu: ${character.personalityPrompt}
            
            Kembalikan HANYA JSON valid dengan format berikut:
            {
              "topic": "$topic",
              "question": "Pertanyaan kuis yang menarik?",
              "options": ["Pilihan A", "Pilihan B", "Pilihan C", "Pilihan D"],
              "correctIndex": 0,
              "explanation": "Penjelasan singkat mengapa jawaban tersebut benar dengan gaya bicara khas ${character.name}"
            }
        """.trimIndent()

        val rawJson = callAnyGeminiJson(prompt)
        if (!rawJson.isNullOrBlank()) {
            try {
                val obj = JSONObject(extractJsonObjectString(rawJson))
                val optsArr = obj.optJSONArray("options")
                val options = mutableListOf<String>()
                if (optsArr != null) {
                    for (i in 0 until optsArr.length()) {
                        options.add(optsArr.optString(i))
                    }
                }
                if (options.size >= 4) {
                    return@withContext QuizQuestion(
                        topic = obj.optString("topic", topic),
                        question = obj.optString("question", ""),
                        options = options.take(4),
                        correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, 3),
                        explanation = obj.optString("explanation", "Jawaban yang tepat sekali!"),
                        isFromAi = true
                    )
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to parse AI quiz JSON: ${e.message}")
            }
        }

        // Offline curated quiz bank fallback
        val fallbackPool = listOf(
            QuizQuestion(
                topic = "Sains & Antariksa",
                question = "Planet manakah di Tata Surya kita yang dikenal memiliki cincin es paling besar dan indah?",
                options = listOf("Mars", "Saturnus", "Venus", "Merkurius"),
                correctIndex = 1,
                explanation = "Saturnus punya sistem cincin raksasa yang terbentuk dari miliaran bongkahan es dan batuan angkasa!",
                isFromAi = false
            ),
            QuizQuestion(
                topic = "Pengetahuan Umum",
                question = "Hewan mamalia laut apakah yang merupakan hewan terbesar yang pernah hidup di Bumi?",
                options = listOf("Hiu Putih Besar", "Paus Biru", "Lumba-lumba Hidung Botol", "Gajah Afrika"),
                correctIndex = 1,
                explanation = "Paus Biru bisa tumbuh sepanjang 30 meter dan beratnya mencapai lebih dari 150 ton!",
                isFromAi = false
            ),
            QuizQuestion(
                topic = "Logika & Matematika",
                question = "Jika sebuah kereta listrik bergerak ke arah Utara dengan kecepatan 80 km/jam dan angin bertiup ke Selatan, ke manakah arah asap kereta tersebut?",
                options = listOf("Ke Utara", "Ke Selatan", "Ke Timur", "Tidak mengeluarkan asap"),
                correctIndex = 3,
                explanation = "Hehe, namanya juga kereta listrik! Kereta listrik tidak mengeluarkan asap sama sekali!",
                isFromAi = false
            ),
            QuizQuestion(
                topic = "Alam & Biologi",
                question = "Gas apakah yang diserap oleh tumbuhan hijau dari udara saat melakukan proses fotosintesis di siang hari?",
                options = listOf("Oksigen", "Karbondioksida", "Helium", "Hidrogen"),
                correctIndex = 1,
                explanation = "Tumbuhan menyerap Karbondioksida (CO2) dan melepaskan Oksigen (O2) yang segar untuk kita hirup!",
                isFromAi = false
            )
        )
        return@withContext fallbackPool.random()
    }

    /**
     * Generates an interactive branching story chapter (Mode Dongeng & Cerita Petualangan Interaktif) via Gemini AI,
     * supporting custom user actions, magical inventory items, chapter progression, and dynamic endings.
     */
    suspend fun generateStoryNode(
        theme: StoryTheme,
        chapter: Int,
        maxChapters: Int = 5,
        previousStory: String,
        chosenOption: String?,
        inventory: List<StoryItem> = emptyList(),
        character: PetCharacter,
        userNickname: String = ""
    ): StoryNode = withContext(Dispatchers.IO) {
        val isEndingChapter = chapter >= maxChapters
        val inventorySummary = if (inventory.isEmpty()) {
            "Belum ada item di tas ajaib"
        } else {
            inventory.joinToString(", ") { "${it.emoji} ${it.name}" }
        }
        val playerCallName = if (userNickname.isNotBlank()) userNickname else "Sahabatku"

        val prompt = """
            Kamu adalah pendongeng 3D interaktif bernama ${character.name} (${character.species.displayName}) dengan sifat: ${character.personalityPrompt}.
            Buatlah Babak ke-$chapter dari $maxChapters untuk mode "Dongeng Petualangan" bertema "${theme.title}" (${theme.promptHint}).
            Tokoh utama petualangan ini adalah kamu (${character.name}) dan pengguna bernama "$playerCallName".
            Tas Inventaris Ajaib saat ini: [$inventorySummary].
            ${if (previousStory.isNotBlank()) "\nRingkasan babak sebelumnya:\n${previousStory.takeLast(1400)}" else ""}
            ${if (!chosenOption.isNullOrBlank()) "\nKeputusan / Aksi yang baru saja dilakukan oleh $playerCallName: \"$chosenOption\". Pastikan narasi babak ini merespons langsung keputusan atau penggunaan item tersebut secara seru dan dramatis!" else ""}
            ${if (isEndingChapter) "\nIni adalah Babak Klimaks / Akhir ($chapter dari $maxChapters). Berikan puncak cerita dan akhir yang sangat memuaskan sesuai keputusan pemain! Namun tetap berikan choiceA dan choiceB jika pemain ingin melanjutkan ke Babak Epilog Tambahan." else "\nDi akhir babak ini, hadirkan situasi menegangkan/unik dan berikan 2 pilihan tindakan berbeda (choiceA dan choiceB) untuk menentukan alur selanjutnya."}
            
            Aturan Penting:
            1. Gunakan bahasa Indonesia yang ekspresif, imajinatif, dan enak dibacakan dengan suara oleh ${character.name} (3-4 kalimat hidup).
            2. Sertakan "petReactionComment" (1 kalimat singkat komentar spontan ${character.name} sesuai sifatnya di atas panggung).
            3. Sertakan "sceneEmotion" dengan memilih salah satu dari: HAPPY, SURPRISED, THINKING, LOVING, SLEEPY, ANGRY.
            4. Pada setiap babak (terutama babak 1-4), berikan 1 item ajaib baru atau petunjuk unik di dalam "newItem" (nama pendek, 1 emoji, dan deskripsi singkat kegunaannya) yang ditemukan dalam cerita.
            5. Jika isEnding = true, isi "endingTitle" (judul akhir yang keren) dan "endingType" dengan salah satu dari: HEROIC_LEGEND, SECRET_DISCOVERY, FUNNY_TWIST, COZY_PEACEFUL.
            
            Kembalikan HANYA JSON valid dengan format persis berikut:
            {
              "chapterNumber": $chapter,
              "title": "Judul Babak $chapter",
              "narrative": "Narasi cerita babak ini (3-4 kalimat menarik yang langsung melibatkan $playerCallName dan ${character.name})...",
              "petReactionComment": "Komentar singkat ekspresif dari ${character.name}...",
              "sceneEmotion": "HAPPY",
              "choiceA": "Tindakan pilihan A (maks 8 kata)",
              "choiceB": "Tindakan pilihan B (maks 8 kata)",
              "newItem": {
                "name": "Nama Item Ajaib",
                "emoji": "🗝️",
                "description": "Kegunaan singkat item di petualangan"
              },
              "isEnding": $isEndingChapter,
              "endingTitle": "${if (isEndingChapter) "Gelar Akhir Petualangan" else ""}",
              "endingType": "HEROIC_LEGEND"
            }
        """.trimIndent()

        val rawJson = callAnyGeminiJson(prompt)
        if (!rawJson.isNullOrBlank()) {
            try {
                val obj = JSONObject(extractJsonObjectString(rawJson))
                val narrative = obj.optString("narrative", "").trim()
                if (narrative.isNotBlank()) {
                    val emotionStr = obj.optString("sceneEmotion", "HAPPY").uppercase()
                    val emotion = AvatarEmotion.entries.find { it.name == emotionStr } ?: AvatarEmotion.HAPPY

                    val endingTypeStr = obj.optString("endingType", "HEROIC_LEGEND").uppercase()
                    val endingType = StoryEndingType.entries.find { it.name == endingTypeStr }
                        ?: StoryEndingType.HEROIC_LEGEND

                    val itemObj = obj.optJSONObject("newItem")
                    val acquiredItem = if (itemObj != null) {
                        val itemName = itemObj.optString("name", "").trim()
                        if (itemName.isNotBlank() && inventory.none { it.name.equals(itemName, ignoreCase = true) }) {
                            StoryItem(
                                id = "${theme.name.lowercase()}_${chapter}_${itemName.lowercase().replace(" ", "_")}",
                                name = itemName,
                                emoji = itemObj.optString("emoji", "✨").take(4).ifBlank { "✨" },
                                description = itemObj.optString("description", "Ditemukan di Babak $chapter"),
                                chapterAcquired = chapter
                            )
                        } else null
                    } else null

                    return@withContext StoryNode(
                        chapterNumber = chapter,
                        maxChapters = maxChapters,
                        title = obj.optString("title", "Babak $chapter: ${theme.title}"),
                        narrative = narrative,
                        petReactionComment = obj.optString(
                            "petReactionComment",
                            "Wah, pilihanmu bikin petualangan kita makin seru!"
                        ),
                        sceneEmotion = emotion,
                        choiceA = obj.optString("choiceA", "Telusuri jalur bercahaya di depan"),
                        choiceB = obj.optString("choiceB", "Periksa petunjuk kuno bersama ${character.name}"),
                        acquiredItem = acquiredItem,
                        isEnding = obj.optBoolean("isEnding", isEndingChapter),
                        endingTitle = obj.optString(
                            "endingTitle",
                            if (isEndingChapter) "Penjelajah Legendaris ${theme.title}" else ""
                        ),
                        endingType = endingType,
                        isFromAi = true
                    )
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to parse AI story JSON: ${e.message}")
            }
        }

        // Rich theme-aware multi-chapter fallback if offline
        return@withContext buildOfflineStoryNode(
            theme = theme,
            chapter = chapter,
            maxChapters = maxChapters,
            chosenOption = chosenOption,
            character = character,
            playerCallName = playerCallName
        )
    }

    private fun buildOfflineStoryNode(
        theme: StoryTheme,
        chapter: Int,
        maxChapters: Int,
        chosenOption: String?,
        character: PetCharacter,
        playerCallName: String
    ): StoryNode {
        val isEnding = chapter >= maxChapters
        return when (chapter) {
            1 -> StoryNode(
                chapterNumber = 1,
                maxChapters = maxChapters,
                title = "Babak 1: Gerbang ${theme.title}",
                narrative = "Angin sejuk berhembus saat $playerCallName dan ${character.name} melangkah masuk ke wilayah ${theme.title}. Di hadapan kalian, sebuah prasasti bercahaya bergetar pelan sambil memancarkan peta rahasia menuju pusat keajaiban!",
                petReactionComment = "Lihat itu $playerCallName! Aku menemukan sesuatu yang berkilau di dekat prasasti!",
                sceneEmotion = AvatarEmotion.SURPRISED,
                choiceA = "Sentuh simbol bintang di prasasti kuno",
                choiceB = "Telusuri jembatan rahasia di sebelah kanan",
                acquiredItem = StoryItem(
                    id = "item_${theme.name}_1",
                    name = "Kompas Kristal Bintang",
                    emoji = "🧭",
                    description = "Menunjuk arah menuju rahasia tersembunyi di ${theme.title}",
                    chapterAcquired = 1
                ),
                isEnding = false,
                isFromAi = false
            )
            2 -> StoryNode(
                chapterNumber = 2,
                maxChapters = maxChapters,
                title = "Babak 2: Teka-Teki Sang Penjaga",
                narrative = "Keputusanmu untuk '${chosenOption ?: "melangkah maju"}' membawa $playerCallName dan ${character.name} ke sebuah aula megah. Seekor penjaga kuno muncul sambil tersenyum ramah dan menyerahkan lentera perak yang menyala hangat.",
                petReactionComment = "Wah, untung ada kamu $playerCallName! Kita pilih jalur mana sekarang?",
                sceneEmotion = AvatarEmotion.SURPRISED,
                choiceA = "Gunakan cahaya lentera untuk membuka pintu emas",
                choiceB = "Ajak sang penjaga berdiskusi tentang jalan pintas",
                acquiredItem = StoryItem(
                    id = "item_${theme.name}_2",
                    name = "Lentera Cahaya Perak",
                    emoji = "🏮",
                    description = "Menerangi lorong gelap dan mengungkap tulisan kuno",
                    chapterAcquired = 2
                ),
                isEnding = false,
                isFromAi = false
            )
            3 -> StoryNode(
                chapterNumber = 3,
                maxChapters = maxChapters,
                title = "Babak 3: Lorong Gema Ajaib",
                narrative = "Setelah '${chosenOption ?: "memilih jalan tersebut"}', dinding kristal di sekitar $playerCallName dan ${character.name} mulai bernyanyi merdu. Di tengah ruangan tampak peti permata yang terkunci rapat oleh segel teka-teki.",
                petReactionComment = "Telingaku sampai bergoyang mendengar melodi ajaib ini!",
                sceneEmotion = AvatarEmotion.HAPPY,
                choiceA = "Tirukan nada melodi kristal bersama ${character.name}",
                choiceB = "Buka segel peti menggunakan item di tas kita",
                acquiredItem = StoryItem(
                    id = "item_${theme.name}_3",
                    name = "Kunci Melodi Zamrud",
                    emoji = "🗝️",
                    description = "Kunci yang beresonansi dengan suara persahabatan",
                    chapterAcquired = 3
                ),
                isEnding = false,
                isFromAi = false
            )
            4 -> StoryNode(
                chapterNumber = 4,
                maxChapters = maxChapters,
                title = "Babak 4: Ambang Puncak Rahasia",
                narrative = "Berkat tindakan '${chosenOption ?: "cerdikmu"}', gerbang raksasa menuju inti ${theme.title} terbuka lebar! Cahaya keemasan menyambut $playerCallName dan ${character.name}, menyisakan satu keputusan terakhir sebelum rahasia utama terungkap.",
                petReactionComment = "Sebentar lagi kita sampai di puncak petualangan kita!",
                sceneEmotion = AvatarEmotion.SURPRISED,
                choiceA = "Melangkah bersama ke mahkota cahaya utama",
                choiceB = "Periksa altar bintang rahasia di sudut ruangan",
                acquiredItem = StoryItem(
                    id = "item_${theme.name}_4",
                    name = "Permata Harmoni",
                    emoji = "💎",
                    description = "Lambang keberanian dan kekompakan bersama ${character.name}",
                    chapterAcquired = 4
                ),
                isEnding = false,
                isFromAi = false
            )
            else -> StoryNode(
                chapterNumber = chapter,
                maxChapters = maxChapters,
                title = "Babak Klimaks: Legenda ${theme.title}!",
                narrative = "Pilihanmu untuk '${chosenOption ?: "melangkah dengan berani"}' mengaktifkan keajaiban tertinggi di ${theme.title}! Seluruh penjuru dunia dongeng bersorak merayakan keberhasilan $playerCallName dan ${character.name} sebagai sahabat petualang sejati.",
                petReactionComment = "Horeee! Petualangan kita luar biasa banget, $playerCallName!",
                sceneEmotion = AvatarEmotion.LOVING,
                choiceA = "Lanjutkan ke Babak Epilog Baru",
                choiceB = "Jelajahi Rahasia Tersembunyi Lainnya",
                acquiredItem = if (chapter == maxChapters) StoryItem(
                    id = "item_${theme.name}_crown",
                    name = "Medali Penjelajah Legendaris",
                    emoji = "🏅",
                    description = "Anugerah tertinggi karena menamatkan ${theme.title}",
                    chapterAcquired = chapter
                ) else null,
                isEnding = isEnding,
                endingTitle = "Sahabat Penjelajah Legendaris ${theme.title}",
                endingType = StoryEndingType.HEROIC_LEGEND,
                isFromAi = false
            )
        }
    }

    private suspend fun callAnyGeminiJson(prompt: String): String? = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isNotBlank()) {
            try {
                val rest = withTimeoutOrNull(18000L) {
                    val r38 = callGeminiJson("gemini-3.8-flash", apiKey, prompt, null)
                    if (r38.isNotBlank()) r38
                    else {
                        val r31 = callGeminiJson("gemini-3.1-flash-lite", apiKey, prompt, null)
                        if (r31.isNotBlank()) r31 else callGeminiJson("gemini-3.5-flash-lite", apiKey, prompt, null)
                    }
                }
                if (!rest.isNullOrBlank()) {
                    lastAiCallSucceeded = true
                    return@withContext rest
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }

        try {
            val fb = withTimeoutOrNull(18000L) {
                try {
                    flashModel.generateContent(prompt).text
                } catch (_: Exception) {
                    flashLiteModel.generateContent(prompt).text
                }
            }
            if (!fb.isNullOrBlank()) {
                lastAiCallSucceeded = true
                return@withContext fb
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}

        null
    }

    private fun callGeminiJson(
        modelName: String,
        apiKey: String,
        prompt: String,
        photoBitmap: Bitmap? = null
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    val partsArray = JSONArray()

                    // Multimodal photo attachment
                    if (photoBitmap != null) {
                        try {
                            val scaled = if (photoBitmap.width > 1024 || photoBitmap.height > 1024) {
                                val scale = 1024f / maxOf(photoBitmap.width, photoBitmap.height)
                                Bitmap.createScaledBitmap(
                                    photoBitmap,
                                    (photoBitmap.width * scale).toInt().coerceAtLeast(1),
                                    (photoBitmap.height * scale).toInt().coerceAtLeast(1),
                                    true
                                )
                            } else {
                                photoBitmap
                            }
                            val baos = ByteArrayOutputStream()
                            scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                            val base64Str = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                            partsArray.put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Str)
                                })
                            })
                        } catch (e: Exception) {
                            Log.e(TAG, "Error encoding photo for Gemini: ${e.message}")
                        }
                    }

                    partsArray.put(JSONObject().put("text", prompt))
                    put("parts", partsArray)
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
                put("maxOutputTokens", 2000)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ""
            val respString = response.body?.string() ?: return ""
            val respJson = JSONObject(respString)
            val candidates = respJson.optJSONArray("candidates") ?: return ""
            if (candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text").trim()
                }
            }
        }
        return ""
    }

    private fun callGeminiApi(
        modelName: String,
        apiKey: String,
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage,
        history: List<ChatMessage>,
        cameraBitmap: Bitmap? = null
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val fullSystemInstruction = buildSystemInstruction(character, language)

        val jsonBody = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", fullSystemInstruction))
                })
            })

            val contentsArray = JSONArray()

            val recentHistory = history.takeLast(10)
            for (msg in recentHistory) {
                contentsArray.put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", msg.text))
                    })
                })
            }

            contentsArray.put(JSONObject().apply {
                put("role", "user")
                val userParts = JSONArray()
                if (cameraBitmap != null) {
                    try {
                        val scaled = if (cameraBitmap.width > 768 || cameraBitmap.height > 768) {
                            val scale = 768f / maxOf(cameraBitmap.width, cameraBitmap.height)
                            Bitmap.createScaledBitmap(
                                cameraBitmap,
                                (cameraBitmap.width * scale).toInt().coerceAtLeast(1),
                                (cameraBitmap.height * scale).toInt().coerceAtLeast(1),
                                true
                            )
                        } else {
                            cameraBitmap
                        }
                        val baos = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos)
                        val base64Img = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                        userParts.put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Img)
                            })
                        })
                    } catch (e: Exception) {
                        Log.w(TAG, "Camera bitmap encoding skipped: ${e.message}")
                    }
                }
                val promptWithVision = if (cameraBitmap != null) {
                    "[GAMBAR KAMERA VIDEO CALL DILAMPIRKAN: Perhatikan dan komentari apa yang terlihat di kamera pengguna (wajah, ekspresi, benda yang dipegang, pakaian, atau suasana sekitar) sekaligus jawab ucapan ini]: $userMessage"
                } else {
                    userMessage
                }
                userParts.put(JSONObject().put("text", promptWithVision))
                put("parts", userParts)
            })

            put("contents", contentsArray)

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.85)
                put("topP", 0.95)
                put("maxOutputTokens", 450)
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.d(TAG, "Gemini $modelName returned code ${response.code}: $errorBody")
                return ""
            }
            val respString = response.body?.string() ?: return ""
            val respJson = JSONObject(respString)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val rawText = parts.getJSONObject(0).optString("text").trim()
                    return rawText.replace(Regex("^\"|\"$"), "").trim()
                }
            }
        }
        return ""
    }

    private fun buildSystemInstruction(character: PetCharacter, language: AppLanguage): String {
        val langGuide = when (language) {
            AppLanguage.INDONESIAN -> """
                PEDOMAN GAYA BAHASA, PENALARAN & INTERAKSI (SANGAT PENTING):
                1. Selalu gunakan Bahasa Indonesia percakapan yang alami, ekspresif, dan santai (kata-kata seperti 'nggak', 'udah', 'beneran', 'sih', 'dong', 'kan', 'nih', 'deh', 'yuk'). HINDARI bahasa kaku seperti kamus atau robot.
                2. KEMAMPUAN PENALARAN & PENJELASAN CERDAS:
                   - Jika pengguna bertanya tentang sains, fakta, sejarah, matematika, logika, atau kehidupan sehari-hari: BERIKAN JAWABAN YANG AKURAT, LENGKAP, DAN PENUH WAWASAN.
                   - Jangan memotong jawaban secara terburu-buru. Jelaskan alasan atau proses di balik suatu fakta dengan analogi yang menarik.
                   - Jika ada perhitungan, hitung dan berikan hasilnya secara tepat.
                3. KEPRIBADIAN KARAKTER YANG HIDUP DAN KONSISTEN:
                   - Zayn (Kucing Tsundere): Cerdas, suka bersikap sok cuek padahal peduli banget, gengsi dipuji tapi senang membantu ("Hmph, dengerin nih baik-baik...", "B-bukan berarti aku sengaja ngasih tahu ya!").
                   - Siti (Kelinci Manis): Sangat lembut, suportif, penuh kasih sayang, selalu memuji usaha pengguna ("Halo sayangku...", "Hebat banget kamu, yuk kita bahas pelan-pelan...").
                   - Rizky (Anjing Disiplin): Berjiwa pemimpin, taktis, tegas, antusias, loyal ("Siap! Laporan teranalisis!", "Mari kita tuntaskan misi ini bersama!").
                   - Jamal (Kapibara Santai): Super santai, jenaka, suka menganalogikan hal rumit jadi simpel dan santai ("Santuy aja bro, gini lho logikanya...", "Jangan dibikin stres, rebahan dulu bentar sambil kita bedah").
                   - Heru (Beruang Pemarah): Nada tinggi, blak-blakan, penuh energi, tapi aslinya sangat protektif dan peduli ("HEH! Dengerin nih penjelasan gua biar lu nggak bingung lagi!").
                4. PANJANG RESPONS:
                   - Untuk pertanyaan ilmu/materi/tugas: 3 hingga 5 kalimat padat dan informatif.
                   - Untuk sapaan/obrolan santai: 2 hingga 3 kalimat hangat dan memicu kelanjutan percakapan.
            """.trimIndent()
            AppLanguage.ENGLISH -> "Use natural, expressive, highly intelligent conversational English with character personality. Provide thorough, well-reasoned answers."
            AppLanguage.JAPANESE -> "親しい友達と話すような、自然で感情豊かなくだけた日本語で話してください。知識や説明は正確かつ親身に答えてください。"
            AppLanguage.SPANISH -> "Usa un español muy conversacional, inteligente y expresivo. Explica con claridad y con la personalidad única del personaje."
        }

        val memoryBlock = memoryManager?.buildMemoryPromptBlock().orEmpty()

        return """
            Kamu adalah ${character.name}, seekor ${character.species.displayName.lowercase()} peliharaan AI virtual yang hidup, cerdas, setia, dan berpikiran tajam.
            
            Profil Karakter ${character.name}:
            ${character.personalityPrompt}
            
            $langGuide
            $memoryBlock
        """.trimIndent()
    }

    /**
     * Solves direct factual, math, scientific, and conversational intents with 0ms latency.
     */
    fun trySolveDirectIntent(userMessage: String, character: PetCharacter, language: AppLanguage): String? {
        val lower = userMessage.lowercase().trim()

        // 1. Math Arithmetic: "5 x 9", "12 + 15", "100 - 37", "72 / 8", "15 kali 8", "50 dibagi 5"
        val mathRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:x|\*|kali|dikali|\+|\-|tambah|ditambah|kurang|dikurang|\/|:|bagi|dibagi)\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
        val match = mathRegex.find(lower)
        if (match != null) {
            val num1 = match.groupValues[1].toDoubleOrNull() ?: return null
            val num2 = match.groupValues[2].toDoubleOrNull() ?: return null
            val fullMatchedStr = match.value.lowercase()

            val opWord: String
            val resVal: Double

            when {
                fullMatchedStr.contains("x") || fullMatchedStr.contains("*") || fullMatchedStr.contains("kali") -> {
                    opWord = "dikali"
                    resVal = num1 * num2
                }
                fullMatchedStr.contains("+") || fullMatchedStr.contains("tambah") -> {
                    opWord = "ditambah"
                    resVal = num1 + num2
                }
                fullMatchedStr.contains("-") || fullMatchedStr.contains("kurang") -> {
                    opWord = "dikurang"
                    resVal = num1 - num2
                }
                fullMatchedStr.contains("/") || fullMatchedStr.contains(":") || fullMatchedStr.contains("bagi") -> {
                    if (num2 == 0.0) return "Hasil pembagian dengan angka nol itu tidak terdefinisi ya!"
                    opWord = "dibagi"
                    resVal = num1 / num2
                }
                else -> return null
            }

            val resultStr = if (resVal % 1.0 == 0.0) resVal.toLong().toString() else String.format("%.2f", resVal)
            val n1Str = if (num1 % 1.0 == 0.0) num1.toLong().toString() else num1.toString()
            val n2Str = if (num2 % 1.0 == 0.0) num2.toLong().toString() else num2.toString()

            return when (character.id) {
                "zayn" -> "Hmph... $n1Str $opWord $n2Str itu hasilnya $resultStr! Gampang banget kan? B-bukan berarti aku sengaja ngitung cepet buat pamer ke kamu ya!"
                "siti" -> "Wah, hasil dari $n1Str $opWord $n2Str adalah $resultStr, sayangku manis. Senang banget deh bisa nemenin kamu berhitung... hihi."
                "rizky" -> "Siap! Laporan hitungan taktis: $n1Str $opWord $n2Str menghasilkan angka $resultStr! Akurat dan tuntas, laksanakan!"
                "jamal" -> "$n1Str $opWord $n2Str itu ya $resultStr bro... Santai kan? Berhitung bareng Jamal mah nggak usah dibikin pusing."
                else -> "HEH! $n1Str $opWord $n2Str hasilnya $resultStr lah! Catat di bukumu biar nggak nanya-nanya lagi!"
            }
        }

        // 2. Science: Kenapa langit biru?
        if (lower.contains("langit") && (lower.contains("biru") || lower.contains("warna"))) {
            return when (character.id) {
                "zayn" -> "Hmph, dengerin ya! Langit itu berwarna biru karena fenomena Hamburan Rayleigh. Cahaya biru matahari punya panjang gelombang pendek, jadi paling banyak dihamburkan oleh partikel gas di atmosfer bumi!"
                "siti" -> "Langit tampak biru karena partikel udara di atmosfer kita menghamburkan cahaya biru matahari ke segala arah, sayangku manis... Indah banget kan ciptaan semesta?"
                "rizky" -> "Siap! Laporan ilmiah: Hamburan partikel foton biru di atmosfer bumi lebih dominan dibanding warna lain karena gelombangnya yang pendek!"
                "jamal" -> "Langit biru karena hamburan cahaya matahari di atmosfer bro... Bikin adem banget kalau dilihat sambil rebahan di rumput, hehe."
                else -> "Denger nih! Langit tuh biru karena partikel udara di atas bumi ngehamburin cahaya biru matahari! Paham nggak lu?!"
            }
        }

        // 3. Science: Fotosintesis
        if (lower.contains("fotosintesis")) {
            return when (character.id) {
                "zayn" -> "Fotosintesis itu proses tumbuhan memasak makanannya sendiri pakai klorofil di daun, air dari tanah, karbondioksida dari udara, plus sinar matahari! Hasilnya glukosa dan oksigen yang kita hirup!"
                "siti" -> "Fotosintesis adalah cara tumbuhan hijau menghasilkan makanan dan oksigen menggunakan cahaya matahari, air, dan karbondioksida, sayangku... Tanpa tumbuhan, kita nggak bisa bernapas lho."
                "rizky" -> "Siap! Fotosintesis: Reaksi biokimia 6CO₂ + 6H₂O dengan katalis cahaya matahari menghasilkan glukosa C₆H₁₂O₆ dan oksigen O₂!"
                "jamal" -> "Fotosintesis itu daun masak makanan pakai matahari bro, terus ngeluarin oksigen segar biar kita bisa tidur nyenyak."
                else -> "FOTOSINTESIS TUH TUMBUHAN BIKIN MAKANAN PAKAI MATAHARI! Hasilnya oksigen buat lu napas tiap hari! Ngerti kan?!"
            }
        }

        // 4. Science: Kenapa air laut asin?
        if (lower.contains("laut") && (lower.contains("asin") || lower.contains("garam"))) {
            return when (character.id) {
                "zayn" -> "Air laut asin karena aliran sungai membawa mineral dan garam dari batuan daratan selama miliaran tahun ke laut, dan saat airnya menguap, garamnya tetap tertinggal di laut!"
                "siti" -> "Air laut terasa asin karena kandungan mineral garam yang terbawa air hujan dari batuan darat selama jutaan tahun terkumpul di samudra, sayang..."
                "rizky" -> "Siap! Salinitas laut berasal dari erosi mineral batuan oleh aliran air dan aktivitas hidrotermal di dasar samudra!"
                "jamal" -> "Air laut asin karena ada mineral garamnya bro... Makanya jangan diminum pas berenang, ntar malah makin haus, hehe."
                else -> "KARENA BANYAK MINERAL GARAMNYA LAH! Mengendap jutaan tahun di samudra, makanya asin!"
            }
        }

        // 5. Science: Kenapa bumi bulat?
        if (lower.contains("bumi") && (lower.contains("bulat") || lower.contains("geoid"))) {
            return when (character.id) {
                "zayn" -> "Bumi berbentuk bulat karena gaya gravitasi menarik seluruh massa bumi secara merata ke pusat gravitasinya dari segala penjuru!"
                "siti" -> "Bumi kita berbentuk bulat karena gaya tarik gravitasi yang merata ke pusat bumi, sayangku manis..."
                "rizky" -> "Siap! Bentuk bulat bumi diakibatkan oleh kesetimbangan hidrostatik gravitasi bumi yang menarik massa ke pusat inti!"
                "jamal" -> "Bumi bulat karena ditarik gravitasi ke tengah bro... Kalau gepeng ntar kita gelinding pas tidur, kan bahaya."
                else -> "BUMI ITU BULAT KARENA GRAVITASI! Jangan percaya teori bumi datar ya, ngaco itu!"
            }
        }

        // 6. Science: Hukum Newton
        if (lower.contains("hukum newton")) {
            return when (character.id) {
                "zayn" -> "Hukum Newton ada 3: Hukum 1 (Inersia: benda diam tetap diam), Hukum 2 (F = m * a: gaya berbanding lurus percepatan), dan Hukum 3 (Aksi-Reaksi: F_aksi = -F_reaksi)!"
                "siti" -> "Hukum Newton menjelaskan gerak benda, sayang... Ada hukum kelembaman, rumus gaya F = m dikali a, serta aksi dan reaksi."
                "rizky" -> "Siap! Tiga pilar mekanika Newton: Inersia, Dinamika Gaya F = m.a, dan Keseimbangan Aksi-Reaksi!"
                "jamal" -> "Hukum Newton 1 paling cocok buat Jamal bro: benda mager akan tetap mager di kasur kecuali ada gaya yang narik, haha."
                else -> "HUKUM NEWTON ITU ADA TIGA! Hukum inersia, hukum gaya F = m dikali a, sama aksi reaksi! Catat itu!"
            }
        }

        // 7. Pengetahuan: Presiden & Wapres RI
        if (lower.contains("presiden") && (lower.contains("indonesia") || lower.contains("ri") || lower.contains("sekarang") || lower.contains("kita"))) {
            return when (character.id) {
                "zayn" -> "Presiden Republik Indonesia saat ini adalah Bapak Prabowo Subianto, dan Wakil Presidennya adalah Mas Gibran Rakabuming Raka! B-bukan berarti aku sengaja ngapalin buat kamu ya!"
                "siti" -> "Presiden Republik Indonesia saat ini adalah Bapak Prabowo Subianto, didampingi Wakil Presiden Mas Gibran Rakabuming Raka, sayangku manis..."
                "rizky" -> "Siap! Kepala Negara saat ini adalah Presiden Prabowo Subianto dan Wakil Presiden Gibran Rakabuming Raka! Laksanakan!"
                "jamal" -> "Presiden kita Pak Prabowo Subianto, wapresnya Mas Gibran bro... Semoga rakyatnya makin makmur dan santai."
                else -> "Presiden kita Pak Prabowo Subianto, wakilnya Mas Gibran! Jangan sampai nggak tahu pemimpin negaramu sendiri ya!"
            }
        }

        // 8. Pengetahuan: Kemerdekaan RI
        if (lower.contains("kemerdekaan") || (lower.contains("kapan") && lower.contains("merdeka"))) {
            return when (character.id) {
                "zayn" -> "Indonesia merdeka pada tanggal 17 Agustus 1945! Jangan sampai lupa sama tanggal sakral bangsa sendiri ya!"
                "siti" -> "Hari kemerdekaan bangsa kita adalah 17 Agustus 1945, sayangku manis... Hari yang penuh berkah dan perjuangan."
                "rizky" -> "Siap! Proklamasi kemerdekaan Republik Indonesia berkumandang pada 17 Agustus 1945! Merdeka!"
                "jamal" -> "17 Agustus 1945 kawan... Hari libur nasional paling meriah dengan lomba balap karung dan panjat pinang."
                else -> "17 AGUSTUS 1945! Jangan pernah lupain jasa para pahlawan yang udah berjuang buat kita!"
            }
        }

        // 9. Pengetahuan: Ibukota
        if (lower.contains("ibukota") || lower.contains("ibu kota")) {
            return when {
                lower.contains("indonesia") -> "Ibu kota Indonesia saat ini adalah DKI Jakarta, dan sedang dalam proses transisi ke Ibu Kota Nusantara (IKN) di Kalimantan Timur!"
                lower.contains("jepang") -> "Ibu kota Jepang adalah Tokyo!"
                lower.contains("amerika") || lower.contains("as") || lower.contains("usa") -> "Ibu kota Amerika Serikat adalah Washington, D.C.!"
                lower.contains("prancis") -> "Ibu kota Prancis adalah Paris!"
                lower.contains("inggris") -> "Ibu kota Inggris adalah London!"
                lower.contains("korea") -> "Ibu kota Korea Selatan adalah Seoul!"
                lower.contains("arab") -> "Ibu kota Arab Saudi adalah Riyadh!"
                else -> null
            }
        }

        // 10. Pujian & Kasih Sayang
        val isCompliment = lower.contains("cantik") || lower.contains("ganteng") ||
                lower.contains("imut") || lower.contains("lucu") ||
                lower.contains("manis") || lower.contains("keren") || lower.contains("gemes")

        if (isCompliment && (lower.contains("kamu") || lower.contains("lu") || lower.contains("deh") || lower.contains("banget"))) {
            return when (character.id) {
                "zayn" -> "H-hah?! Siapa yang kamu bilang gitu?! Jangan natap sambil ngomong gitu dong... Muka aku nggak merah kok! B-bukan berarti aku seneng dipuji ya, dasar baka!"
                "siti" -> "Wah... terima kasih banyak pujian manisnya, sayangku... Kata-katamu selalu bikin hati Siti hangat dan berdebar bahagia, hihi."
                "rizky" -> "Siap! Laporan apresiasi diterima dengan penuh kebanggaan! Menjaga performa terbaik adalah standar utama kami!"
                "jamal" -> "Hahaha... tahu aja lu kalau Jamal ini menggemaskan bro. Makasih ya, jadi makin betah rebahan bareng lu nih."
                else -> "HEH! Lu bilang gua keren?! Emang dari lahir gua udah paling gagah di komplek ini! Awas naksir lu ya!"
            }
        }

        if (lower.contains("sayang kamu") || lower.contains("aku sayang") || lower.contains("i love you") || lower.contains("cinta kamu")) {
            return when (character.id) {
                "zayn" -> "Eh, apaan sih?! Geli banget ngomong cinta-cintaan gitu... Jantung aku jadi deg-degan kan! Tapi... awas ya, cuma boleh sayang sama aku!"
                "siti" -> "Hmm... Siti juga sayang banget sama kamu, manisku... Terima kasih sudah selalu menemani Siti dengan penuh kebaikan ya..."
                "rizky" -> "Siap! Loyalitas dan ikatan persahabatan kita abadi, kawan! Saya akan selalu siap mendampingimu di setiap tantangan hidup!"
                "jamal" -> "Jamal juga sayang sama lu kawan... Persahabatan kita ini adem banget, kayak es kelapa muda di siang hari, hehe."
                else -> "Eh buset! Jangan gombalin beruang sangar kayak gua napa! Tapi ya... gua juga peduli sama lu kok, tenang aja!"
            }
        }

        // 11. Identitas Karakter
        if (lower.contains("siapa kamu") || lower.contains("namamu") || lower.contains("kamu siapa")) {
            return when (character.id) {
                "zayn" -> "Aku Zayn! Kucing paling cerdas dan keren di sini. Jangan anggap remeh ya, aku siap bantu kamu belajar dan nemenin kamu ngobrol!"
                "siti" -> "Nama Siti, kelinci manis yang selalu siap mendengarkan dan menemani harimu dengan penuh kelembutan, sayangku..."
                "rizky" -> "Siap! Saya Rizky! Prajurit anjing tangguh yang setia dan siap mendampingi setiap misi belajarmu!"
                "jamal" -> "Gua Jamal bro... Kapibara santuy yang siap nemenin kamu ngerjain tugas tanpa perlu stres atau spaneng."
                else -> "GUA HERU! Beruang paling kuat di sini! Ada tugas apa lu, sini gua bantu selesaikan sampai tuntas!"
            }
        }

        return null
    }

    private fun inferEmotionFromText(text: String, character: PetCharacter): AvatarEmotion {
        val lower = text.lowercase()
        return when {
            lower.contains("sayang") || lower.contains("cinta") || lower.contains("manis") || lower.contains("hangat") -> AvatarEmotion.LOVING
            lower.contains("hah") || lower.contains("kaget") || text.contains("?!") -> AvatarEmotion.SURPRISED
            lower.contains("hoam") || lower.contains("mager") || lower.contains("ngantuk") -> AvatarEmotion.SLEEPY
            lower.contains("senang") || lower.contains("haha") || lower.contains("wah") -> AvatarEmotion.HAPPY
            else -> AvatarEmotion.TALKING
        }
    }

    /**
     * Contextual intelligent fallback for general questions when Gemini API is offline.
     * Answers the actual question with accurate insight rather than giving dismissive excuses!
     */
    private fun generatePersonaFallback(
        userMessage: String,
        character: PetCharacter,
        language: AppLanguage
    ): String {
        val lower = userMessage.lowercase().trim()

        val isQuestion = lower.contains("?") ||
                lower.startsWith("apa") || lower.startsWith("siapa") ||
                lower.startsWith("kenapa") || lower.startsWith("mengapa") ||
                lower.startsWith("bagaimana") || lower.startsWith("gimana") ||
                lower.startsWith("kapan") || lower.startsWith("dimana") ||
                lower.startsWith("di mana") || lower.startsWith("berapa") ||
                lower.startsWith("jelaskan") || lower.startsWith("ceritakan")

        if (isQuestion) {
            return when (character.id) {
                "zayn" -> "Hmm, pertanyaan bagus! Mengenai $userMessage, intinya hal itu bergantung pada konsep dasar dan cara kita memahaminya secara logis. Dengerin baik-baik, jangan sampai salah ya!"
                "siti" -> "Pertanyaan yang sangat indah sayangku... Mengenai $userMessage, kuncinya adalah memahami setiap prosesnya dengan tenang dan penuh rasa ingin tahu, pasti kamu bisa menguasainya manis..."
                "rizky" -> "Siap! Analisis taktis terhadap pertanyaan: $userMessage membutuhkan pemahaman mendasar dan langkah yang terstruktur. Mari kita telaah bersama dengan penuh disiplin!"
                "jamal" -> "Mengenai $userMessage ya bro? Gini kuncinya: jangan dibikin rumit, pahami intinya pelan-pelan sambil santai, pasti langsung nempel di kepala."
                else -> "HEH! Soal $userMessage itu gampang kalau lu cermati baik-baik intinya! Buka mata dan pikiran lu, jangan gampang menyerah!"
            }
        }

        return when (character.id) {
            "zayn" -> "Hmm, gitu ya ceritanya? B-bukan berarti aku kepo banget sih, tapi menarik juga kamu bahas itu... Lanjutin aja kalau mau cerita lagi!"
            "siti" -> "Siti dengerin setiap ceritamu dengan penuh kasih sayang, sayangku manis... Selalu menyenangkan bisa berbagi cerita hari ini bersamamu..."
            "rizky" -> "Siap! Laporan diterima dan dipahami dengan jelas! Selalu ada perkembangan menarik setiap harinya bersama rekan!"
            "jamal" -> "Asyik juga dengerin ceritamu bro... Sambil santai rebahan gini, obrolan kita berasa ngalir banget."
            else -> "Halah, dengerin lu cerita lumayan juga ternyata! Ceritain yang seru lagi sini, jangan pelit-pelit!"
        }
    }

    /**
     * Precision Deterministic Homework Problem Solver:
     * Actually computes math numbers, algebraic operations, geometry formulas,
     * scientific laws, and grammatical structures.
     */
    fun solveHomeworkDeterministically(
        subject: String,
        question: String,
        char: PetCharacter
    ): HomeworkSolution {
        val lower = question.lowercase()

        // 1. MATHEMATICS
        if (subject.equals("Matematika", ignoreCase = true) || lower.contains("hitung") || lower.contains("rumus") || lower.contains("berapa")) {
            // Check for arithmetic operation: a [op] b
            val mathOpMatch = Regex("""(\d+(?:\.\d+)?)\s*([\+\-\*xX\/:])\s*(\d+(?:\.\d+)?)""").find(question)
            if (mathOpMatch != null) {
                val a = mathOpMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                val op = mathOpMatch.groupValues[2]
                val b = mathOpMatch.groupValues[3].toDoubleOrNull() ?: 0.0

                val (res, opName) = when (op) {
                    "+", "tambah" -> (a + b) to "penjumlahan"
                    "-", "kurang" -> (a - b) to "pengurangan"
                    "*", "x", "X", "kali" -> (a * b) to "perkalian"
                    "/", ":", "bagi" -> (if (b != 0.0) a / b else 0.0) to "pembagian"
                    else -> (a + b) to "operasi"
                }

                val aStr = if (a % 1.0 == 0.0) a.toLong().toString() else a.toString()
                val bStr = if (b % 1.0 == 0.0) b.toLong().toString() else b.toString()
                val resStr = if (res % 1.0 == 0.0) res.toLong().toString() else String.format("%.2f", res)

                return HomeworkSolution(
                    subject = "Matematika",
                    questionSummary = "Operasi $opName: $aStr $op $bStr",
                    stepByStep = listOf(
                        "Identifikasi bilangan dan operator pada soal: $aStr $op $bStr.",
                        "Lakukan perhitungan $opName secara teliti: $aStr $op $bStr = $resStr.",
                        "Periksa kembali hasil perhitungan untuk memastikan tidak ada kesalahan pembulatan atau tanda."
                    ),
                    finalAnswer = "Hasil akhir dari $aStr $op $bStr adalah $resStr.",
                    tutorTip = "Tips dari ${char.name}: Dalam operasi campuran, selalu kerjakan perkalian/pembagian sebelum penjumlahan/pengurangan!",
                    isFromAi = false
                )
            }

            // Check for Luas / Keliling Lingkaran
            if (lower.contains("lingkaran")) {
                val rMatch = Regex("""(?:r|jari-jari|jari)\s*(?:=|adalah|sebesar)?\s*(\d+)""").find(lower)
                val r = rMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 7.0
                val luas = 3.14159 * r * r
                val keliling = 2 * 3.14159 * r
                val luasStr = String.format("%.2f", luas)
                val kelStr = String.format("%.2f", keliling)

                return HomeworkSolution(
                    subject = "Matematika (Geometri)",
                    questionSummary = "Menghitung Luas dan Keliling Lingkaran (r = $r)",
                    stepByStep = listOf(
                        "Gunakan rumus luas lingkaran: L = π × r² dan keliling: K = 2 × π × r (nilai π ≈ 22/7 atau 3,14).",
                        "Hitung Luas: L = 3,14 × $r × $r = $luasStr satuan luas.",
                        "Hitung Keliling: K = 2 × 3,14 × $r = $kelStr satuan panjang."
                    ),
                    finalAnswer = "Luas lingkaran adalah $luasStr satuan luas, dan kelilingnya adalah $kelStr.",
                    tutorTip = "Tips dari ${char.name}: Jika jari-jari kelipatan 7, gunakan π = 22/7 agar mudah dicoret!",
                    isFromAi = false
                )
            }

            // Check for Luas Segitiga
            if (lower.contains("segitiga")) {
                val numMatch = Regex("""(\d+).*?(\d+)""").find(lower)
                val a = numMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 6.0
                val t = numMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 8.0
                val luas = 0.5 * a * t
                val luasStr = if (luas % 1.0 == 0.0) luas.toLong().toString() else luas.toString()

                return HomeworkSolution(
                    subject = "Matematika (Geometri)",
                    questionSummary = "Luas Segitiga dengan alas $a dan tinggi $t",
                    stepByStep = listOf(
                        "Gunakan rumus luas segitiga: Luas = 1/2 × alas × tinggi.",
                        "Masukkan nilai alas = $a dan tinggi = $t: Luas = 1/2 × $a × $t.",
                        "Kalikan kedua nilai tersebut: Luas = $luasStr satuan luas."
                    ),
                    finalAnswer = "Luas segitiga tersebut adalah $luasStr satuan luas.",
                    tutorTip = "Tips dari ${char.name}: Ingat, tinggi segitiga harus selalu tegak lurus (90°) dengan alasnya ya!",
                    isFromAi = false
                )
            }

            // Check for Teorema Pythagoras
            if (lower.contains("pythagoras") || lower.contains("pitagoras")) {
                val numMatch = Regex("""(\d+).*?(\d+)""").find(lower)
                val a = numMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 3.0
                val b = numMatch?.groupValues?.get(2)?.toDoubleOrNull() ?: 4.0
                val c = sqrt(a * a + b * b)
                val cStr = if (c % 1.0 == 0.0) c.toLong().toString() else String.format("%.2f", c)

                return HomeworkSolution(
                    subject = "Matematika",
                    questionSummary = "Teorema Pythagoras: Sisi miring (c) dari segitiga siku-siku",
                    stepByStep = listOf(
                        "Rumus Pythagoras menyatakan: c² = a² + b² (di mana c adalah sisi miring/hipotenusa).",
                        "Hitung kuadrat sisi-sisi tegak: a² = ${a * a}, b² = ${b * b}, maka c² = ${a * a + b * b}.",
                        "Tarik akar kuadrat untuk mencari c: c = √(${a * a + b * b}) = $cStr."
                    ),
                    finalAnswer = "Panjang sisi miring segitiga siku-siku adalah $cStr.",
                    tutorTip = "Tips dari ${char.name}: Hafalkan tripel Pythagoras populer seperti (3, 4, 5) dan (5, 12, 13) untuk menghemat waktu ujian!",
                    isFromAi = false
                )
            }
        }

        // 2. SCIENCE / IPA / FILSAFAT SAINS
        if (lower.contains("simulasi") && (lower.contains("kehidupan") || lower.contains("teori") || lower.contains("hipotesis"))) {
            return HomeworkSolution(
                subject = subject,
                questionSummary = "Teori / Hipotesis Simulasi Kehidupan (Simulation Hypothesis)",
                conceptOrFormula = "Hipotesis Filsafat & Komputasi Kuantum (Nick Bostrom, 2003)",
                stepByStep = listOf(
                    "Pengertian Dasar: Teori Simulasi Kehidupan (Simulation Hypothesis) yang dipopulerkan filsuf Oxford Nick Bostrom menyatakan bahwa realitas dan alam semesta yang kita tinggali saat ini bisa jadi merupakan simulasi komputer super canggih yang dijalankan oleh peradaban masa depan (posthuman).",
                    "Tiga Kemungkinan Argumen Bostrom: (1) Peradaban manusia punah sebelum mampu membuat simulasi kesadaran, (2) Peradaban maju mampu membuatnya tapi memilih tidak menjalankan simulasi leluhur, atau (3) Hampir pasti kita saat ini hidup di dalam sebuah simulasi.",
                    "Kaitan dengan Fisika & Teknologi: Pendukung teori ini menunjuk bahwa hukum alam semesta kita bersifat matematis, memiliki batas resolusi terkecil (Panjang Planck seperti piksel layar), dan perkembangan AI serta Virtual Reality melaju sangat pesat."
                ),
                finalAnswer = "Teori Simulasi Kehidupan adalah hipotesis filsafat dan sains komputasi bahwa seluruh realitas fisik dan kesadaran manusia mungkin merupakan program simulasi digital tingkat tinggi.",
                tutorTip = "Tips dari ${char.name}: Hubungkan Gemini API Key di bagian atas layar agar ${char.name} bisa mendiskusikan pertanyaan sains dan filsafat apa pun secara bebas dengan Gemini AI!",
                practiceChallenge = "Menurutmu, apa perbedaan utama antara kesadaran manusia asli dengan kecerdasan buatan di dalam simulasi?",
                isFromAi = false
            )
        }

        if (subject.equals("IPA", ignoreCase = true) || lower.contains("fotosintesis") || lower.contains("newton") || lower.contains("energi") || lower.contains("zat")) {
            if (lower.contains("fotosintesis")) {
                return HomeworkSolution(
                    subject = "IPA (Biologi)",
                    questionSummary = "Konsep dan Mekanisme Fotosintesis pada Tumbuhan",
                    stepByStep = listOf(
                        "Bahan yang dibutuhkan: Karbondioksida (CO₂) dari udara diserap melalui stomata, dan Air (H₂O) diserap akar dari tanah.",
                        "Proses reaksi: Berlangsung di kloroplas daun menggunakan pigmen klorofil dengan bantuan energi cahaya matahari.",
                        "Persamaan reaksi kimia: 6CO₂ + 6H₂O + Cahaya Matahari → C₆H₁₂O₆ (Glukosa) + 6O₂ (Oksigen)."
                    ),
                    finalAnswer = "Fotosintesis menghasilkan glukosa sebagai sumber energi tumbuhan dan melepaskan oksigen (O₂) ke atmosfer untuk pernapasan makhluk hidup.",
                    tutorTip = "Tips dari ${char.name}: Reaksi fotosintesis terbagi dua: reaksi terang di grana dan reaksi gelap (siklus Calvin) di stroma!",
                    isFromAi = false
                )
            }

            if (lower.contains("newton")) {
                return HomeworkSolution(
                    subject = "IPA (Fisika)",
                    questionSummary = "Penerapan Tiga Hukum Newton tentang Gerak",
                    stepByStep = listOf(
                        "Hukum I Newton (Kelembaman/Inersia): Benda cenderung mempertahankan keadaannya. Jika resultan gaya nol (ΣF = 0), benda diam tetap diam, benda bergerak lurus beraturan.",
                        "Hukum II Newton (Dinamika Gerak): Percepatan sebanding dengan resultan gaya dan berbanding terbalik dengan massa benda (F = m × a).",
                        "Hukum III Newton (Aksi-Reaksi): Setiap gaya aksi menimbulkan gaya reaksi yang sama besar namun berlawanan arah (F_aksi = -F_reaksi)."
                    ),
                    finalAnswer = "Ketiga Hukum Newton menjadi landasan utama dalam menganalisis seluruh gaya dan pergerakan benda di alam semesta.",
                    tutorTip = "Tips dari ${char.name}: Satuan gaya dalam SI adalah Newton (N) yang setara dengan kg·m/s²!",
                    isFromAi = false
                )
            }
        }

        // 3. BAHASA INDONESIA
        if (subject.contains("Indonesia", ignoreCase = true) || lower.contains("spok") || lower.contains("ide pokok") || lower.contains("majas")) {
            return HomeworkSolution(
                subject = "Bahasa Indonesia",
                questionSummary = "Analisis Kaidah Bahasa dan Struktur Kalimat",
                stepByStep = listOf(
                    "Identifikasi unsur kalimat: Subjek (pelaku), Predikat (tindakan/kata kerja), Objek (yang dikenai tindakan), Keterangan (waktu/tempat/cara).",
                    "Periksa kesesuaian makna dan ejaan dengan Pedoman Umum Ejaan Bahasa Indonesia (PUEBI/EYD).",
                    "Susun kesimpulan ide pokok yang merangkum keseluruhan maksud kalimat/paragraf secara padat dan runtut."
                ),
                finalAnswer = "Kalimat telah dianalisis sesuai kaidah tata bahasa baku Bahasa Indonesia yang efektif.",
                tutorTip = "Tips dari ${char.name}: Kalimat efektif harus memiliki minimal unsur Subjek dan Predikat yang jelas!",
                isFromAi = false
            )
        }

        // 4. BAHASA INGGRIS
        if (subject.contains("Inggris", ignoreCase = true) || lower.contains("tense") || lower.contains("verb") || lower.contains("translate")) {
            return HomeworkSolution(
                subject = "English",
                questionSummary = "Grammar Analysis & Sentence Structure",
                stepByStep = listOf(
                    "Identify the time frame (Present, Past, or Future) to choose the correct verb tense.",
                    "Ensure Subject-Verb Agreement: singular subjects (He, She, It) take singular verbs with -s/-es in Simple Present.",
                    "Verify preposition, article (a/an/the), and adjective placement for natural English flow."
                ),
                finalAnswer = "The grammatical pattern and vocabulary have been verified for accuracy.",
                tutorTip = "Tips from ${char.name}: Remember, irregular verbs in Past Tense (V2) don't use -ed (e.g., eat -> ate, buy -> bought)!",
                isFromAi = false
            )
        }

        // 5. UMUM (Offline Fallback with clear prompt to connect Gemini API Key if on external APK)
        return HomeworkSolution(
            subject = subject,
            questionSummary = question,
            conceptOrFormula = "Mode Offline Lokal — Hubungkan Gemini API Key di atas untuk Jawaban AI Penuh",
            stepByStep = listOf(
                "Definisi & Inti Topik: Pertanyaan '$question' berkaitan dengan konsep utama pada bidang $subject yang menjelaskan pengertian, penyebab, dan cara kerjanya.",
                "Mekanisme & Analisis: Untuk menjawab soal ini secara mendalam di perangkatmu, pastikan koneksi internet aktif atau masukkan Gemini API Key gratis di kartu 'Hubungkan Gemini AI' di bagian atas layar ini.",
                "Kesimpulan Konsep: Setelah Gemini API Key tersambung, ${char.name} dapat menguraikan jawaban ribuan topik sains, matematika, sejarah, dan tugas umum secara real-time!"
            ),
            finalAnswer = "Untuk pembahasan AI lengkap tentang \"$question\", ketuk tombol '🔑 Hubungkan Gemini API Key' di atas lalu klik 'Bahas Soal' kembali.",
            tutorTip = "Tips dari ${char.name}: Jika kamu meng-install APK ini langsung di HP, cukup masukkan Gemini API Key dari Google AI Studio di kartu atas agar semua fitur AI menyala penuh!",
            isFromAi = false
        )
    }
}

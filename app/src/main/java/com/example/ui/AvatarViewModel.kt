package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.ai.GeminiChatService
import com.example.ai.GeminiLiveSessionClient
import com.example.audio.ContinuousMicWakeDetector
import com.example.audio.SpeechRecognitionHelper
import com.example.audio.SpeechTtsEngine
import com.example.model.AppLanguage
import com.example.model.AvatarCustomization
import com.example.model.AvatarEmotion
import com.example.model.CharacterCatalog
import com.example.model.ChatMessage
import com.example.model.GeminiModelTier
import com.example.model.PetCharacter
import com.example.model.PetSpecies
import com.example.model.ReactionCatalog
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.TimeoutCancellationException

import com.example.model.BackgroundTheme
import com.example.model.TimeOfDay
import com.example.model.TouchZone
import com.example.model.EarStyle
import com.example.audio.VoiceEngineMode

import com.example.data.StoryAdventureRepository
import com.example.model.CareOverlayType
import com.example.model.FoodItem
import com.example.model.FunActivityTab
import com.example.model.HealthyHabitType
import com.example.model.IdlePetBehavior
import com.example.model.MemoryFact
import com.example.model.PetMemoryManager
import com.example.model.QuizQuestion
import com.example.model.RpsChoice
import com.example.model.RpsResult
import com.example.model.RpsRoundState
import com.example.model.SavedStorySession
import com.example.model.StoryItem
import com.example.model.StoryNode
import com.example.model.StoryTheme
import com.example.ui.components.RoomPropAction
import kotlinx.coroutines.isActive

enum class FaceToFaceCallState {
    CONNECTING,
    LISTENING,
    THINKING,
    SPEAKING,
    MUTED
}

enum class VoiceSessionState {
    STANDBY_WAKE,
    WAKE_RESPONDING,
    GEMINI_LIVE_LISTENING,
    GEMINI_LIVE_THINKING,
    GEMINI_LIVE_SPEAKING
}

data class AvatarUiState(
    val activeCharacter: PetCharacter = CharacterCatalog.characters.first(),
    val customization: AvatarCustomization = AvatarCustomization(),
    val messages: List<ChatMessage> = emptyList(),
    val lastAvatarSpeech: String = "",
    val isTalking: Boolean = false,
    val isListeningMic: Boolean = false,
    val showVoiceDialog: Boolean = false,
    val speechTranscript: String = "",
    val speechRms: Float = 0f,
    val speechServiceUnavailable: Boolean = false,
    val triggerSystemSpeechFallback: Boolean = false,
    val currentEmotion: AvatarEmotion = AvatarEmotion.IDLE,
    val affectionScore: Int = 20,
    val selectedLanguage: AppLanguage = AppLanguage.INDONESIAN,
    val voicePitch: Float = 1.25f,
    val speechRate: Float = 1.15f,
    val voiceEngineMode: VoiceEngineMode = VoiceEngineMode.GEMINI_FLASH_TTS,
    val isLoadingAi: Boolean = false,
    val backgroundTheme: BackgroundTheme = BackgroundTheme.LIVING_ROOM,
    val currentTimeOfDay: TimeOfDay = TimeOfDay.fromCurrentHour(),
    // Face-to-Face live hands-free conversation state
    val isFaceToFaceActive: Boolean = false,
    val faceToFaceCallState: FaceToFaceCallState = FaceToFaceCallState.CONNECTING,
    val isFaceToFaceMuted: Boolean = false,
    val liveUserSpeech: String = "",
    val liveAvatarSpeech: String = "",
    val callDurationSeconds: Int = 0,
    // Talking Tom Living Care System & Leveling
    val playerLevel: Int = 12,
    val levelProgress: Float = 0.65f, // Circular progress for LVL 12 badge
    val coins: Int = 1500, // Matches 1,500 coins capsule in screenshot
    val diamonds: Int = 240, // Matches 240 diamonds capsule in screenshot
    val hungerLevel: Float = 0.85f, // "Makan" 85%
    val toiletLevel: Float = 0.70f, // "Toilet" 70%
    val energyLevel: Float = 0.60f, // "Tidur" 60%
    val funLevel: Float = 0.90f, // "Bermain/Hiburan" 90%
    val activeCareOverlay: CareOverlayType = CareOverlayType.NONE,
    val isMimicModeActive: Boolean = false,
    val miniGameScore: Int = 0,
    val careFeedbackMessage: String = "",
    // Mode Ngerjain Tugas Sekolah
    val showHomeworkMode: Boolean = false,
    val homeworkSubject: String = "Matematika",
    val homeworkQuestion: String = "",
    val homeworkSolution: HomeworkSolution? = null,
    val isSolvingHomework: Boolean = false,
    val homeworkPhotoBitmap: android.graphics.Bitmap? = null,
    // Speak to Wake ("Petto") Continuous Hands-Free Listener via Microphone API & Gemini Live
    val isHandsFreeWakeEnabled: Boolean = true,
    val isWakeCommandListening: Boolean = false,
    val voiceSessionState: VoiceSessionState = VoiceSessionState.STANDBY_WAKE,
    val isUserSpeakingNow: Boolean = false,
    // Autonomous 3D Idle Behavior System when not in conversation
    val activeIdleBehavior: IdlePetBehavior = IdlePetBehavior.NONE,
    // 1. Long-Term Character Memory & User Profile
    val userNickname: String = "",
    val memories: List<MemoryFact> = emptyList(),
    // 2. Live Camera Vision during Video Call
    val liveCameraBitmap: android.graphics.Bitmap? = null,
    // 6. Interactive Room & 3D Props state
    val isLofiRadioPlaying: Boolean = false,
    val isNightLampDimmed: Boolean = false,
    // Fun Activities Hub (Mini-Game, Interactive Storytelling, Healthy Routines)
    val showFunActivitiesDialog: Boolean = false,
    val activeFunTab: FunActivityTab = FunActivityTab.MINI_GAME,
    val rpsRound: RpsRoundState? = null,
    val rpsWins: Int = 0,
    val rpsDraws: Int = 0,
    val rpsLosses: Int = 0,
    val currentQuizQuestion: QuizQuestion? = null,
    val selectedQuizOption: Int? = null,
    val quizScore: Int = 0,
    val isGeneratingQuiz: Boolean = false,
    val selectedStoryTheme: StoryTheme = StoryTheme.CRYSTAL_FOREST,
    val currentStoryNode: StoryNode? = null,
    val storyContextHistory: String = "",
    val isGeneratingStory: Boolean = false,
    val showStoryStageMode: Boolean = false,
    val activeStorySessionId: String = "",
    val storyInventory: List<StoryItem> = emptyList(),
    val storyHistoryLog: List<String> = emptyList(),
    val savedStorySessions: List<SavedStorySession> = emptyList(),
    val waterGlassesToday: Int = 0,
    val stretchCountToday: Int = 0,
    // Gemini Multi-Turn Model Tier & Gemini 3.8 Live Real-Time Session state
    val selectedModelTier: GeminiModelTier = GeminiModelTier.AUTO,
    val isGeminiLiveConnected: Boolean = false,
    val activeLiveModelName: String = "gemini-3.8-live"
)

data class HomeworkSolution(
    val subject: String,
    val questionSummary: String,
    val stepByStep: List<String>,
    val finalAnswer: String,
    val tutorTip: String,
    val conceptOrFormula: String = "",
    val practiceChallenge: String = "",
    val isFromAi: Boolean = true
)

class AvatarViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AvatarUiState())
    val uiState: StateFlow<AvatarUiState> = _uiState.asStateFlow()

    private val aiService = GeminiChatService(application)
    private val memoryManager = PetMemoryManager(application)
    private val storyRepository = StoryAdventureRepository(application)
    private var emotionResetJob: Job? = null
    private var callTimerJob: Job? = null
    private var autoListenJob: Job? = null
    private var wakeListenJob: Job? = null
    private var audioAnalysisJob: Job? = null
    private var idleBehaviorJob: Job? = null
    private var lastIdleBehavior: IdlePetBehavior = IdlePetBehavior.NONE
    private var rapidSpeechErrorCount: Int = 0
    private var isSpeakingWakeAck: Boolean = false
    private var isSpeakingVoiceReply: Boolean = false
    private var ttsWatchdogJob: Job? = null
    private var wakeGuardJob: Job? = null
    private var lastVoiceUnavailableNoticeMs: Long = 0L

    private val micWakeDetector = ContinuousMicWakeDetector(
        context = application,
        onRmsChanged = { rms ->
            if (!_uiState.value.isFaceToFaceActive && !_uiState.value.showVoiceDialog) {
                _uiState.value = _uiState.value.copy(speechRms = rms)
            }
        },
        onSpeechActivityChanged = { isSpeaking ->
            if (isSpeaking && _uiState.value.activeIdleBehavior != IdlePetBehavior.NONE) {
                _uiState.value = _uiState.value.copy(
                    isUserSpeakingNow = true,
                    activeIdleBehavior = IdlePetBehavior.NONE
                )
            } else {
                _uiState.value = _uiState.value.copy(isUserSpeakingNow = isSpeaking)
            }
        },
        onVoiceSegmentCaptured = { wavBytes, durationMs, _ ->
            handleCapturedMicAudioSegment(wavBytes, durationMs)
        },
        onLiveSessionSilenceTimeout = {
            // Automatically return to passive "Petto" wake-word standby after silence
            _uiState.value = _uiState.value.copy(
                voiceSessionState = VoiceSessionState.STANDBY_WAKE,
                isWakeCommandListening = false,
                isUserSpeakingNow = false,
                currentEmotion = if (_uiState.value.isTalking) _uiState.value.currentEmotion else AvatarEmotion.IDLE
            )
        }
    )

    private val geminiLiveClient = GeminiLiveSessionClient(
        context = application,
        onConnectionStateChanged = { connected, modelName ->
            _uiState.value = _uiState.value.copy(
                isGeminiLiveConnected = connected,
                activeLiveModelName = modelName.ifBlank { "gemini-3.8-live" }
            )
        },
        onSpeakingStateChanged = { isSpeaking ->
            if (isSpeaking) {
                micWakeDetector.setPaused(true)
            }
            _uiState.value = _uiState.value.copy(
                isTalking = isSpeaking,
                activeIdleBehavior = if (isSpeaking) IdlePetBehavior.NONE else _uiState.value.activeIdleBehavior,
                currentEmotion = if (isSpeaking) AvatarEmotion.TALKING else AvatarEmotion.IDLE,
                faceToFaceCallState = if (_uiState.value.isFaceToFaceActive && isSpeaking) {
                    FaceToFaceCallState.SPEAKING
                } else {
                    _uiState.value.faceToFaceCallState
                }
            )
            if (!isSpeaking) {
                if (_uiState.value.isFaceToFaceActive && !_uiState.value.isFaceToFaceMuted) {
                    onAvatarFinishedSpeakingInCall()
                } else if (_uiState.value.isHandsFreeWakeEnabled && !_uiState.value.showVoiceDialog) {
                    enterGeminiLiveListeningAfterSpeech(delayMs = 280L)
                }
            }
        },
        onLiveTranscriptUpdate = { partialText ->
            if (partialText.isNotBlank()) {
                _uiState.value = _uiState.value.copy(
                    lastAvatarSpeech = partialText,
                    liveAvatarSpeech = partialText
                )
            }
        },
        onTurnCompleted = { fullText ->
            if (fullText.isNotBlank()) {
                val state = _uiState.value
                val liveMsg = ChatMessage(
                    senderName = state.activeCharacter.name,
                    text = fullText,
                    isUser = false,
                    emotion = AvatarEmotion.TALKING,
                    modelUsed = state.activeLiveModelName.ifBlank { "gemini-3.8-live" }
                )
                _uiState.value = state.copy(
                    messages = state.messages + liveMsg,
                    lastAvatarSpeech = fullText,
                    liveAvatarSpeech = fullText,
                    isLoadingAi = false
                )
            }
        }
    )

    private val ttsEngine = SpeechTtsEngine(application) { isSpeaking ->
        // Pause continuous mic while character is speaking to prevent acoustic echo
        if (isSpeaking) {
            micWakeDetector.setPaused(true)
        }
        _uiState.value = _uiState.value.copy(
            isTalking = isSpeaking,
            activeIdleBehavior = if (isSpeaking) IdlePetBehavior.NONE else _uiState.value.activeIdleBehavior,
            currentEmotion = if (isSpeaking) AvatarEmotion.TALKING else AvatarEmotion.IDLE
        )
        if (!isSpeaking) {
            ttsWatchdogJob?.cancel()
            Log.d("AvatarViewModel", "TTS done: faceToFace=" + _uiState.value.isFaceToFaceActive + " wakeAck=" + isSpeakingWakeAck + " voiceReply=" + isSpeakingVoiceReply + " handsFree=" + _uiState.value.isHandsFreeWakeEnabled + " voiceDialog=" + _uiState.value.showVoiceDialog + " session=" + _uiState.value.voiceSessionState)
            if (_uiState.value.isFaceToFaceActive && !_uiState.value.isFaceToFaceMuted) {
                onAvatarFinishedSpeakingInCall()
            } else if (isSpeakingWakeAck) {
                isSpeakingWakeAck = false
                // Immediately enter active Gemini Live listening right after "nyaut"
                enterGeminiLiveListeningAfterSpeech(delayMs = 240L)
            } else if (isSpeakingVoiceReply) {
                isSpeakingVoiceReply = false
                // Stay in active Gemini Live session for follow-up until silence timeout
                enterGeminiLiveListeningAfterSpeech(delayMs = 280L)
            } else if (_uiState.value.isHandsFreeWakeEnabled && !_uiState.value.showVoiceDialog) {
                resumeContinuousWakeStandby(delayMs = 300L)
            }
        }
    }

    private val speechRecognitionHelper: SpeechRecognitionHelper = SpeechRecognitionHelper(
        context = application,
        onResult = { recognizedText ->
            rapidSpeechErrorCount = 0
            val clean = recognizedText.trim()
            if (clean.isNotBlank()) {
                if (_uiState.value.isFaceToFaceActive) {
                    handleFaceToFaceUserUtterance(clean)
                    return@SpeechRecognitionHelper
                }

                if (_uiState.value.showVoiceDialog) {
                    _uiState.value = _uiState.value.copy(
                        speechTranscript = clean,
                        isListeningMic = false
                    )
                    return@SpeechRecognitionHelper
                }

                // Check if user said wake phrase ("Hey Petto", "Halo Petto", character name, etc.)
                val (isWake, commandAfterWake) = parseWakePhrase(clean, _uiState.value.activeCharacter.name)
                if (isWake) {
                    if (commandAfterWake.isNotBlank()) {
                        // User said "Hey Petto <pertanyaan>" in one breath -> answer directly with voice!
                        handleDirectVoiceUtterance(commandAfterWake)
                    } else {
                        // User called "Hey Petto" -> character immediately answers ("nyaut") and listens!
                        callHeyPetto()
                    }
                    return@SpeechRecognitionHelper
                }

                // If character was actively listening after wake or user spoke directly in room
                if (_uiState.value.isWakeCommandListening || _uiState.value.isHandsFreeWakeEnabled) {
                    handleDirectVoiceUtterance(clean)
                    return@SpeechRecognitionHelper
                }
            }
            scheduleHandsFreeWakeRestart(700L)
        },
        onPartialResult = { partial ->
            if (partial.isNotBlank()) {
                rapidSpeechErrorCount = 0
                if (_uiState.value.isFaceToFaceActive) {
                    _uiState.value = _uiState.value.copy(liveUserSpeech = partial)
                } else if (_uiState.value.showVoiceDialog) {
                    _uiState.value = _uiState.value.copy(speechTranscript = partial)
                }
            }
        },
        onRmsChanged = { rms ->
            _uiState.value = _uiState.value.copy(speechRms = rms)
        },
        onListeningStateChanged = { isListening ->
            _uiState.value = _uiState.value.copy(isListeningMic = isListening)
            if (_uiState.value.isFaceToFaceActive && !_uiState.value.isFaceToFaceMuted && isListening) {
                if (_uiState.value.faceToFaceCallState != FaceToFaceCallState.THINKING &&
                    _uiState.value.faceToFaceCallState != FaceToFaceCallState.SPEAKING) {
                    _uiState.value = _uiState.value.copy(faceToFaceCallState = FaceToFaceCallState.LISTENING)
                }
            }
        },
        onServiceUnavailable = {
            wakeListenJob?.cancel()
            autoListenJob?.cancel()
            _uiState.value = _uiState.value.copy(
                speechServiceUnavailable = true,
                triggerSystemSpeechFallback = _uiState.value.showVoiceDialog,
                isListeningMic = false,
                isWakeCommandListening = false,
                isHandsFreeWakeEnabled = false
            )
        },
        onErrorCallback = { errorCode, durationMs ->
            val isSilenceTimeout = errorCode == android.speech.SpeechRecognizer.ERROR_NO_MATCH ||
                errorCode == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT

            if (isSilenceTimeout && durationMs >= 500L) {
                // Normal silence after listening window: reset active command mode to passive wake mode
                rapidSpeechErrorCount = 0
                if (_uiState.value.isWakeCommandListening) {
                    _uiState.value = _uiState.value.copy(isWakeCommandListening = false)
                }
                onSpeechErrorInCall(errorCode)
                scheduleHandsFreeWakeRestart(900L)
                return@SpeechRecognitionHelper
            }

            // Hardware/service error or instantaneous failure (< 500ms)
            if (durationMs < 500L) {
                rapidSpeechErrorCount++
            }
            val isFatalError = errorCode == android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                errorCode == android.speech.SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                errorCode == android.speech.SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE

            if (isFatalError || rapidSpeechErrorCount >= 4) {
                wakeListenJob?.cancel()
                autoListenJob?.cancel()
                _uiState.value = _uiState.value.copy(
                    isWakeCommandListening = false,
                    isHandsFreeWakeEnabled = false
                )
            } else {
                onSpeechErrorInCall(errorCode)
                scheduleHandsFreeWakeRestart(1800L)
            }
        }
    )

    private fun parseWakePhrase(rawText: String, charName: String): Pair<Boolean, String> {
        val trimmed = rawText.trim()
        val cName = charName.lowercase()
        val wakeRegex = Regex(
            pattern = """^(?:hey|hei|halo|hai|hi|oi|woi|ya)?\s*(?:petto|peto|pito|$cName)\b[,!.?\s]*(.*)$""",
            option = RegexOption.IGNORE_CASE
        )
        val match = wakeRegex.find(trimmed)
        if (match != null) {
            val remainder = match.groupValues.getOrNull(1)?.trim().orEmpty()
            return true to remainder
        }
        val lower = trimmed.lowercase()
        val prefixes = listOf("hey petto", "hei petto", "halo petto", "hai petto", "hey $cName", "halo $cName", "hai $cName")
        for (prefix in prefixes) {
            val idx = lower.indexOf(prefix)
            if (idx >= 0) {
                val remainder = trimmed.substring(idx + prefix.length).trimStart(',', '.', '!', '?', ' ')
                return true to remainder
            }
        }
        return false to trimmed
    }

    private fun enterGeminiLiveListeningAfterSpeech(delayMs: Long = 250L) {
        wakeListenJob?.cancel()
        if (_uiState.value.isFaceToFaceActive || _uiState.value.showVoiceDialog) {
            Log.d("AvatarViewModel", "enterLiveListen skipped: faceToFace=" + _uiState.value.isFaceToFaceActive + " voiceDialog=" + _uiState.value.showVoiceDialog)
            return
        }
        wakeListenJob = viewModelScope.launch {
            delay(delayMs)
            if (!_uiState.value.isTalking && !_uiState.value.isFaceToFaceActive && !_uiState.value.showVoiceDialog) {
                Log.d("AvatarViewModel", "enterLiveListen: mic unpaused, GEMINI_LIVE_LISTENING")
                _uiState.value = _uiState.value.copy(
                    voiceSessionState = VoiceSessionState.GEMINI_LIVE_LISTENING,
                    isWakeCommandListening = true
                )
                micWakeDetector.setLiveSessionMode(true)
                micWakeDetector.setPaused(false)
                micWakeDetector.start()
            }
        }
    }

    private fun resumeContinuousWakeStandby(delayMs: Long = 300L) {
        wakeListenJob?.cancel()
        if (!_uiState.value.isHandsFreeWakeEnabled ||
            _uiState.value.isFaceToFaceActive ||
            _uiState.value.isTalking ||
            isSpeakingWakeAck ||
            isSpeakingVoiceReply ||
            _uiState.value.showVoiceDialog
        ) {
            Log.d("AvatarViewModel", "resumeWake skipped: handsFree=" + _uiState.value.isHandsFreeWakeEnabled + " faceToFace=" + _uiState.value.isFaceToFaceActive + " talking=" + _uiState.value.isTalking + " wakeAck=" + isSpeakingWakeAck + " voiceReply=" + isSpeakingVoiceReply + " voiceDialog=" + _uiState.value.showVoiceDialog)
            return
        }
        wakeListenJob = viewModelScope.launch {
            delay(delayMs)
            if (!_uiState.value.isTalking && !_uiState.value.isFaceToFaceActive && !_uiState.value.showVoiceDialog) {
                Log.d("AvatarViewModel", "resumeWake: restoring STANDBY_WAKE, mic unpaused")
                _uiState.value = _uiState.value.copy(
                    voiceSessionState = VoiceSessionState.STANDBY_WAKE,
                    isWakeCommandListening = false
                )
                micWakeDetector.setLiveSessionMode(false)
                micWakeDetector.setPaused(false)
                micWakeDetector.start()
            }
        }
    }

    private fun handleCapturedMicAudioSegment(wavBytes: ByteArray, durationMs: Long) {
        if (_uiState.value.isFaceToFaceActive ||
            _uiState.value.showVoiceDialog ||
            _uiState.value.isTalking ||
            isSpeakingWakeAck ||
            isSpeakingVoiceReply
        ) {
            return
        }

        val currentSession = _uiState.value.voiceSessionState
        val isStandby = currentSession == VoiceSessionState.STANDBY_WAKE
        Log.i("AvatarViewModel", "Mic segment captured: " + durationMs + "ms (" + wavBytes.size + " bytes), session=" + currentSession)

        audioAnalysisJob?.cancel()
        audioAnalysisJob = viewModelScope.launch {
            if (!isStandby) {
                micWakeDetector.setPaused(true)
                _uiState.value = _uiState.value.copy(
                    voiceSessionState = VoiceSessionState.GEMINI_LIVE_THINKING,
                    isLoadingAi = true,
                    currentEmotion = AvatarEmotion.SURPRISED
                )
            }

            try {
                val state = _uiState.value
                val result = aiService.analyzeVoiceAudioSegment(
                    wavBytes = wavBytes,
                    durationMs = durationMs,
                    isStandbyWakeMode = isStandby,
                    character = state.activeCharacter,
                    language = state.selectedLanguage,
                    history = state.messages
                )
                Log.i("AvatarViewModel", "Voice result: wake=" + result.wakeDetected + ", wakeOnly=" + result.isWakeOnly + ", offline=" + result.isOfflineFallback + ", transcript=" + result.transcript + ", replyLen=" + result.replyText.length)

                if (result.isOfflineFallback) {
                    notifyVoiceUnavailable()
                    _uiState.value = _uiState.value.copy(isLoadingAi = false)
                    if (isStandby) {
                        micWakeDetector.setLiveSessionMode(false)
                    } else {
                        _uiState.value = _uiState.value.copy(voiceSessionState = VoiceSessionState.GEMINI_LIVE_LISTENING)
                        micWakeDetector.setLiveSessionMode(true)
                    }
                    micWakeDetector.setPaused(false)
                    return@launch
                }

                if (isStandby) {
                    if (result.wakeDetected) {
                        if (result.isWakeOnly || result.replyText.isBlank()) {
                            // User said "Petto" / "Hey Petto" -> chime, answer briefly, and enter Gemini Live session
                            callHeyPetto()
                        } else {
                            // User said "Petto <pertanyaan>" in one breath -> answer directly in Gemini Live session
                            deliverGeminiLiveVoiceReply(
                                userTranscript = result.transcript.ifBlank { "Petto" },
                                replyText = result.replyText,
                                emotion = result.emotion
                            )
                        }
                    } else {
                        // Not wake word -> stay in passive standby
                        micWakeDetector.setLiveSessionMode(false)
                        micWakeDetector.setPaused(false)
                    }
                } else {
                    // Active Gemini Live session
                    if (result.wakeDetected && result.replyText.isNotBlank()) {
                        deliverGeminiLiveVoiceReply(
                            userTranscript = result.transcript.ifBlank { "(Suara)" },
                            replyText = result.replyText,
                            emotion = result.emotion
                        )
                    } else {
                        // Unclear/ambient sound during live session -> resume listening until silence timeout
                        _uiState.value = _uiState.value.copy(
                            voiceSessionState = VoiceSessionState.GEMINI_LIVE_LISTENING,
                            isLoadingAi = false
                        )
                        micWakeDetector.setLiveSessionMode(true)
                        micWakeDetector.setPaused(false)
                    }
                }
            } catch (e: Exception) {
                Log.w("AvatarViewModel", "Audio segment analysis error: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoadingAi = false)
                if (!isStandby) {
                    _uiState.value = _uiState.value.copy(voiceSessionState = VoiceSessionState.GEMINI_LIVE_LISTENING)
                    micWakeDetector.setLiveSessionMode(true)
                }
                micWakeDetector.setPaused(false)
            }
        }
    }

    private fun deliverGeminiLiveVoiceReply(
        userTranscript: String,
        replyText: String,
        emotion: AvatarEmotion
    ) {
        val state = _uiState.value
        val userMsg = ChatMessage(senderName = "Kamu", text = userTranscript, isUser = true)
        val avatarMsg = ChatMessage(
            senderName = state.activeCharacter.name,
            text = replyText,
            isUser = false,
            emotion = emotion,
            modelUsed = state.activeLiveModelName.ifBlank { "gemini-3.8-live" }
        )

        isSpeakingVoiceReply = true
        micWakeDetector.setPaused(true)
        _uiState.value = state.copy(
            messages = state.messages + userMsg + avatarMsg,
            liveUserSpeech = userTranscript,
            lastAvatarSpeech = replyText,
            isLoadingAi = false,
            isWakeCommandListening = true,
            voiceSessionState = VoiceSessionState.GEMINI_LIVE_SPEAKING,
            affectionScore = (state.affectionScore + 3).coerceAtMost(100),
            userNickname = memoryManager.getUserNickname(),
            memories = memoryManager.getMemories()
        )
        speakVoiceOnly(replyText, emotion)
    }

    private fun startActiveWakeCommandListening(delayMs: Long = 250L) {
        enterGeminiLiveListeningAfterSpeech(delayMs)
    }

    private fun scheduleHandsFreeWakeRestart(delayMs: Long) {
        resumeContinuousWakeStandby(delayMs)
    }

    fun toggleHandsFreeWake() {
        val newState = !_uiState.value.isHandsFreeWakeEnabled
        rapidSpeechErrorCount = 0
        wakeListenJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isHandsFreeWakeEnabled = newState,
            isWakeCommandListening = false,
            voiceSessionState = VoiceSessionState.STANDBY_WAKE
        )
        if (newState) {
            startHandsFreeWakeListening()
        } else {
            micWakeDetector.stop()
            speechRecognitionHelper.stopListening()
        }
    }

    fun triggerDirectVoiceListen() {
        callHeyPetto()
    }

    fun startHandsFreeWakeListening() {
        if (!_uiState.value.isHandsFreeWakeEnabled ||
            _uiState.value.isFaceToFaceActive ||
            _uiState.value.showVoiceDialog
        ) {
            return
        }
        try {
            val isCurrentlyTalking = _uiState.value.isTalking || isSpeakingWakeAck || isSpeakingVoiceReply
            Log.d("AvatarViewModel", "startHandsFreeWake: talking=" + isCurrentlyTalking + " session=" + _uiState.value.voiceSessionState + " paused=" + micWakeDetector.isPaused)
            micWakeDetector.setLiveSessionMode(
                _uiState.value.voiceSessionState == VoiceSessionState.GEMINI_LIVE_LISTENING
            )
            micWakeDetector.setPaused(isCurrentlyTalking)
            micWakeDetector.start()
        } catch (e: Exception) {
            Log.w("AvatarViewModel", "Continuous mic wake start failed: ${e.message}")
        }
    }

    private fun onSpeechErrorInCall(errorCode: Int) {
        if (_uiState.value.isFaceToFaceActive && !_uiState.value.isFaceToFaceMuted && rapidSpeechErrorCount < 4) {
            if (errorCode == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                errorCode == android.speech.SpeechRecognizer.ERROR_NO_MATCH) {
                autoListenJob?.cancel()
                autoListenJob = viewModelScope.launch {
                    delay(1200)
                    if (_uiState.value.isFaceToFaceActive &&
                        !_uiState.value.isFaceToFaceMuted &&
                        !_uiState.value.isTalking &&
                        _uiState.value.faceToFaceCallState != FaceToFaceCallState.THINKING) {
                        _uiState.value = _uiState.value.copy(faceToFaceCallState = FaceToFaceCallState.LISTENING)
                        speechRecognitionHelper.startListening(_uiState.value.selectedLanguage)
                    }
                }
            }
        }
    }

    /**
     * Self-healing heartbeat: if a missed TTS callback (or any guard) leaves the mic paused while idle
     * in standby, force it back on. Without this, one missed callback silences the app until restart.
     */
    private fun startWakeGuardLoop() {
        wakeGuardJob?.cancel()
        wakeGuardJob = viewModelScope.launch {
            while (isActive) {
                delay(2000L)
                val s = _uiState.value
                if (s.isHandsFreeWakeEnabled && !s.isFaceToFaceActive && !s.isTalking &&
                    !isSpeakingWakeAck && !isSpeakingVoiceReply && !s.showVoiceDialog &&
                    s.voiceSessionState == VoiceSessionState.STANDBY_WAKE
                ) {
                    if (micWakeDetector.isPaused) {
                        Log.w("AvatarViewModel", "Wake guard: mic was paused in standby -> resuming")
                        micWakeDetector.setPaused(false)
                    }
                    micWakeDetector.start()
                }
            }
        }
    }

    private val prefs by lazy {
        application.getSharedPreferences("pawpal_ai_prefs", android.content.Context.MODE_PRIVATE)
    }

    init {
        val initialChar = _uiState.value.activeCharacter
        val initialMode = getVoiceEngineMode()

        _uiState.value = _uiState.value.copy(
            voicePitch = initialChar.defaultVoicePitch,
            speechRate = initialChar.defaultSpeechRate,
            voiceEngineMode = initialMode,
            customization = _uiState.value.customization.copy(furColor = initialChar.defaultFurColor),
            userNickname = memoryManager.getUserNickname(),
            memories = memoryManager.getMemories(),
            savedStorySessions = storyRepository.getSavedSessions(),
            waterGlassesToday = prefs.getInt("water_glasses_today", 0),
            stretchCountToday = prefs.getInt("stretch_count_today", 0)
        )

        ttsEngine.engineMode = initialMode
        ttsEngine.elevenLabsApiKey = getElevenLabsApiKey()
        ttsEngine.googleApiKey = getGoogleCloudTtsApiKey()
        ttsEngine.setVoiceProfile(initialChar.defaultVoicePitch, initialChar.defaultSpeechRate)
        ttsEngine.setLanguage(_uiState.value.selectedLanguage)

        startWakeGuardLoop()

        viewModelScope.launch {
            delay(500)
            deliverGreeting(initialChar, _uiState.value.selectedLanguage)
        }

        startIdleBehaviorLoop()
    }

    /**
     * Periodically triggers lifelike 3D idle animations (stretching, looking around, sneezing,
     * yawning, ear scratching, happy bounce) when the pet is not in an active conversation.
     */
    private fun startIdleBehaviorLoop() {
        idleBehaviorJob?.cancel()
        idleBehaviorJob = viewModelScope.launch {
            while (isActive) {
                val waitTimeMs = (6500..11000).random().toLong()
                delay(waitTimeMs)

                val s = _uiState.value
                val isCompletelyIdle = !s.isTalking &&
                    !s.isLoadingAi &&
                    !s.isFaceToFaceActive &&
                    !s.showHomeworkMode &&
                    !s.showVoiceDialog &&
                    s.voiceSessionState == VoiceSessionState.STANDBY_WAKE &&
                    !s.isUserSpeakingNow &&
                    !isSpeakingWakeAck &&
                    !isSpeakingVoiceReply

                if (isCompletelyIdle) {
                    val candidates = IdlePetBehavior.activeBehaviors.filter { it != lastIdleBehavior }
                    val picked = candidates.randomOrNull() ?: IdlePetBehavior.LOOKING_AROUND
                    lastIdleBehavior = picked

                    val idleEmotion = when (picked) {
                        IdlePetBehavior.STRETCHING -> AvatarEmotion.HAPPY
                        IdlePetBehavior.LOOKING_AROUND -> AvatarEmotion.IDLE
                        IdlePetBehavior.SNEEZING -> AvatarEmotion.SURPRISED
                        IdlePetBehavior.YAWNING -> AvatarEmotion.SLEEPY
                        IdlePetBehavior.EAR_SCRATCH -> AvatarEmotion.HAPPY
                        IdlePetBehavior.HAPPY_BOUNCE -> AvatarEmotion.HAPPY
                        IdlePetBehavior.NONE -> AvatarEmotion.IDLE
                    }

                    _uiState.value = _uiState.value.copy(
                        activeIdleBehavior = picked,
                        currentEmotion = idleEmotion
                    )

                    // Duration of the 3D idle behavior choreography
                    delay(2600L)

                    if (_uiState.value.activeIdleBehavior == picked && !_uiState.value.isTalking) {
                        _uiState.value = _uiState.value.copy(
                            activeIdleBehavior = IdlePetBehavior.NONE,
                            currentEmotion = AvatarEmotion.IDLE
                        )
                    }
                }
            }
        }
    }

    private fun deliverGreeting(character: PetCharacter, language: AppLanguage) {
        val timeOfDay = TimeOfDay.fromCurrentHour()
        val nickname = memoryManager.getUserNickname()
        val nameSuffix = if (nickname.isNotBlank()) " $nickname" else ""
        val timeWord = when (timeOfDay) {
            TimeOfDay.DAWN -> "pagi"
            TimeOfDay.DAY -> "siang"
            TimeOfDay.SUNSET -> "sore"
            TimeOfDay.NIGHT -> "malam"
        }
        val greetingText = when (character.id) {
            "zayn" -> "Cih! Selamat $timeWord$nameSuffix! Aku Zayn. B-bukan berarti aku seneng ditemenin kamu $timeWord ini ya! Jangan lupa minum air putih!"
            "siti" -> "Selamat $timeWord sayangku manis$nameSuffix, nama Siti. Siti sangat bersyukur bisa mendampingimu $timeWord ini. Ceritakan harimu ya sayang."
            "rizky" -> "Siap! Selamat $timeWord$nameSuffix! Rizky siap bertugas! Pertahankan disiplin, kesehatan, dan fokus pada target kita!"
            "jamal" -> "Hoaaammm... Selamat $timeWord bro$nameSuffix. Jamal lagi santuy banget nih, enak nih kalau kita main atau dengerin cerita bareng."
            else -> "HEH! Selamat $timeWord$nameSuffix! Gua Heru! Jangan lupa jaga kesehatan lu hari ini kalau gak mau kena semprot!"
        }

        speakVoiceOnly(greetingText, AvatarEmotion.HAPPY)
    }

    /**
     * "Hey Google" style Wake Word response ("Hey Petto" / "Halo Petto"):
     * Character immediately chimes, answers briefly with voice,
     * and transitions directly into active listening for the user's spoken question.
     */
    fun callHeyPetto() {
        Log.i("AvatarViewModel", "callHeyPetto triggered (session=" + _uiState.value.voiceSessionState + ")")
        wakeListenJob?.cancel()
        audioAnalysisJob?.cancel()
        micWakeDetector.setPaused(true)
        speechRecognitionHelper.stopListening()
        val char = _uiState.value.activeCharacter
        val shortWakeAck = when (char.id) {
            "zayn" -> "Ya, ada apa? Cepetan ngomong, aku dengerin!"
            "siti" -> "Iya sayang? Siti dengerin, mau cerita apa?"
            "rizky" -> "Siap hadir! Silakan bicara, aku mendengarkan!"
            "jamal" -> "Yo bro, kenapa? Ngomong aja, Jamal dengerin."
            else -> "Apa?! Ngomong aja, gua dengerin!"
        }

        isSpeakingWakeAck = true
        _uiState.value = _uiState.value.copy(
            currentEmotion = AvatarEmotion.SURPRISED,
            activeIdleBehavior = IdlePetBehavior.NONE,
            isWakeCommandListening = true,
            voiceSessionState = VoiceSessionState.WAKE_RESPONDING,
            liveUserSpeech = "Petto!",
            lastAvatarSpeech = shortWakeAck,
            showVoiceDialog = false
        )
        ttsEngine.playHappyChime()
        speakVoiceOnly(shortWakeAck, AvatarEmotion.SURPRISED)

        // Fallback guard in case TTS engine finishes early or is not yet initialized
        viewModelScope.launch {
            delay(2200)
            if (isSpeakingWakeAck && !_uiState.value.isTalking) {
                isSpeakingWakeAck = false
                enterGeminiLiveListeningAfterSpeech(delayMs = 100L)
            }
        }
    }

    /**
     * Handles user's spoken utterance directly like a voice assistant ("Hey Google"):
     * Processes speech with AI and replies purely via voice without creating text bubbles.
     */
    private fun handleDirectVoiceUtterance(spokenText: String) {
        val clean = spokenText.trim()
        if (clean.isBlank()) return

        wakeListenJob?.cancel()
        speechRecognitionHelper.stopListening()
        val state = _uiState.value
        val userMsg = ChatMessage(senderName = "Kamu", text = clean, isUser = true)
        val updatedHistory = state.messages + userMsg

        _uiState.value = state.copy(
            messages = updatedHistory,
            liveUserSpeech = clean,
            isLoadingAi = true,
            isWakeCommandListening = false,
            isListeningMic = false,
            currentEmotion = AvatarEmotion.SURPRISED
        )

        viewModelScope.launch {
            try {
                val reply = withTimeoutOrNull(8500L) {
                    aiService.generateReply(
                        userMessage = clean,
                        character = state.activeCharacter,
                        language = state.selectedLanguage,
                        history = updatedHistory,
                        preferredTier = state.selectedModelTier
                    )
                } ?: aiService.getFastFallbackReply(
                    userMessage = clean,
                    character = state.activeCharacter,
                    language = state.selectedLanguage
                )

                val avatarReplyMsg = ChatMessage(
                    senderName = state.activeCharacter.name,
                    text = reply.text,
                    isUser = false,
                    emotion = reply.emotion,
                    modelUsed = "gemini-3.8-live"
                )

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + avatarReplyMsg,
                    lastAvatarSpeech = reply.text,
                    isLoadingAi = false,
                    affectionScore = (state.affectionScore + 3).coerceAtMost(100),
                    userNickname = memoryManager.getUserNickname(),
                    memories = memoryManager.getMemories()
                )

                isSpeakingVoiceReply = true
                speakVoiceOnly(reply.text, reply.emotion)
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Error in handleDirectVoiceUtterance: ${e.message}", e)
                val fallbackReply = aiService.getFastFallbackReply(
                    userMessage = clean,
                    character = state.activeCharacter,
                    language = state.selectedLanguage
                )
                _uiState.value = _uiState.value.copy(
                    lastAvatarSpeech = fallbackReply.text,
                    isLoadingAi = false
                )
                isSpeakingVoiceReply = true
                speakVoiceOnly(fallbackReply.text, fallbackReply.emotion)
            }
        }
    }

    /**
     * Speaks voice audio, shows the conversation bubble, and animates character lip-sync/emotion.
     */
    fun speakVoiceOnly(text: String, emotion: AvatarEmotion = AvatarEmotion.TALKING) {
        if (text.isBlank()) return
        wakeListenJob?.cancel()
        micWakeDetector.setPaused(true)
        speechRecognitionHelper.stopListening()

        _uiState.value = _uiState.value.copy(
            lastAvatarSpeech = text,
            currentEmotion = emotion
        )

        ttsEngine.speak(
            text = text,
            pitch = _uiState.value.voicePitch,
            rate = _uiState.value.speechRate,
            character = _uiState.value.activeCharacter
        )
        armTtsWatchdog(text)

        emotionResetJob?.cancel()
        emotionResetJob = viewModelScope.launch {
            delay(3500)
            if (_uiState.value.currentEmotion != AvatarEmotion.IDLE && !_uiState.value.isTalking) {
                _uiState.value = _uiState.value.copy(currentEmotion = AvatarEmotion.IDLE)
            }
        }
    }

    /**
     * Safety net: if the neural TTS engine never reports "finished" (network drop, audio focus loss),
     * isSpeakingWakeAck / isSpeakingVoiceReply / isTalking would stay true forever and the mic would never resume.
     */
    private fun armTtsWatchdog(text: String) {
        ttsWatchdogJob?.cancel()
        val timeoutMs = (text.length * 120L + 8_000L).coerceIn(12_000L, 45_000L)
        ttsWatchdogJob = viewModelScope.launch {
            delay(timeoutMs)
            if (isSpeakingWakeAck || isSpeakingVoiceReply || _uiState.value.isTalking) {
                Log.w("AvatarViewModel", "TTS watchdog fired after " + timeoutMs + "ms -> forcing mic resume")
                isSpeakingWakeAck = false
                isSpeakingVoiceReply = false
                _uiState.value = _uiState.value.copy(isTalking = false)
                if (!_uiState.value.isFaceToFaceActive) {
                    if (_uiState.value.voiceSessionState == VoiceSessionState.STANDBY_WAKE) {
                        resumeContinuousWakeStandby(delayMs = 200L)
                    } else {
                        enterGeminiLiveListeningAfterSpeech(delayMs = 200L)
                    }
                }
            }
        }
    }

    private fun notifyVoiceUnavailable() {
        val now = System.currentTimeMillis()
        if (now - lastVoiceUnavailableNoticeMs < 30_000L) return
        lastVoiceUnavailableNoticeMs = now
        val char = _uiState.value.activeCharacter
        val notice = ChatMessage(
            senderName = char.name,
            text = "(Suara belum bisa kupahami: koneksi Gemini API gagal. Cek API key atau internet, ya.)",
            isUser = false,
            emotion = AvatarEmotion.SURPRISED
        )
        _uiState.value = _uiState.value.copy(messages = _uiState.value.messages + notice)
    }

    fun updateSpeechAndAutoSpeak(text: String, emotion: AvatarEmotion = AvatarEmotion.TALKING) {
        if (text.isBlank()) return
        wakeListenJob?.cancel()
        micWakeDetector.setPaused(true)
        speechRecognitionHelper.stopListening()

        _uiState.value = _uiState.value.copy(
            lastAvatarSpeech = text,
            currentEmotion = emotion
        )

        ttsEngine.speak(
            text = text,
            pitch = _uiState.value.voicePitch,
            rate = _uiState.value.speechRate,
            character = _uiState.value.activeCharacter
        )

        emotionResetJob?.cancel()
        emotionResetJob = viewModelScope.launch {
            delay(3500)
            if (_uiState.value.currentEmotion != AvatarEmotion.IDLE && !_uiState.value.isTalking) {
                _uiState.value = _uiState.value.copy(currentEmotion = AvatarEmotion.IDLE)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val state = _uiState.value

        // Check wake word in text
        val lower = text.lowercase()
        if (lower.contains("hey petto") || lower.contains("halo petto") || lower.contains("hai petto")) {
            val userMsg = ChatMessage(senderName = "Kamu", text = text, isUser = true)
            val wakeReply = ReactionCatalog.getWakeWordResponse(state.activeCharacter, state.selectedLanguage)
            val avatarReplyMsg = ChatMessage(senderName = state.activeCharacter.name, text = wakeReply, isUser = false, emotion = AvatarEmotion.SURPRISED)
            _uiState.value = state.copy(
                messages = state.messages + userMsg + avatarReplyMsg,
                affectionScore = (state.affectionScore + 3).coerceAtMost(100)
            )
            updateSpeechAndAutoSpeak(wakeReply, AvatarEmotion.SURPRISED)
            return
        }

        val userMsg = ChatMessage(
            senderName = "Kamu",
            text = text,
            isUser = true
        )

        val updatedMessages = state.messages + userMsg
        _uiState.value = state.copy(
            messages = updatedMessages,
            liveUserSpeech = text,
            isLoadingAi = true,
            currentEmotion = AvatarEmotion.TALKING
        )

        viewModelScope.launch {
            try {
                // Generates AI reply using selected or auto-routed Gemini model tier (gemini-4-argon, gemini-3.8-flash, gemini-3.1-flash-lite)
                val reply = withTimeoutOrNull(11000L) {
                    aiService.generateReply(
                        userMessage = text,
                        character = state.activeCharacter,
                        language = state.selectedLanguage,
                        history = updatedMessages,
                        preferredTier = state.selectedModelTier
                    )
                } ?: aiService.getFastFallbackReply(
                    userMessage = text,
                    character = state.activeCharacter,
                    language = state.selectedLanguage
                )

                val avatarReplyMsg = ChatMessage(
                    senderName = state.activeCharacter.name,
                    text = reply.text,
                    isUser = false,
                    emotion = reply.emotion,
                    modelUsed = reply.modelUsed
                )

                val newAffection = (state.affectionScore + 3).coerceAtMost(100)

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + avatarReplyMsg,
                    isLoadingAi = false,
                    affectionScore = newAffection,
                    userNickname = memoryManager.getUserNickname(),
                    memories = memoryManager.getMemories()
                )

                updateSpeechAndAutoSpeak(reply.text, reply.emotion)
            } catch (e: TimeoutCancellationException) {
                Log.d("AvatarViewModel", "AI response timeout: ${e.message}, using fallback")
                val fallbackReply = aiService.getFastFallbackReply(
                    userMessage = text,
                    character = state.activeCharacter,
                    language = state.selectedLanguage
                )
                val avatarReplyMsg = ChatMessage(
                    senderName = state.activeCharacter.name,
                    text = fallbackReply.text,
                    isUser = false,
                    emotion = fallbackReply.emotion,
                    modelUsed = fallbackReply.modelUsed
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + avatarReplyMsg,
                    isLoadingAi = false
                )
                updateSpeechAndAutoSpeak(fallbackReply.text, fallbackReply.emotion)
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Error in sendMessage: ${e.message}", e)
                val fallbackReply = aiService.getFastFallbackReply(
                    userMessage = text,
                    character = state.activeCharacter,
                    language = state.selectedLanguage
                )
                val avatarReplyMsg = ChatMessage(
                    senderName = state.activeCharacter.name,
                    text = fallbackReply.text,
                    isUser = false,
                    emotion = fallbackReply.emotion,
                    modelUsed = fallbackReply.modelUsed
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + avatarReplyMsg,
                    isLoadingAi = false
                )
                updateSpeechAndAutoSpeak(fallbackReply.text, fallbackReply.emotion)
            } finally {
                _uiState.value = _uiState.value.copy(isLoadingAi = false)
            }
        }
    }

    fun setModelTier(tier: GeminiModelTier) {
        _uiState.value = _uiState.value.copy(selectedModelTier = tier)
    }

    fun onCharacterTouched(zone: TouchZone) {
        val state = _uiState.value
        val newAffection = (state.affectionScore + 3).coerceAtMost(100)

        // Trigger expressive procedural cartoon touch reaction sound effect (giggle, boing, pop, purr, grunt)
        ttsEngine.playTouchSound(state.activeCharacter.species, zone)

        val reactionText = ReactionCatalog.getTouchZoneReaction(state.activeCharacter, zone, state.selectedLanguage)
        val emotion = when (zone) {
            TouchZone.HEAD -> AvatarEmotion.HAPPY
            TouchZone.BELLY -> AvatarEmotion.LOVING
            TouchZone.HANDS -> AvatarEmotion.TALKING
            TouchZone.FEET -> AvatarEmotion.SURPRISED
        }
        _uiState.value = state.copy(
            affectionScore = newAffection,
            activeIdleBehavior = IdlePetBehavior.NONE
        )
        speakVoiceOnly(reactionText, emotion)
    }

    fun petAvatar() {
        onCharacterTouched(TouchZone.HEAD)
    }

    fun setBackgroundTheme(theme: BackgroundTheme) {
        val oldTheme = _uiState.value.backgroundTheme
        _uiState.value = _uiState.value.copy(backgroundTheme = theme)
        if (oldTheme != theme) {
            val char = _uiState.value.activeCharacter
            val comment = when (theme) {
                BackgroundTheme.LIVING_ROOM -> when (char.id) {
                    "zayn" -> "Sofa ruang tamu ini empuk juga sih. B-bukan berarti aku mau mager di sini ya!"
                    "siti" -> "Ruang tamu yang ceria dan penuh mainan sayang, Siti senang bermain di sini bersamamu."
                    "rizky" -> "Pangkalan ruang tamu siap patroli! Kondisi aman terkendali!"
                    "jamal" -> "Nah, sofa ruang tamu emang spot rebahan paling santuy bro."
                    else -> "SOFA KARTUNNYA KEREN JUGA! Awas lu jangan berantakin mainan di lantai!"
                }
                BackgroundTheme.BEDROOM -> when (char.id) {
                    "zayn" -> "Kamar tidur? Cih, ngantuk ya tidur aja sana! B-bukan berarti aku mau nemenin kamu ya!"
                    "siti" -> "Selamat datang di kamar tidur yang hangat manisku, semoga istirahatmu ditemani mimpi indah."
                    "rizky" -> "Waktu istirahat taktis di kamar tidur! Pulihkan tenaga untuk misi berikutnya!"
                    "jamal" -> "HOAAAMMM! Ini ruangan favorit Jamal sedunia! Jangan bangunin gua ya, mau tidur."
                    else -> "KASUR NYAMAN BANGET! Lu mau tidur sekarang hah?! Awas lu ngorok gua lempar bantal!"
                }
                BackgroundTheme.KITCHEN -> when (char.id) {
                    "zayn" -> "Dapur? Hah?! Mau bikinin makanan buat aku?! B-bukan berarti aku laper ya!"
                    "siti" -> "Dapur yang cantik sayang! Siti mau buatkan camilan lezat dan susu hangat untukmu."
                    "rizky" -> "Stasiun logistik nutrisi terkonfirmasi! Asupan energi siap dikonsumsi!"
                    "jamal" -> "KULKAS! Buka kulkasnya bro, ambilin ikan sama susu dingin dong buat Jamal."
                    else -> "DAPUR! MANA MAKANAN GUA?! Beruang laper nih, cepetan buka kulkasnya lu!"
                }
                else -> "${char.name} menyukai suasana ruangan ${theme.displayName}!"
            }
            speakVoiceOnly(comment, AvatarEmotion.HAPPY)
        }
    }

    fun cycleBackgroundTheme() {
        val themes = BackgroundTheme.values()
        val currentIndex = themes.indexOf(_uiState.value.backgroundTheme)
        val nextTheme = themes[(currentIndex + 1) % themes.size]
        setBackgroundTheme(nextTheme)
    }

    fun openVoiceInput() {
        wakeListenJob?.cancel()
        micWakeDetector.setPaused(true)
        rapidSpeechErrorCount = 0
        _uiState.value = _uiState.value.copy(
            showVoiceDialog = true,
            speechTranscript = "",
            speechServiceUnavailable = false,
            triggerSystemSpeechFallback = false
        )
        speechRecognitionHelper.startListening(_uiState.value.selectedLanguage)
    }

    fun onSystemSpeechFallbackConsumed() {
        _uiState.value = _uiState.value.copy(triggerSystemSpeechFallback = false)
    }

    fun closeVoiceInput() {
        wakeListenJob?.cancel()
        speechRecognitionHelper.stopListening()
        _uiState.value = _uiState.value.copy(
            showVoiceDialog = false,
            speechTranscript = "",
            isListeningMic = false,
            triggerSystemSpeechFallback = false
        )
        resumeContinuousWakeStandby(350L)
    }

    fun onFallbackSpeechResult(text: String) {
        if (text.isNotBlank()) {
            _uiState.value = _uiState.value.copy(
                speechTranscript = text,
                showVoiceDialog = true
            )
        }
    }

    fun sendVoiceTranscript(text: String) {
        val wasStoryStage = _uiState.value.showStoryStageMode
        val wasHomework = _uiState.value.showHomeworkMode
        closeVoiceInput()
        if (wasStoryStage && _uiState.value.currentStoryNode != null) {
            chooseStoryBranch(text)
        } else if (wasHomework) {
            setHomeworkQuestion(text)
        } else {
            sendMessage(text)
        }
    }

    fun replayCurrentSpeech() {
        val speech = _uiState.value.lastAvatarSpeech
        if (speech.isNotBlank()) {
            ttsEngine.speak(
                text = speech,
                pitch = _uiState.value.voicePitch,
                rate = _uiState.value.speechRate,
                character = _uiState.value.activeCharacter
            )
        }
    }

    fun speakText(text: String) {
        updateSpeechAndAutoSpeak(text, AvatarEmotion.TALKING)
    }

    fun updateCustomization(newCustomization: AvatarCustomization) {
        val old = _uiState.value.customization
        val char = _uiState.value.activeCharacter
        val lang = _uiState.value.selectedLanguage
        _uiState.value = _uiState.value.copy(customization = newCustomization)

        val reactionText = when {
            newCustomization.costume != old.costume ->
                ReactionCatalog.getCostumeReaction(newCustomization.costume, char, lang)
            newCustomization.accessory != old.accessory ->
                ReactionCatalog.getAccessoryReaction(newCustomization.accessory, char, lang)
            newCustomization.furColorId != old.furColorId ->
                ReactionCatalog.getFurColorReaction(newCustomization.furColorId, char, lang)
            newCustomization.roomTheme != old.roomTheme ->
                when (char.id) {
                    "zayn" -> "Hmph! Ruangan ${newCustomization.roomTheme.displayName}? Baguslah, lumayan nyaman."
                    "siti" -> "Ruangan ${newCustomization.roomTheme.displayName} ini sangat indah dan damai sayang."
                    "rizky" -> "Lokasi pangkalan ${newCustomization.roomTheme.displayName} telah siap beroperasi!"
                    "jamal" -> "Ruangan baru? Yang penting kasurnya empuk buat rebahan."
                    else -> "Ruangan ${newCustomization.roomTheme.displayName}? Jangan sampai berantakan, awas lu!"
                }
            else -> ""
        }

        if (reactionText.isNotBlank()) {
            speakVoiceOnly(reactionText, AvatarEmotion.HAPPY)
        }
    }

    fun selectCharacter(character: PetCharacter) {
        ttsEngine.stop()
        ttsEngine.setVoiceProfile(character.defaultVoicePitch, character.defaultSpeechRate)

        _uiState.value = _uiState.value.copy(
            activeCharacter = character,
            voicePitch = character.defaultVoicePitch,
            speechRate = character.defaultSpeechRate,
            customization = _uiState.value.customization.copy(
                furColor = character.defaultFurColor,
                earStyle = if (character.species == PetSpecies.DOG) com.example.model.EarStyle.DOG_FLOPPY else com.example.model.EarStyle.CAT_POINTY
            )
        )

        deliverGreeting(character, _uiState.value.selectedLanguage)
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
        ttsEngine.setLanguage(language)
        deliverGreeting(_uiState.value.activeCharacter, language)
    }

    fun setVoicePitch(pitch: Float) {
        _uiState.value = _uiState.value.copy(voicePitch = pitch)
        ttsEngine.setVoiceProfile(pitch, _uiState.value.speechRate)
    }

    fun setSpeechRate(rate: Float) {
        _uiState.value = _uiState.value.copy(speechRate = rate)
        ttsEngine.setVoiceProfile(_uiState.value.voicePitch, rate)
    }

    fun testVoice() {
        val testPhrase = when (_uiState.value.selectedLanguage) {
            AppLanguage.INDONESIAN -> "Halo! Suaraku terdengar bagus dan jernih, kan?"
            AppLanguage.ENGLISH -> "Hello! My voice sounds crisp and friendly, right?"
            AppLanguage.JAPANESE -> "こんにちは！私の声は聞こえますか？"
            AppLanguage.SPANISH -> "¡Hola! Mi voz se escucha muy bien, ¿verdad?"
        }
        updateSpeechAndAutoSpeak(testPhrase, AvatarEmotion.HAPPY)
    }

    fun clearHistory() {
        _uiState.value = _uiState.value.copy(messages = emptyList())
    }

    fun getCustomApiKey(): String = aiService.getCustomApiKey()
    fun setCustomApiKey(key: String) {
        aiService.setCustomApiKey(key)
        ttsEngine.googleApiKey = getGoogleCloudTtsApiKey()
    }
    fun isGeminiConnected(): Boolean = aiService.isGeminiConnected()

    fun getVoiceEngineMode(): VoiceEngineMode {
        val name = prefs.getString("voice_engine_mode", VoiceEngineMode.GEMINI_FLASH_TTS.name)
        return try {
            VoiceEngineMode.valueOf(name ?: VoiceEngineMode.GEMINI_FLASH_TTS.name)
        } catch (e: Exception) {
            VoiceEngineMode.GEMINI_FLASH_TTS
        }
    }

    fun setVoiceEngineMode(mode: VoiceEngineMode) {
        prefs.edit().putString("voice_engine_mode", mode.name).apply()
        _uiState.value = _uiState.value.copy(voiceEngineMode = mode)
        ttsEngine.engineMode = mode
    }

    fun getElevenLabsApiKey(): String = prefs.getString("elevenlabs_api_key", "") ?: ""
    fun setElevenLabsApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("elevenlabs_api_key", trimmed).apply()
        ttsEngine.elevenLabsApiKey = trimmed
    }

    fun getGoogleCloudTtsApiKey(): String {
        val custom = prefs.getString("google_cloud_tts_api_key", "") ?: ""
        if (custom.isNotBlank()) return custom
        return aiService.getEffectiveApiKey()
    }

    fun setGoogleCloudTtsApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("google_cloud_tts_api_key", trimmed).apply()
        ttsEngine.googleApiKey = if (trimmed.isNotBlank()) trimmed else aiService.getEffectiveApiKey()
    }

    fun startFaceToFaceCall() {
        ttsEngine.stop()
        micWakeDetector.setPaused(true)
        speechRecognitionHelper.stopListening()
        wakeListenJob?.cancel()
        autoListenJob?.cancel()
        callTimerJob?.cancel()
        audioAnalysisJob?.cancel()
        rapidSpeechErrorCount = 0
        isSpeakingWakeAck = false
        isSpeakingVoiceReply = false

        val char = _uiState.value.activeCharacter
        val openingLine = when (char.id) {
            "zayn" -> "Hah?! Ngapain kamu video call ke aku?! B-bukan berarti aku seneng ya! Mau ngomong apa, cepetan?!"
            "siti" -> "Halo sayangku manis, Siti senang sekali bisa tatap muka langsung sama kamu. Ada cerita apa hari ini sayang?"
            "rizky" -> "Panggilan tatap muka terhubung! Sersan Rizky siap bertugas dan mendengarkan laporanmu. Silakan bicara!"
            "jamal" -> "Hoaaam... Video call segala bro, Jamal lagi pw rebahan nih. Yaudah mau ngobrolin apa santai aja..."
            else -> "HEH! Nelpon-nelpon gua lu! Awas aja kalau gak penting gua cakar! Ngomong sekarang lu mau apa?!"
        }

        _uiState.value = _uiState.value.copy(
            isFaceToFaceActive = true,
            faceToFaceCallState = FaceToFaceCallState.SPEAKING,
            isFaceToFaceMuted = false,
            liveUserSpeech = "",
            liveAvatarSpeech = openingLine,
            callDurationSeconds = 0,
            showVoiceDialog = false,
            isWakeCommandListening = false,
            voiceSessionState = VoiceSessionState.STANDBY_WAKE,
            lastAvatarSpeech = "",
            currentEmotion = AvatarEmotion.HAPPY
        )

        // Start call duration timer
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_uiState.value.isFaceToFaceActive) {
                    _uiState.value = _uiState.value.copy(callDurationSeconds = _uiState.value.callDurationSeconds + 1)
                } else {
                    break
                }
            }
        }

        // Connect real-time Gemini 3.8 Live WebSocket session if API key is available
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank()) {
            val liveSysPrompt = aiService.buildLiveSystemInstruction(
                character = char,
                language = _uiState.value.selectedLanguage
            )
            geminiLiveClient.connect(
                character = char,
                language = _uiState.value.selectedLanguage,
                systemInstruction = liveSysPrompt
            )
        }

        // Deliver spoken opening line
        ttsEngine.playHappyChime()
        ttsEngine.speak(
            text = openingLine,
            pitch = _uiState.value.voicePitch,
            rate = _uiState.value.speechRate,
            character = char
        )
    }

    fun endFaceToFaceCall() {
        callTimerJob?.cancel()
        autoListenJob?.cancel()
        wakeListenJob?.cancel()
        geminiLiveClient.disconnect()
        ttsEngine.stop()
        speechRecognitionHelper.stopListening()

        _uiState.value = _uiState.value.copy(
            isFaceToFaceActive = false,
            faceToFaceCallState = FaceToFaceCallState.CONNECTING,
            liveUserSpeech = "",
            liveAvatarSpeech = "",
            liveCameraBitmap = null,
            isListeningMic = false,
            isGeminiLiveConnected = false,
            voiceSessionState = VoiceSessionState.STANDBY_WAKE
        )
        resumeContinuousWakeStandby(500L)
    }

    fun updateLiveCameraFrame(bitmap: android.graphics.Bitmap?) {
        _uiState.value = _uiState.value.copy(liveCameraBitmap = bitmap)
    }

    fun askCameraVisionInCall() {
        val prompt = "Lihat aku dari kamera sekarang! Apa yang sedang kamu lihat di kameraku dan gimana penampilanku?"
        handleFaceToFaceUserUtterance(prompt)
    }

    fun toggleFaceToFaceMute() {
        val newMuted = !_uiState.value.isFaceToFaceMuted
        rapidSpeechErrorCount = 0
        _uiState.value = _uiState.value.copy(
            isFaceToFaceMuted = newMuted,
            faceToFaceCallState = if (newMuted) FaceToFaceCallState.MUTED else FaceToFaceCallState.LISTENING
        )
        if (newMuted) {
            speechRecognitionHelper.stopListening()
            autoListenJob?.cancel()
        } else {
            if (!_uiState.value.isTalking) {
                speechRecognitionHelper.startListening(_uiState.value.selectedLanguage)
            }
        }
    }

    fun interruptOrSpeak() {
        autoListenJob?.cancel()
        rapidSpeechErrorCount = 0
        if (_uiState.value.isTalking) {
            geminiLiveClient.interruptPlayback()
            ttsEngine.stop()
        }
        _uiState.value = _uiState.value.copy(
            isFaceToFaceMuted = false,
            faceToFaceCallState = FaceToFaceCallState.LISTENING
        )
        speechRecognitionHelper.startListening(_uiState.value.selectedLanguage)
    }

    fun handleFaceToFaceUserUtterance(text: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return

        autoListenJob?.cancel()
        val state = _uiState.value

        val userMsg = ChatMessage(senderName = "Kamu", text = cleanText, isUser = true)
        val updatedMessages = state.messages + userMsg

        _uiState.value = state.copy(
            liveUserSpeech = cleanText,
            messages = updatedMessages,
            faceToFaceCallState = FaceToFaceCallState.THINKING,
            currentEmotion = AvatarEmotion.TALKING,
            isListeningMic = false
        )

        // If Gemini 3.8 Live bidirectional WebSocket is active, stream turn directly for real-time audio!
        if (geminiLiveClient.isSetupComplete.get()) {
            geminiLiveClient.sendUserTurn(cleanText, state.liveCameraBitmap)
            return
        }

        viewModelScope.launch {
            try {
                val reply = withTimeoutOrNull(9500L) {
                    aiService.generateReply(
                        userMessage = cleanText,
                        character = state.activeCharacter,
                        language = state.selectedLanguage,
                        history = updatedMessages,
                        cameraBitmap = state.liveCameraBitmap,
                        preferredTier = state.selectedModelTier
                    )
                } ?: aiService.getFastFallbackReply(
                    userMessage = cleanText,
                    character = state.activeCharacter,
                    language = state.selectedLanguage
                )

                val avatarReplyMsg = ChatMessage(
                    senderName = state.activeCharacter.name,
                    text = reply.text,
                    isUser = false,
                    emotion = reply.emotion,
                    modelUsed = "gemini-3.8-live"
                )

                val newAffection = (state.affectionScore + 3).coerceAtMost(100)

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + avatarReplyMsg,
                    liveAvatarSpeech = reply.text,
                    lastAvatarSpeech = reply.text,
                    currentEmotion = reply.emotion,
                    faceToFaceCallState = FaceToFaceCallState.SPEAKING,
                    affectionScore = newAffection,
                    userNickname = memoryManager.getUserNickname(),
                    memories = memoryManager.getMemories()
                )

                ttsEngine.speak(
                    text = reply.text,
                    pitch = _uiState.value.voicePitch,
                    rate = _uiState.value.speechRate,
                    character = _uiState.value.activeCharacter
                )
            } catch (e: TimeoutCancellationException) {
                Log.d("AvatarViewModel", "Face-to-face AI timeout: ${e.message}, using fallback")
                val fallbackReply = aiService.getFastFallbackReply(
                    userMessage = cleanText,
                    character = state.activeCharacter,
                    language = state.selectedLanguage
                )
                _uiState.value = _uiState.value.copy(
                    liveAvatarSpeech = fallbackReply.text,
                    lastAvatarSpeech = fallbackReply.text,
                    currentEmotion = fallbackReply.emotion,
                    faceToFaceCallState = FaceToFaceCallState.SPEAKING
                )
                ttsEngine.speak(
                    text = fallbackReply.text,
                    pitch = _uiState.value.voicePitch,
                    rate = _uiState.value.speechRate,
                    character = _uiState.value.activeCharacter
                )
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Error in handleFaceToFaceUserUtterance: ${e.message}", e)
                val fallbackReply = aiService.getFastFallbackReply(
                    userMessage = cleanText,
                    character = state.activeCharacter,
                    language = state.selectedLanguage
                )
                _uiState.value = _uiState.value.copy(
                    liveAvatarSpeech = fallbackReply.text,
                    lastAvatarSpeech = fallbackReply.text,
                    currentEmotion = fallbackReply.emotion,
                    faceToFaceCallState = FaceToFaceCallState.SPEAKING
                )
                ttsEngine.speak(
                    text = fallbackReply.text,
                    pitch = _uiState.value.voicePitch,
                    rate = _uiState.value.speechRate,
                    character = _uiState.value.activeCharacter
                )
            }
        }
    }

    fun quickPromptFaceToFace(promptText: String) {
        handleFaceToFaceUserUtterance(promptText)
    }

    fun switchCharacterInFaceToFace(character: PetCharacter) {
        ttsEngine.stop()
        speechRecognitionHelper.stopListening()
        autoListenJob?.cancel()

        ttsEngine.setVoiceProfile(character.defaultVoicePitch, character.defaultSpeechRate)

        val greeting = when (character.id) {
            "zayn" -> "Hmph! Sekarang kamu ngobrol sama Zayn! Jangan mikir aneh-aneh ya!"
            "siti" -> "Halo sayang, Siti hadir di sini untukmu. Siti siap mendengarkan."
            "rizky" -> "Pergantian personel! Sersan Rizky mengambil alih komunikasi!"
            "jamal" -> "Yoo, sekarang giliran Jamal. Santai aja ya ngobrolnya..."
            else -> "SEKARANG GUA HERU YANG HADEPIN LU! Mau ngomong apa lu?!"
        }

        _uiState.value = _uiState.value.copy(
            activeCharacter = character,
            voicePitch = character.defaultVoicePitch,
            speechRate = character.defaultSpeechRate,
            liveAvatarSpeech = greeting,
            lastAvatarSpeech = greeting,
            faceToFaceCallState = FaceToFaceCallState.SPEAKING,
            customization = _uiState.value.customization.copy(
                furColor = character.defaultFurColor,
                earStyle = if (character.species == PetSpecies.DOG) com.example.model.EarStyle.DOG_FLOPPY else com.example.model.EarStyle.CAT_POINTY
            )
        )

        ttsEngine.speak(
            text = greeting,
            pitch = character.defaultVoicePitch,
            rate = character.defaultSpeechRate,
            character = character
        )
    }

    private fun onAvatarFinishedSpeakingInCall() {
        autoListenJob?.cancel()
        autoListenJob = viewModelScope.launch {
            delay(350)
            if (_uiState.value.isFaceToFaceActive &&
                !_uiState.value.isFaceToFaceMuted &&
                !_uiState.value.isTalking &&
                _uiState.value.faceToFaceCallState != FaceToFaceCallState.THINKING) {
                _uiState.value = _uiState.value.copy(faceToFaceCallState = FaceToFaceCallState.LISTENING)
                speechRecognitionHelper.startListening(_uiState.value.selectedLanguage)
            }
        }
    }

    // ========================================================
    // TALKING TOM LIVING CARE SYSTEMS (MAKAN, TOILET, TIDUR, HIBURAN)
    // ========================================================

    fun setCareOverlay(overlay: CareOverlayType) {
        _uiState.value = _uiState.value.copy(activeCareOverlay = overlay)
    }

    fun closeCareOverlay() {
        _uiState.value = _uiState.value.copy(activeCareOverlay = CareOverlayType.NONE)
    }

    fun feedPet(food: FoodItem) {
        val newHunger = (_uiState.value.hungerLevel + food.hungerRestore).coerceAtMost(1.0f)
        val newProgress = (_uiState.value.levelProgress + 0.12f)
        val newLevel = if (newProgress >= 1f) _uiState.value.playerLevel + 1 else _uiState.value.playerLevel
        val finalProgress = if (newProgress >= 1f) newProgress - 1f else newProgress
        val newCoins = (_uiState.value.coins - food.coinCost).coerceAtLeast(0)

        val foodLine = when (food) {
            FoodItem.STRAWBERRY -> "Mmm nyam! Strawberry manis ini segar banget!"
            FoodItem.CHEESE -> "Nyam nyam keju! Gurih banget, favorit Tom!"
            FoodItem.FISH -> "Wah ikan segar! Ini makanan paling lezat di dunia!"
            FoodItem.MILK -> "Glek glek glek... Segarnya susu hangat!"
            FoodItem.BURGER -> "Waaah burger mini! Kenyang dan mantap banget!"
            FoodItem.CAKE -> "Manisnya kue ini bikin Tom berenergi lagi!"
        }

        _uiState.value = _uiState.value.copy(
            hungerLevel = newHunger,
            levelProgress = finalProgress,
            playerLevel = newLevel,
            coins = newCoins,
            currentEmotion = AvatarEmotion.TALKING,
            lastAvatarSpeech = foodLine,
            careFeedbackMessage = "+${(food.hungerRestore * 100).toInt()}% Kenyang! 😋"
        )

        speakText(foodLine)
    }

    fun useToilet() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                activeCareOverlay = CareOverlayType.TOILET_ROOM,
                currentEmotion = AvatarEmotion.SURPRISED,
                lastAvatarSpeech = "Sebentar ya, Tom mau ke toilet dulu...",
                careFeedbackMessage = "Menuju toilet... 🚽"
            )
            speakText("Sebentar ya, Tom mau ke toilet dulu...")
            delay(2200)

            val newProgress = (_uiState.value.levelProgress + 0.15f)
            val newLevel = if (newProgress >= 1f) _uiState.value.playerLevel + 1 else _uiState.value.playerLevel
            val finalProgress = if (newProgress >= 1f) newProgress - 1f else newProgress

            _uiState.value = _uiState.value.copy(
                toiletLevel = 1.0f,
                levelProgress = finalProgress,
                playerLevel = newLevel,
                coins = _uiState.value.coins + 15,
                currentEmotion = AvatarEmotion.HAPPY,
                lastAvatarSpeech = "Aaaah lega dan bersih banget! Sekarang Tom siap beraktivitas lagi!",
                careFeedbackMessage = "Toilet Bersih 100%! +15 Koin ✨"
            )
            speakText("Aaaah lega dan bersih banget! Sekarang Tom siap beraktivitas lagi!")
            delay(2000)
            closeCareOverlay()
        }
    }

    fun toggleSleep() {
        if (_uiState.value.activeCareOverlay == CareOverlayType.SLEEP_MODE) {
            // Wake up
            val newProgress = (_uiState.value.levelProgress + 0.20f)
            val newLevel = if (newProgress >= 1f) _uiState.value.playerLevel + 1 else _uiState.value.playerLevel
            val finalProgress = if (newProgress >= 1f) newProgress - 1f else newProgress

            _uiState.value = _uiState.value.copy(
                activeCareOverlay = CareOverlayType.NONE,
                energyLevel = 1.0f,
                levelProgress = finalProgress,
                playerLevel = newLevel,
                currentEmotion = AvatarEmotion.HAPPY,
                lastAvatarSpeech = "Hoaaaam! Bangun tidur segar banget! Ayo kita main lagi!",
                careFeedbackMessage = "Energi Pulih 100%! ⚡"
            )
            speakText("Hoaaaam! Bangun tidur segar banget! Ayo kita main lagi!")
        } else {
            // Go to sleep
            _uiState.value = _uiState.value.copy(
                activeCareOverlay = CareOverlayType.SLEEP_MODE,
                currentEmotion = AvatarEmotion.IDLE,
                lastAvatarSpeech = "Hoaaam... Tom ngantuk banget. Tarik saklar lampunya ya biar gelap...",
                careFeedbackMessage = "Mode Tidur Nyenyak 🌙 Zzz..."
            )
            speakText("Hoaaam... Tom ngantuk banget. Selamat tidur...")
        }
    }

    fun playBallBounce() {
        val newFun = (_uiState.value.funLevel + 0.10f).coerceAtMost(1.0f)
        val newScore = _uiState.value.miniGameScore + 1
        val newCoins = _uiState.value.coins + 10
        val newProgress = (_uiState.value.levelProgress + 0.05f)
        val newLevel = if (newProgress >= 1f) _uiState.value.playerLevel + 1 else _uiState.value.playerLevel
        val finalProgress = if (newProgress >= 1f) newProgress - 1f else newProgress

        _uiState.value = _uiState.value.copy(
            funLevel = newFun,
            miniGameScore = newScore,
            coins = newCoins,
            levelProgress = finalProgress,
            playerLevel = newLevel,
            currentEmotion = AvatarEmotion.HAPPY,
            careFeedbackMessage = "Mantap! Pantulan ke-$newScore (+10 Koin) ⚽"
        )
    }

    fun toggleMimicMode() {
        val newMode = !_uiState.value.isMimicModeActive
        _uiState.value = _uiState.value.copy(
            isMimicModeActive = newMode,
            lastAvatarSpeech = if (newMode) "Mode Tiru Suara Aktif! Bicaralah, Tom akan menirumu!" else "Mode Tiru Suara Mati."
        )
        if (newMode) {
            speakText("Mode Tiru Suara Aktif! Bicaralah, Tom akan menirumu!")
        }
    }

    fun openHomeworkMode() {
        val char = _uiState.value.activeCharacter
        val greeting = when (char.id) {
            "zayn" -> "Hmph... Ada tugas sekolah apa yang bikin kamu pusing? Sini aku bantu jelasin... bukan berarti aku peduli ya!"
            "siti" -> "Halo sayangku! Siti siap menemani belajar dan ngerjain tugas sekolah. Mau bahas soal apa hari ini manis?"
            "rizky" -> "Siap komando belajar! Tunjukkan soal tugas sekolah yang harus kita selesaikan sekarang, laksanakan!"
            "jamal" -> "Hoaaammm... Ngerjain PR bareng Jamal santuy aja bro. Santai tapi pasti beres... Soal apa nih?"
            else -> "HEH! Buka buku PR lu! Mau nanya tugas apa ke beruang pintar kayak gua?! Cepetan sini!"
        }
        _uiState.value = _uiState.value.copy(
            showHomeworkMode = true,
            currentEmotion = AvatarEmotion.HAPPY,
            lastAvatarSpeech = greeting
        )
        speakText(greeting)
    }

    fun closeHomeworkMode() {
        _uiState.value = _uiState.value.copy(showHomeworkMode = false)
    }

    fun setHomeworkSubject(subject: String) {
        _uiState.value = _uiState.value.copy(homeworkSubject = subject)
    }

    fun setHomeworkQuestion(question: String) {
        _uiState.value = _uiState.value.copy(homeworkQuestion = question)
    }

    fun setHomeworkPhoto(bitmap: android.graphics.Bitmap?) {
        _uiState.value = _uiState.value.copy(homeworkPhotoBitmap = bitmap)
    }

    fun solveHomework(customQuestion: String? = null) {
        val rawQ = customQuestion ?: _uiState.value.homeworkQuestion
        val photo = _uiState.value.homeworkPhotoBitmap
        val q = if (rawQ.isBlank() && photo != null) {
            "Tolong analisis dan jelaskan langkah penyelesaian soal tugas yang ada pada foto ini secara lengkap dan benar."
        } else {
            rawQ
        }
        val subject = _uiState.value.homeworkSubject
        val char = _uiState.value.activeCharacter
        val lang = _uiState.value.selectedLanguage

        if (q.isBlank() && photo == null) return

        _uiState.value = _uiState.value.copy(isSolvingHomework = true)

        viewModelScope.launch {
            try {
                // Resolves via Live Gemini AI (Firebase AI Logic or Direct Gemini API Key) with realistic 30s timeout
                val solution = withTimeoutOrNull(30000L) {
                    aiService.generateHomeworkHelp(subject, q, char, lang, photo)
                } ?: aiService.solveHomeworkDeterministically(subject, q, char)

                _uiState.value = _uiState.value.copy(
                    isSolvingHomework = false,
                    homeworkSolution = solution,
                    currentEmotion = AvatarEmotion.TALKING
                )
                val spokenSummary = "${char.name} sudah menemukan solusinya! ${solution.finalAnswer}. Simak langkah-langkahnya ya!"
                speakText(spokenSummary)
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Error solving homework: ${e.message}", e)
                val fallbackSolution = aiService.solveHomeworkDeterministically(subject, q, char)
                _uiState.value = _uiState.value.copy(
                    isSolvingHomework = false,
                    homeworkSolution = fallbackSolution,
                    currentEmotion = AvatarEmotion.TALKING
                )
                speakText("${char.name} sudah siapkan pembahasan lengkapnya. Yuk disimak!")
            } finally {
                _uiState.value = _uiState.value.copy(isSolvingHomework = false)
            }
        }
    }

    // ========================================================
    // 1. LONG-TERM CHARACTER MEMORY & USER PROFILE MANAGEMENT
    // ========================================================

    fun saveUserNickname(nickname: String) {
        memoryManager.setUserNickname(nickname)
        val clean = nickname.trim()
        if (clean.isNotBlank()) {
            memoryManager.addOrUpdateFact("Nama Panggilan", "Nama panggilan user adalah $clean")
        }
        _uiState.value = _uiState.value.copy(
            userNickname = memoryManager.getUserNickname(),
            memories = memoryManager.getMemories()
        )
        if (clean.isNotBlank()) {
            val char = _uiState.value.activeCharacter
            val ack = when (char.id) {
                "zayn" -> "Hmph! Oke $clean, aku bakal inget namamu! B-bukan berarti namamu keren ya!"
                "siti" -> "Baik sayangku $clean, Siti akan selalu mengingat nama indahmu."
                "rizky" -> "Siap! Identitas panggilan '$clean' telah tercatat dalam memori komando!"
                "jamal" -> "Sip bro $clean, gampang diinget tuh sambil rebahan."
                else -> "Oke $clean! Awas aja kalau lu lupa nama gua Heru!"
            }
            speakVoiceOnly(ack, AvatarEmotion.HAPPY)
        }
    }

    fun addMemoryFact(category: String, content: String) {
        memoryManager.addOrUpdateFact(category, content)
        _uiState.value = _uiState.value.copy(
            userNickname = memoryManager.getUserNickname(),
            memories = memoryManager.getMemories()
        )
        ttsEngine.playHappyChime()
    }

    fun deleteMemoryFact(factId: String) {
        memoryManager.removeFact(factId)
        _uiState.value = _uiState.value.copy(
            userNickname = memoryManager.getUserNickname(),
            memories = memoryManager.getMemories()
        )
    }

    fun clearAllMemories() {
        memoryManager.clearAllMemories()
        _uiState.value = _uiState.value.copy(
            userNickname = "",
            memories = emptyList()
        )
    }

    // ========================================================
    // 6. INTERACTIVE 3D ROOM & PROPS HANDLER
    // ========================================================

    fun onRoomPropTriggered(action: RoomPropAction, food: FoodItem? = null) {
        val state = _uiState.value
        val char = state.activeCharacter

        when (action) {
            RoomPropAction.TOGGLE_LOFI_RADIO -> {
                val newPlaying = !state.isLofiRadioPlaying
                ttsEngine.playHappyChime()
                val line = if (newPlaying) {
                    when (char.id) {
                        "zayn" -> "Hmph, musik Lo-Fi di ruang tamu ini lumayan enak juga buat santai..."
                        "siti" -> "Wah, alunan musik radio Lo-Fi ini bikin suasana ruang tamu makin hangat sayang!"
                        "rizky" -> "Frekuensi radio Lo-Fi diaktifkan! Ritme santai untuk pemulihan fokus!"
                        "jamal" -> "Nah gitu dong, nyalain musik Lo-Fi biar rebahan makin syahdu bro~"
                        else -> "Wih musik apaan nih?! Lumayan asik juga ketukannya buat goyang dikit!"
                    }
                } else {
                    "Radio Lo-Fi dimatikan dulu ya, sekarang ruangan tenang kembali."
                }
                _uiState.value = state.copy(
                    isLofiRadioPlaying = newPlaying,
                    activeIdleBehavior = if (newPlaying) IdlePetBehavior.HAPPY_BOUNCE else IdlePetBehavior.NONE,
                    currentEmotion = AvatarEmotion.HAPPY,
                    liveUserSpeech = if (newPlaying) "📻 Nyalakan Radio Lo-Fi" else "📻 Matikan Radio Lo-Fi"
                )
                speakVoiceOnly(line, AvatarEmotion.HAPPY)
            }

            RoomPropAction.BOUNCE_TOY_BALL -> {
                ttsEngine.playTouchSound(char.species, TouchZone.HANDS)
                val line = when (char.id) {
                    "zayn" -> "Hup! Tangkap bolanya! Jangan remehkan refleks kucingku ya!"
                    "siti" -> "Yeeay! Seru sekali main lempar bola sama kamu sayang!"
                    "rizky" -> "Target bola berhasil ditangkap dengan presisi tinggi! Latihan refleks mantap!"
                    "jamal" -> "Hup... duh bro, main bolanya jangan jauh-jauh ya, Jamal mager lari."
                    else -> "HAHAHA! Sini lempar bolanya yang kenceng biar gua tangkep!"
                }
                _uiState.value = state.copy(
                    activeIdleBehavior = IdlePetBehavior.HAPPY_BOUNCE,
                    currentEmotion = AvatarEmotion.HAPPY,
                    miniGameScore = state.miniGameScore + 1,
                    affectionScore = (state.affectionScore + 4).coerceAtMost(100),
                    liveUserSpeech = "⚽ Main Bola Mainan"
                )
                speakVoiceOnly(line, AvatarEmotion.HAPPY)
            }

            RoomPropAction.TOGGLE_NIGHT_LAMP -> {
                val newDimmed = !state.isNightLampDimmed
                ttsEngine.playHappyChime()
                val line = if (newDimmed) {
                    when (char.id) {
                        "zayn" -> "Lampunya diredupkan ya... Hoaaam, jadi agak ngantuk nih."
                        "siti" -> "Cahaya lampu tidur bintangnya teduh sekali sayang, nyaman buat istirahat."
                        "rizky" -> "Mode pencahayaan malam aktif! Bersiap untuk istirahat berkualitas."
                        "jamal" -> "Hoaaammm... Nah gelap-gelap adem gini baru surga dunia buat tidur..."
                        else -> "Gelap juga ya! Awas lu jangan nakut-nakutin gua pas lampu redup!"
                    }
                } else {
                    "Lampu kamar dinyalakan terang lagi! Ayo semangat beraktivitas!"
                }
                _uiState.value = state.copy(
                    isNightLampDimmed = newDimmed,
                    activeIdleBehavior = if (newDimmed) IdlePetBehavior.YAWNING else IdlePetBehavior.STRETCHING,
                    currentEmotion = if (newDimmed) AvatarEmotion.SLEEPY else AvatarEmotion.HAPPY,
                    liveUserSpeech = if (newDimmed) "🌙 Redupkan Lampu Tidur" else "☀️ Nyalakan Lampu Kamar"
                )
                speakVoiceOnly(line, if (newDimmed) AvatarEmotion.SLEEPY else AvatarEmotion.HAPPY)
            }

            RoomPropAction.HUG_STAR_PLUSH -> {
                ttsEngine.playTouchSound(char.species, TouchZone.BELLY)
                val line = when (char.id) {
                    "zayn" -> "B-boneka bintang ini empuk banget... Jangan liatin aku meluk boneka ya!"
                    "siti" -> "Awww boneka bintangnya lembut sekali sayang, rasanya kayak dipeluk kamu!"
                    "rizky" -> "Bantal taktis bentuk bintang ini sangat ergonomis untuk istirahat!"
                    "jamal" -> "Bantal bintang empuk... pas banget buat ganjel kepala Jamal..."
                    else -> "Hmph! Boneka bintang ini punya gua sekarang, empuk juga!"
                }
                _uiState.value = state.copy(
                    activeIdleBehavior = IdlePetBehavior.STRETCHING,
                    currentEmotion = AvatarEmotion.LOVING,
                    affectionScore = (state.affectionScore + 5).coerceAtMost(100),
                    liveUserSpeech = "🧸 Peluk Boneka Bintang"
                )
                speakVoiceOnly(line, AvatarEmotion.LOVING)
            }

            RoomPropAction.SERVE_KITCHEN_SNACK -> {
                val chosenFood = food ?: FoodItem.STRAWBERRY
                ttsEngine.playHappyChime()
                val line = when (char.id) {
                    "zayn" -> "Nyam nyam... ${chosenFood.displayName} ini enak juga! Makasih ya... dikit!"
                    "siti" -> "Mmm lezatnya ${chosenFood.displayName} ini sayang! Terima kasih sudah menyuapi Siti!"
                    "rizky" -> "Asupan nutrisi ${chosenFood.displayName} diterima! Energi tempur meningkat 100%!"
                    "jamal" -> "Nyam... Mantap bro, makan ${chosenFood.displayName} tanpa perlu bangun dari rebahan."
                    else -> "NYAM NYAM! Enak banget ${chosenFood.displayName}-nya! Tambah lagi dong!"
                }
                _uiState.value = state.copy(
                    activeIdleBehavior = IdlePetBehavior.HAPPY_BOUNCE,
                    currentEmotion = AvatarEmotion.HAPPY,
                    hungerLevel = (state.hungerLevel + chosenFood.hungerRestore).coerceAtMost(1f),
                    affectionScore = (state.affectionScore + 5).coerceAtMost(100),
                    liveUserSpeech = "${chosenFood.emoji} Sajikan ${chosenFood.displayName}"
                )
                speakVoiceOnly(line, AvatarEmotion.HAPPY)
            }

            RoomPropAction.BREW_WARM_TEA -> {
                ttsEngine.playHappyChime()
                val line = when (char.id) {
                    "zayn" -> "Slurp... Teh hangatnya wangi juga. Bikin tenggorokan enak!"
                    "siti" -> "Slurp~ Teh hangat buatanmu menenangkan hati sekali sayangku."
                    "rizky" -> "Seduhan teh hangat siap! Suhu optimal untuk menyegarkan pikiran!"
                    "jamal" -> "Slurp... Aaaah seger banget teh angetnya bro..."
                    else -> "Slurp! Awas panas! Tapi enak juga teh bikinan lu!"
                }
                _uiState.value = state.copy(
                    activeIdleBehavior = IdlePetBehavior.STRETCHING,
                    currentEmotion = AvatarEmotion.LOVING,
                    liveUserSpeech = "🍵 Seduh Teh Hangat"
                )
                speakVoiceOnly(line, AvatarEmotion.LOVING)
            }

            RoomPropAction.BLOW_BUBBLES -> {
                ttsEngine.playHappyChime()
                val line = "Waaah lihat gelembung sabunnya melayang warna-warni! Seru banget!"
                _uiState.value = state.copy(
                    activeIdleBehavior = IdlePetBehavior.HAPPY_BOUNCE,
                    currentEmotion = AvatarEmotion.HAPPY,
                    liveUserSpeech = "🫧 Tiup Gelembung"
                )
                speakVoiceOnly(line, AvatarEmotion.HAPPY)
            }
        }
    }

    // ========================================================
    // FUN ACTIVITIES HUB (MINI-GAME, STORYTELLING, HEALTHY ROUTINES)
    // ========================================================

    fun openFunActivities(tab: FunActivityTab = _uiState.value.activeFunTab) {
        _uiState.value = _uiState.value.copy(
            showFunActivitiesDialog = true,
            activeFunTab = tab,
            currentTimeOfDay = TimeOfDay.fromCurrentHour()
        )
    }

    fun closeFunActivities() {
        _uiState.value = _uiState.value.copy(showFunActivitiesDialog = false)
    }

    fun selectFunTab(tab: FunActivityTab) {
        _uiState.value = _uiState.value.copy(activeFunTab = tab)
    }

    fun playRockPaperScissors(userChoice: RpsChoice) {
        val state = _uiState.value
        val char = state.activeCharacter
        val petChoice = RpsChoice.entries.random()

        val result = when {
            userChoice == petChoice -> RpsResult.DRAW
            (userChoice == RpsChoice.ROCK && petChoice == RpsChoice.SCISSORS) ||
                (userChoice == RpsChoice.SCISSORS && petChoice == RpsChoice.PAPER) ||
                (userChoice == RpsChoice.PAPER && petChoice == RpsChoice.ROCK) -> RpsResult.WIN
            else -> RpsResult.LOSE
        }

        val commentary = when (result) {
            RpsResult.WIN -> when (char.id) {
                "zayn" -> "H-hah?! Kok kamu bisa menang lawan aku?! Tadi aku cuma sengaja ngalah dikit ya!"
                "siti" -> "Yeeay! Kamu menang sayangku! Hebat sekali tebakanmu, Siti ikut senang!"
                "rizky" -> "Strategi lawan yang luar biasa! Kemenangan mutlak untukmu, komandan!"
                "jamal" -> "Wah lu menang bro... Jamal telat mikir tadi gara-gara ngantuk, hehe."
                else -> "APA?! GUA KALAH SUIT?! Pasti lu curang nih, ayo tanding ulang sekarang!"
            }
            RpsResult.DRAW -> when (char.id) {
                "zayn" -> "Cih, seri! Pikiran kita kebetulan aja sama, jangan geer!"
                "siti" -> "Wah kita seri sayang! Hati kita sehati banget ya, hihi."
                "rizky" -> "Kekuatan seimbang! Pertahanan dan serangan sama kuat, ayo ulangi!"
                "jamal" -> "Seri bro~ Emang kita kompak banget kalau soal santai gini."
                else -> "Hmph, seri! Ayo suit sekali lagi sampai ada yang menang!"
            }
            RpsResult.LOSE -> when (char.id) {
                "zayn" -> "Hehehe, aku menang! Sudah kuduga kamu bakal ngeluarin ${userChoice.label}!"
                "siti" -> "Hihi, kali ini Siti yang menang sayang. Jangan menyerah ya, ayo coba lagi!"
                "rizky" -> "Poin taktis untuk Sersan Rizky! Jangan lengah, ayo coba ronde berikutnya!"
                "jamal" -> "Wih Jamal menang sambil merem nih bro, hoki banget hari ini."
                else -> "HAHAHA! GUA MENANG TELAK! Makanya jangan remehin beruang pintar kayak gua!"
            }
        }

        ttsEngine.playHappyChime()
        _uiState.value = state.copy(
            rpsRound = RpsRoundState(
                userChoice = userChoice,
                petChoice = petChoice,
                result = result,
                commentary = commentary
            ),
            rpsWins = state.rpsWins + if (result == RpsResult.WIN) 1 else 0,
            rpsDraws = state.rpsDraws + if (result == RpsResult.DRAW) 1 else 0,
            rpsLosses = state.rpsLosses + if (result == RpsResult.LOSE) 1 else 0,
            activeIdleBehavior = IdlePetBehavior.HAPPY_BOUNCE,
            currentEmotion = if (result == RpsResult.LOSE) AvatarEmotion.SURPRISED else AvatarEmotion.HAPPY,
            liveUserSpeech = "Suit: ${userChoice.emoji} ${userChoice.label}"
        )
        speakVoiceOnly(commentary, AvatarEmotion.HAPPY)
    }

    fun generateNewQuiz(topic: String) {
        val state = _uiState.value
        val char = state.activeCharacter
        _uiState.value = state.copy(
            isGeneratingQuiz = true,
            selectedQuizOption = null
        )

        viewModelScope.launch {
            try {
                val quiz = aiService.generateQuizQuestion(topic, char)
                _uiState.value = _uiState.value.copy(
                    currentQuizQuestion = quiz,
                    selectedQuizOption = null,
                    isGeneratingQuiz = false,
                    currentEmotion = AvatarEmotion.TALKING
                )
                speakVoiceOnly("Pertanyaan kuis $topic: ${quiz.question}", AvatarEmotion.TALKING)
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Error generating quiz: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isGeneratingQuiz = false)
            }
        }
    }

    fun answerQuizOption(selectedIndex: Int) {
        val state = _uiState.value
        val quiz = state.currentQuizQuestion ?: return
        if (state.selectedQuizOption != null) return

        val isCorrect = selectedIndex == quiz.correctIndex
        val newScore = if (isCorrect) state.quizScore + 10 else state.quizScore
        val char = state.activeCharacter

        val spokenReaction = if (isCorrect) {
            "Betul sekali! +10 poin! ${quiz.explanation}"
        } else {
            val rightAns = quiz.options.getOrElse(quiz.correctIndex) { "" }
            "Hampir tepat! Jawaban yang benar adalah $rightAns. ${quiz.explanation}"
        }

        ttsEngine.playHappyChime()
        _uiState.value = state.copy(
            selectedQuizOption = selectedIndex,
            quizScore = newScore,
            activeIdleBehavior = if (isCorrect) IdlePetBehavior.HAPPY_BOUNCE else IdlePetBehavior.LOOKING_AROUND,
            currentEmotion = if (isCorrect) AvatarEmotion.HAPPY else AvatarEmotion.SURPRISED
        )
        speakVoiceOnly(spokenReaction, if (isCorrect) AvatarEmotion.HAPPY else AvatarEmotion.TALKING)
    }

    fun openStoryStageMode() {
        val state = _uiState.value
        val saved = storyRepository.getSavedSessions()
        // If no current story node is loaded yet, auto-load the most recent saved session or start a new one
        if (state.currentStoryNode == null) {
            val latestSession = saved.firstOrNull()
            if (latestSession != null) {
                _uiState.value = state.copy(
                    showFunActivitiesDialog = false,
                    showStoryStageMode = true,
                    activeStorySessionId = latestSession.sessionId,
                    selectedStoryTheme = latestSession.theme,
                    currentStoryNode = latestSession.currentNode,
                    storyInventory = latestSession.inventory,
                    storyHistoryLog = latestSession.storyHistoryLog,
                    storyContextHistory = latestSession.storyHistoryLog.joinToString("\n"),
                    savedStorySessions = saved,
                    currentEmotion = latestSession.currentNode.sceneEmotion
                )
                speakVoiceOnly(latestSession.currentNode.narrative, latestSession.currentNode.sceneEmotion)
                return
            } else {
                _uiState.value = state.copy(
                    showFunActivitiesDialog = false,
                    showStoryStageMode = true,
                    savedStorySessions = saved
                )
                startInteractiveStory(state.selectedStoryTheme)
                return
            }
        }
        _uiState.value = state.copy(
            showFunActivitiesDialog = false,
            showStoryStageMode = true,
            savedStorySessions = saved
        )
    }

    fun closeStoryStageMode() {
        _uiState.value = _uiState.value.copy(
            showStoryStageMode = false,
            currentEmotion = AvatarEmotion.IDLE
        )
    }

    fun startInteractiveStory(theme: StoryTheme) {
        val state = _uiState.value
        val char = state.activeCharacter
        val newSessionId = "story_${theme.name.lowercase()}_${System.currentTimeMillis()}"
        _uiState.value = state.copy(
            showFunActivitiesDialog = false,
            showStoryStageMode = true,
            activeStorySessionId = newSessionId,
            selectedStoryTheme = theme,
            currentStoryNode = null,
            storyInventory = emptyList(),
            storyHistoryLog = emptyList(),
            isGeneratingStory = true,
            storyContextHistory = ""
        )

        viewModelScope.launch {
            try {
                val firstNode = aiService.generateStoryNode(
                    theme = theme,
                    chapter = 1,
                    maxChapters = 5,
                    previousStory = "",
                    chosenOption = null,
                    inventory = emptyList(),
                    character = char,
                    userNickname = state.userNickname
                )
                val updatedInventory = if (firstNode.acquiredItem != null) {
                    listOf(firstNode.acquiredItem)
                } else {
                    emptyList()
                }
                val initialHistoryLog = listOf("Babak 1 (${firstNode.title}): ${firstNode.narrative}")
                val session = SavedStorySession(
                    sessionId = newSessionId,
                    theme = theme,
                    characterId = char.id,
                    currentNode = firstNode,
                    storyHistoryLog = initialHistoryLog,
                    inventory = updatedInventory,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
                val updatedSaved = storyRepository.saveOrUpdateSession(session)

                _uiState.value = _uiState.value.copy(
                    currentStoryNode = firstNode,
                    storyContextHistory = firstNode.narrative,
                    storyHistoryLog = initialHistoryLog,
                    storyInventory = updatedInventory,
                    savedStorySessions = updatedSaved,
                    isGeneratingStory = false,
                    activeIdleBehavior = IdlePetBehavior.HAPPY_BOUNCE,
                    currentEmotion = firstNode.sceneEmotion
                )
                speakVoiceOnly(firstNode.narrative, firstNode.sceneEmotion)
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Error starting story: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isGeneratingStory = false)
            }
        }
    }

    fun chooseStoryBranch(chosenOptionText: String) {
        val state = _uiState.value
        val currentNode = state.currentStoryNode ?: return
        val nextChapter = currentNode.chapterNumber + 1
        val char = state.activeCharacter
        val sessionId = state.activeStorySessionId.ifBlank {
            "story_${state.selectedStoryTheme.name.lowercase()}_${System.currentTimeMillis()}"
        }

        _uiState.value = state.copy(
            isGeneratingStory = true,
            activeStorySessionId = sessionId,
            currentEmotion = AvatarEmotion.SURPRISED
        )

        viewModelScope.launch {
            try {
                val nextNode = aiService.generateStoryNode(
                    theme = state.selectedStoryTheme,
                    chapter = nextChapter,
                    maxChapters = currentNode.maxChapters,
                    previousStory = state.storyContextHistory,
                    chosenOption = chosenOptionText,
                    inventory = state.storyInventory,
                    character = char,
                    userNickname = state.userNickname
                )
                val updatedHistory = "${state.storyContextHistory}\n[Keputusan: $chosenOptionText]\n${nextNode.narrative}"
                val updatedLog = state.storyHistoryLog +
                    "[Keputusan: $chosenOptionText]" +
                    "Babak $nextChapter (${nextNode.title}): ${nextNode.narrative}"

                val updatedInventory = if (nextNode.acquiredItem != null &&
                    state.storyInventory.none { it.name.equals(nextNode.acquiredItem.name, ignoreCase = true) }
                ) {
                    state.storyInventory + nextNode.acquiredItem
                } else {
                    state.storyInventory
                }

                val session = SavedStorySession(
                    sessionId = sessionId,
                    theme = state.selectedStoryTheme,
                    characterId = char.id,
                    currentNode = nextNode,
                    storyHistoryLog = updatedLog,
                    inventory = updatedInventory,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
                val updatedSaved = storyRepository.saveOrUpdateSession(session)

                ttsEngine.playHappyChime()
                _uiState.value = _uiState.value.copy(
                    currentStoryNode = nextNode,
                    storyContextHistory = updatedHistory,
                    storyHistoryLog = updatedLog,
                    storyInventory = updatedInventory,
                    savedStorySessions = updatedSaved,
                    isGeneratingStory = false,
                    activeIdleBehavior = IdlePetBehavior.HAPPY_BOUNCE,
                    currentEmotion = nextNode.sceneEmotion
                )
                speakVoiceOnly(nextNode.narrative, nextNode.sceneEmotion)
            } catch (e: Exception) {
                Log.e("AvatarViewModel", "Error advancing story branch: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isGeneratingStory = false)
            }
        }
    }

    fun resumeSavedStorySession(session: SavedStorySession) {
        _uiState.value = _uiState.value.copy(
            showFunActivitiesDialog = false,
            showStoryStageMode = true,
            activeStorySessionId = session.sessionId,
            selectedStoryTheme = session.theme,
            currentStoryNode = session.currentNode,
            storyInventory = session.inventory,
            storyHistoryLog = session.storyHistoryLog,
            storyContextHistory = session.storyHistoryLog.joinToString("\n"),
            currentEmotion = session.currentNode.sceneEmotion
        )
        speakVoiceOnly(session.currentNode.narrative, session.currentNode.sceneEmotion)
    }

    fun deleteSavedStorySession(sessionId: String) {
        val updated = storyRepository.deleteSession(sessionId)
        _uiState.value = _uiState.value.copy(savedStorySessions = updated)
    }

    fun triggerHealthyHabit(habit: HealthyHabitType) {
        val state = _uiState.value
        val char = state.activeCharacter
        ttsEngine.playHappyChime()

        when (habit) {
            HealthyHabitType.DRINK_WATER -> {
                val newWater = (state.waterGlassesToday + 1).coerceAtMost(12)
                prefs.edit().putInt("water_glasses_today", newWater).apply()
                val msg = when (char.id) {
                    "zayn" -> "Glek glek! Bagus, kamu udah minum $newWater gelas air putih hari ini. Jangan sampai dehidrasi ya!"
                    "siti" -> "Alhamdulillah, segar sekali sayang! Sudah $newWater gelas air putih hari ini, tubuhmu pasti makin sehat."
                    "rizky" -> "Hidrasi terkonfirmasi! Gelas ke-$newWater berhasil diminum! Jaga stamina tetap prima!"
                    "jamal" -> "Seger banget bro air putihnya... udah $newWater gelas nih hari ini, mantap."
                    else -> "BAGUS! Minum air putih yang rajin! Udah $newWater gelas nih, pertahankan!"
                }
                _uiState.value = state.copy(
                    waterGlassesToday = newWater,
                    activeIdleBehavior = IdlePetBehavior.HAPPY_BOUNCE,
                    currentEmotion = AvatarEmotion.HAPPY,
                    liveUserSpeech = "💧 Minum Air Putih ($newWater/8)"
                )
                speakVoiceOnly(msg, AvatarEmotion.HAPPY)
            }

            HealthyHabitType.BODY_STRETCH -> {
                val newStretch = state.stretchCountToday + 1
                prefs.edit().putInt("stretch_count_today", newStretch).apply()
                val msg = "Ayo rentangkan tangan ke atas dan putar bahumu pelan-pelan bersama ${char.name}... Nah, badan jadi enteng dan segar lagi kan!"
                _uiState.value = state.copy(
                    stretchCountToday = newStretch,
                    activeIdleBehavior = IdlePetBehavior.STRETCHING,
                    currentEmotion = AvatarEmotion.HAPPY,
                    liveUserSpeech = "🧘 Peregangan Badan Bersama"
                )
                speakVoiceOnly(msg, AvatarEmotion.HAPPY)
            }

            HealthyHabitType.EYE_REST -> {
                val msg = "Sekarang alihkan pandanganmu dari layar HP ke objek jauh selama 20 detik sambil berkedip santai... Matamu pasti terasa lebih adem!"
                _uiState.value = state.copy(
                    activeIdleBehavior = IdlePetBehavior.LOOKING_AROUND,
                    currentEmotion = AvatarEmotion.LOVING,
                    liveUserSpeech = "👀 Istirahat Mata 20-20-20"
                )
                speakVoiceOnly(msg, AvatarEmotion.LOVING)
            }

            HealthyHabitType.DEEP_BREATH -> {
                val msg = "Mari tarik napas dalam-dalam lewat hidung... tahan sebentar... lalu hembuskan perlahan lewat mulut. Pikiran jadi jauh lebih tenang!"
                _uiState.value = state.copy(
                    activeIdleBehavior = IdlePetBehavior.YAWNING,
                    currentEmotion = AvatarEmotion.LOVING,
                    liveUserSpeech = "🌿 Napas Relaksasi"
                )
                speakVoiceOnly(msg, AvatarEmotion.LOVING)
            }
        }
    }

    fun triggerTimeOfDayGreeting() {
        val current = TimeOfDay.fromCurrentHour()
        _uiState.value = _uiState.value.copy(currentTimeOfDay = current)
        deliverGreeting(_uiState.value.activeCharacter, _uiState.value.selectedLanguage)
    }

    override fun onCleared() {
        super.onCleared()
        geminiLiveClient.disconnect()
        micWakeDetector.stop()
        ttsEngine.release()
        speechRecognitionHelper.destroy()
    }
}

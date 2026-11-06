package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SwapHoriz
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CharacterCatalog
import com.example.model.PetCharacter
import com.example.model.PetSpecies
import com.example.ui.AvatarUiState
import com.example.ui.FaceToFaceCallState

@Composable
fun FaceToFaceCallView(
    uiState: AvatarUiState,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onInterruptOrSpeak: () -> Unit,
    onQuickPrompt: (String) -> Unit,
    onSwitchCharacter: (PetCharacter) -> Unit,
    onPetAvatar: () -> Unit,
    onCameraFrameUpdated: (android.graphics.Bitmap?) -> Unit = {},
    onAskCameraVision: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val char = uiState.activeCharacter
    val callState = uiState.faceToFaceCallState

    var showCharacterPickerModal by remember { mutableStateOf(false) }

    // Pulse animation for audio/thinking wave
    val infiniteTransition = rememberInfiniteTransition(label = "call_audio_wave")
    val waveScale1 by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale1"
    )
    val waveAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveAlpha1"
    )

    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    // Call duration formatted mm:ss
    val minutes = uiState.callDurationSeconds / 60
    val seconds = uiState.callDurationSeconds % 60
    val durationText = "%02d:%02d".format(minutes, seconds)

    val personalityBadge = when (char.id) {
        "zayn" -> "😼 Tsundere & Ketus"
        "siti" -> "🐰 Penyayang & Lembut"
        "rizky" -> "🐶 Tegas & Disiplin"
        "jamal" -> "🦫 Pemalas & Nakal"
        else -> "🐻 Pemarah & Sangar"
    }

    val quickQuestions = listOf(
        "👁️ Lihat ekspresiku di kamera!",
        "👁️ Tebak aku lagi pegang apa?",
        "5 x 9 apa?",
        "Lagi ngapain kamu?",
        "Ceritakan lelucon dong!"
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Fullscreen dynamic ornate background behind the call
        DynamicBackgroundCanvas(
            theme = uiState.backgroundTheme,
            currentTimeOfDay = uiState.currentTimeOfDay,
            modifier = Modifier.fillMaxSize()
        )
        // Frosted call tint for optimal video call contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x750D1127))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Call Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Character Pill & Switcher
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .clickable { showCharacterPickerModal = true }
                        .testTag("call_switch_character_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = char.species.emoji,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = char.name,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = personalityBadge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFB0B7D6),
                                    fontSize = 10.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Ganti Karakter",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Call Duration & Live Status
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (callState) {
                                        FaceToFaceCallState.LISTENING -> Color(0xFF4CAF50).copy(alpha = blinkAlpha)
                                        FaceToFaceCallState.THINKING -> Color(0xFFAB47BC).copy(alpha = blinkAlpha)
                                        FaceToFaceCallState.SPEAKING -> Color(0xFF29B6F6)
                                        FaceToFaceCallState.MUTED -> Color(0xFFFFB300)
                                        else -> Color.Gray
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = durationText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🎙️ ${uiState.activeLiveModelName.ifBlank { "gemini-3.8-live" }}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF6EE7B7),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Center Stage: Giant Interactive Avatar with Audio Reactive Waves
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                // Background Audio Rings
                Canvas(
                    modifier = Modifier
                        .size(320.dp)
                        .scale(
                            when (callState) {
                                FaceToFaceCallState.LISTENING -> 1f + (uiState.speechRms.coerceIn(0f, 10f) * 0.03f)
                                FaceToFaceCallState.SPEAKING -> waveScale1
                                FaceToFaceCallState.THINKING -> waveScale1
                                else -> 1f
                            }
                        )
                ) {
                    val radius = size.minDimension / 2f
                    val ringColor = when (callState) {
                        FaceToFaceCallState.LISTENING -> Color(0xFF4CAF50).copy(alpha = waveAlpha1)
                        FaceToFaceCallState.THINKING -> Color(0xFFAB47BC).copy(alpha = waveAlpha1)
                        FaceToFaceCallState.SPEAKING -> Color(0xFF00E5FF).copy(alpha = waveAlpha1 * 0.8f)
                        FaceToFaceCallState.MUTED -> Color(0xFFFFB300).copy(alpha = 0.15f)
                        else -> Color.White.copy(alpha = 0.1f)
                    }

                    drawCircle(
                        color = ringColor,
                        radius = radius * 0.95f,
                        center = center
                    )
                    drawCircle(
                        color = ringColor.copy(alpha = ringColor.alpha * 0.5f),
                        radius = radius * 1.08f,
                        center = center
                    )
                }

                // Giant Animated 3D Avatar
                Avatar3DCanvas(
                    species = char.species,
                    customization = uiState.customization,
                    emotion = uiState.currentEmotion,
                    isTalking = uiState.isTalking,
                    onZoneTouched = { onPetAvatar() },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("facetoface_avatar_canvas")
                )

                // Status Indicator Pill (overlayed on top of avatar)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (callState) {
                            FaceToFaceCallState.LISTENING -> Color(0xFF4CAF50)
                            FaceToFaceCallState.THINKING -> Color(0xFFAB47BC)
                            FaceToFaceCallState.SPEAKING -> Color(0xFF29B6F6)
                            FaceToFaceCallState.MUTED -> Color(0xFFFFB300)
                            else -> Color.White.copy(alpha = 0.3f)
                        }
                    ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        val statusEmoji = when (callState) {
                            FaceToFaceCallState.LISTENING -> "🟢"
                            FaceToFaceCallState.THINKING -> "🧠"
                            FaceToFaceCallState.SPEAKING -> "🗣️"
                            FaceToFaceCallState.MUTED -> "🔇"
                            else -> "⚡"
                        }
                        val statusText = when (callState) {
                            FaceToFaceCallState.LISTENING -> "Mendengarkan kamu..."
                            FaceToFaceCallState.THINKING -> "${char.name} sedang berpikir..."
                            FaceToFaceCallState.SPEAKING -> "${char.name} sedang berbicara..."
                            FaceToFaceCallState.MUTED -> "Mikrofon Dimatikan"
                            else -> "Menghubungkan..."
                        }

                        Text(text = statusEmoji, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            // Real-time Live Subtitles Card (Floating speech display - not an input text)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF13152C).copy(alpha = 0.88f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // User's speech subtitle
                    if (uiState.liveUserSpeech.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "👤 Kamu: ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF80D8FF)
                                )
                            )
                            Text(
                                text = "\"${uiState.liveUserSpeech}\"",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.9f)
                                ),
                                maxLines = 2
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Avatar spoken line subtitle
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "🐾 ${char.name}: ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F)
                            )
                        )
                        Text(
                            text = if (callState == FaceToFaceCallState.THINKING) {
                                "Sedang merangkai kata-kata..."
                            } else {
                                uiState.liveAvatarSpeech.ifBlank { "Halo! Aku siap mendengarkanmu langsung." }
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 20.sp
                            ),
                            maxLines = 5
                        )
                    }
                }
            }

            // Quick Prompt Suggestions (Hands-free test pills)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                items(quickQuestions) { q ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        modifier = Modifier
                            .clickable { onQuickPrompt(q) }
                            .testTag("call_quick_prompt_${q.hashCode()}")
                    ) {
                        Text(
                            text = q,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFE0E0FF),
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom Call Action Controls Bar
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                // Mute / Unmute Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = if (uiState.isFaceToFaceMuted) Color(0xFFFFB300) else Color.White.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .size(56.dp)
                            .clickable { onToggleMute() }
                            .testTag("call_mute_toggle_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isFaceToFaceMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (uiState.isFaceToFaceMuted) "Nyalakan Mic" else "Matikan Mic",
                                tint = if (uiState.isFaceToFaceMuted) Color.Black else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (uiState.isFaceToFaceMuted) "Unmute" else "Mute",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    )
                }

                // Red End Call Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE53935),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(68.dp)
                            .clickable { onEndCall() }
                            .testTag("call_end_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Akhiri Panggilan",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tutup",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFFF8A80),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                // Live Camera Vision ("Lihat Aku") Manual Trigger Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6EE7B7)),
                        modifier = Modifier
                            .size(56.dp)
                            .clickable { onAskCameraVision() }
                            .testTag("call_camera_vision_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "👁️", fontSize = 22.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lihat Aku",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF6EE7B7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                // Interrupt / Speak Now Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = if (callState == FaceToFaceCallState.SPEAKING) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .size(56.dp)
                            .clickable { onInterruptOrSpeak() }
                            .testTag("call_interrupt_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Bicara Langsung / Interupsi",
                                tint = if (callState == FaceToFaceCallState.SPEAKING) Color.Black else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (callState == FaceToFaceCallState.SPEAKING) "Interupsi" else "Bicara",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Floating Front Camera PiP Window (WhatsApp / FaceTime style) with Live Vision Capture
        FrontCameraPiP(
            onCameraFrameUpdated = onCameraFrameUpdated,
            onAskCameraVision = onAskCameraVision,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 62.dp, end = 16.dp)
        )

        // Quick Character Picker Modal within Face-to-Face call
        if (showCharacterPickerModal) {
            CharacterPickerDialog(
                currentCharacter = char,
                onSelectCharacter = { newChar ->
                    onSwitchCharacter(newChar)
                    showCharacterPickerModal = false
                },
                onPreviewVoice = { /* preview */ },
                onDismiss = { showCharacterPickerModal = false }
            )
        }
    }
}

/**
 * Floating CameraX Front Camera Picture-in-Picture (PiP) card.
 * Uses DisposableEffect to bind/unbind CameraX cleanly once per state change
 * without triggering duplicate capture session configurations in AndroidView.update.
 */
@Composable
fun FrontCameraPiP(
    onCameraFrameUpdated: (android.graphics.Bitmap?) -> Unit = {},
    onAskCameraVision: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isFrontCamera by remember { mutableStateOf(true) }
    // Default camera off on call start to prevent emulator Camera2 HAL timeout unless user enables video
    var isCameraMuted by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (granted) {
                isCameraMuted = false
            }
        }
    )

    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Periodically capture camera snapshot while camera is active so speaking automatically includes Live Vision
    LaunchedEffect(hasCameraPermission, isCameraMuted, isFrontCamera) {
        if (hasCameraPermission && !isCameraMuted) {
            while (true) {
                kotlinx.coroutines.delay(1400L)
                try {
                    val bmp = previewView.bitmap
                    if (bmp != null) {
                        onCameraFrameUpdated(bmp)
                    }
                } catch (_: Exception) {
                }
            }
        } else {
            onCameraFrameUpdated(null)
        }
    }

    DisposableEffect(lifecycleOwner, hasCameraPermission, isCameraMuted, isFrontCamera) {
        var boundProvider: ProcessCameraProvider? = null
        if (hasCameraPermission && !isCameraMuted) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val listener = Runnable {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    boundProvider = cameraProvider
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val selector = when {
                        isFrontCamera && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                            CameraSelector.DEFAULT_BACK_CAMERA
                        else -> null
                    }
                    cameraProvider.unbindAll()
                    if (selector != null) {
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                    } else {
                        isCameraMuted = true
                    }
                } catch (e: Exception) {
                    Log.w("FrontCameraPiP", "Camera binding skipped: ${e.message}")
                    isCameraMuted = true
                }
            }
            cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        }

        onDispose {
            onCameraFrameUpdated(null)
            try {
                boundProvider?.unbindAll()
            } catch (e: Exception) {
                Log.w("FrontCameraPiP", "Camera unbind error: ${e.message}")
            }
        }
    }

    Box(
        modifier = modifier
            .size(width = 114.dp, height = 158.dp)
            .shadow(12.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0F172A))
            .border(2.dp, Color(0xFF10B981), RoundedCornerShape(18.dp))
            .testTag("front_camera_pip_card")
    ) {
        if (hasCameraPermission && !isCameraMuted) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Top overlay bar on camera PiP
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Red.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = "LIVE AI",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Switch camera toggle
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { isFrontCamera = !isFrontCamera },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Balik Kamera",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    // Mute video toggle
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { isCameraMuted = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = "Tutup Kamera",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Bottom manual "Lihat" vision trigger button on PiP
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xD910B981),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .clickable {
                        try {
                            previewView.bitmap?.let { onCameraFrameUpdated(it) }
                        } catch (_: Exception) {
                        }
                        onAskCameraVision()
                    }
            ) {
                Text(
                    text = "👁️ Lihat Ini",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        } else if (isCameraMuted && hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Kamera Siap",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { isCameraMuted = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Nyalakan", fontSize = 9.sp)
                }
            }
        } else {
            // Permission request prompt
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Kamera Depan",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Buka", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

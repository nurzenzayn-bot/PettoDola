package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PetCharacter
import com.example.ui.HomeworkSolution

data class SubjectOption(
    val name: String,
    val icon: String,
    val color: Color
)

val SUBJECT_OPTIONS = listOf(
    SubjectOption("Matematika", "📐", Color(0xFF3B82F6)),
    SubjectOption("IPA", "🔬", Color(0xFF10B981)),
    SubjectOption("Bahasa Indonesia", "🇮🇩", Color(0xFFEF4444)),
    SubjectOption("Bahasa Inggris", "🇬🇧", Color(0xFF8B5CF6)),
    SubjectOption("IPS & Sejarah", "🌍", Color(0xFFF59E0B)),
    SubjectOption("Tugas Umum", "💡", Color(0xFF06B6D4))
)

/**
 * Dedicated Homework & Study Mode (Mode Ngerjain Tugas Sekolah).
 * Features:
 * - Subject selector (Math, Science, Indonesian, English, Social Studies, General)
 * - Step-by-step AI tutor explanations tailored to the pet's personality
 * - Direct camera photo capture of homework sheets & gallery photo picker
 * - Spoken explanation audio via TTS
 */
@Composable
fun HomeworkStudyView(
    character: PetCharacter,
    selectedSubject: String,
    questionText: String,
    solution: HomeworkSolution?,
    isSolving: Boolean,
    photoBitmap: Bitmap?,
    onSelectSubject: (String) -> Unit,
    onQuestionChange: (String) -> Unit,
    onSetPhoto: (Bitmap?) -> Unit,
    onSolve: (String?) -> Unit,
    onSpeakExplanation: (String) -> Unit,
    onClose: () -> Unit,
    onOpenVoiceInput: () -> Unit,
    isGeminiConnected: Boolean = true,
    currentApiKey: String = "",
    onSaveApiKey: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showApiKeyCard by remember { mutableStateOf(false) }
    var apiKeyInput by remember(currentApiKey) { mutableStateOf(currentApiKey) }

    // Camera capture launcher (direct thumbnail)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            onSetPhoto(bitmap)
            onSolve(if (questionText.isBlank()) "Tolong jelaskan dan selesaikan soal yang ada di foto tugas ini" else null)
        }
    }

    // Gallery photo picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                onSetPhoto(bitmap)
                onSolve(if (questionText.isBlank()) "Tolong jelaskan dan selesaikan soal yang ada di foto tugas ini" else null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF090D1A)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Ruangan",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📚 Mode Tugas Sekolah",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                        Text(
                            text = "Tutor: ${character.name} (${character.title})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFA5B4FC)
                            )
                        )
                    }
                }

                // Character Avatar & Gemini AI Connection Badge (Tappable to configure API Key on APK)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isGeminiConnected) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFFF59E0B).copy(alpha = 0.22f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isGeminiConnected) Color(0xFF34D399).copy(alpha = 0.55f) else Color(0xFFFBBF24).copy(alpha = 0.65f)
                    ),
                    modifier = Modifier
                        .padding(2.dp)
                        .clickable { showApiKeyCard = !showApiKeyCard }
                        .testTag("homework_gemini_status_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = if (isGeminiConnected) "✨" else "🔑", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isGeminiConnected) "Gemini AI Aktif" else "Hubungkan Gemini",
                            color = if (isGeminiConnected) Color(0xFF6EE7B7) else Color(0xFFFDE68A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Quick Gemini API Key Configuration Card (shown when user taps badge or when a fallback response occurred on an installed APK)
                if (showApiKeyCard || (solution != null && !solution.isFromAi && currentApiKey.isBlank())) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF1E1B4B).copy(alpha = 0.92f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF818CF8)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🔑", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Hubungkan Gemini API Key (Untuk APK di HP)",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Tempel API Key gratis dari aistudio.google.com/apikey agar AI menjawab semua soal",
                                                color = Color(0xFFA5B4FC),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    if (showApiKeyCard) {
                                        IconButton(
                                            onClick = { showApiKeyCard = false },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Tutup",
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = apiKeyInput,
                                    onValueChange = { apiKeyInput = it },
                                    placeholder = {
                                        Text(
                                            text = "AIzaSy...",
                                            color = Color(0xFF64748B),
                                            fontSize = 12.sp
                                        )
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF34D399),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("homework_api_key_input")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            onSaveApiKey(apiKeyInput.trim())
                                            showApiKeyCard = false
                                            focusManager.clearFocus()
                                            if (questionText.isNotBlank() || photoBitmap != null) {
                                                onSolve(null)
                                            }
                                        },
                                        enabled = apiKeyInput.trim().isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (questionText.isNotBlank()) "Simpan & Bahas Ulang Soal ✨" else "Simpan Gemini API Key",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. Subject Category Chips
                item {
                    Text(
                        text = "Pilih Mata Pelajaran:",
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SUBJECT_OPTIONS) { subject ->
                            val isSelected = selectedSubject.equals(subject.name, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) subject.color else Color.White.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) subject.color else Color.White.copy(alpha = 0.18f)
                                ),
                                modifier = Modifier
                                    .clickable { onSelectSubject(subject.name) }
                                    .testTag("subject_chip_${subject.name}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(text = subject.icon, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = subject.name,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Photo Attachment Card (if photo taken/selected)
                if (photoBitmap != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Image(
                                    bitmap = photoBitmap.asImageBitmap(),
                                    contentDescription = "Foto Soal Tugas",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Foto Soal Terlampir",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "AI siap menganalisis soal dari foto lembaran ini",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(
                                    onClick = { onSetPhoto(null) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus Foto",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Question Input Box
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.07f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Tulis Soal / Materi yang Ingin Dibahas:",
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = questionText,
                                onValueChange = onQuestionChange,
                                placeholder = {
                                    Text(
                                        text = "Contoh: Rumus luas tabung, atau 'Sebuah mobil melaju 60 km/jam...'",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                },
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    if (questionText.isNotBlank()) onSolve(null)
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color(0xFFE2E8F0),
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    cursorColor = Color(0xFF818CF8)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("homework_question_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons: Foto Soal (Camera), Galeri, Mic, & Pecahkan Soal
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Foto Kamera Langsung
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF3B82F6).copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .clickable { cameraLauncher.launch(null) }
                                            .testTag("homework_camera_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CameraAlt,
                                                contentDescription = "Foto Soal",
                                                tint = Color(0xFF60A5FA),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Foto Soal",
                                                color = Color(0xFF93C5FD),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    // Galeri Foto
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.1f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .clickable {
                                                galleryLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                            .testTag("homework_gallery_button")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddPhotoAlternate,
                                                contentDescription = "Pilih Gambar",
                                                tint = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Voice Dictation
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.1f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .clickable { onOpenVoiceInput() }
                                            .testTag("homework_mic_button")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Bicara Soal",
                                                tint = Color(0xFF34D399),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Tombol Pecahkan / Bahas
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        onSolve(null)
                                    },
                                    enabled = !isSolving && (questionText.isNotBlank() || photoBitmap != null),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6366F1),
                                        contentColor = Color.White,
                                        disabledContainerColor = Color.White.copy(alpha = 0.12f),
                                        disabledContentColor = Color.White.copy(alpha = 0.35f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("homework_solve_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Bahas Soal",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Solving Loading Indicator
                if (isSolving) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF1E1B4B).copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF818CF8),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "${character.name} sedang membaca soal dan menyusun langkah penyelesaian...",
                                    color = Color(0xFFE0E7FF),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 5. Solution Presentation Cards
                if (solution != null && !isSolving) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                // Subject Tag & Read Aloud Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (solution.isFromAi) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (solution.isFromAi) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFF59E0B).copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Text(
                                            text = if (solution.isFromAi) "✨ Dijawab Gemini AI • ${solution.subject}" else "⚡ Pembahasan Lokal • ${solution.subject}",
                                            color = if (solution.isFromAi) Color(0xFF34D399) else Color(0xFFFBBF24),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // Spoken Audio TTS Button
                                    Button(
                                        onClick = {
                                            val fullText = "${solution.finalAnswer}. ${solution.tutorTip}"
                                            onSpeakExplanation(fullText)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.12f),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Dengarkan",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Dengar", fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Question Summary
                                Text(
                                    text = solution.questionSummary,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Concept or Formula Card (if available)
                                if (solution.conceptOrFormula.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF6366F1).copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(10.dp)
                                        ) {
                                            Text(text = "💡", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Konsep / Rumus Utama:",
                                                    color = Color(0xFFA5B4FC),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = solution.conceptOrFormula,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Step-by-Step Cards
                                Text(
                                    text = "Langkah-Langkah Pengerjaan:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                solution.stepByStep.forEachIndexed { index, step ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF6366F1),
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = step,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Final Answer Box
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Kesimpulan & Jawaban:",
                                            color = Color(0xFF6EE7B7),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = solution.finalAnswer,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Tutor Tip from Character
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = solution.tutorTip,
                                        color = Color(0xFFFDE68A),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }

                                // Practice Challenge (if available)
                                if (solution.practiceChallenge.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF3B82F6).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                            .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(text = "🎯", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Tantangan Latihan Mandiri:",
                                                color = Color(0xFF93C5FD),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = solution.practiceChallenge,
                                                color = Color(0xFFEFF6FF),
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

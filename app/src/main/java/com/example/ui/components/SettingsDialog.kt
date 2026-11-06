package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.VoiceEngineMode
import com.example.model.AppLanguage

@Composable
fun SettingsDialog(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    voicePitch: Float,
    onPitchChanged: (Float) -> Unit,
    speechRate: Float,
    onSpeechRateChanged: (Float) -> Unit,
    onTestVoice: () -> Unit,
    onClearHistory: () -> Unit,
    isGeminiConnected: Boolean = true,
    currentApiKey: String = "",
    onApiKeySaved: (String) -> Unit = {},
    voiceEngineMode: VoiceEngineMode = VoiceEngineMode.CARTOON_MIMIC,
    onVoiceEngineModeSelected: (VoiceEngineMode) -> Unit = {},
    elevenLabsApiKey: String = "",
    onElevenLabsApiKeySaved: (String) -> Unit = {},
    googleCloudTtsApiKey: String = "",
    onGoogleCloudTtsApiKeySaved: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var geminiKeyInput by remember { mutableStateOf(currentApiKey) }
    var geminiKeySavedToast by remember { mutableStateOf(false) }

    var elevenLabsKeyInput by remember { mutableStateOf(elevenLabsApiKey) }
    var elevenLabsSavedToast by remember { mutableStateOf(false) }

    var googleTtsKeyInput by remember { mutableStateOf(googleCloudTtsApiKey) }
    var googleTtsSavedToast by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pengaturan Suara & AI",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                // Section: Voice Engine Selection (Neural vs Cartoon Mimic)
                Text(
                    text = "Sistem Suara & Audio Karakter",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pilih arsitektur audio agar suara terdengar alami, ekspresif, dan tidak kaku seperti robot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                VoiceEngineMode.values().forEach { mode ->
                    val isSelected = mode == voiceEngineMode
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onVoiceEngineModeSelected(mode) }
                            .testTag("voice_engine_${mode.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(text = mode.icon, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { onVoiceEngineModeSelected(mode) }
                            )
                        }
                    }
                }

                // ElevenLabs Key Config
                if (voiceEngineMode == VoiceEngineMode.ELEVENLABS_NEURAL) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kunci ElevenLabs API",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = elevenLabsKeyInput,
                            onValueChange = {
                                elevenLabsKeyInput = it
                                elevenLabsSavedToast = false
                            },
                            placeholder = { Text("xi-api-key...", fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onElevenLabsApiKeySaved(elevenLabsKeyInput)
                                elevenLabsSavedToast = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("Simpan", fontSize = 12.sp)
                        }
                    }
                    if (elevenLabsSavedToast) {
                        Text(
                            text = "✓ Kunci ElevenLabs disimpan!",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF2E7D32)),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Google Cloud TTS Key Config
                if (voiceEngineMode == VoiceEngineMode.GOOGLE_CLOUD_NEURAL) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kunci Google Cloud Text-to-Speech API",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = googleTtsKeyInput,
                            onValueChange = {
                                googleTtsKeyInput = it
                                googleTtsSavedToast = false
                            },
                            placeholder = { Text("Google Cloud API Key...", fontSize = 12.sp) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(50.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onGoogleCloudTtsApiKeySaved(googleTtsKeyInput)
                                googleTtsSavedToast = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("Simpan", fontSize = 12.sp)
                        }
                    }
                    if (googleTtsSavedToast) {
                        Text(
                            text = "✓ Kunci Google TTS disimpan!",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF2E7D32)),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Pitch Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Nada Suara Avatar (Pitch): ${String.format("%.2f", voicePitch)}x",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    IconButton(
                        onClick = onTestVoice,
                        modifier = Modifier.size(36.dp).testTag("test_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Uji Suara",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Slider(
                    value = voicePitch,
                    onValueChange = onPitchChanged,
                    valueRange = 0.6f..1.9f,
                    modifier = Modifier.fillMaxWidth().testTag("voice_pitch_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Voice Speed Slider
                Text(
                    text = "Kecepatan Bicara (Speed): ${String.format("%.2f", speechRate)}x",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Slider(
                    value = speechRate,
                    onValueChange = onSpeechRateChanged,
                    valueRange = 0.7f..1.6f,
                    modifier = Modifier.fillMaxWidth().testTag("voice_speed_slider")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Gemini AI Status Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Google Gemini AI",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isGeminiConnected) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isGeminiConnected) Color(0xFF2E7D32) else Color(0xFFEF6C00))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isGeminiConnected) "Aktif & Terhubung" else "Mode Persona Pintar",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGeminiConnected) Color(0xFF2E7D32) else Color(0xFFEF6C00)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Model Gemini 3.8 Flash, Gemini 4 Argon (Tutor PR), dan Gemini 3.8 Flash TTS aktif dengan fallback lokal otomatis.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom API Key Input
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = geminiKeyInput,
                                onValueChange = {
                                    geminiKeyInput = it
                                    geminiKeySavedToast = false
                                },
                                placeholder = { Text("Kunci Gemini API (Opsional)", fontSize = 12.sp) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(50.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    onApiKeySaved(geminiKeyInput)
                                    geminiKeySavedToast = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Text("Simpan", fontSize = 12.sp)
                            }
                        }

                        if (geminiKeySavedToast) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "✓ Kunci API berhasil disimpan!",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF2E7D32))
                            )
                        }
                    }
                }

                // Multi-Language selection
                Text(
                    text = "Bahasa Percakapan",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                AppLanguage.entries.forEach { lang ->
                    val isSelected = lang == selectedLanguage
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onLanguageSelected(lang) }
                            .testTag("language_option_${lang.code}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(text = lang.flag, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = { onLanguageSelected(lang) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Clear history button
                Button(
                    onClick = onClearHistory,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("clear_chat_history_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus Riwayat Chat")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_settings_button")
            ) {
                Text("Selesai")
            }
        }
    )
}

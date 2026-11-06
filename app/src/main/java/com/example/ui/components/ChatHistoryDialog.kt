package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ChatMessage
import com.example.model.GeminiModelTier
import com.example.model.PetCharacter

/**
 * Full Multi-Turn Gemini Chatbot Interface:
 * - Maintains & displays scrollable conversation history thread
 * - Applies character-specific system instructions/roles
 * - Supports Auto & Manual selection of `gemini-4-argon` (complex), `gemini-3.8-flash` (general),
 *   and `gemini-3.1-flash-lite` (fast)
 * - Allows sending multi-turn messages & voice dictation directly inside the chat window
 */
@Composable
fun ChatHistoryDialog(
    character: PetCharacter,
    messages: List<ChatMessage>,
    selectedModelTier: GeminiModelTier,
    isLoading: Boolean,
    onSelectModelTier: (GeminiModelTier) -> Unit,
    onSendMessage: (String) -> Unit,
    onOpenVoiceInput: () -> Unit,
    onClearHistory: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "Halo ${character.name}!" to "🚀 Kilat",
        "Ceritakan hal seru hari ini dong" to "⚡ Umum",
        "Jelaskan secara mendalam mengapa langit berwarna biru dan bagaimana cahaya bekerja" to "🧠 Kompleks"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xF50F172A),
            border = BorderStroke(1.5.dp, Color(0x66818CF8)),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("gemini_multi_turn_chatbot_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 1. Chatbot Header with Role & System Instruction Persona
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x336366F1))
                        ) {
                            Text(text = character.species.emoji, fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Chatbot Gemini • ${character.name}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            )
                            Text(
                                text = "Peran Sistem: ${character.title} • Multi-Turn Memory",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF93C5FD),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (messages.isNotEmpty()) {
                            IconButton(
                                onClick = onClearHistory,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22FFFFFF))
                                    .testTag("clear_chatbot_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Bersihkan Riwayat",
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                                .testTag("close_chat_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Model Tier Selector (Auto / gemini-4-argon / gemini-3.8-flash / gemini-3.1-flash-lite)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x221E293B),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(GeminiModelTier.entries) { tier ->
                                val isSelected = tier == selectedModelTier
                                val chipColor = when (tier) {
                                    GeminiModelTier.AUTO -> Color(0xFF6366F1)
                                    GeminiModelTier.ARGON_COMPLEX -> Color(0xFFA855F7)
                                    GeminiModelTier.FLASH_GENERAL -> Color(0xFF0EA5E9)
                                    GeminiModelTier.FLASH_LITE_FAST -> Color(0xFF10B981)
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) chipColor else Color(0x1FFFFFFF),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color.White.copy(alpha = 0.7f) else Color(0x33FFFFFF)
                                    ),
                                    modifier = Modifier
                                        .clickable { onSelectModelTier(tier) }
                                        .testTag("model_tier_chip_${tier.name}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = tier.emoji, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = tier.label,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedModelTier.subtitle,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Scrollable Multi-Turn Conversation Thread
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(16.dp)
                        ) {
                            Text(text = character.species.emoji, fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Mulai Obrolan Multi-Turn dengan ${character.name}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Chatbot ini menjaga konteks percakapan penuh dan mendukung pemilihan otomatis antara gemini-4-argon, gemini-3.8-flash, dan gemini-3.1-flash-lite.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .testTag("chatbot_scrollable_thread")
                        ) {
                            items(messages, key = { it.id }) { msg ->
                                ChatMessageBubbleItem(
                                    msg = msg,
                                    characterEmoji = character.species.emoji,
                                    onSpeakMessage = onSpeakMessage
                                )
                            }

                            if (isLoading) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF818CF8),
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${character.name} sedang menyusun balasan...",
                                            color = Color(0xFF93C5FD),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Quick Multi-Turn Prompt Chips (Fast / General / Complex)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickPrompts) { (promptText, tagLabel) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x2438BDF8),
                            border = BorderStroke(1.dp, Color(0x4538BDF8)),
                            modifier = Modifier.clickable(enabled = !isLoading) {
                                onSendMessage(promptText)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = tagLabel,
                                    color = Color(0xFF7DD3FC),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = promptText,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Bottom Multi-Turn Chat Input Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "Tulis pesan ke ${character.name}...",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF818CF8),
                            unfocusedBorderColor = Color(0x35FFFFFF),
                            focusedContainerColor = Color(0x330F172A),
                            unfocusedContainerColor = Color(0x220F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isLoading) {
                                    onSendMessage(inputText.trim())
                                    inputText = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chatbot_dialog_input_field")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onOpenVoiceInput,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0EA5E9))
                            .testTag("chatbot_dialog_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = " Bicara",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isLoading) {
                                onSendMessage(inputText.trim())
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank() && !isLoading) Color(0xFF6366F1)
                                else Color(0x22FFFFFF)
                            )
                            .testTag("chatbot_dialog_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubbleItem(
    msg: ChatMessage,
    characterEmoji: String,
    onSpeakMessage: (String) -> Unit
) {
    Row(
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (msg.isUser) 18.dp else 4.dp,
                bottomEnd = if (msg.isUser) 4.dp else 18.dp
            ),
            color = if (msg.isUser) Color(0xFF4F46E5) else Color(0xFF1E293B),
            border = BorderStroke(
                1.dp,
                if (msg.isUser) Color(0xFF818CF8) else Color(0x38FFFFFF)
            ),
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .testTag("chat_history_message_${msg.id}")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!msg.isUser) {
                            Text(text = characterEmoji, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = msg.senderName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (msg.isUser) Color(0xFFE0E7FF) else Color(0xFF93C5FD)
                            )
                        )
                        if (!msg.isUser && msg.modelUsed.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            val modelBadgeEmoji = when {
                                msg.modelUsed.contains("argon", ignoreCase = true) -> "🧠"
                                msg.modelUsed.contains("lite", ignoreCase = true) -> "🚀"
                                msg.modelUsed.contains("live", ignoreCase = true) -> "🎙️"
                                else -> "⚡"
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0x33FFFFFF)
                            ) {
                                Text(
                                    text = "$modelBadgeEmoji ${msg.modelUsed}",
                                    color = Color(0xFFBAE6FD),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!msg.isUser) {
                        IconButton(
                            onClick = { onSpeakMessage(msg.text) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Dengarkan Suara Gemini AI",
                                tint = Color(0xFF93C5FD),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = msg.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

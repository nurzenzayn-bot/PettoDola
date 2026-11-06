package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AvatarCustomization
import com.example.model.AvatarEmotion
import com.example.model.BackgroundTheme
import com.example.model.IdlePetBehavior
import com.example.model.PetCharacter
import com.example.model.SavedStorySession
import com.example.model.StoryItem
import com.example.model.StoryNode
import com.example.model.StoryTheme
import com.example.model.TouchZone

/**
 * Full-Screen 3D Story Theater ("Panggung Dongeng Petualangan Interaktif"):
 * - Displays 3D Petto on stage reacting to each chapter's emotion and narrating the story
 * - Supports Branching Choices A/B + Custom Free-Form Action (Type or Voice Mic)
 * - Displays Chapter Progress & Multi-Ending Trophy cards
 * - Interactive Magic Item Inventory bar (tap an item to inspect or use it in the story)
 * - Saved Story Sessions manager (auto-saved & resumable anytime)
 */
@Composable
fun InteractiveStoryStageView(
    character: PetCharacter,
    customization: AvatarCustomization,
    isTalking: Boolean,
    activeIdleBehavior: IdlePetBehavior,
    selectedTheme: StoryTheme,
    currentNode: StoryNode?,
    inventory: List<StoryItem>,
    storyHistoryLog: List<String>,
    savedSessions: List<SavedStorySession>,
    isGenerating: Boolean,
    onSelectThemeAndStart: (StoryTheme) -> Unit,
    onChooseAction: (String) -> Unit,
    onStartNewStory: () -> Unit,
    onResumeSavedSession: (SavedStorySession) -> Unit,
    onDeleteSavedSession: (String) -> Unit,
    onSpeakNarrative: (String) -> Unit,
    onOpenVoiceAction: () -> Unit,
    onCharacterTouched: (TouchZone) -> Unit,
    onClose: () -> Unit
) {
    var customActionText by remember { mutableStateOf("") }
    var inspectedItem by remember { mutableStateOf<StoryItem?>(null) }
    var showSavedSessionsModal by remember { mutableStateOf(false) }
    var showStoryLogModal by remember { mutableStateOf(false) }

    val stageTopColor = Color(selectedTheme.stageTopColorHex)
    val stageBottomColor = Color(selectedTheme.stageBottomColorHex)
    val themeAccentColor = Color(selectedTheme.accentColorHex)

    val infiniteTransition = rememberInfiniteTransition(label = "storyStageTransition")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    val stageEmotion = currentNode?.sceneEmotion ?: AvatarEmotion.HAPPY

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(stageTopColor, stageBottomColor, Color(0xFF070B14))
                )
            )
            .testTag("interactive_story_stage_view")
    ) {
        // Floating atmospheric theme particles in background
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp)
                .offset(y = floatOffset.dp)
        ) {
            selectedTheme.ambientParticles.forEach { symbol ->
                Text(
                    text = symbol,
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.28f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. Top Theater Header Bar
            StoryStageHeaderBar(
                selectedTheme = selectedTheme,
                chapterNumber = currentNode?.chapterNumber ?: 1,
                maxChapters = currentNode?.maxChapters ?: 5,
                savedCount = savedSessions.size,
                hasHistoryLog = storyHistoryLog.isNotEmpty(),
                onOpenSavedSessions = { showSavedSessionsModal = true },
                onOpenStoryLog = { showStoryLogModal = true },
                onClose = onClose
            )

            // 2. Theme Selector Strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(StoryTheme.entries) { theme ->
                    val isSelected = theme == selectedTheme
                    val accent = Color(theme.accentColorHex)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) accent.copy(alpha = 0.24f) else Color(0x22FFFFFF),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) accent else Color(0x30FFFFFF)
                        ),
                        modifier = Modifier
                            .clickable(enabled = !isGenerating) {
                                onSelectThemeAndStart(theme)
                            }
                            .testTag("stage_theme_chip_${theme.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(text = theme.emoji, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = theme.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 3. 3D Petto Narrator Stage + Reaction Speech Pill
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
            ) {
                // Spotlight glow under Petto 3D
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    themeAccentColor.copy(alpha = 0.32f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Avatar3DCanvas(
                    species = character.species,
                    customization = customization,
                    emotion = stageEmotion,
                    isTalking = isTalking,
                    onZoneTouched = onCharacterTouched,
                    backgroundTheme = BackgroundTheme.BEDROOM,
                    idleBehavior = activeIdleBehavior,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("story_stage_3d_avatar")
                )

                // Top-right Emotion & Narrator Status Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xCC0F172A),
                    border = BorderStroke(1.dp, themeAccentColor.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp, top = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(text = character.species.emoji, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isTalking) "🎙️ ${character.name} Mendongeng..."
                            else "✨ Pendongeng: ${character.name}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom-center Petto In-Character Stage Reaction Comment
                val comment = currentNode?.petReactionComment.orEmpty()
                if (comment.isNotBlank() && !isGenerating) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xE61E293B),
                        border = BorderStroke(1.dp, themeAccentColor.copy(alpha = 0.7f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${character.species.emoji} \"$comment\"",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // 4. Magic Inventory Bar ("Tas Inventaris Ajaib")
            MagicInventoryStrip(
                inventory = inventory,
                accentColor = themeAccentColor,
                onItemClick = { inspectedItem = it }
            )

            // 5. Story Scroll & Interactive Branching Controls
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isGenerating) {
                    StoryGeneratingCard(
                        characterName = character.name,
                        themeTitle = selectedTheme.title,
                        accentColor = themeAccentColor
                    )
                } else if (currentNode == null) {
                    StoryEmptyWelcomeCard(
                        character = character,
                        selectedTheme = selectedTheme,
                        accentColor = themeAccentColor,
                        onStart = { onSelectThemeAndStart(selectedTheme) }
                    )
                } else {
                    // Chapter Progress Indicator
                    val progress = (currentNode.chapterNumber.toFloat() / currentNode.maxChapters.toFloat())
                        .coerceIn(0.1f, 1f)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (currentNode.chapterNumber <= currentNode.maxChapters) {
                                    "Progres Cerita • Babak ${currentNode.chapterNumber} dari ${currentNode.maxChapters}"
                                } else {
                                    "Babak Epilog Tanpa Batas • Babak ${currentNode.chapterNumber}"
                                },
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (currentNode.isFromAi) "✨ Gemini Story AI" else "📖 Story Engine",
                                color = themeAccentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            color = themeAccentColor,
                            trackColor = Color(0x33FFFFFF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }

                    // Newly acquired item banner if an item was found in this chapter
                    currentNode.acquiredItem?.let { newItem ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x33F59E0B),
                            border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inspectedItem = newItem }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(text = newItem.emoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🎁 Item Ajaib Ditemukan: ${newItem.name}",
                                        color = Color(0xFFFDE68A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = newItem.description,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Main Story Chapter Narrative Card
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xD90F172A),
                        border = BorderStroke(1.5.dp, themeAccentColor.copy(alpha = 0.65f)),
                        shadowElevation = 10.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = themeAccentColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "📖 ${currentNode.title}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onSpeakNarrative(currentNode.narrative) },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x28FFFFFF))
                                            .testTag("story_replay_voice_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Bacakan Ulang Narasi",
                                            tint = themeAccentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = currentNode.narrative,
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    // Ending Trophy Banner when user reaches climax / ending chapter
                    if (currentNode.isEnding) {
                        val badgeColor = Color(currentNode.endingType.badgeColorHex)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = badgeColor.copy(alpha = 0.18f),
                            border = BorderStroke(1.5.dp, badgeColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(text = currentNode.endingType.badgeEmoji, fontSize = 34.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentNode.endingType.title,
                                    color = badgeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = currentNode.endingTitle.ifBlank { "Penjelajah Legendaris ${selectedTheme.title}" },
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = onStartNewStory,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cerita Baru", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onChooseAction(currentNode.choiceA) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF6366F1),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("➕ Babak Epilog", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Branching Choices A & B
                    Text(
                        text = "Tentukan Keputusanmu Selanjutnya:",
                        color = Color(0xFFBAE6FD),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { onChooseAction(currentNode.choiceA) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stage_choice_a_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "🅰️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = currentNode.choiceA,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = { onChooseAction(currentNode.choiceB) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stage_choice_b_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "🅱️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = currentNode.choiceB,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Custom Free-Form Action Input (Type or Speak Your Own Decision)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xCC0F172A),
                        border = BorderStroke(1.dp, Color(0x45FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "✨ Atau Ketik / Ucapkan Aksi Bebasmu Sendiri:",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = customActionText,
                                    onValueChange = { customActionText = it },
                                    placeholder = {
                                        Text(
                                            "Contoh: Ajak naga itu makan bareng...",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = themeAccentColor,
                                        unfocusedBorderColor = Color(0x33FFFFFF),
                                        focusedContainerColor = Color(0x33000000),
                                        unfocusedContainerColor = Color(0x22000000),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (customActionText.isNotBlank()) {
                                                onChooseAction(customActionText.trim())
                                                customActionText = ""
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("story_custom_action_input")
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                // Voice Action Dictation Button
                                IconButton(
                                    onClick = onOpenVoiceAction,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0EA5E9))
                                        .testTag("story_voice_action_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Ucapkan Aksi Cerita",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Submit Custom Action Button
                                IconButton(
                                    onClick = {
                                        if (customActionText.isNotBlank()) {
                                            onChooseAction(customActionText.trim())
                                            customActionText = ""
                                        }
                                    },
                                    enabled = customActionText.isNotBlank(),
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (customActionText.isNotBlank()) themeAccentColor
                                            else Color(0x22FFFFFF)
                                        )
                                        .testTag("story_send_custom_action_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Kirim Aksi",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // Modal 1: Inspect & Use Magic Inventory Item
        inspectedItem?.let { item ->
            Dialog(onDismissRequest = { inspectedItem = null }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xF50F172A),
                    border = BorderStroke(1.5.dp, Color(0xFFFBBF24)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(text = item.emoji, fontSize = 44.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.name,
                            color = Color(0xFFFDE68A),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Diperoleh pada Babak ${item.chapterAcquired}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = item.description,
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { inspectedItem = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF334155),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Tutup", fontSize = 12.sp)
                            }
                            if (currentNode != null && !isGenerating) {
                                Button(
                                    onClick = {
                                        val action = "Gunakan item ajaib ${item.emoji} ${item.name}"
                                        inspectedItem = null
                                        onChooseAction(action)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD97706),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1.4f)
                                ) {
                                    Text("✨ Gunakan di Cerita", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modal 2: Saved Story Sessions (Resume or Delete)
        if (showSavedSessionsModal) {
            SavedStoriesDialog(
                savedSessions = savedSessions,
                onResume = { session ->
                    showSavedSessionsModal = false
                    onResumeSavedSession(session)
                },
                onDelete = onDeleteSavedSession,
                onDismiss = { showSavedSessionsModal = false }
            )
        }

        // Modal 3: Full Story Log so far
        if (showStoryLogModal) {
            StoryHistoryLogDialog(
                themeTitle = selectedTheme.title,
                historyEntries = storyHistoryLog,
                onSpeakAll = {
                    val combined = storyHistoryLog.joinToString(" ")
                    if (combined.isNotBlank()) onSpeakNarrative(combined)
                },
                onDismiss = { showStoryLogModal = false }
            )
        }
    }
}

@Composable
private fun StoryStageHeaderBar(
    selectedTheme: StoryTheme,
    chapterNumber: Int,
    maxChapters: Int,
    savedCount: Int,
    hasHistoryLog: Boolean,
    onOpenSavedSessions: () -> Unit,
    onOpenStoryLog: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .testTag("close_story_stage_button")
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
                    Text(text = selectedTheme.emoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Dongeng Petualangan",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
                Text(
                    text = "${selectedTheme.title} • Babak $chapterNumber/$maxChapters",
                    color = Color(0xFFBAE6FD),
                    fontSize = 11.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (hasHistoryLog) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x28FFFFFF),
                    border = BorderStroke(1.dp, Color(0x40FFFFFF)),
                    modifier = Modifier
                        .clickable(onClick = onOpenStoryLog)
                        .testTag("open_story_log_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Alur Cerita",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Alur",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0x336366F1),
                border = BorderStroke(1.dp, Color(0xFF818CF8)),
                modifier = Modifier
                    .clickable(onClick = onOpenSavedSessions)
                    .testTag("open_saved_stories_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Cerita Tersimpan",
                        tint = Color(0xFFC7D2FE),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Simpanan ($savedCount)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MagicInventoryStrip(
    inventory: List<StoryItem>,
    accentColor: Color,
    onItemClick: (StoryItem) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x880F172A),
        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = "🎒 Tas Ajaib:",
                color = Color(0xFFFDE68A),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (inventory.isEmpty()) {
                Text(
                    text = "Belum ada item (Jelajahi babak untuk menemukan item ajaib!)",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(inventory) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .clickable { onItemClick(item) }
                                .testTag("inventory_item_${item.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = item.emoji, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryGeneratingCard(
    characterName: String,
    themeTitle: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xCC0F172A),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp)
        ) {
            CircularProgressIndicator(
                color = accentColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$characterName sedang merangkai kisah $themeTitle...",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Gemini AI sedang menyusun cabang cerita, item ajaib, dan dialog suara.",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StoryEmptyWelcomeCard(
    character: PetCharacter,
    selectedTheme: StoryTheme,
    accentColor: Color,
    onStart: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xCC0F172A),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(22.dp)
        ) {
            Text(text = selectedTheme.emoji, fontSize = 40.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = selectedTheme.title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = selectedTheme.subtitle,
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_stage_story_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mulai Petualangan Bersama ${character.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SavedStoriesDialog(
    savedSessions: List<SavedStorySession>,
    onResume: (SavedStorySession) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF50F172A),
            border = BorderStroke(1.5.dp, Color(0xFF818CF8)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📚 Riwayat & Simpanan Dongeng",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White
                        )
                    }
                }
                Text(
                    text = "Lanjutkan petualangan yang belum tamat atau lihat koleksi item ajaibmu.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (savedSessions.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Belum ada petualangan tersimpan.\nMulai cerita baru dan progresmu akan otomatis tersimpan!",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(savedSessions, key = { it.sessionId }) { session ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0x28FFFFFF),
                                border = BorderStroke(1.dp, Color(0x40FFFFFF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(text = session.theme.emoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = session.theme.title,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (session.currentNode.isEnding) {
                                                        "🏆 Tamat • ${session.inventory.size} Item Ajaib"
                                                    } else {
                                                        "Babak ${session.currentNode.chapterNumber}/${session.currentNode.maxChapters} • ${session.inventory.size} Item Ajaib"
                                                    },
                                                    color = Color(0xFF93C5FD),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { onDelete(session.sessionId) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Simpanan",
                                                tint = Color(0xFFF87171),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = session.currentNode.narrative,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { onResume(session) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4F46E5),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (session.currentNode.isEnding) "Buka & Lanjutkan Epilog"
                                            else "Lanjutkan Babak ${session.currentNode.chapterNumber}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
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

@Composable
private fun StoryHistoryLogDialog(
    themeTitle: String,
    historyEntries: List<String>,
    onSpeakAll: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF50F172A),
            border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📜 Alur Kisah: $themeTitle",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(historyEntries) { entry ->
                        val isChoice = entry.startsWith("[Keputusan:")
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChoice) Color(0x3338BDF8) else Color(0x22FFFFFF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = entry,
                                color = if (isChoice) Color(0xFF7DD3FC) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (isChoice) FontWeight.Bold else FontWeight.Normal,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onSpeakAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bacakan Seluruh Kisah", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

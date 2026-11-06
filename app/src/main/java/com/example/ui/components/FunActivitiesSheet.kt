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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.FunActivityTab
import com.example.model.HealthyHabitType
import com.example.model.PetCharacter
import com.example.model.QuizQuestion
import com.example.model.RpsChoice
import com.example.model.RpsResult
import com.example.model.RpsRoundState
import com.example.model.StoryNode
import com.example.model.StoryTheme
import com.example.model.TimeOfDay

@Composable
fun FunActivitiesDialog(
    character: PetCharacter,
    currentTimeOfDay: TimeOfDay,
    activeTab: FunActivityTab,
    onSelectTab: (FunActivityTab) -> Unit,
    // Mini-Game (Rock-Paper-Scissors & Voice Quiz)
    rpsRound: RpsRoundState?,
    rpsWins: Int,
    rpsDraws: Int,
    rpsLosses: Int,
    onPlayRps: (RpsChoice) -> Unit,
    quizQuestion: QuizQuestion?,
    selectedQuizOption: Int?,
    quizScore: Int,
    isGeneratingQuiz: Boolean,
    onGenerateQuiz: (String) -> Unit,
    onAnswerQuiz: (Int) -> Unit,
    // Interactive Storytelling (Dongeng AI)
    selectedStoryTheme: StoryTheme,
    currentStoryNode: StoryNode?,
    isGeneratingStory: Boolean,
    savedStoriesCount: Int = 0,
    onStartStory: (StoryTheme) -> Unit,
    onChooseStoryBranch: (String) -> Unit,
    onOpenFullStoryStage: () -> Unit = {},
    // Healthy Routines & Time Greetings
    waterGlassesToday: Int,
    stretchCountToday: Int,
    onTriggerHealthyHabit: (HealthyHabitType) -> Unit,
    onTriggerTimeGreeting: () -> Unit,
    onSpeakText: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xF20F172A),
            border = BorderStroke(1.5.dp, Color(0x55818CF8)),
            shadowElevation = 20.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .testTag("fun_activities_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x336366F1))
                        ) {
                            Text(text = "✨", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Aktivitas Seru Bersama ${character.name}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                            Text(
                                text = "Mini-Game • Dongeng Interaktif • Rutinitas Sehat",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF93C5FD),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3-Tab Switcher
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FunActivityTab.entries.forEach { tab ->
                        val isSelected = tab == activeTab
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFF6366F1) else Color(0x1FFFFFFF),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFA5B4FC) else Color(0x30FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectTab(tab) }
                                .testTag("fun_tab_${tab.name}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                            ) {
                                Text(text = tab.emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = tab.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    when (activeTab) {
                        FunActivityTab.MINI_GAME -> {
                            item {
                                MiniGameSection(
                                    character = character,
                                    rpsRound = rpsRound,
                                    rpsWins = rpsWins,
                                    rpsDraws = rpsDraws,
                                    rpsLosses = rpsLosses,
                                    onPlayRps = onPlayRps,
                                    quizQuestion = quizQuestion,
                                    selectedQuizOption = selectedQuizOption,
                                    quizScore = quizScore,
                                    isGeneratingQuiz = isGeneratingQuiz,
                                    onGenerateQuiz = onGenerateQuiz,
                                    onAnswerQuiz = onAnswerQuiz,
                                    onSpeakText = onSpeakText
                                )
                            }
                        }

                        FunActivityTab.STORYTELLING -> {
                            item {
                                StorytellingSection(
                                    character = character,
                                    selectedTheme = selectedStoryTheme,
                                    storyNode = currentStoryNode,
                                    isGenerating = isGeneratingStory,
                                    savedStoriesCount = savedStoriesCount,
                                    onStartStory = onStartStory,
                                    onChooseBranch = onChooseStoryBranch,
                                    onOpenFullStoryStage = onOpenFullStoryStage,
                                    onSpeakText = onSpeakText
                                )
                            }
                        }

                        FunActivityTab.HEALTHY_ROUTINE -> {
                            item {
                                HealthyRoutineSection(
                                    character = character,
                                    currentTimeOfDay = currentTimeOfDay,
                                    waterGlassesToday = waterGlassesToday,
                                    stretchCountToday = stretchCountToday,
                                    onTriggerHabit = onTriggerHealthyHabit,
                                    onTriggerTimeGreeting = onTriggerTimeGreeting
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
private fun MiniGameSection(
    character: PetCharacter,
    rpsRound: RpsRoundState?,
    rpsWins: Int,
    rpsDraws: Int,
    rpsLosses: Int,
    onPlayRps: (RpsChoice) -> Unit,
    quizQuestion: QuizQuestion?,
    selectedQuizOption: Int?,
    quizScore: Int,
    isGeneratingQuiz: Boolean,
    onGenerateQuiz: (String) -> Unit,
    onAnswerQuiz: (Int) -> Unit,
    onSpeakText: (String) -> Unit
) {
    var selectedQuizTopic by remember { mutableStateOf("Sains & Antariksa") }
    val quizTopics = listOf(
        "Sains & Antariksa",
        "Pengetahuan Umum",
        "Logika & Teka-Teki",
        "Dunia Hewan",
        "Sejarah & Budaya"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Rock Paper Scissors Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0x241E293B),
            border = BorderStroke(1.dp, Color(0x45F59E0B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "✊✌️✋", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Adu Suit 3D Lawan ${character.name}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x33F59E0B)
                    ) {
                        Text(
                            text = "M:$rpsWins • S:$rpsDraws • K:$rpsLosses",
                            color = Color(0xFFFDE68A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RpsChoice.entries.forEach { choice ->
                        Button(
                            onClick = { onPlayRps(choice) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF334155),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rps_btn_${choice.name}")
                        ) {
                            Text(text = "${choice.emoji} ${choice.label}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (rpsRound != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val resultColor = when (rpsRound.result) {
                        RpsResult.WIN -> Color(0xFF10B981)
                        RpsResult.DRAW -> Color(0xFFF59E0B)
                        RpsResult.LOSE -> Color(0xFFEF4444)
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = resultColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, resultColor.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Kamu: ${rpsRound.userChoice.emoji} ${rpsRound.userChoice.label}  vs  ${character.name}: ${rpsRound.petChoice.emoji} ${rpsRound.petChoice.label}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${rpsRound.result.badgeEmoji} ${rpsRound.result.title}",
                                    color = resultColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${rpsRound.commentary}\"",
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Voice & AI Trivia Quiz Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0x241E293B),
            border = BorderStroke(1.dp, Color(0x456366F1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🧠", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kuis Suara Cerdas Cermat AI",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x3310B981)
                    ) {
                        Text(
                            text = "Skor: $quizScore ⭐",
                            color = Color(0xFF6EE7B7),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quizTopics) { topic ->
                        val isSel = topic == selectedQuizTopic
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) Color(0xFF6366F1) else Color(0x22FFFFFF),
                            modifier = Modifier.clickable { selectedQuizTopic = topic }
                        ) {
                            Text(
                                text = topic,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onGenerateQuiz(selectedQuizTopic) },
                    enabled = !isGeneratingQuiz,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_quiz_button")
                ) {
                    if (isGeneratingQuiz) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini AI sedang menyiapkan soal...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (quizQuestion == null) "Mulai Soal Kuis AI ($selectedQuizTopic)" else "Soal Kuis Baru ($selectedQuizTopic)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (quizQuestion != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = quizQuestion.question,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onSpeakText(quizQuestion.question) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Bacakan Soal",
                                tint = Color(0xFF93C5FD),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val optionLabels = listOf("A", "B", "C", "D")
                    quizQuestion.options.forEachIndexed { idx, optText ->
                        val isAnswered = selectedQuizOption != null
                        val isThisSelected = selectedQuizOption == idx
                        val isCorrect = idx == quizQuestion.correctIndex

                        val bgColor = when {
                            !isAnswered -> Color(0x28FFFFFF)
                            isCorrect -> Color(0x3810B981)
                            isThisSelected -> Color(0x38EF4444)
                            else -> Color(0x18FFFFFF)
                        }
                        val borderColor = when {
                            !isAnswered -> Color(0x35FFFFFF)
                            isCorrect -> Color(0xFF10B981)
                            isThisSelected -> Color(0xFFEF4444)
                            else -> Color.Transparent
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = bgColor,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable(enabled = !isAnswered) { onAnswerQuiz(idx) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = "${optionLabels.getOrElse(idx) { "" }}.",
                                    color = Color(0xFF93C5FD),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = optText,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (selectedQuizOption != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val won = selectedQuizOption == quizQuestion.correctIndex
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (won) Color(0x2510B981) else Color(0x25F59E0B),
                            border = BorderStroke(1.dp, if (won) Color(0xFF10B981) else Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (won) "🎉 Jawabanmu Benar!" else "💡 Jawaban yang tepat: ${quizQuestion.options.getOrElse(quizQuestion.correctIndex) { "" }}",
                                    color = if (won) Color(0xFF6EE7B7) else Color(0xFFFDE68A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = quizQuestion.explanation,
                                    color = Color.White,
                                    fontSize = 12.sp
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
private fun StorytellingSection(
    character: PetCharacter,
    selectedTheme: StoryTheme,
    storyNode: StoryNode?,
    isGenerating: Boolean,
    savedStoriesCount: Int,
    onStartStory: (StoryTheme) -> Unit,
    onChooseBranch: (String) -> Unit,
    onOpenFullStoryStage: () -> Unit,
    onSpeakText: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Prominent Full-Screen 3D Story Theater Banner
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0x386366F1),
            border = BorderStroke(1.5.dp, Color(0xFF818CF8)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenFullStoryStage)
                .testTag("open_fullscreen_story_stage_banner")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "🎭", fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Panggung Dongeng 3D Layar Penuh",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Petto 3D ikut beraksi • Tas Item Ajaib • Ketik/Suara Bebas • Simpanan ($savedStoriesCount)",
                            color = Color(0xFFC7D2FE),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onOpenFullStoryStage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Buka 3D", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "Pilih Dunia Dongeng & Petualangan:",
            color = Color(0xFFE2E8F0),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(StoryTheme.entries) { theme ->
                val isSelected = theme == selectedTheme
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color(0x386366F1) else Color(0x1FFFFFFF),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF818CF8) else Color(0x30FFFFFF)),
                    modifier = Modifier
                        .width(175.dp)
                        .clickable { onStartStory(theme) }
                        .testTag("story_theme_${theme.name}")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = theme.emoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = theme.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = theme.subtitle,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            maxLines = 2,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }

        if (isGenerating) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0x281E1B4B),
                border = BorderStroke(1.dp, Color(0x55818CF8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF818CF8),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${character.name} sedang merangkai babak cerita selanjutnya...",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (storyNode == null) {
            Button(
                onClick = { onStartStory(selectedTheme) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("start_story_button")
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mulai Dongeng: ${selectedTheme.title}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x281E293B),
                border = BorderStroke(1.5.dp, Color(0x66818CF8)),
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
                            color = Color(0x336366F1)
                        ) {
                            Text(
                                text = "📖 ${storyNode.title}",
                                color = Color(0xFFC7D2FE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { onSpeakText(storyNode.narrative) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Bacakan Cerita",
                                tint = Color(0xFF93C5FD)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = storyNode.narrative,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (storyNode.isEnding) {
                        Button(
                            onClick = { onStartStory(selectedTheme) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("✨ Mulai Petualangan Baru", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Text(
                            text = "Tentukan Pilihan Jalan Ceritamu:",
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onChooseBranch(storyNode.choiceA) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4F46E5),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("story_choice_a")
                        ) {
                            Text(
                                text = "🅰️ ${storyNode.choiceA}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = { onChooseBranch(storyNode.choiceB) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0EA5E9),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("story_choice_b")
                        ) {
                            Text(
                                text = "🅱️ ${storyNode.choiceB}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthyRoutineSection(
    character: PetCharacter,
    currentTimeOfDay: TimeOfDay,
    waterGlassesToday: Int,
    stretchCountToday: Int,
    onTriggerHabit: (HealthyHabitType) -> Unit,
    onTriggerTimeGreeting: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Automatic Time-of-Day Greeting Banner
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0x2838BDF8),
            border = BorderStroke(1.dp, Color(0x5538BDF8)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = currentTimeOfDay.icon, fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Suasana Waktu: ${currentTimeOfDay.displayName}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Dengarkan sapaan & pesan perhatian waktu nyata dari ${character.name}",
                            color = Color(0xFFBAE6FD),
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onTriggerTimeGreeting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("time_greeting_button")
                ) {
                    Text("🔊 Sapa Aku", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Daily Hydration Progress Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0x241E293B),
            border = BorderStroke(1.dp, Color(0x4510B981)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💧 Target Minum Air Hari Ini",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "$waterGlassesToday / 8 Gelas • Peregangan: ${stretchCountToday}x",
                        color = Color(0xFF6EE7B7),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (waterGlassesToday / 8f).coerceIn(0f, 1f) },
                    color = Color(0xFF10B981),
                    trackColor = Color(0x33FFFFFF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // 3. Interactive Healthy Habits List
        HealthyHabitType.entries.forEach { habit ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0x1FFFFFFF),
                border = BorderStroke(1.dp, Color(0x30FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = habit.emoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = habit.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = habit.subtitle,
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onTriggerHabit(habit) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("habit_btn_${habit.name}")
                    ) {
                        Text(habit.buttonLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

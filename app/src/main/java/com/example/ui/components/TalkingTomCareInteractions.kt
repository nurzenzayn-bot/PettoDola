package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FoodItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/**
 * 1. Interactive Food Tray for Feeding Tom
 */
@Composable
fun FoodFeedingTray(
    hungerLevel: Float,
    onFeedFood: (FoodItem) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xF02B1810),
                        Color(0xF01A0E08)
                    )
                )
            )
            .border(2.dp, Color(0xFFE5A93C), RoundedCornerShape(24.dp))
            .padding(14.dp)
            .testTag("food_feeding_tray")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🍽️ Beri Makan Tom",
                        color = Color(0xFFFFD54F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tingkat Kenyang: ${(hungerLevel * 100).toInt()}%",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Piring",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Food items row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(FoodItem.values()) { food ->
                    FoodCard(food = food, onSelect = { onFeedFood(food) })
                }
            }
        }
    }
}

@Composable
private fun FoodCard(
    food: FoodItem,
    onSelect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag("food_item_${food.id}")
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF3E2723))
            .border(1.5.dp, Color(0xFFFFB74D), RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF))
        ) {
            Text(text = food.emoji, fontSize = 28.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = food.displayName,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "+${(food.hungerRestore * 100).toInt()}%",
            color = Color(0xFF4ADE80),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 2. Toilet Sequence Overlay
 */
@Composable
fun ToiletRoomOverlay(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val flushAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        flushAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
        )
        onDone()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xB00284C7))
            .testTag("toilet_overlay")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🚽",
                fontSize = 80.sp,
                modifier = Modifier.graphicsLayer {
                    rotationZ = sin(flushAnim.value * 12.56f) * 12f
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sedang di Toilet...",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "🌊 Flush! Bersih dan Segar!",
                color = Color(0xFFE0F2FE),
                fontSize = 15.sp
            )
        }
    }
}

/**
 * 3. Sleep Mode with Lamp Pull Cord and Floating "Zzz"
 */
@Composable
fun SleepModeOverlay(
    isLightsOff: Boolean,
    onToggleLight: () -> Unit,
    onWakeUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val zzzParticles = remember { mutableStateListOf<Float>() }

    LaunchedEffect(isLightsOff) {
        while (isLightsOff && isActive) {
            delay(1200)
            zzzParticles.add(Random.nextFloat())
            if (zzzParticles.size > 5) {
                zzzParticles.removeAt(0)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isLightsOff) Color(0xD8050C1A) else Color.Transparent)
            .testTag("sleep_mode_overlay")
    ) {
        // Ceiling hanging lamp with pull switch (Top Center)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            // Lamp cord line
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(55.dp)
                    .background(Color(0xFFFFD54F))
            )

            // Pull bulb switch
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(if (isLightsOff) Color(0xFF374151) else Color(0xFFFFD54F))
                    .border(2.dp, Color.White, CircleShape)
                    .clickable(onClick = onToggleLight)
                    .testTag("sleep_lamp_pull_switch")
            ) {
                Text(
                    text = if (isLightsOff) "💡" else "⭐",
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isLightsOff) "Sentuh Lampu untuk Bangun" else "Tarik Lampu untuk Tidur",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black,
                        offset = Offset(1f, 1f),
                        blurRadius = 2f
                    )
                )
            )
        }

        // Floating "Zzz" Letters if lights off
        if (isLightsOff) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-60).dp)
            ) {
                Text(
                    text = "Z z z . . .",
                    color = Color(0xFF93C5FD),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Tom sedang tidur lelap 🌙",
                    color = Color(0xFFBFDBFE),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 4. Bouncing Beach Ball Mini-game for "Bermain/Hiburan"
 */
@Composable
fun BouncingBallMiniGame(
    score: Int,
    onBallBounce: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val ballY = remember { Animatable(0.2f) }
    val ballX = remember { Animatable(0.5f) }
    val ballScale = remember { Animatable(1f) }

    // Ball gravity bounce physics
    LaunchedEffect(Unit) {
        while (isActive) {
            // Fall down
            ballY.animateTo(
                targetValue = 0.72f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
            // Auto bounce back if not tapped
            ballY.animateTo(
                targetValue = 0.35f,
                animationSpec = tween(900, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("bouncing_ball_minigame")
    ) {
        // Top Game Score Header
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .shadow(6.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xCC000000))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚽ Pantulan: $score",
                color = Color(0xFF4ADE80),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Ketuk Bola!",
                color = Color.White,
                fontSize = 14.sp
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup Permainan",
                    tint = Color.White
                )
            }
        }

        // Bouncy Beach Ball
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxSize()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = ((ballX.value * 280) - 140).dp.roundToPx(),
                            y = (ballY.value * 500).dp.roundToPx()
                        )
                    }
                    .size(68.dp)
                    .scale(ballScale.value)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFFF5964),
                                Color(0xFFFFD166),
                                Color(0xFF06D6A0),
                                Color(0xFF118AB2)
                            )
                        )
                    )
                    .border(2.5.dp, Color.White, CircleShape)
                    .clickable {
                        onBallBounce()
                        coroutineScope.launch {
                            // Squash & bounce high
                            ballScale.snapTo(1.25f)
                            ballScale.animateTo(1f, spring(dampingRatio = 0.4f))
                        }
                        coroutineScope.launch {
                            ballY.animateTo(0.18f, tween(350, easing = FastOutSlowInEasing))
                        }
                    }
                    .testTag("interactive_bouncing_ball")
            ) {
                Text(
                    text = "🏐",
                    fontSize = 38.sp
                )
            }
        }
    }
}

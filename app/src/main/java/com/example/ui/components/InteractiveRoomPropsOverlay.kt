package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BackgroundTheme
import com.example.model.FoodItem
import kotlinx.coroutines.launch
import kotlin.math.sin

enum class RoomPropAction {
    TOGGLE_LOFI_RADIO,
    BOUNCE_TOY_BALL,
    TOGGLE_NIGHT_LAMP,
    HUG_STAR_PLUSH,
    SERVE_KITCHEN_SNACK,
    BREW_WARM_TEA,
    BLOW_BUBBLES
}

/**
 * Interactive 3D Room Props Overlay:
 * Renders contextual room objects (Lo-Fi Radio, Bouncing Ball, Night Lamp, Star Plush,
 * Kitchen Snacks, Warm Tea Kettle) that users can tap to interact with the environment and pet.
 */
@Composable
fun InteractiveRoomPropsOverlay(
    theme: BackgroundTheme,
    isRadioPlaying: Boolean,
    isNightLampDimmed: Boolean,
    ballBounceCount: Int,
    onPropTriggered: (RoomPropAction, FoodItem?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // Bouncing ball physics when triggered in Living Room
    val ballOffsetY = remember { Animatable(0f) }
    var snackIndex by remember { mutableIntStateOf(0) }
    var isTeaSteaming by remember { mutableStateOf(false) }
    var areBubblesActive by remember { mutableStateOf(false) }

    val snacks = remember {
        listOf(FoodItem.STRAWBERRY, FoodItem.BURGER, FoodItem.MILK, FoodItem.CAKE)
    }
    val currentSnack = snacks[snackIndex % snacks.size]

    val infiniteTransition = rememberInfiniteTransition(label = "roomPropsAmbient")
    val floatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floatPhase"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Reset tea steam after a few seconds
    LaunchedEffect(isTeaSteaming) {
        if (isTeaSteaming) {
            kotlinx.coroutines.delay(5000)
            isTeaSteaming = false
        }
    }

    LaunchedEffect(areBubblesActive) {
        if (areBubblesActive) {
            kotlinx.coroutines.delay(5500)
            areBubblesActive = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Bedroom Night Lamp Warm Dimming & Starlight Overlay
        if (isNightLampDimmed) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x33F59E0B),
                            Color(0x99090D1A),
                            Color(0xCC050811)
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.55f),
                        radius = size.maxDimension * 0.65f
                    )
                )
                // Twinkling cozy night stars
                val starPositions = listOf(
                    Offset(0.18f, 0.22f),
                    Offset(0.82f, 0.20f),
                    Offset(0.28f, 0.34f),
                    Offset(0.74f, 0.32f),
                    Offset(0.50f, 0.18f)
                )
                starPositions.forEachIndexed { idx, pos ->
                    val alpha = (0.4f + 0.5f * sin(floatPhase + idx * 1.3f)).coerceIn(0.15f, 0.95f)
                    drawCircle(
                        color = Color(0xFFFFF59D).copy(alpha = alpha),
                        radius = 4.dp.toPx(),
                        center = Offset(size.width * pos.x, size.height * pos.y)
                    )
                }
            }
        }

        // 2. Floating Lo-Fi Radio Music Notes when Radio is ON
        if (isRadioPlaying) {
            val notes = listOf("🎵", "🎶", "🎷", "✨")
            notes.forEachIndexed { idx, note ->
                val yOffset = -((floatPhase * 28f + idx * 42f) % 160f)
                val xWave = sin(floatPhase + idx) * 18f
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 28.dp, top = 80.dp)
                        .offset { IntOffset((xWave + idx * 12f).toInt(), yOffset.toInt()) }
                ) {
                    Text(text = note, fontSize = 20.sp)
                }
            }
        }

        // 3. Kitchen Warm Tea Steam Particles
        if (isTeaSteaming) {
            val steamSymbols = listOf("♨️", "☁️", "✨")
            steamSymbols.forEachIndexed { idx, sym ->
                val yOffset = -((floatPhase * 24f + idx * 35f) % 130f)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 32.dp, top = 60.dp)
                        .offset { IntOffset(0, yOffset.toInt()) }
                ) {
                    Text(text = sym, fontSize = 18.sp)
                }
            }
        }

        // 4. Floating Iridescent Bubbles (Garden / Custom themes)
        if (areBubblesActive) {
            val bubbles = listOf("🫧", "✨", "🫧", "🦋")
            bubbles.forEachIndexed { idx, sym ->
                val yOffset = -((floatPhase * 30f + idx * 45f) % 180f)
                val xWave = sin(floatPhase + idx * 1.5f) * 24f
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset((xWave + (idx - 1.5f) * 55f).toInt(), yOffset.toInt()) }
                ) {
                    Text(text = sym, fontSize = 22.sp)
                }
            }
        }

        // 5. Left & Right Interactive 3D Room Prop Dock
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 22.dp)
        ) {
            when (theme) {
                BackgroundTheme.LIVING_ROOM -> {
                    // Left Prop: Lo-Fi Radio
                    InteractivePropCard(
                        emoji = "📻",
                        label = if (isRadioPlaying) "Radio ON" else "Radio Lo-Fi",
                        badgeText = if (isRadioPlaying) "🎵" else null,
                        isActive = isRadioPlaying,
                        activeColor = Color(0xFF8B5CF6),
                        scaleMultiplier = if (isRadioPlaying) pulseScale else 1f,
                        testTag = "prop_lofi_radio",
                        onClick = {
                            onPropTriggered(RoomPropAction.TOGGLE_LOFI_RADIO, null)
                        }
                    )

                    // Right Prop: Bouncing Toy Ball
                    Box(
                        modifier = Modifier.offset { IntOffset(0, ballOffsetY.value.toInt()) }
                    ) {
                        InteractivePropCard(
                            emoji = "⚽",
                            label = "Main Bola",
                            badgeText = if (ballBounceCount > 0) "x$ballBounceCount" else null,
                            isActive = ballBounceCount > 0,
                            activeColor = Color(0xFF10B981),
                            testTag = "prop_toy_ball",
                            onClick = {
                                onPropTriggered(RoomPropAction.BOUNCE_TOY_BALL, null)
                                scope.launch {
                                    ballOffsetY.animateTo(-65f, tween(160, easing = FastOutSlowInEasing))
                                    ballOffsetY.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 450f))
                                }
                            }
                        )
                    }
                }

                BackgroundTheme.BEDROOM -> {
                    // Left Prop: Night Lamp Switch
                    InteractivePropCard(
                        emoji = if (isNightLampDimmed) "🌙" else "🏮",
                        label = if (isNightLampDimmed) "Lampu Redup" else "Lampu Tidur",
                        badgeText = if (isNightLampDimmed) "ON" else null,
                        isActive = isNightLampDimmed,
                        activeColor = Color(0xFFF59E0B),
                        testTag = "prop_night_lamp",
                        onClick = {
                            onPropTriggered(RoomPropAction.TOGGLE_NIGHT_LAMP, null)
                        }
                    )

                    // Right Prop: Star Plushie
                    InteractivePropCard(
                        emoji = "🌟",
                        label = "Boneka Bintang",
                        isActive = false,
                        activeColor = Color(0xFFEC4899),
                        testTag = "prop_star_plush",
                        onClick = {
                            onPropTriggered(RoomPropAction.HUG_STAR_PLUSH, null)
                        }
                    )
                }

                BackgroundTheme.KITCHEN -> {
                    // Left Prop: Serve Fresh Snack
                    InteractivePropCard(
                        emoji = currentSnack.emoji,
                        label = "Beri ${currentSnack.displayName}",
                        badgeText = "🍽️",
                        isActive = true,
                        activeColor = Color(0xFFF97316),
                        testTag = "prop_kitchen_snack",
                        onClick = {
                            val chosen = currentSnack
                            snackIndex++
                            onPropTriggered(RoomPropAction.SERVE_KITCHEN_SNACK, chosen)
                        }
                    )

                    // Right Prop: Warm Tea Kettle
                    InteractivePropCard(
                        emoji = "🍵",
                        label = "Teh Hangat",
                        badgeText = if (isTeaSteaming) "♨️" else null,
                        isActive = isTeaSteaming,
                        activeColor = Color(0xFF14B8A6),
                        testTag = "prop_warm_tea",
                        onClick = {
                            isTeaSteaming = true
                            onPropTriggered(RoomPropAction.BREW_WARM_TEA, null)
                        }
                    )
                }

                else -> {
                    // Left Prop: Lo-Fi Music
                    InteractivePropCard(
                        emoji = "📻",
                        label = if (isRadioPlaying) "Musik ON" else "Musik Santai",
                        isActive = isRadioPlaying,
                        activeColor = Color(0xFF8B5CF6),
                        scaleMultiplier = if (isRadioPlaying) pulseScale else 1f,
                        testTag = "prop_lofi_radio",
                        onClick = {
                            onPropTriggered(RoomPropAction.TOGGLE_LOFI_RADIO, null)
                        }
                    )

                    // Right Prop: Blow Bubbles
                    InteractivePropCard(
                        emoji = "🫧",
                        label = "Gelembung",
                        isActive = areBubblesActive,
                        activeColor = Color(0xFF38BDF8),
                        testTag = "prop_blow_bubbles",
                        onClick = {
                            areBubblesActive = true
                            onPropTriggered(RoomPropAction.BLOW_BUBBLES, null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractivePropCard(
    emoji: String,
    label: String,
    badgeText: String? = null,
    isActive: Boolean,
    activeColor: Color,
    scaleMultiplier: Float = 1f,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) activeColor.copy(alpha = 0.28f) else Color(0xBF0F172A),
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) activeColor else Color.White.copy(alpha = 0.22f)
        ),
        shadowElevation = 8.dp,
        modifier = Modifier
            .scale(scaleMultiplier)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) activeColor.copy(alpha = 0.35f)
                            else Color.White.copy(alpha = 0.10f)
                        )
                ) {
                    Text(text = emoji, fontSize = 22.sp)
                }

                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = activeColor
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            )
        }
    }
}

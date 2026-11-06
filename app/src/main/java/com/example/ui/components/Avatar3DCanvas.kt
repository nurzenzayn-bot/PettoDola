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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccessoryStyle
import com.example.model.AvatarCustomization
import com.example.model.AvatarEmotion
import com.example.model.BackgroundTheme
import com.example.model.CostumeStyle
import com.example.model.EarStyle
import com.example.model.EyeStyle
import com.example.model.IdlePetBehavior
import com.example.model.PetSpecies
import com.example.model.TouchZone
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

data class TouchParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val symbol: String,
    val color: Color
)

/**
 * Fully integrated modular 3D cartoon Talking Tom pet avatar.
 *
 * All facial and bodily components (eyes, pupils, eyelids, eyebrows, snout, talking mouth cavity,
 * teeth, tongue, ears, torso, limbs, costumes, and accessories) are natively integrated in a
 * unified 3D mathematical coordinate system.
 */
@Composable
fun Avatar3DCanvas(
    species: PetSpecies,
    customization: AvatarCustomization,
    emotion: AvatarEmotion,
    isTalking: Boolean,
    onZoneTouched: (TouchZone) -> Unit,
    modifier: Modifier = Modifier,
    backgroundTheme: BackgroundTheme = BackgroundTheme.LIVING_ROOM,
    idleBehavior: IdlePetBehavior = IdlePetBehavior.NONE
) {
    val coroutineScope = rememberCoroutineScope()

    // 3D Tilt angles (rotationX, rotationY in degrees)
    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }

    // Dynamic Gaze Tracking (pupils follow finger/touch location or idle look-around)
    val gazeX = remember { Animatable(0f) }
    val gazeY = remember { Animatable(0f) }

    // Zone & Idle reaction animations
    val headNuzzle = remember { Animatable(0f) }         // Head tilts up/down
    val bellyJiggle = remember { Animatable(0f) }        // Belly squash & stretch
    val handsWave = remember { Animatable(0f) }          // Paw high-five
    val feetJump = remember { Animatable(0f) }           // Jump / hop
    val cheekBlushAlpha = remember { Animatable(0f) }    // Rosy cheeks
    val earTwitch = remember { Animatable(0f) }          // Playful ear wiggle
    val leftArmAngle = remember { Animatable(0f) }       // 3D Left arm rotation offset
    val rightArmAngle = remember { Animatable(0f) }      // 3D Right arm rotation offset
    val idleStretchY = remember { Animatable(1f) }       // Vertical spine stretch factor
    val idleMouthOpen = remember { Animatable(0f) }      // Mouth opening for yawn / sneeze
    val idleBlinkOverride = remember { Animatable(0f) }  // Eyelid squint/close for stretch / sneeze / yawn

    // Active touch reaction zone for expressive rendering
    var activeReactionZone by remember { mutableStateOf<TouchZone?>(null) }

    // Natural eye blinking loop (smooth eyelid closing and opening)
    var blinkProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay((2800..4500).random().toLong())
            val anim = Animatable(0f)
            anim.animateTo(1f, tween(110, easing = LinearEasing))
            blinkProgress = 1f
            anim.animateTo(0f, tween(120, easing = LinearEasing))
            blinkProgress = 0f
        }
    }

    // Occasional playful ear twitch loop
    LaunchedEffect(Unit) {
        while (isActive) {
            delay((3400..5500).random().toLong())
            earTwitch.animateTo(7f, tween(80))
            earTwitch.animateTo(-5f, tween(90))
            earTwitch.animateTo(0f, tween(80))
        }
    }

    // Idle chest breathing & floating physics
    val infiniteTransition = rememberInfiniteTransition(label = "idleTalkingTomModular")
    val idleFloatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleFloat"
    )

    // Breathing chest expansion (squash & stretch)
    val chestBreathScale by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(2300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chestBreath"
    )

    // Tail wagging animation
    val tailWag by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tailWag"
    )

    // Animated mouth speech cycle
    val mouthCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(125, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouthCycle"
    )

    // Floating particles on touch or idle behaviors
    val touchParticles = remember { mutableStateListOf<TouchParticle>() }

    // Particle cleanup loop
    LaunchedEffect(touchParticles.size) {
        if (touchParticles.isNotEmpty()) {
            delay(1100)
            if (touchParticles.isNotEmpty()) {
                touchParticles.removeAt(0)
            }
        }
    }

    BoxWithConstraints(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newY = (tiltY.value + dragAmount.x * 0.16f).coerceIn(-18f, 18f)
                            val newX = (tiltX.value + dragAmount.y * 0.12f).coerceIn(-14f, 14f)
                            tiltY.snapTo(newY)
                            tiltX.snapTo(newX)

                            gazeX.snapTo((newY / 18f).coerceIn(-1f, 1f))
                            gazeY.snapTo((newX / 14f).coerceIn(-1f, 1f))
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            tiltX.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
                        }
                        coroutineScope.launch {
                            tiltY.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
                        }
                        coroutineScope.launch {
                            gazeX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f))
                            gazeY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f))
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        val charH = h * 0.76f
                        val charTop = h - charH - (h * 0.04f)
                        val relativeY = (offset.y - charTop) / charH
                        val relativeX = (offset.x - (w * 0.5f)) / (w * 0.5f)

                        // Direct pupil gaze to tap point
                        coroutineScope.launch {
                            gazeX.animateTo(relativeX.coerceIn(-1f, 1f), tween(90))
                            gazeY.animateTo((relativeY - 0.25f).coerceIn(-1f, 1f), tween(90))
                            delay(850)
                            gazeX.animateTo(0f, spring(dampingRatio = 0.6f))
                            gazeY.animateTo(0f, spring(dampingRatio = 0.6f))
                        }

                        val zone = when {
                            relativeY < 0.32f -> TouchZone.HEAD
                            relativeY > 0.76f -> TouchZone.FEET
                            abs(relativeX) > 0.44f && relativeY in 0.32f..0.74f -> TouchZone.HANDS
                            else -> TouchZone.BELLY
                        }

                        activeReactionZone = zone
                        onZoneTouched(zone)

                        // Trigger expressive living character physics
                        coroutineScope.launch {
                            when (zone) {
                                TouchZone.HEAD -> {
                                    launch {
                                        cheekBlushAlpha.animateTo(1f, tween(200))
                                        delay(1400)
                                        cheekBlushAlpha.animateTo(0f, tween(400))
                                    }
                                    headNuzzle.animateTo(-16f, spring(dampingRatio = 0.4f, stiffness = 600f))
                                    headNuzzle.animateTo(6f, spring(dampingRatio = 0.5f))
                                    headNuzzle.animateTo(0f, spring(dampingRatio = 0.6f))
                                    activeReactionZone = null
                                }
                                TouchZone.BELLY -> {
                                    bellyJiggle.animateTo(24f, spring(dampingRatio = 0.3f, stiffness = 800f))
                                    bellyJiggle.animateTo(-18f, spring(dampingRatio = 0.35f))
                                    bellyJiggle.animateTo(10f, spring(dampingRatio = 0.45f))
                                    bellyJiggle.animateTo(0f, spring(dampingRatio = 0.6f))
                                    activeReactionZone = null
                                }
                                TouchZone.HANDS -> {
                                    handsWave.animateTo(25f, spring(dampingRatio = 0.4f, stiffness = 650f))
                                    handsWave.animateTo(0f, spring(dampingRatio = 0.6f))
                                    activeReactionZone = null
                                }
                                TouchZone.FEET -> {
                                    feetJump.animateTo(-55f, spring(dampingRatio = 0.35f, stiffness = 550f))
                                    delay(90)
                                    feetJump.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
                                    activeReactionZone = null
                                }
                            }
                        }

                        val (symbol, color) = when (zone) {
                            TouchZone.HEAD -> "💖" to Color(0xFFFF4081)
                            TouchZone.BELLY -> "✨" to Color(0xFFFFB300)
                            TouchZone.HANDS -> "⭐" to Color(0xFF00B4D8)
                            TouchZone.FEET -> "💫" to Color(0xFF4CAF50)
                        }
                        touchParticles.add(
                            TouchParticle(
                                id = System.currentTimeMillis() + Random.nextLong(1000),
                                x = offset.x,
                                y = offset.y,
                                symbol = symbol,
                                color = color
                            )
                        )
                    }
                )
            }
    ) {
        val canvasWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(300f)
        val canvasHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(500f)

        // Autonomous 3D Idle Behavior Choreography (Stretching, Looking Around, Sneezing, Yawning, Ear Scratch, Happy Bounce)
        LaunchedEffect(idleBehavior, isTalking) {
            if (isTalking || idleBehavior == IdlePetBehavior.NONE) {
                launch { leftArmAngle.animateTo(0f, tween(220)) }
                launch { rightArmAngle.animateTo(0f, tween(220)) }
                launch { idleStretchY.animateTo(1f, tween(220)) }
                launch { idleMouthOpen.animateTo(0f, tween(180)) }
                launch { idleBlinkOverride.animateTo(0f, tween(180)) }
                return@LaunchedEffect
            }

            when (idleBehavior) {
                IdlePetBehavior.STRETCHING -> {
                    // Big full-body stretch: raise both arms high overhead, elongate torso, tilt head up, close eyes contentedly
                    launch { leftArmAngle.animateTo(-128f, tween(650, easing = FastOutSlowInEasing)) }
                    launch { rightArmAngle.animateTo(128f, tween(650, easing = FastOutSlowInEasing)) }
                    launch { idleStretchY.animateTo(1.075f, tween(650, easing = FastOutSlowInEasing)) }
                    launch { headNuzzle.animateTo(-14f, tween(600, easing = FastOutSlowInEasing)) }
                    launch { idleBlinkOverride.animateTo(0.85f, tween(450)) }
                    launch { idleMouthOpen.animateTo(0.35f, tween(450)) }
                    delay(450)
                    touchParticles.add(
                        TouchParticle(
                            id = System.currentTimeMillis() + Random.nextLong(1000),
                            x = canvasWidthPx * 0.65f,
                            y = canvasHeightPx * 0.28f,
                            symbol = "✨",
                            color = Color(0xFFFFD54F)
                        )
                    )
                    delay(950)
                    // Relax back down with a gentle bounce
                    launch { leftArmAngle.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 320f)) }
                    launch { rightArmAngle.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 320f)) }
                    launch { idleStretchY.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 350f)) }
                    launch { headNuzzle.animateTo(0f, spring(dampingRatio = 0.6f)) }
                    launch { idleBlinkOverride.animateTo(0f, tween(260)) }
                    launch { idleMouthOpen.animateTo(0f, tween(260)) }
                }

                IdlePetBehavior.LOOKING_AROUND -> {
                    // Curious room scan left -> right -> up -> center
                    launch { tiltY.animateTo(-14f, tween(520, easing = FastOutSlowInEasing)) }
                    launch { gazeX.animateTo(-0.88f, tween(420, easing = FastOutSlowInEasing)) }
                    launch { gazeY.animateTo(-0.15f, tween(420)) }
                    launch { earTwitch.animateTo(9f, tween(250)) }
                    delay(750)
                    touchParticles.add(
                        TouchParticle(
                            id = System.currentTimeMillis() + Random.nextLong(1000),
                            x = canvasWidthPx * 0.32f,
                            y = canvasHeightPx * 0.26f,
                            symbol = "👀",
                            color = Color(0xFF38BDF8)
                        )
                    )
                    launch { tiltY.animateTo(14f, tween(680, easing = FastOutSlowInEasing)) }
                    launch { gazeX.animateTo(0.88f, tween(580, easing = FastOutSlowInEasing)) }
                    launch { gazeY.animateTo(-0.35f, tween(580)) }
                    launch { earTwitch.animateTo(-8f, tween(250)) }
                    delay(800)
                    launch { tiltY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                    launch { gazeX.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                    launch { gazeY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                    launch { earTwitch.animateTo(0f, tween(220)) }
                }

                IdlePetBehavior.SNEEZING -> {
                    // Stage 1: "Ah... ah..." deep inhale build-up
                    launch { headNuzzle.animateTo(-16f, tween(380, easing = FastOutSlowInEasing)) }
                    launch { tiltX.animateTo(-8f, tween(380)) }
                    launch { idleBlinkOverride.animateTo(0.65f, tween(350)) }
                    launch { idleMouthOpen.animateTo(0.55f, tween(350)) }
                    launch { idleStretchY.animateTo(1.04f, tween(380)) }
                    delay(420)
                    // Second deeper inhale
                    launch { headNuzzle.animateTo(-24f, tween(320, easing = FastOutSlowInEasing)) }
                    launch { idleMouthOpen.animateTo(0.82f, tween(300)) }
                    launch { idleBlinkOverride.animateTo(0.85f, tween(300)) }
                    delay(360)
                    // Stage 2: "HACHOO!" sudden forward snap & hop
                    launch { headNuzzle.animateTo(26f, tween(130, easing = LinearEasing)) }
                    launch { tiltX.animateTo(12f, tween(130)) }
                    launch { feetJump.animateTo(-24f, tween(130)) }
                    launch { idleStretchY.animateTo(0.94f, tween(130)) }
                    launch { idleBlinkOverride.animateTo(1f, tween(90)) }
                    launch { leftArmAngle.animateTo(-45f, tween(130)) }
                    launch { rightArmAngle.animateTo(45f, tween(130)) }
                    touchParticles.add(
                        TouchParticle(
                            id = System.currentTimeMillis() + Random.nextLong(1000),
                            x = canvasWidthPx * 0.56f,
                            y = canvasHeightPx * 0.36f,
                            symbol = "💨",
                            color = Color(0xFF93C5FD)
                        )
                    )
                    delay(160)
                    launch { feetJump.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 450f)) }
                    launch { idleMouthOpen.animateTo(0.15f, tween(220)) }
                    delay(350)
                    // Recover & wiggle ears
                    launch { headNuzzle.animateTo(0f, spring(dampingRatio = 0.55f)) }
                    launch { tiltX.animateTo(0f, spring(dampingRatio = 0.55f)) }
                    launch { idleStretchY.animateTo(1f, spring(dampingRatio = 0.55f)) }
                    launch { leftArmAngle.animateTo(0f, spring(dampingRatio = 0.6f)) }
                    launch { rightArmAngle.animateTo(0f, spring(dampingRatio = 0.6f)) }
                    launch { idleBlinkOverride.animateTo(0f, tween(280)) }
                    launch { idleMouthOpen.animateTo(0f, tween(280)) }
                }

                IdlePetBehavior.YAWNING -> {
                    // Sleepy wide yawn while raising left paw toward mouth
                    launch { leftArmAngle.animateTo(-86f, tween(550, easing = FastOutSlowInEasing)) }
                    launch { idleMouthOpen.animateTo(0.92f, tween(600, easing = FastOutSlowInEasing)) }
                    launch { idleBlinkOverride.animateTo(0.88f, tween(520)) }
                    launch { headNuzzle.animateTo(-9f, tween(550)) }
                    launch { tiltY.animateTo(-7f, tween(550)) }
                    delay(550)
                    touchParticles.add(
                        TouchParticle(
                            id = System.currentTimeMillis() + Random.nextLong(1000),
                            x = canvasWidthPx * 0.64f,
                            y = canvasHeightPx * 0.27f,
                            symbol = "💤",
                            color = Color(0xFFA78BFA)
                        )
                    )
                    delay(950)
                    launch { leftArmAngle.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
                    launch { idleMouthOpen.animateTo(0f, tween(350)) }
                    launch { idleBlinkOverride.animateTo(0f, tween(350)) }
                    launch { headNuzzle.animateTo(0f, spring(dampingRatio = 0.6f)) }
                    launch { tiltY.animateTo(0f, spring(dampingRatio = 0.6f)) }
                }

                IdlePetBehavior.EAR_SCRATCH -> {
                    // Tilt head to right and scratch ear with right paw
                    launch { tiltY.animateTo(13f, tween(380, easing = FastOutSlowInEasing)) }
                    launch { headNuzzle.animateTo(7f, tween(380)) }
                    launch { rightArmAngle.animateTo(108f, tween(380, easing = FastOutSlowInEasing)) }
                    launch { idleBlinkOverride.animateTo(0.55f, tween(300)) }
                    delay(380)
                    touchParticles.add(
                        TouchParticle(
                            id = System.currentTimeMillis() + Random.nextLong(1000),
                            x = canvasWidthPx * 0.66f,
                            y = canvasHeightPx * 0.28f,
                            symbol = "🐾",
                            color = Color(0xFFF472B6)
                        )
                    )
                    repeat(4) {
                        rightArmAngle.animateTo(122f, tween(110, easing = LinearEasing))
                        earTwitch.snapTo(10f)
                        rightArmAngle.animateTo(96f, tween(110, easing = LinearEasing))
                        earTwitch.snapTo(-6f)
                    }
                    earTwitch.animateTo(0f, tween(150))
                    launch { rightArmAngle.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 320f)) }
                    launch { tiltY.animateTo(0f, spring(dampingRatio = 0.6f)) }
                    launch { headNuzzle.animateTo(0f, spring(dampingRatio = 0.6f)) }
                    launch { idleBlinkOverride.animateTo(0f, tween(250)) }
                }

                IdlePetBehavior.HAPPY_BOUNCE -> {
                    // Two cheerful little bounces in place with musical note
                    launch { cheekBlushAlpha.animateTo(0.85f, tween(250)) }
                    touchParticles.add(
                        TouchParticle(
                            id = System.currentTimeMillis() + Random.nextLong(1000),
                            x = canvasWidthPx * 0.64f,
                            y = canvasHeightPx * 0.27f,
                            symbol = "🎵",
                            color = Color(0xFF34D399)
                        )
                    )
                    repeat(2) {
                        launch { leftArmAngle.animateTo(-48f, tween(180)) }
                        launch { rightArmAngle.animateTo(48f, tween(180)) }
                        feetJump.animateTo(-30f, tween(180, easing = FastOutSlowInEasing))
                        launch { leftArmAngle.animateTo(0f, tween(200)) }
                        launch { rightArmAngle.animateTo(0f, tween(200)) }
                        feetJump.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 450f))
                        delay(90)
                    }
                    launch { cheekBlushAlpha.animateTo(0f, tween(400)) }
                }

                IdlePetBehavior.NONE -> Unit
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("avatar_3d_canvas")
        ) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f

            // ========================================================
            // 76% SCREEN HEIGHT SCALE (Talking Tom Standard)
            // ========================================================
            val targetHeight = h * 0.76f
            val baseScale = (targetHeight / 480f).coerceIn(1.20f, 2.70f)

            // Anchor point: feet planted firmly on carpet floor
            val floorY = h * 0.84f
            val feetAnchor = 220f * baseScale
            val cy = floorY - feetAnchor
            val dynamicY = cy + idleFloatY + feetJump.value + (headNuzzle.value * 0.4f)

            // Room lighting colors
            val (roomKeyLight, roomRimColor, roomShadowColor) = when (backgroundTheme) {
                BackgroundTheme.LIVING_ROOM -> Triple(
                    Color(0xFFFFF9ED),
                    Color(0xFFFFD54F).copy(alpha = 0.55f),
                    Color(0x95261405)
                )
                BackgroundTheme.BEDROOM -> Triple(
                    Color(0xFFEAF2FD),
                    Color(0xFF81D4FA).copy(alpha = 0.60f),
                    Color(0x900D152B)
                )
                BackgroundTheme.KITCHEN -> Triple(
                    Color(0xFFF0FDF4),
                    Color(0xFFA7F3D0).copy(alpha = 0.55f),
                    Color(0x850E2413)
                )
                else -> Triple(
                    Color(0xFFFFFBEB),
                    Color(0xFFFFE082).copy(alpha = 0.50f),
                    Color(0x851C1710)
                )
            }

            // ========================================================
            // 1. DUAL-PASS FLOOR CONTACT SHADOWS (Room Grounding)
            // ========================================================
            val jumpPercent = (abs(feetJump.value) / 55f).coerceIn(0f, 1f)
            val shadowScale = (1f - jumpPercent * 0.35f)
            val shadowAlpha = (1f - jumpPercent * 0.45f)
            val shadowWidth = 280f * baseScale * shadowScale
            val shadowHeight = 56f * baseScale * shadowScale
            val shadowCenterY = floorY + 10f * baseScale

            // Pass 1: Soft Ambient Occlusion Body Shadow on the rug
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        roomShadowColor.copy(alpha = 0.88f * shadowAlpha),
                        roomShadowColor.copy(alpha = 0.35f * shadowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(cx + tiltY.value * 1.8f, shadowCenterY),
                    radius = shadowWidth * 0.55f
                ),
                topLeft = Offset(cx - shadowWidth * 0.5f + tiltY.value * 1.8f, shadowCenterY - shadowHeight * 0.5f),
                size = Size(shadowWidth, shadowHeight)
            )

            // Pass 2: Tight Dark Contact Shadows directly under left & right foot
            val footContactSpacing = 68f * baseScale
            val contactShadowW = 68f * baseScale * shadowScale
            val contactShadowH = 22f * baseScale * shadowScale
            val contactColor = Color(0xB5080503).copy(alpha = 0.88f * shadowAlpha)

            drawOval(
                color = contactColor,
                topLeft = Offset(cx - footContactSpacing - contactShadowW * 0.5f, floorY - contactShadowH * 0.2f),
                size = Size(contactShadowW, contactShadowH)
            )
            drawOval(
                color = contactColor,
                topLeft = Offset(cx + footContactSpacing - contactShadowW * 0.5f, floorY - contactShadowH * 0.2f),
                size = Size(contactShadowW, contactShadowH)
            )

            // ========================================================
            // 2. UNIFIED MODULAR 3D CARTOON CHARACTER (100% Integrated)
            // ========================================================
            val rotY = tiltY.value
            val rotX = tiltX.value
            val offsetX3D = rotY * 1.8f
            val offsetY3D = -rotX * 1.2f

            translate(left = offsetX3D, top = offsetY3D) {
                rotate(degrees = rotY * 0.22f, pivot = Offset(cx, dynamicY)) {
                    // Squash & stretch on breathing + tickle + idle stretch
                    val tickleSquash = (bellyJiggle.value * 0.007f) - (feetJump.value * 0.003f)
                    val stretchFactor = idleStretchY.value
                    val sX = (chestBreathScale + tickleSquash) / stretchFactor.coerceAtLeast(0.85f)
                    val sY = ((2f - chestBreathScale) - tickleSquash) * stretchFactor

                    scale(scaleX = sX, scaleY = sY, pivot = Offset(cx, floorY)) {
                        drawModularTalkingTomCharacter(
                            species = species,
                            customization = customization,
                            emotion = emotion,
                            cx = cx,
                            cy = dynamicY,
                            scale = baseScale,
                            rotY = rotY,
                            rotX = rotX,
                            gazeX = gazeX.value,
                            gazeY = gazeY.value,
                            blinkProgress = maxOf(blinkProgress, idleBlinkOverride.value),
                            earTwitch = earTwitch.value,
                            tailWag = tailWag,
                            isTalking = isTalking,
                            mouthCycle = mouthCycle,
                            activeReactionZone = activeReactionZone,
                            cheekBlushAlpha = cheekBlushAlpha.value,
                            roomKeyLight = roomKeyLight,
                            roomRimColor = roomRimColor,
                            leftArmAngleOffset = leftArmAngle.value,
                            rightArmAngleOffset = rightArmAngle.value,
                            idleMouthOpenOverride = idleMouthOpen.value
                        )
                    }
                }
            }
        }

        // ========================================================
        // 3. FLOATING TOUCH PARTICLES (Hearts, Stars, Sparks)
        // ========================================================
        touchParticles.forEach { p ->
            Box(
                modifier = Modifier
                    .offset { IntOffset(p.x.toInt() - 20, p.y.toInt() - 20) }
                    .size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = p.symbol,
                    fontSize = 26.sp
                )
            }
        }
    }
}

/**
 * Unified modular renderer for all 5 Talking Tom characters, including full 3D Costumes,
 * Accessories, EyeStyles, EarStyles, and Dynamic Expressions.
 */
private fun DrawScope.drawModularTalkingTomCharacter(
    species: PetSpecies,
    customization: AvatarCustomization,
    emotion: AvatarEmotion,
    cx: Float,
    cy: Float,
    scale: Float,
    rotY: Float,
    rotX: Float,
    gazeX: Float,
    gazeY: Float,
    blinkProgress: Float,
    earTwitch: Float,
    tailWag: Float,
    isTalking: Boolean,
    mouthCycle: Float,
    activeReactionZone: TouchZone?,
    cheekBlushAlpha: Float,
    roomKeyLight: Color,
    roomRimColor: Color,
    leftArmAngleOffset: Float = 0f,
    rightArmAngleOffset: Float = 0f,
    idleMouthOpenOverride: Float = 0f
) {
    val (primaryFur, darkFur, chestCream) = when (species) {
        PetSpecies.CAT -> Triple(
            customization.furColor.takeIf { it != Color.Unspecified } ?: Color(0xFF90A4AE),
            Color(0xFF546E7A),
            Color(0xFFECEFF1)
        )
        PetSpecies.RABBIT -> Triple(
            customization.furColor.takeIf { it != Color.Unspecified } ?: Color(0xFFFAFAFA),
            Color(0xFFE0E0E0),
            Color(0xFFFFF0F5)
        )
        PetSpecies.DOG -> Triple(
            customization.furColor.takeIf { it != Color.Unspecified } ?: Color(0xFFE5A93C),
            Color(0xFFB27B1E),
            Color(0xFFFFF8E7)
        )
        PetSpecies.CAPYBARA -> Triple(
            customization.furColor.takeIf { it != Color.Unspecified } ?: Color(0xFF8D6E63),
            Color(0xFF5D4037),
            Color(0xFFD7CCC8)
        )
        PetSpecies.BEAR -> Triple(
            customization.furColor.takeIf { it != Color.Unspecified } ?: Color(0xFF5D4037),
            Color(0xFF3E2723),
            Color(0xFFD7CCC8)
        )
    }

    val (irisTop, irisBottom) = when (species) {
        PetSpecies.CAT -> Color(0xFF00C853) to Color(0xFF69F0AE)
        PetSpecies.RABBIT -> Color(0xFFE91E63) to Color(0xFFFF80AB)
        PetSpecies.DOG -> Color(0xFFFF8F00) to Color(0xFFFFD54F)
        PetSpecies.CAPYBARA -> Color(0xFF5D4037) to Color(0xFF8D6E63)
        PetSpecies.BEAR -> Color(0xFFE65100) to Color(0xFFFFB300)
    }

    val noseColor = when (species) {
        PetSpecies.CAT, PetSpecies.RABBIT -> Color(0xFFFF8A80)
        PetSpecies.DOG -> Color(0xFF263238)
        PetSpecies.CAPYBARA -> Color(0xFF4E342E)
        PetSpecies.BEAR -> Color(0xFF212121)
    }

    val bodyW = 168f * scale
    val bodyH = 205f * scale
    val bodyY = cy + 42f * scale

    val headRadius = 104f * scale
    val headY = cy - 78f * scale

    // -------------------------------------------------------------
    // 0. BACK CAPES (Superhero Cape / Royal Robe behind torso)
    // -------------------------------------------------------------
    if (customization.costume == CostumeStyle.SUPERHERO || customization.costume == CostumeStyle.ROYAL_ROBE) {
        val capeColor = if (customization.costume == CostumeStyle.SUPERHERO) Color(0xFFD32F2F) else Color(0xFFB71C1C)
        val capeTrim = if (customization.costume == CostumeStyle.SUPERHERO) Color(0xFFFFCA28) else Color(0xFFFFF8E1)
        val capePath = Path().apply {
            moveTo(cx - bodyW * 0.42f, bodyY - bodyH * 0.35f)
            lineTo(cx + bodyW * 0.42f, bodyY - bodyH * 0.35f)
            lineTo(cx + bodyW * 0.68f + tailWag * 0.4f * scale, bodyY + bodyH * 0.58f)
            quadraticBezierTo(cx, bodyY + bodyH * 0.68f, cx - bodyW * 0.68f + tailWag * 0.4f * scale, bodyY + bodyH * 0.58f)
            close()
        }
        drawPath(
            path = capePath,
            brush = Brush.verticalGradient(listOf(capeColor, capeColor.copy(alpha = 0.88f)))
        )
        drawPath(
            path = capePath,
            color = capeTrim,
            style = Stroke(width = 4.5f * scale)
        )
    }

    // -------------------------------------------------------------
    // 1. TAIL (Behind Body)
    // -------------------------------------------------------------
    if (species == PetSpecies.CAT || species == PetSpecies.DOG) {
        val tailStartX = cx + (if (species == PetSpecies.CAT) 52f else 46f) * scale
        val tailStartY = bodyY + 68f * scale
        val tailPath = Path().apply {
            moveTo(tailStartX, tailStartY)
            quadraticBezierTo(
                tailStartX + (62f + tailWag) * scale,
                tailStartY - 48f * scale,
                tailStartX + (78f + tailWag * 1.5f) * scale,
                tailStartY - 92f * scale
            )
        }
        drawPath(
            path = tailPath,
            color = darkFur,
            style = Stroke(width = 24f * scale, cap = StrokeCap.Round)
        )
        drawCircle(
            color = Color.White,
            radius = 14f * scale,
            center = Offset(tailStartX + (78f + tailWag * 1.5f) * scale, tailStartY - 92f * scale)
        )
    }

    // -------------------------------------------------------------
    // 2. LEGS & FEET (Planting on Floor)
    // -------------------------------------------------------------
    val legSpacing = 48f * scale
    val footW = 56f * scale
    val footH = 34f * scale
    val footY = cy + 195f * scale

    drawOval(
        brush = Brush.verticalGradient(listOf(primaryFur, darkFur), startY = bodyY + 60f * scale, endY = footY),
        topLeft = Offset(cx - legSpacing - footW * 0.5f, footY - footH * 0.5f),
        size = Size(footW, footH)
    )
    drawCircle(color = chestCream, radius = 7f * scale, center = Offset(cx - legSpacing - 12f * scale, footY + 4f * scale))
    drawCircle(color = chestCream, radius = 7f * scale, center = Offset(cx - legSpacing, footY + 6f * scale))
    drawCircle(color = chestCream, radius = 7f * scale, center = Offset(cx - legSpacing + 12f * scale, footY + 4f * scale))

    drawOval(
        brush = Brush.verticalGradient(listOf(primaryFur, darkFur), startY = bodyY + 60f * scale, endY = footY),
        topLeft = Offset(cx + legSpacing - footW * 0.5f, footY - footH * 0.5f),
        size = Size(footW, footH)
    )
    drawCircle(color = chestCream, radius = 7f * scale, center = Offset(cx + legSpacing - 12f * scale, footY + 4f * scale))
    drawCircle(color = chestCream, radius = 7f * scale, center = Offset(cx + legSpacing, footY + 6f * scale))
    drawCircle(color = chestCream, radius = 7f * scale, center = Offset(cx + legSpacing + 12f * scale, footY + 4f * scale))

    // -------------------------------------------------------------
    // 3. TORSO, CHUBBY BELLY & 3D COSTUME LAYER
    // -------------------------------------------------------------
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(primaryFur, darkFur),
            center = Offset(cx - 24f * scale, bodyY - 20f * scale),
            radius = bodyW * 0.85f
        ),
        topLeft = Offset(cx - bodyW * 0.5f, bodyY - bodyH * 0.5f),
        size = Size(bodyW, bodyH)
    )

    val bellyW = bodyW * 0.65f
    val bellyH = bodyH * 0.68f
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(chestCream, chestCream.copy(alpha = 0.85f), darkFur.copy(alpha = 0.15f)),
            center = Offset(cx, bodyY + 12f * scale),
            radius = bellyW * 0.75f
        ),
        topLeft = Offset(cx - bellyW * 0.5f, bodyY - bellyH * 0.42f),
        size = Size(bellyW, bellyH)
    )

    // Render 3D Outfit / Costume over Torso
    draw3DCostumeOnBody(
        costume = customization.costume,
        cx = cx,
        bodyY = bodyY,
        bodyW = bodyW,
        bodyH = bodyH,
        scale = scale
    )

    // -------------------------------------------------------------
    // 4. ARMS & FRONT PAWS
    // -------------------------------------------------------------
    val armW = 44f * scale
    val armH = 92f * scale
    val armY = bodyY - 14f * scale

    val sleeveColor = when (customization.costume) {
        CostumeStyle.NONE -> null
        CostumeStyle.SUPERHERO -> null
        else -> customization.costume.primaryColor
    }

    val leftArmDeg = (if (activeReactionZone == TouchZone.HANDS) -28f else 12f) + leftArmAngleOffset
    rotate(degrees = leftArmDeg, pivot = Offset(cx - bodyW * 0.45f, armY)) {
        drawOval(
            brush = Brush.linearGradient(
                if (sleeveColor != null) listOf(sleeveColor, sleeveColor.copy(alpha = 0.85f))
                else listOf(primaryFur, darkFur)
            ),
            topLeft = Offset(cx - bodyW * 0.55f - armW * 0.5f, armY),
            size = Size(armW, armH)
        )
        drawCircle(
            color = chestCream,
            radius = armW * 0.44f,
            center = Offset(cx - bodyW * 0.55f, armY + armH - 12f * scale)
        )
    }

    val rightArmDeg = (if (activeReactionZone == TouchZone.HANDS) 28f else -12f) + rightArmAngleOffset
    rotate(degrees = rightArmDeg, pivot = Offset(cx + bodyW * 0.45f, armY)) {
        drawOval(
            brush = Brush.linearGradient(
                if (sleeveColor != null) listOf(sleeveColor, sleeveColor.copy(alpha = 0.85f))
                else listOf(primaryFur, darkFur)
            ),
            topLeft = Offset(cx + bodyW * 0.55f - armW * 0.5f, armY),
            size = Size(armW, armH)
        )
        drawCircle(
            color = chestCream,
            radius = armW * 0.44f,
            center = Offset(cx + bodyW * 0.55f, armY + armH - 12f * scale)
        )
    }

    // Neck Accessories (Bell Collar, Star Choker, Red Bowtie)
    drawNeckAccessory(
        accessory = customization.accessory,
        cx = cx,
        neckY = bodyY - bodyH * 0.34f,
        bodyW = bodyW,
        scale = scale
    )

    // -------------------------------------------------------------
    // 5. 3D SCULPTED HEAD & CHUBBY CHEEKS
    // -------------------------------------------------------------
    val earSpacing = (when (species) {
        PetSpecies.RABBIT -> 44f
        PetSpecies.DOG -> 82f
        else -> 66f
    }) * scale

    val earBaseY = headY - 68f * scale

    drawModularEar(
        species = species,
        earStyle = customization.earStyle,
        isLeft = true,
        baseX = cx - earSpacing,
        baseY = earBaseY,
        scale = scale,
        furColor = primaryFur,
        darkFur = darkFur,
        twitch = earTwitch
    )

    drawModularEar(
        species = species,
        earStyle = customization.earStyle,
        isLeft = false,
        baseX = cx + earSpacing,
        baseY = earBaseY,
        scale = scale,
        furColor = primaryFur,
        darkFur = darkFur,
        twitch = -earTwitch
    )

    // Main 3D Head Sphere with Volumetric Lighting
    val headHighlightX = cx - 34f * scale + (rotY * 0.8f)
    val headHighlightY = headY - 34f * scale
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(roomKeyLight.copy(alpha = 0.55f), primaryFur, darkFur),
            center = Offset(headHighlightX, headHighlightY),
            radius = headRadius * 1.15f
        ),
        radius = headRadius,
        center = Offset(cx, headY)
    )

    // Ribbon Headband if RIBBON_EARS is selected
    if (customization.earStyle == EarStyle.RIBBON_EARS) {
        val bowY = headY - headRadius * 0.88f
        drawOval(
            color = Color(0xFFE91E63),
            topLeft = Offset(cx - 38f * scale, bowY - 14f * scale),
            size = Size(34f * scale, 26f * scale)
        )
        drawOval(
            color = Color(0xFFE91E63),
            topLeft = Offset(cx + 4f * scale, bowY - 14f * scale),
            size = Size(34f * scale, 26f * scale)
        )
        drawCircle(
            color = Color(0xFFFF80AB),
            radius = 10f * scale,
            center = Offset(cx, bowY - 1f * scale)
        )
    }

    // Chubby Cheeks
    val cheekRadius = 48f * scale
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(chestCream, primaryFur),
            center = Offset(cx - 56f * scale, headY + 34f * scale),
            radius = cheekRadius * 1.1f
        ),
        radius = cheekRadius,
        center = Offset(cx - 56f * scale, headY + 34f * scale)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(chestCream, primaryFur),
            center = Offset(cx + 56f * scale, headY + 34f * scale),
            radius = cheekRadius * 1.1f
        ),
        radius = cheekRadius,
        center = Offset(cx + 56f * scale, headY + 34f * scale)
    )

    // -------------------------------------------------------------
    // 6. INTEGRATED 3D EYES & EYEBROWS
    // -------------------------------------------------------------
    val eyeSpacing = 42f * scale
    val eyeCenterY = headY - 14f * scale
    val eyeW = 34f * scale
    val eyeH = 46f * scale

    val isHappyPet = activeReactionZone == TouchZone.HEAD ||
            emotion == AvatarEmotion.HAPPY ||
            emotion == AvatarEmotion.LOVING
    val isTickleGiggle = activeReactionZone == TouchZone.BELLY
    val isSurprised = activeReactionZone == TouchZone.FEET ||
            emotion == AvatarEmotion.SURPRISED
    val isSleepy = emotion == AvatarEmotion.SLEEPY || customization.eyeStyle == EyeStyle.SLEEPY

    // Left Eye
    drawIntegrated3DEye(
        centerX = cx - eyeSpacing,
        centerY = eyeCenterY,
        width = eyeW,
        height = eyeH,
        scale = scale,
        irisTop = irisTop,
        irisBottom = irisBottom,
        furColor = primaryFur,
        gazeX = gazeX,
        gazeY = gazeY,
        blinkProgress = if (isSleepy) maxOf(blinkProgress, 0.55f) else blinkProgress,
        isHappy = isHappyPet,
        isTickle = isTickleGiggle,
        isSurprised = isSurprised,
        eyeStyle = customization.eyeStyle,
        isLeftEye = true,
        isLovingEmotion = emotion == AvatarEmotion.LOVING
    )

    // Right Eye
    drawIntegrated3DEye(
        centerX = cx + eyeSpacing,
        centerY = eyeCenterY,
        width = eyeW,
        height = eyeH,
        scale = scale,
        irisTop = irisTop,
        irisBottom = irisBottom,
        furColor = primaryFur,
        gazeX = gazeX,
        gazeY = gazeY,
        blinkProgress = if (isSleepy) maxOf(blinkProgress, 0.55f) else blinkProgress,
        isHappy = isHappyPet,
        isTickle = isTickleGiggle,
        isSurprised = isSurprised,
        eyeStyle = customization.eyeStyle,
        isLeftEye = false,
        isLovingEmotion = emotion == AvatarEmotion.LOVING
    )

    // Expressive 3D Eyebrows above eyes
    val browY = eyeCenterY - eyeH * 0.68f - (if (isSurprised) 8f * scale else 0f)
    val browTilt = when {
        isSurprised -> -6f
        isHappyPet || emotion == AvatarEmotion.LOVING -> -4f
        else -> 2f
    }
    drawLine(
        color = darkFur,
        start = Offset(cx - eyeSpacing - 16f * scale, browY + browTilt * scale * 0.5f),
        end = Offset(cx - eyeSpacing + 14f * scale, browY - browTilt * scale * 0.5f),
        strokeWidth = 4.2f * scale,
        cap = StrokeCap.Round
    )
    drawLine(
        color = darkFur,
        start = Offset(cx + eyeSpacing - 14f * scale, browY - browTilt * scale * 0.5f),
        end = Offset(cx + eyeSpacing + 16f * scale, browY + browTilt * scale * 0.5f),
        strokeWidth = 4.2f * scale,
        cap = StrokeCap.Round
    )

    // Rosy Cheeks when Happy / Petted / Loving
    val totalBlush = maxOf(cheekBlushAlpha, if (isHappyPet || customization.eyeStyle == EyeStyle.HEARTS) 0.65f else 0f)
    if (totalBlush > 0.05f) {
        val blushY = headY + 28f * scale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF4081).copy(alpha = totalBlush * 0.60f), Color.Transparent),
                center = Offset(cx - 56f * scale, blushY),
                radius = 28f * scale
            ),
            radius = 28f * scale,
            center = Offset(cx - 56f * scale, blushY)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF4081).copy(alpha = totalBlush * 0.60f), Color.Transparent),
                center = Offset(cx + 56f * scale, blushY),
                radius = 28f * scale
            ),
            radius = 28f * scale,
            center = Offset(cx + 56f * scale, blushY)
        )
    }

    // -------------------------------------------------------------
    // 7. INTEGRATED 3D SNOUT & TALKING MOUTH
    // -------------------------------------------------------------
    val snoutW = 86f * scale
    val snoutH = 58f * scale
    val snoutY = headY + 36f * scale

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(chestCream, chestCream.copy(alpha = 0.90f), primaryFur.copy(alpha = 0.35f)),
            center = Offset(cx, snoutY - 10f * scale),
            radius = snoutW * 0.65f
        ),
        topLeft = Offset(cx - snoutW * 0.5f, snoutY - snoutH * 0.5f),
        size = Size(snoutW, snoutH)
    )

    val noseW = 24f * scale
    val noseH = 16f * scale
    val noseY = snoutY - 14f * scale
    drawOval(
        color = noseColor,
        topLeft = Offset(cx - noseW * 0.5f, noseY - noseH * 0.5f),
        size = Size(noseW, noseH)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.65f),
        radius = 3.5f * scale,
        center = Offset(cx - 4f * scale, noseY - 3f * scale)
    )

    val mouthY = snoutY + 8f * scale
    val baseOpenProgress = when {
        isTickleGiggle -> 0.90f
        isTalking -> (0.25f + mouthCycle * 0.75f)
        isHappyPet -> 0.35f
        isSurprised -> 0.70f
        else -> 0.10f
    }
    val openProgress = maxOf(baseOpenProgress, idleMouthOpenOverride)

    drawIntegratedTalkingMouth(
        cx = cx,
        mouthY = mouthY,
        scale = scale,
        openProgress = openProgress,
        isTalking = isTalking || idleMouthOpenOverride > 0.35f,
        isLaughing = isTickleGiggle,
        isHappy = isHappyPet
    )

    // Whiskers (for Cat & Rabbit)
    if (species == PetSpecies.CAT || species == PetSpecies.RABBIT) {
        val whiskerStroke = Stroke(width = 2.2f * scale, cap = StrokeCap.Round)
        val whiskerColor = Color(0x90263238)

        drawLine(whiskerColor, Offset(cx - 36f * scale, snoutY), Offset(cx - 84f * scale, snoutY - 8f * scale), strokeWidth = whiskerStroke.width)
        drawLine(whiskerColor, Offset(cx - 36f * scale, snoutY + 8f * scale), Offset(cx - 88f * scale, snoutY + 12f * scale), strokeWidth = whiskerStroke.width)

        drawLine(whiskerColor, Offset(cx + 36f * scale, snoutY), Offset(cx + 84f * scale, snoutY - 8f * scale), strokeWidth = whiskerStroke.width)
        drawLine(whiskerColor, Offset(cx + 36f * scale, snoutY + 8f * scale), Offset(cx + 88f * scale, snoutY + 12f * scale), strokeWidth = whiskerStroke.width)
    }

    // Capybara Yuzu Citrus on Head (when no hat/crown accessory overrides it)
    if (species == PetSpecies.CAPYBARA &&
        customization.accessory != AccessoryStyle.ROYAL_CROWN &&
        customization.accessory != AccessoryStyle.WIZARD_HAT &&
        customization.costume != CostumeStyle.CHEF &&
        customization.costume != CostumeStyle.DETECTIVE
    ) {
        val yuzuRadius = 26f * scale
        val yuzuY = headY - headRadius - 10f * scale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFD54F), Color(0xFFFF9800)),
                center = Offset(cx - 6f * scale, yuzuY - 6f * scale),
                radius = yuzuRadius
            ),
            radius = yuzuRadius,
            center = Offset(cx, yuzuY)
        )
        drawOval(
            color = Color(0xFF4CAF50),
            topLeft = Offset(cx - 4f * scale, yuzuY - yuzuRadius - 8f * scale),
            size = Size(18f * scale, 10f * scale)
        )
    }

    // -------------------------------------------------------------
    // 8. HEAD & EYEWEAR ACCESSORIES (Glasses, Shades, Crown, Hats, Headset)
    // -------------------------------------------------------------
    drawHeadAndFaceAccessories(
        accessory = customization.accessory,
        costume = customization.costume,
        eyeStyle = customization.eyeStyle,
        cx = cx,
        headY = headY,
        headRadius = headRadius,
        eyeCenterY = eyeCenterY,
        eyeSpacing = eyeSpacing,
        eyeW = eyeW,
        scale = scale
    )
}

/**
 * Renders 3D outfits over the character's torso.
 */
private fun DrawScope.draw3DCostumeOnBody(
    costume: CostumeStyle,
    cx: Float,
    bodyY: Float,
    bodyW: Float,
    bodyH: Float,
    scale: Float
) {
    if (costume == CostumeStyle.NONE) return

    val topY = bodyY - bodyH * 0.38f
    val shirtH = bodyH * 0.76f

    when (costume) {
        CostumeStyle.HOODIE -> {
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF3F51B5), Color(0xFF283593))),
                topLeft = Offset(cx - bodyW * 0.48f, topY),
                size = Size(bodyW * 0.96f, shirtH),
                cornerRadius = CornerRadius(42f * scale, 42f * scale)
            )
            // Kangaroo front pocket
            drawRoundRect(
                color = Color(0xFF1A237E).copy(alpha = 0.65f),
                topLeft = Offset(cx - bodyW * 0.28f, bodyY + 18f * scale),
                size = Size(bodyW * 0.56f, 34f * scale),
                cornerRadius = CornerRadius(14f * scale, 14f * scale)
            )
            // White hoodie drawstrings
            drawLine(Color.White, Offset(cx - 16f * scale, topY + 6f * scale), Offset(cx - 16f * scale, topY + 42f * scale), strokeWidth = 3.5f * scale, cap = StrokeCap.Round)
            drawLine(Color.White, Offset(cx + 16f * scale, topY + 6f * scale), Offset(cx + 16f * scale, topY + 42f * scale), strokeWidth = 3.5f * scale, cap = StrokeCap.Round)
        }

        CostumeStyle.SUPERHERO -> {
            // Hero suit chest & golden star emblem
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF1E88E5), Color(0xFF1565C0))),
                topLeft = Offset(cx - bodyW * 0.46f, topY),
                size = Size(bodyW * 0.92f, shirtH * 0.92f),
                cornerRadius = CornerRadius(40f * scale, 40f * scale)
            )
            // Golden belt
            drawRoundRect(
                color = Color(0xFFFFCA28),
                topLeft = Offset(cx - bodyW * 0.47f, bodyY + 38f * scale),
                size = Size(bodyW * 0.94f, 16f * scale),
                cornerRadius = CornerRadius(8f * scale, 8f * scale)
            )
            // Hero Diamond Emblem on Chest
            val emblemPath = Path().apply {
                moveTo(cx, bodyY - 24f * scale)
                lineTo(cx + 26f * scale, bodyY - 4f * scale)
                lineTo(cx, bodyY + 22f * scale)
                lineTo(cx - 26f * scale, bodyY - 4f * scale)
                close()
            }
            drawPath(emblemPath, Color(0xFFFFCA28))
            drawPath(emblemPath, Color(0xFFD32F2F), style = Stroke(width = 3f * scale))
        }

        CostumeStyle.CHEF -> {
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFECEFF1))),
                topLeft = Offset(cx - bodyW * 0.48f, topY),
                size = Size(bodyW * 0.96f, shirtH),
                cornerRadius = CornerRadius(40f * scale, 40f * scale)
            )
            // Red chef neckerchief
            val scarfPath = Path().apply {
                moveTo(cx - 36f * scale, topY + 4f * scale)
                lineTo(cx + 36f * scale, topY + 4f * scale)
                lineTo(cx, topY + 36f * scale)
                close()
            }
            drawPath(scarfPath, Color(0xFFE53935))
            // Double-breasted black chef buttons
            for (row in 0..2) {
                val by = bodyY - 10f * scale + row * 22f * scale
                drawCircle(Color(0xFF263238), radius = 4.5f * scale, center = Offset(cx - 18f * scale, by))
                drawCircle(Color(0xFF263238), radius = 4.5f * scale, center = Offset(cx + 18f * scale, by))
            }
        }

        CostumeStyle.DETECTIVE -> {
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF8D6E63), Color(0xFF5D4037))),
                topLeft = Offset(cx - bodyW * 0.49f, topY),
                size = Size(bodyW * 0.98f, shirtH),
                cornerRadius = CornerRadius(40f * scale, 40f * scale)
            )
            // Center coat seam & brass buttons
            drawLine(Color(0xFF3E2723), Offset(cx, topY + 10f * scale), Offset(cx, topY + shirtH), strokeWidth = 3f * scale)
            drawCircle(Color(0xFFFFD54F), radius = 5f * scale, center = Offset(cx - 14f * scale, bodyY))
            drawCircle(Color(0xFFFFD54F), radius = 5f * scale, center = Offset(cx + 14f * scale, bodyY))
            drawCircle(Color(0xFFFFD54F), radius = 5f * scale, center = Offset(cx - 14f * scale, bodyY + 26f * scale))
            drawCircle(Color(0xFFFFD54F), radius = 5f * scale, center = Offset(cx + 14f * scale, bodyY + 26f * scale))
        }

        CostumeStyle.PAJAMAS -> {
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF5C6BC0), Color(0xFF3949AB))),
                topLeft = Offset(cx - bodyW * 0.48f, topY),
                size = Size(bodyW * 0.96f, shirtH),
                cornerRadius = CornerRadius(42f * scale, 42f * scale)
            )
            // Cute Yellow Stars & Crescent Moon pattern
            drawCircle(Color(0xFFFFEE58), radius = 7f * scale, center = Offset(cx - 32f * scale, bodyY - 12f * scale))
            drawCircle(Color(0xFFFFEE58), radius = 6f * scale, center = Offset(cx + 28f * scale, bodyY + 18f * scale))
            drawCircle(Color(0xFFFFEE58), radius = 8f * scale, center = Offset(cx + 18f * scale, bodyY - 22f * scale))
            drawCircle(Color(0xFFFFEE58), radius = 6f * scale, center = Offset(cx - 22f * scale, bodyY + 28f * scale))
        }

        CostumeStyle.ROYAL_ROBE -> {
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFC62828), Color(0xFF8E0000))),
                topLeft = Offset(cx - bodyW * 0.48f, topY),
                size = Size(bodyW * 0.96f, shirtH),
                cornerRadius = CornerRadius(42f * scale, 42f * scale)
            )
            // White royal ermine fur trim down center
            drawRoundRect(
                color = Color(0xFFFFF8E1),
                topLeft = Offset(cx - 14f * scale, topY),
                size = Size(28f * scale, shirtH),
                cornerRadius = CornerRadius(10f * scale, 10f * scale)
            )
            // Gold royal clasp at collar
            drawCircle(Color(0xFFFFD700), radius = 12f * scale, center = Offset(cx, topY + 16f * scale))
            drawCircle(Color(0xFF00E676), radius = 6f * scale, center = Offset(cx, topY + 16f * scale))
        }

        CostumeStyle.CYBER_JACKET -> {
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))),
                topLeft = Offset(cx - bodyW * 0.49f, topY),
                size = Size(bodyW * 0.98f, shirtH),
                cornerRadius = CornerRadius(40f * scale, 40f * scale)
            )
            // Glowing Neon Cyan Cyber Lines
            val neonCyan = Color(0xFF00E5FF)
            drawLine(neonCyan, Offset(cx - 28f * scale, topY + 8f * scale), Offset(cx - 12f * scale, topY + shirtH - 8f * scale), strokeWidth = 4f * scale, cap = StrokeCap.Round)
            drawLine(neonCyan, Offset(cx + 28f * scale, topY + 8f * scale), Offset(cx + 12f * scale, topY + shirtH - 8f * scale), strokeWidth = 4f * scale, cap = StrokeCap.Round)
            drawCircle(Color(0xFFFF007F), radius = 10f * scale, center = Offset(cx, bodyY))
        }

        CostumeStyle.WINTER_COAT -> {
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF0288D1), Color(0xFF01579B))),
                topLeft = Offset(cx - bodyW * 0.50f, topY),
                size = Size(bodyW, shirtH),
                cornerRadius = CornerRadius(44f * scale, 44f * scale)
            )
            // Fluffy white shearling collar & hem
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cx - bodyW * 0.46f, topY - 4f * scale),
                size = Size(bodyW * 0.92f, 22f * scale),
                cornerRadius = CornerRadius(12f * scale, 12f * scale)
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(cx - bodyW * 0.48f, topY + shirtH - 16f * scale),
                size = Size(bodyW * 0.96f, 18f * scale),
                cornerRadius = CornerRadius(10f * scale, 10f * scale)
            )
        }

        else -> {}
    }
}

/**
 * Renders 3D neck accessories (Bell Collar, Star Choker, Red Bowtie).
 */
private fun DrawScope.drawNeckAccessory(
    accessory: AccessoryStyle,
    cx: Float,
    neckY: Float,
    bodyW: Float,
    scale: Float
) {
    when (accessory) {
        AccessoryStyle.BELL_COLLAR -> {
            drawRoundRect(
                color = Color(0xFFD32F2F),
                topLeft = Offset(cx - bodyW * 0.34f, neckY),
                size = Size(bodyW * 0.68f, 14f * scale),
                cornerRadius = CornerRadius(8f * scale, 8f * scale)
            )
            // 3D Shiny Golden Bell
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFFFFF176), Color(0xFFFFB300))),
                radius = 13f * scale,
                center = Offset(cx, neckY + 14f * scale)
            )
            drawCircle(
                color = Color(0xFF5D4037),
                radius = 3.5f * scale,
                center = Offset(cx, neckY + 17f * scale)
            )
        }

        AccessoryStyle.STAR_CHOKER -> {
            drawRoundRect(
                color = Color(0xFF311B92),
                topLeft = Offset(cx - bodyW * 0.34f, neckY),
                size = Size(bodyW * 0.68f, 12f * scale),
                cornerRadius = CornerRadius(6f * scale, 6f * scale)
            )
            drawCircle(
                color = Color(0xFFFFD700),
                radius = 11f * scale,
                center = Offset(cx, neckY + 12f * scale)
            )
        }

        AccessoryStyle.RED_BOWTIE -> {
            val leftWing = Path().apply {
                moveTo(cx, neckY + 8f * scale)
                lineTo(cx - 28f * scale, neckY - 6f * scale)
                lineTo(cx - 28f * scale, neckY + 22f * scale)
                close()
            }
            val rightWing = Path().apply {
                moveTo(cx, neckY + 8f * scale)
                lineTo(cx + 28f * scale, neckY - 6f * scale)
                lineTo(cx + 28f * scale, neckY + 22f * scale)
                close()
            }
            drawPath(leftWing, Color(0xFFE53935))
            drawPath(rightWing, Color(0xFFE53935))
            drawCircle(Color(0xFFB71C1C), radius = 8f * scale, center = Offset(cx, neckY + 8f * scale))
        }

        else -> {}
    }
}

/**
 * Renders 3D head hats, crowns, glasses, shades, flower pins, and gaming headsets.
 */
private fun DrawScope.drawHeadAndFaceAccessories(
    accessory: AccessoryStyle,
    costume: CostumeStyle,
    eyeStyle: EyeStyle,
    cx: Float,
    headY: Float,
    headRadius: Float,
    eyeCenterY: Float,
    eyeSpacing: Float,
    eyeW: Float,
    scale: Float
) {
    // 1. Eyewear (Sunglasses, Cool Shades EyeStyle, or Professor Round Glasses)
    if (accessory == AccessoryStyle.SUNGLASSES || eyeStyle == EyeStyle.COOL_SHADES) {
        val lensW = eyeW * 1.45f
        val lensH = eyeW * 1.05f
        // Bridge
        drawLine(Color(0xFF111827), Offset(cx - eyeSpacing, eyeCenterY - 4f * scale), Offset(cx + eyeSpacing, eyeCenterY - 4f * scale), strokeWidth = 5f * scale)
        // Left & Right Dark Lenses
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0xFF1F2937), Color(0xFF030712))),
            topLeft = Offset(cx - eyeSpacing - lensW * 0.5f, eyeCenterY - lensH * 0.5f),
            size = Size(lensW, lensH),
            cornerRadius = CornerRadius(8f * scale, 8f * scale)
        )
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0xFF1F2937), Color(0xFF030712))),
            topLeft = Offset(cx + eyeSpacing - lensW * 0.5f, eyeCenterY - lensH * 0.5f),
            size = Size(lensW, lensH),
            cornerRadius = CornerRadius(8f * scale, 8f * scale)
        )
        // Glossy white lens reflection slash
        drawLine(Color.White.copy(alpha = 0.55f), Offset(cx - eyeSpacing - 12f * scale, eyeCenterY - 8f * scale), Offset(cx - eyeSpacing + 4f * scale, eyeCenterY + 8f * scale), strokeWidth = 3f * scale, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.55f), Offset(cx + eyeSpacing - 12f * scale, eyeCenterY - 8f * scale), Offset(cx + eyeSpacing + 4f * scale, eyeCenterY + 8f * scale), strokeWidth = 3f * scale, cap = StrokeCap.Round)
    } else if (accessory == AccessoryStyle.ROUND_GLASSES) {
        val r = eyeW * 0.72f
        val frameStroke = Stroke(width = 3.5f * scale)
        val goldColor = Color(0xFFFFB300)
        drawLine(goldColor, Offset(cx - eyeSpacing + r, eyeCenterY), Offset(cx + eyeSpacing - r, eyeCenterY), strokeWidth = 3.5f * scale)
        drawCircle(goldColor, radius = r, center = Offset(cx - eyeSpacing, eyeCenterY), style = frameStroke)
        drawCircle(goldColor, radius = r, center = Offset(cx + eyeSpacing, eyeCenterY), style = frameStroke)
    }

    // 2. Sakura Flower Pin
    if (accessory == AccessoryStyle.FLOWER_PIN) {
        val pinX = cx - headRadius * 0.72f
        val pinY = headY - headRadius * 0.62f
        for (i in 0 until 5) {
            val angle = Math.toRadians((i * 72).toDouble())
            val px = pinX + (kotlin.math.cos(angle) * 12f * scale).toFloat()
            val py = pinY + (kotlin.math.sin(angle) * 12f * scale).toFloat()
            drawCircle(Color(0xFFFF80AB), radius = 10f * scale, center = Offset(px, py))
        }
        drawCircle(Color(0xFFFFEE58), radius = 7f * scale, center = Offset(pinX, pinY))
    }

    // 3. Gaming Headset
    if (accessory == AccessoryStyle.GAMING_HEADSET) {
        val arcRectSize = Size(headRadius * 2.15f, headRadius * 1.9f)
        drawArc(
            color = Color(0xFF1E293B),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - arcRectSize.width * 0.5f, headY - headRadius * 1.05f),
            size = arcRectSize,
            style = Stroke(width = 12f * scale, cap = StrokeCap.Round)
        )
        // Left & Right RGB Ear Cups
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(cx - headRadius * 1.14f, headY - 24f * scale),
            size = Size(24f * scale, 54f * scale),
            cornerRadius = CornerRadius(12f * scale, 12f * scale)
        )
        drawRoundRect(
            color = Color(0xFF00E5FF),
            topLeft = Offset(cx - headRadius * 1.12f, headY - 16f * scale),
            size = Size(8f * scale, 38f * scale),
            cornerRadius = CornerRadius(4f * scale, 4f * scale)
        )
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(cx + headRadius * 0.90f, headY - 24f * scale),
            size = Size(24f * scale, 54f * scale),
            cornerRadius = CornerRadius(12f * scale, 12f * scale)
        )
        drawRoundRect(
            color = Color(0xFF00E5FF),
            topLeft = Offset(cx + headRadius * 1.04f, headY - 16f * scale),
            size = Size(8f * scale, 38f * scale),
            cornerRadius = CornerRadius(4f * scale, 4f * scale)
        )
    }

    // 4. Royal Crown (Accessory or Royal Robe Costume)
    if (accessory == AccessoryStyle.ROYAL_CROWN || costume == CostumeStyle.ROYAL_ROBE) {
        val crownBaseY = headY - headRadius * 0.86f
        val crownW = 84f * scale
        val crownH = 44f * scale
        val crownPath = Path().apply {
            moveTo(cx - crownW * 0.5f, crownBaseY)
            lineTo(cx - crownW * 0.55f, crownBaseY - crownH)
            lineTo(cx - crownW * 0.25f, crownBaseY - crownH * 0.45f)
            lineTo(cx, crownBaseY - crownH * 1.15f)
            lineTo(cx + crownW * 0.25f, crownBaseY - crownH * 0.45f)
            lineTo(cx + crownW * 0.55f, crownBaseY - crownH)
            lineTo(cx + crownW * 0.5f, crownBaseY)
            close()
        }
        drawPath(
            path = crownPath,
            brush = Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFB300)))
        )
        drawPath(crownPath, Color(0xFFFF8F00), style = Stroke(width = 2.5f * scale))
        // Crown Jewels
        drawCircle(Color(0xFFE53935), radius = 6f * scale, center = Offset(cx, crownBaseY - crownH * 0.35f))
        drawCircle(Color(0xFF00E676), radius = 4.5f * scale, center = Offset(cx - 22f * scale, crownBaseY - crownH * 0.28f))
        drawCircle(Color(0xFF2979FF), radius = 4.5f * scale, center = Offset(cx + 22f * scale, crownBaseY - crownH * 0.28f))
    }

    // 5. Wizard Hat
    if (accessory == AccessoryStyle.WIZARD_HAT) {
        val hatBaseY = headY - headRadius * 0.82f
        // Brim
        drawOval(
            color = Color(0xFF4A148C),
            topLeft = Offset(cx - 76f * scale, hatBaseY - 12f * scale),
            size = Size(152f * scale, 28f * scale)
        )
        // Cone
        val conePath = Path().apply {
            moveTo(cx - 48f * scale, hatBaseY)
            lineTo(cx + 12f * scale, hatBaseY - 96f * scale)
            lineTo(cx + 48f * scale, hatBaseY)
            close()
        }
        drawPath(conePath, Brush.verticalGradient(listOf(Color(0xFF7B1FA2), Color(0xFF4A148C))))
        // Golden ribbon & star
        drawRoundRect(
            color = Color(0xFFFFD54F),
            topLeft = Offset(cx - 46f * scale, hatBaseY - 10f * scale),
            size = Size(92f * scale, 10f * scale),
            cornerRadius = CornerRadius(4f * scale, 4f * scale)
        )
        drawCircle(Color(0xFFFFEE58), radius = 6f * scale, center = Offset(cx, hatBaseY - 44f * scale))
    }

    // 6. Chef Toque Hat (when wearing Master Chef costume and no crown/wizard hat)
    if (costume == CostumeStyle.CHEF &&
        accessory != AccessoryStyle.ROYAL_CROWN &&
        accessory != AccessoryStyle.WIZARD_HAT
    ) {
        val hatBaseY = headY - headRadius * 0.86f
        drawCircle(Color.White, radius = 28f * scale, center = Offset(cx - 22f * scale, hatBaseY - 28f * scale))
        drawCircle(Color.White, radius = 32f * scale, center = Offset(cx, hatBaseY - 36f * scale))
        drawCircle(Color.White, radius = 28f * scale, center = Offset(cx + 22f * scale, hatBaseY - 28f * scale))
        drawRoundRect(
            color = Color(0xFFECEFF1),
            topLeft = Offset(cx - 42f * scale, hatBaseY - 18f * scale),
            size = Size(84f * scale, 22f * scale),
            cornerRadius = CornerRadius(6f * scale, 6f * scale)
        )
    }
}

/**
 * Draws a 100% integrated 3D eye with smooth eyelids, specular shine, EyeStyle variations, and gaze tracking.
 */
private fun DrawScope.drawIntegrated3DEye(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    scale: Float,
    irisTop: Color,
    irisBottom: Color,
    furColor: Color,
    gazeX: Float,
    gazeY: Float,
    blinkProgress: Float,
    isHappy: Boolean,
    isTickle: Boolean,
    isSurprised: Boolean,
    eyeStyle: EyeStyle = EyeStyle.SPARKLING,
    isLeftEye: Boolean = true,
    isLovingEmotion: Boolean = false
) {
    val isWinkingEye = eyeStyle == EyeStyle.PLAYFUL_WINK && !isLeftEye

    when {
        // Happy squint crescents (⌒) when petted on head or winking right eye
        isHappy || isWinkingEye -> {
            val strokeW = 4.8f * scale
            val arcW = width * 1.35f
            val arcH = height * 0.75f
            val happyPath = Path().apply {
                moveTo(centerX - arcW * 0.5f, centerY + arcH * 0.25f)
                cubicTo(
                    centerX - arcW * 0.25f, centerY - arcH * 0.65f,
                    centerX + arcW * 0.25f, centerY - arcH * 0.65f,
                    centerX + arcW * 0.5f, centerY + arcH * 0.25f
                )
            }
            drawPath(happyPath, Color(0xFF263238), style = Stroke(strokeW, cap = StrokeCap.Round))
        }

        // Giggling squints (> <) when tickled on belly
        isTickle -> {
            val strokeW = 4.8f * scale
            val armL = width * 0.75f
            drawLine(
                color = Color(0xFF263238),
                start = Offset(centerX - armL * 0.6f, centerY - armL * 0.5f),
                end = Offset(centerX + armL * 0.3f, centerY),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF263238),
                start = Offset(centerX + armL * 0.3f, centerY),
                end = Offset(centerX - armL * 0.6f, centerY + armL * 0.5f),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }

        // Normal Eye / Blinking / Gaze Tracking
        else -> {
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFECEFF1)),
                    center = Offset(centerX, centerY - 6f * scale),
                    radius = width
                ),
                topLeft = Offset(centerX - width * 0.5f, centerY - height * 0.5f),
                size = Size(width, height)
            )

            val maxShiftX = width * 0.26f
            val maxShiftY = height * 0.24f
            val px = centerX + (gazeX * maxShiftX)
            val py = centerY + (gazeY * maxShiftY)

            val irisRadius = width * (if (isSurprised) 0.34f else 0.44f)
            drawCircle(
                brush = Brush.verticalGradient(listOf(irisTop, irisBottom)),
                radius = irisRadius,
                center = Offset(px, py)
            )

            val pupilRadius = irisRadius * 0.62f
            drawCircle(
                color = Color(0xFF1A1A1A),
                radius = pupilRadius,
                center = Offset(px, py)
            )

            // Loving Heart Highlight inside Eye if HEARTS style or LOVING emotion
            if (eyeStyle == EyeStyle.HEARTS || isLovingEmotion) {
                drawCircle(
                    color = Color(0xFFFF80AB),
                    radius = pupilRadius * 0.42f,
                    center = Offset(px - pupilRadius * 0.22f, py - pupilRadius * 0.12f)
                )
                drawCircle(
                    color = Color(0xFFFF80AB),
                    radius = pupilRadius * 0.42f,
                    center = Offset(px + pupilRadius * 0.22f, py - pupilRadius * 0.12f)
                )
            }

            // Big Glossy Specular Highlight
            drawCircle(
                color = Color.White,
                radius = pupilRadius * 0.48f,
                center = Offset(px - pupilRadius * 0.34f, py - pupilRadius * 0.34f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = pupilRadius * 0.22f,
                center = Offset(px + pupilRadius * 0.36f, py + pupilRadius * 0.36f)
            )
            if (eyeStyle == EyeStyle.SPARKLING) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.90f),
                    radius = pupilRadius * 0.16f,
                    center = Offset(px + pupilRadius * 0.32f, py - pupilRadius * 0.30f)
                )
            }

            drawOval(
                color = Color(0x60263238),
                topLeft = Offset(centerX - width * 0.5f, centerY - height * 0.5f),
                size = Size(width, height),
                style = Stroke(width = 2.4f * scale)
            )

            if (blinkProgress > 0.05f) {
                val lidH = height * blinkProgress * 1.1f
                drawOval(
                    color = furColor,
                    topLeft = Offset(centerX - width * 0.55f, centerY - height * 0.55f),
                    size = Size(width * 1.1f, lidH)
                )
                drawLine(
                    color = Color(0xFF263238),
                    start = Offset(centerX - width * 0.45f, centerY - height * 0.55f + lidH),
                    end = Offset(centerX + width * 0.45f, centerY - height * 0.55f + lidH),
                    strokeWidth = 3f * scale,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Integrated talking mouth cavity with jaw flap, cartoon white teeth, and bouncing pink tongue.
 */
private fun DrawScope.drawIntegratedTalkingMouth(
    cx: Float,
    mouthY: Float,
    scale: Float,
    openProgress: Float,
    isTalking: Boolean,
    isLaughing: Boolean,
    isHappy: Boolean
) {
    val mW = (38f + openProgress * 14f) * scale
    val mH = (10f + openProgress * 28f) * scale

    if (openProgress > 0.18f) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF240707), Color(0xFF150202)),
                center = Offset(cx, mouthY + mH * 0.4f),
                radius = mW * 0.6f
            ),
            topLeft = Offset(cx - mW * 0.5f, mouthY),
            size = Size(mW, mH)
        )

        val teethH = mH * 0.35f
        val teethPath = Path().apply {
            moveTo(cx - mW * 0.32f, mouthY)
            lineTo(cx + mW * 0.32f, mouthY)
            lineTo(cx + mW * 0.24f, mouthY + teethH)
            lineTo(cx - mW * 0.24f, mouthY + teethH)
            close()
        }
        drawPath(teethPath, color = Color.White)

        val tongueW = mW * 0.62f
        val tongueH = mH * 0.65f
        val tongueY = mouthY + mH * 0.40f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF6B81), Color(0xFFE04860)),
                center = Offset(cx, tongueY + tongueH * 0.5f),
                radius = tongueW * 0.6f
            ),
            topLeft = Offset(cx - tongueW * 0.5f, tongueY),
            size = Size(tongueW, tongueH)
        )
        drawLine(
            color = Color(0xFFD83A56),
            start = Offset(cx, tongueY + tongueH * 0.25f),
            end = Offset(cx, tongueY + tongueH * 0.85f),
            strokeWidth = 2.2f * scale,
            cap = StrokeCap.Round
        )

        drawOval(
            color = Color(0x50263238),
            topLeft = Offset(cx - mW * 0.5f, mouthY),
            size = Size(mW, mH),
            style = Stroke(width = 2.4f * scale)
        )
    } else {
        val smilePath = Path().apply {
            moveTo(cx - 18f * scale, mouthY + 2f * scale)
            quadraticBezierTo(cx, mouthY + 12f * scale, cx + 18f * scale, mouthY + 2f * scale)
        }
        drawPath(
            path = smilePath,
            color = Color(0xFF263238),
            style = Stroke(width = 3.2f * scale, cap = StrokeCap.Round)
        )
    }
}

/**
 * Draws species-appropriate or customized ears (pointy cat, long floppy rabbit, cute dog, round bear/capybara).
 */
private fun DrawScope.drawModularEar(
    species: PetSpecies,
    earStyle: EarStyle,
    isLeft: Boolean,
    baseX: Float,
    baseY: Float,
    scale: Float,
    furColor: Color,
    darkFur: Color,
    twitch: Float
) {
    val dir = if (isLeft) -1f else 1f
    rotate(degrees = (if (isLeft) -14f else 14f) + twitch, pivot = Offset(baseX, baseY)) {
        when {
            species == PetSpecies.RABBIT -> {
                val earW = 34f * scale
                val earH = 110f * scale
                drawOval(
                    brush = Brush.linearGradient(listOf(furColor, darkFur)),
                    topLeft = Offset(baseX - earW * 0.5f, baseY - earH),
                    size = Size(earW, earH)
                )
                drawOval(
                    color = Color(0xFFFF80AB).copy(alpha = 0.65f),
                    topLeft = Offset(baseX - earW * 0.28f, baseY - earH * 0.88f),
                    size = Size(earW * 0.56f, earH * 0.76f)
                )
            }

            species == PetSpecies.DOG || earStyle == EarStyle.DOG_FLOPPY -> {
                val earW = 46f * scale
                val earH = 82f * scale
                drawOval(
                    brush = Brush.verticalGradient(listOf(darkFur, furColor)),
                    topLeft = Offset(baseX - earW * 0.5f, baseY - 12f * scale),
                    size = Size(earW, earH)
                )
            }

            species == PetSpecies.BEAR || species == PetSpecies.CAPYBARA || earStyle == EarStyle.BEAR_ROUND -> {
                val earRadius = 28f * scale
                drawCircle(
                    brush = Brush.radialGradient(listOf(furColor, darkFur)),
                    radius = earRadius,
                    center = Offset(baseX, baseY - earRadius * 0.6f)
                )
                drawCircle(
                    color = Color(0x60000000),
                    radius = earRadius * 0.55f,
                    center = Offset(baseX, baseY - earRadius * 0.6f)
                )
            }

            else -> {
                val earW = 44f * scale
                val earH = 64f * scale
                val earPath = Path().apply {
                    moveTo(baseX - earW * 0.5f, baseY)
                    lineTo(baseX + dir * 8f * scale, baseY - earH)
                    lineTo(baseX + earW * 0.5f, baseY)
                    close()
                }
                drawPath(earPath, furColor)
                val innerEarPath = Path().apply {
                    moveTo(baseX - earW * 0.28f, baseY - 4f * scale)
                    lineTo(baseX + dir * 6f * scale, baseY - earH * 0.78f)
                    lineTo(baseX + earW * 0.28f, baseY - 4f * scale)
                    close()
                }
                drawPath(innerEarPath, Color(0xFFFF8A80).copy(alpha = 0.75f))
            }
        }
    }
}

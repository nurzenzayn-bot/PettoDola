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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.AccessoryStyle
import com.example.model.AvatarCustomization
import com.example.model.AvatarEmotion
import com.example.model.CostumeStyle
import com.example.model.EarStyle
import com.example.model.EyeStyle
import com.example.model.PetSpecies
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class FloatingParticle(
    val id: Long,
    val startX: Float,
    val startY: Float,
    val color: Color
)

@Composable
fun AvatarCanvas(
    species: PetSpecies,
    customization: AvatarCustomization,
    emotion: AvatarEmotion,
    isTalking: Boolean,
    onPetAvatar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val bounceScale = remember { Animatable(1f) }
    var isBlinking by remember { mutableStateOf(false) }
    val particles = remember { mutableStateListOf<FloatingParticle>() }

    // Periodic natural blinking
    LaunchedEffect(Unit) {
        while (isActive) {
            delay((2800..4500).random().toLong())
            isBlinking = true
            delay(160)
            isBlinking = false
        }
    }

    // Breathing / Idle hovering animation
    val infiniteTransition = rememberInfiniteTransition(label = "idle")
    val idleOffsetY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleY"
    )

    // Tail wagging animation
    val tailRotation by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(species.let { if (it == PetSpecies.DOG) 350 else 750 }, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tailWag"
    )

    // Mouth animation when speaking
    val mouthOpenProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouthOpen"
    )

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        onPetAvatar()
                        coroutineScope.launch {
                            bounceScale.animateTo(
                                targetValue = 1.08f,
                                animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f)
                            )
                            bounceScale.animateTo(1f, spring(dampingRatio = 0.6f))
                        }
                        // Spawn floating heart particle
                        particles.add(
                            FloatingParticle(
                                id = System.currentTimeMillis() + Random.nextLong(1000),
                                startX = offset.x,
                                startY = offset.y,
                                color = listOf(Color(0xFFFF4081), Color(0xFFFF80AB), Color(0xFFFFB74D)).random()
                            )
                        )
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val centerX = canvasW / 2f
            val centerY = canvasH / 2f + idleOffsetY

            // Scaling & bouncing factor
            val currentScale = bounceScale.value
            val baseHeadRadius = canvasW * 0.28f * currentScale

            // 1. Draw Tail
            drawTail(species, customization.furColor, centerX, centerY, tailRotation, baseHeadRadius)

            // 2. Draw Body & Costume Base
            drawBody(customization, centerX, centerY, baseHeadRadius)

            // 3. Draw Ears
            drawEars(species, customization, centerX, centerY, baseHeadRadius)

            // 4. Draw Head
            drawCircle(
                color = customization.furColor,
                radius = baseHeadRadius,
                center = Offset(centerX, centerY - baseHeadRadius * 0.25f)
            )

            // Optional Tabby / Spotted Markings
            drawHeadMarkings(species, customization, centerX, centerY, baseHeadRadius)

            // 5. Draw Eyes
            val effectiveBlink = isBlinking && emotion != AvatarEmotion.LOVING
            drawEyes(customization.eyeStyle, emotion, effectiveBlink, centerX, centerY, baseHeadRadius)

            // 6. Draw Nose & Whiskers
            drawNoseAndWhiskers(species, centerX, centerY, baseHeadRadius)

            // 7. Draw Mouth (Talking sync)
            val isMouthActive = isTalking || emotion == AvatarEmotion.HAPPY
            val currentMouthOpen = if (isTalking) mouthOpenProgress else if (emotion == AvatarEmotion.HAPPY) 0.6f else 0f
            drawMouth(currentMouthOpen, isMouthActive, centerX, centerY, baseHeadRadius)

            // 8. Draw Rosy Cheeks
            val cheekColor = Color(0xFFFF8A80).copy(alpha = if (emotion == AvatarEmotion.LOVING || emotion == AvatarEmotion.HAPPY) 0.65f else 0.4f)
            val cheekY = centerY - baseHeadRadius * 0.15f
            drawCircle(cheekColor, radius = baseHeadRadius * 0.15f, center = Offset(centerX - baseHeadRadius * 0.55f, cheekY))
            drawCircle(cheekColor, radius = baseHeadRadius * 0.15f, center = Offset(centerX + baseHeadRadius * 0.55f, cheekY))

            // 9. Draw Costume Upper Layer (Hat / Hood / Collar)
            drawCostumeOverlay(customization.costume, centerX, centerY, baseHeadRadius)

            // 10. Draw Accessories (Glasses, Bell, Flower)
            drawAccessories(customization.accessory, centerX, centerY, baseHeadRadius)

            // 11. Draw Floating Hearts from petting
            drawFloatingParticles(particles)
        }
    }
}

private fun DrawScope.drawTail(
    species: PetSpecies,
    furColor: Color,
    cx: Float,
    cy: Float,
    wagAngle: Float,
    headRadius: Float
) {
    val tailBaseX = cx + headRadius * 0.65f
    val tailBaseY = cy + headRadius * 0.75f
    val path = Path().apply {
        moveTo(tailBaseX, tailBaseY)
        if (species == PetSpecies.DOG) {
            // Fluffy wagging dog tail
            quadraticTo(
                tailBaseX + 50f + wagAngle * 2.2f,
                tailBaseY - 60f + wagAngle * 0.8f,
                tailBaseX + 65f + wagAngle * 2.8f,
                tailBaseY - 110f
            )
        } else {
            // Elegant curved cat tail
            cubicTo(
                tailBaseX + 40f + wagAngle * 1.5f, tailBaseY - 30f,
                tailBaseX + 70f + wagAngle * 2f, tailBaseY - 90f,
                tailBaseX + 45f + wagAngle * 1.8f, tailBaseY - 120f
            )
        }
    }
    drawPath(
        path = path,
        color = furColor,
        style = Stroke(width = if (species == PetSpecies.DOG) 34f else 24f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawBody(
    customization: AvatarCustomization,
    cx: Float,
    cy: Float,
    headRadius: Float
) {
    val bodyWidth = headRadius * 1.4f
    val bodyHeight = headRadius * 1.3f
    val bodyTop = cy + headRadius * 0.35f

    // Natural Fur Body
    drawRoundRect(
        color = customization.furColor,
        topLeft = Offset(cx - bodyWidth / 2, bodyTop),
        size = Size(bodyWidth, bodyHeight),
        cornerRadius = CornerRadius(bodyWidth * 0.45f, bodyWidth * 0.45f)
    )

    // Inner belly patch
    drawRoundRect(
        color = Color.White.copy(alpha = 0.75f),
        topLeft = Offset(cx - bodyWidth * 0.3f, bodyTop + 15f),
        size = Size(bodyWidth * 0.6f, bodyHeight * 0.75f),
        cornerRadius = CornerRadius(bodyWidth * 0.35f, bodyWidth * 0.35f)
    )

    // Costume main body overlay
    when (customization.costume) {
        CostumeStyle.HOODIE -> {
            drawRoundRect(
                color = Color(0xFF3F51B5),
                topLeft = Offset(cx - bodyWidth / 2, bodyTop + 10f),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(bodyWidth * 0.4f, bodyWidth * 0.4f)
            )
            // Hoodie pocket
            drawRoundRect(
                color = Color(0xFF303F9F),
                topLeft = Offset(cx - bodyWidth * 0.3f, bodyTop + bodyHeight * 0.45f),
                size = Size(bodyWidth * 0.6f, bodyHeight * 0.35f),
                cornerRadius = CornerRadius(16f, 16f)
            )
        }
        CostumeStyle.SUPERHERO -> {
            // Superhero Suit
            drawRoundRect(
                color = Color(0xFF1E88E5),
                topLeft = Offset(cx - bodyWidth / 2, bodyTop + 10f),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(bodyWidth * 0.4f, bodyWidth * 0.4f)
            )
            // Gold belt
            drawRect(
                color = Color(0xFFFFD54F),
                topLeft = Offset(cx - bodyWidth / 2, bodyTop + bodyHeight * 0.55f),
                size = Size(bodyWidth, 22f)
            )
            // Chest Emblem (Paw Shield)
            drawCircle(
                color = Color(0xFFE53935),
                radius = 24f,
                center = Offset(cx, bodyTop + bodyHeight * 0.28f)
            )
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 16f,
                center = Offset(cx, bodyTop + bodyHeight * 0.28f)
            )
        }
        CostumeStyle.CHEF -> {
            // White Chef Apron
            drawRoundRect(
                color = Color(0xFFFAFAFA),
                topLeft = Offset(cx - bodyWidth / 2, bodyTop + 10f),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(bodyWidth * 0.4f, bodyWidth * 0.4f)
            )
            // Red neckerchief
            val neckPath = Path().apply {
                moveTo(cx - 30f, bodyTop + 10f)
                lineTo(cx + 30f, bodyTop + 10f)
                lineTo(cx, bodyTop + 45f)
                close()
            }
            drawPath(neckPath, color = Color(0xFFE53935))
        }
        CostumeStyle.DETECTIVE -> {
            // Trenchcoat
            drawRoundRect(
                color = Color(0xFF6D4C41),
                topLeft = Offset(cx - bodyWidth / 2, bodyTop + 10f),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(bodyWidth * 0.4f, bodyWidth * 0.4f)
            )
            // Coat Lapels & Buttons
            drawLine(Color(0xFF4E342E), Offset(cx, bodyTop + 15f), Offset(cx, bodyTop + bodyHeight), strokeWidth = 4f)
            drawCircle(Color(0xFFFFD54F), radius = 6f, center = Offset(cx - 16f, bodyTop + bodyHeight * 0.35f))
            drawCircle(Color(0xFFFFD54F), radius = 6f, center = Offset(cx + 16f, bodyTop + bodyHeight * 0.35f))
        }
        CostumeStyle.PAJAMAS -> {
            // Star Pajamas
            drawRoundRect(
                color = Color(0xFF5C6BC0),
                topLeft = Offset(cx - bodyWidth / 2, bodyTop + 10f),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(bodyWidth * 0.4f, bodyWidth * 0.4f)
            )
            // Little cute stars
            drawCircle(Color(0xFFFFEE58), radius = 5f, center = Offset(cx - 25f, bodyTop + 35f))
            drawCircle(Color(0xFFFFEE58), radius = 5f, center = Offset(cx + 30f, bodyTop + 55f))
            drawCircle(Color(0xFFFFEE58), radius = 5f, center = Offset(cx - 15f, bodyTop + 75f))
        }
        CostumeStyle.ROYAL_ROBE -> {
            drawRoundRect(
                color = Color(0xFF8E0038),
                topLeft = Offset(cx - bodyWidth / 2, bodyTop + 10f),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(bodyWidth * 0.4f, bodyWidth * 0.4f)
            )
            drawRoundRect(
                color = Color(0xFFFAFAFA),
                topLeft = Offset(cx - bodyWidth * 0.14f, bodyTop + 10f),
                size = Size(bodyWidth * 0.28f, bodyHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawCircle(Color(0xFFFFD54F), radius = 12f, center = Offset(cx, bodyTop + 26f))
            drawCircle(Color(0xFF00E5FF), radius = 7f, center = Offset(cx, bodyTop + 26f))
        }
        CostumeStyle.CYBER_JACKET -> {
            drawRoundRect(
                color = Color(0xFF1A1C29),
                topLeft = Offset(cx - bodyWidth / 2, bodyTop + 10f),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(bodyWidth * 0.4f, bodyWidth * 0.4f)
            )
            drawLine(Color(0xFF00E5FF), Offset(cx, bodyTop + 12f), Offset(cx, bodyTop + bodyHeight), strokeWidth = 5f)
        }
        CostumeStyle.WINTER_COAT -> {
            drawRoundRect(
                color = Color(0xFF00897B),
                topLeft = Offset(cx - bodyWidth / 2, bodyTop + 10f),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(bodyWidth * 0.4f, bodyWidth * 0.4f)
            )
            drawRoundRect(
                color = Color(0xFFD32F2F),
                topLeft = Offset(cx - bodyWidth * 0.48f, bodyTop + 10f),
                size = Size(bodyWidth * 0.96f, 22f),
                cornerRadius = CornerRadius(11f, 11f)
            )
        }
        CostumeStyle.NONE -> {}
    }

    // Cute front paws
    val pawY = bodyTop + bodyHeight - 12f
    drawCircle(customization.furColor, radius = headRadius * 0.22f, center = Offset(cx - bodyWidth * 0.32f, pawY))
    drawCircle(customization.furColor, radius = headRadius * 0.22f, center = Offset(cx + bodyWidth * 0.32f, pawY))
    drawCircle(Color.White.copy(alpha = 0.8f), radius = headRadius * 0.16f, center = Offset(cx - bodyWidth * 0.32f, pawY))
    drawCircle(Color.White.copy(alpha = 0.8f), radius = headRadius * 0.16f, center = Offset(cx + bodyWidth * 0.32f, pawY))
}

private fun DrawScope.drawEars(
    species: PetSpecies,
    customization: AvatarCustomization,
    cx: Float,
    cy: Float,
    headRadius: Float
) {
    val earStyle = customization.earStyle
    val headCenterY = cy - headRadius * 0.25f

    when (earStyle) {
        EarStyle.CAT_POINTY -> {
            // Pointy Cat Ears
            val leftEar = Path().apply {
                moveTo(cx - headRadius * 0.82f, headCenterY - headRadius * 0.2f)
                lineTo(cx - headRadius * 0.7f, headCenterY - headRadius * 1.15f)
                lineTo(cx - headRadius * 0.25f, headCenterY - headRadius * 0.75f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(cx + headRadius * 0.82f, headCenterY - headRadius * 0.2f)
                lineTo(cx + headRadius * 0.7f, headCenterY - headRadius * 1.15f)
                lineTo(cx + headRadius * 0.25f, headCenterY - headRadius * 0.75f)
                close()
            }
            drawPath(leftEar, customization.furColor)
            drawPath(rightEar, customization.furColor)

            // Inner Pink
            val leftInner = Path().apply {
                moveTo(cx - headRadius * 0.72f, headCenterY - headRadius * 0.3f)
                lineTo(cx - headRadius * 0.65f, headCenterY - headRadius * 0.98f)
                lineTo(cx - headRadius * 0.35f, headCenterY - headRadius * 0.72f)
                close()
            }
            val rightInner = Path().apply {
                moveTo(cx + headRadius * 0.72f, headCenterY - headRadius * 0.3f)
                lineTo(cx + headRadius * 0.65f, headCenterY - headRadius * 0.98f)
                lineTo(cx + headRadius * 0.35f, headCenterY - headRadius * 0.72f)
                close()
            }
            drawPath(leftInner, Color(0xFFFFAB91))
            drawPath(rightInner, Color(0xFFFFAB91))
        }

        EarStyle.DOG_FLOPPY -> {
            // Floppy Dog Ears (hanging on sides)
            val leftFloppy = Path().apply {
                moveTo(cx - headRadius * 0.65f, headCenterY - headRadius * 0.6f)
                cubicTo(
                    cx - headRadius * 1.25f, headCenterY - headRadius * 0.2f,
                    cx - headRadius * 1.15f, headCenterY + headRadius * 0.65f,
                    cx - headRadius * 0.65f, headCenterY + headRadius * 0.6f
                )
                close()
            }
            val rightFloppy = Path().apply {
                moveTo(cx + headRadius * 0.65f, headCenterY - headRadius * 0.6f)
                cubicTo(
                    cx + headRadius * 1.25f, headCenterY - headRadius * 0.2f,
                    cx + headRadius * 1.15f, headCenterY + headRadius * 0.65f,
                    cx + headRadius * 0.65f, headCenterY + headRadius * 0.6f
                )
                close()
            }
            drawPath(leftFloppy, customization.furColor)
            drawPath(rightFloppy, customization.furColor)
        }

        EarStyle.BEAR_ROUND -> {
            // Cute Round Bear/Panda Ears
            val leftCenter = Offset(cx - headRadius * 0.65f, headCenterY - headRadius * 0.75f)
            val rightCenter = Offset(cx + headRadius * 0.65f, headCenterY - headRadius * 0.75f)
            drawCircle(customization.furColor, radius = headRadius * 0.35f, center = leftCenter)
            drawCircle(customization.furColor, radius = headRadius * 0.35f, center = rightCenter)
            drawCircle(Color(0xFFFFAB91), radius = headRadius * 0.22f, center = leftCenter)
            drawCircle(Color(0xFFFFAB91), radius = headRadius * 0.22f, center = rightCenter)
        }

        EarStyle.RIBBON_EARS -> {
            // Pointy Ears with Bow Ribbon
            val leftEar = Path().apply {
                moveTo(cx - headRadius * 0.82f, headCenterY - headRadius * 0.2f)
                lineTo(cx - headRadius * 0.7f, headCenterY - headRadius * 1.1f)
                lineTo(cx - headRadius * 0.25f, headCenterY - headRadius * 0.75f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(cx + headRadius * 0.82f, headCenterY - headRadius * 0.2f)
                lineTo(cx + headRadius * 0.7f, headCenterY - headRadius * 1.1f)
                lineTo(cx + headRadius * 0.25f, headCenterY - headRadius * 0.75f)
                close()
            }
            drawPath(leftEar, customization.furColor)
            drawPath(rightEar, customization.furColor)

            // Red Ribbon in center of head
            val bowCenter = Offset(cx, headCenterY - headRadius * 0.85f)
            drawCircle(Color(0xFFE91E63), radius = 14f, center = bowCenter)
            val leftBow = Path().apply {
                moveTo(bowCenter.x, bowCenter.y)
                lineTo(bowCenter.x - 38f, bowCenter.y - 20f)
                lineTo(bowCenter.x - 38f, bowCenter.y + 20f)
                close()
            }
            val rightBow = Path().apply {
                moveTo(bowCenter.x, bowCenter.y)
                lineTo(bowCenter.x + 38f, bowCenter.y - 20f)
                lineTo(bowCenter.x + 38f, bowCenter.y + 20f)
                close()
            }
            drawPath(leftBow, Color(0xFFE91E63))
            drawPath(rightBow, Color(0xFFE91E63))
        }
    }
}

private fun DrawScope.drawHeadMarkings(
    species: PetSpecies,
    customization: AvatarCustomization,
    cx: Float,
    cy: Float,
    headRadius: Float
) {
    val headCenterY = cy - headRadius * 0.25f
    if (species == PetSpecies.CAT && customization.furColorId in listOf("orange", "golden", "calico")) {
        // Tabby Stripes on forehead
        val stripeColor = Color(0xFFE65100).copy(alpha = 0.7f)
        val path = Path().apply {
            moveTo(cx - 10f, headCenterY - headRadius * 0.85f)
            lineTo(cx, headCenterY - headRadius * 0.5f)
            lineTo(cx + 10f, headCenterY - headRadius * 0.85f)
            moveTo(cx - 30f, headCenterY - headRadius * 0.78f)
            lineTo(cx - 20f, headCenterY - headRadius * 0.55f)
            moveTo(cx + 30f, headCenterY - headRadius * 0.78f)
            lineTo(cx + 20f, headCenterY - headRadius * 0.55f)
        }
        drawPath(path, stripeColor, style = Stroke(width = 7f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawEyes(
    eyeStyle: EyeStyle,
    emotion: AvatarEmotion,
    isBlinking: Boolean,
    cx: Float,
    cy: Float,
    headRadius: Float
) {
    val headCenterY = cy - headRadius * 0.25f
    val eyeSpacing = headRadius * 0.42f
    val eyeY = headCenterY - headRadius * 0.05f
    val leftEyeCenter = Offset(cx - eyeSpacing, eyeY)
    val rightEyeCenter = Offset(cx + eyeSpacing, eyeY)

    if (isBlinking || emotion == AvatarEmotion.SLEEPY) {
        // Closed / Blinking happy arcs
        val arcW = headRadius * 0.22f
        val leftArc = Path().apply {
            moveTo(leftEyeCenter.x - arcW, leftEyeCenter.y)
            quadraticTo(leftEyeCenter.x, leftEyeCenter.y + 14f, leftEyeCenter.x + arcW, leftEyeCenter.y)
        }
        val rightArc = Path().apply {
            moveTo(rightEyeCenter.x - arcW, rightEyeCenter.y)
            quadraticTo(rightEyeCenter.x, rightEyeCenter.y + 14f, rightEyeCenter.x + arcW, rightEyeCenter.y)
        }
        drawPath(leftArc, Color(0xFF212121), style = Stroke(width = 6f, cap = StrokeCap.Round))
        drawPath(rightArc, Color(0xFF212121), style = Stroke(width = 6f, cap = StrokeCap.Round))
        return
    }

    if (emotion == AvatarEmotion.LOVING || eyeStyle == EyeStyle.HEARTS) {
        // Big Sparkling Hearts
        drawHeart(leftEyeCenter, headRadius * 0.22f, Color(0xFFE91E63))
        drawHeart(rightEyeCenter, headRadius * 0.22f, Color(0xFFE91E63))
        return
    }

    if (eyeStyle == EyeStyle.PLAYFUL_WINK) {
        // Left Eye Open, Right Eye Wink
        drawSparklingEye(leftEyeCenter, headRadius * 0.2f)
        val rightWink = Path().apply {
            moveTo(rightEyeCenter.x - 22f, rightEyeCenter.y)
            quadraticTo(rightEyeCenter.x, rightEyeCenter.y - 14f, rightEyeCenter.x + 22f, rightEyeCenter.y)
        }
        drawPath(rightWink, Color(0xFF212121), style = Stroke(width = 6f, cap = StrokeCap.Round))
        return
    }

    // Default Sparkling Anime Eyes
    val eyeR = headRadius * 0.22f
    drawSparklingEye(leftEyeCenter, eyeR)
    drawSparklingEye(rightEyeCenter, eyeR)
}

private fun DrawScope.drawSparklingEye(center: Offset, radius: Float) {
    // Outer dark circle
    drawCircle(Color(0xFF212121), radius = radius, center = center)
    // Pupil Gradient depth
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF424242), Color(0xFF111111)),
            center = center,
            radius = radius
        ),
        radius = radius * 0.95f,
        center = center
    )
    // Big Sparkle Highlight
    drawCircle(Color.White, radius = radius * 0.38f, center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f))
    // Small Sparkle Highlight
    drawCircle(Color.White, radius = radius * 0.16f, center = Offset(center.x + radius * 0.35f, center.y + radius * 0.25f))
}

private fun DrawScope.drawHeart(center: Offset, size: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y + size * 0.5f)
        cubicTo(center.x - size, center.y - size * 0.2f, center.x - size * 0.8f, center.y - size * 0.8f, center.x, center.y - size * 0.3f)
        cubicTo(center.x + size * 0.8f, center.y - size * 0.8f, center.x + size, center.y - size * 0.2f, center.x, center.y + size * 0.5f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawNoseAndWhiskers(
    species: PetSpecies,
    cx: Float,
    cy: Float,
    headRadius: Float
) {
    val headCenterY = cy - headRadius * 0.25f
    val noseY = headCenterY + headRadius * 0.15f

    // Nose
    val nosePath = Path().apply {
        if (species == PetSpecies.DOG) {
            // Dog rounded wide nose
            moveTo(cx - 16f, noseY - 6f)
            lineTo(cx + 16f, noseY - 6f)
            cubicTo(cx + 14f, noseY + 14f, cx - 14f, noseY + 14f, cx - 16f, noseY - 6f)
        } else {
            // Cute pink triangular cat nose
            moveTo(cx - 12f, noseY - 4f)
            lineTo(cx + 12f, noseY - 4f)
            lineTo(cx, noseY + 8f)
            close()
        }
    }
    drawPath(nosePath, if (species == PetSpecies.DOG) Color(0xFF263238) else Color(0xFFFF7043))

    // Cat Whiskers
    if (species == PetSpecies.CAT) {
        val whiskerColor = Color(0xFF9E9E9E).copy(alpha = 0.8f)
        val whiskerY = noseY + 10f
        // Left Whiskers
        drawLine(whiskerColor, Offset(cx - headRadius * 0.45f, whiskerY - 6f), Offset(cx - headRadius * 0.95f, whiskerY - 18f), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(whiskerColor, Offset(cx - headRadius * 0.45f, whiskerY + 6f), Offset(cx - headRadius * 0.98f, whiskerY + 12f), strokeWidth = 3f, cap = StrokeCap.Round)
        // Right Whiskers
        drawLine(whiskerColor, Offset(cx + headRadius * 0.45f, whiskerY - 6f), Offset(cx + headRadius * 0.95f, whiskerY - 18f), strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(whiskerColor, Offset(cx + headRadius * 0.45f, whiskerY + 6f), Offset(cx + headRadius * 0.98f, whiskerY + 12f), strokeWidth = 3f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawMouth(
    openProgress: Float,
    isTalking: Boolean,
    cx: Float,
    cy: Float,
    headRadius: Float
) {
    val headCenterY = cy - headRadius * 0.25f
    val mouthY = headCenterY + headRadius * 0.26f

    if (isTalking && openProgress > 0.1f) {
        // Dynamic opening mouth
        val mouthHeight = 12f + openProgress * 26f
        val mouthWidth = 26f + openProgress * 12f
        drawOval(
            color = Color(0xFFC2185B),
            topLeft = Offset(cx - mouthWidth / 2, mouthY),
            size = Size(mouthWidth, mouthHeight)
        )
        // Little pink tongue
        drawOval(
            color = Color(0xFFFF80AB),
            topLeft = Offset(cx - mouthWidth * 0.35f, mouthY + mouthHeight * 0.4f),
            size = Size(mouthWidth * 0.7f, mouthHeight * 0.6f)
        )
    } else {
        // Classic W / cat smile
        val smilePath = Path().apply {
            moveTo(cx, mouthY - 4f)
            lineTo(cx, mouthY + 5f)
            moveTo(cx, mouthY + 5f)
            quadraticTo(cx - 14f, mouthY + 18f, cx - 24f, mouthY + 8f)
            moveTo(cx, mouthY + 5f)
            quadraticTo(cx + 14f, mouthY + 18f, cx + 24f, mouthY + 8f)
        }
        drawPath(smilePath, Color(0xFF37474F), style = Stroke(width = 4.5f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawCostumeOverlay(
    costume: CostumeStyle,
    cx: Float,
    cy: Float,
    headRadius: Float
) {
    val headCenterY = cy - headRadius * 0.25f

    when (costume) {
        CostumeStyle.CHEF -> {
            // White Chef Hat (Toque) on head
            val hatTopY = headCenterY - headRadius * 1.55f
            val hatPath = Path().apply {
                moveTo(cx - headRadius * 0.6f, headCenterY - headRadius * 0.85f)
                cubicTo(
                    cx - headRadius * 0.8f, hatTopY,
                    cx - headRadius * 0.2f, hatTopY - 25f,
                    cx, hatTopY
                )
                cubicTo(
                    cx + headRadius * 0.2f, hatTopY - 25f,
                    cx + headRadius * 0.8f, hatTopY,
                    cx + headRadius * 0.6f, headCenterY - headRadius * 0.85f
                )
                close()
            }
            drawPath(hatPath, Color(0xFFFFFFFF))
            drawPath(hatPath, Color(0xFFE0E0E0), style = Stroke(width = 3.5f))
            // Hat band
            drawRoundRect(
                color = Color(0xFFEEEEEE),
                topLeft = Offset(cx - headRadius * 0.6f, headCenterY - headRadius * 0.92f),
                size = Size(headRadius * 1.2f, 22f),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
        CostumeStyle.DETECTIVE -> {
            // Detective Cap
            val hatY = headCenterY - headRadius * 0.88f
            drawRoundRect(
                color = Color(0xFF5D4037),
                topLeft = Offset(cx - headRadius * 0.7f, hatY),
                size = Size(headRadius * 1.4f, 28f),
                cornerRadius = CornerRadius(14f, 14f)
            )
            // Bow tie
            drawCircle(Color(0xFFE53935), radius = 10f, center = Offset(cx, cy + headRadius * 0.38f))
        }
        CostumeStyle.SUPERHERO -> {
            // Superhero Red Mask over eyes
            val eyeY = headCenterY - headRadius * 0.05f
            val maskPath = Path().apply {
                moveTo(cx - headRadius * 0.75f, eyeY - 20f)
                cubicTo(cx - 30f, eyeY - 32f, cx - 10f, eyeY - 10f, cx, eyeY - 20f)
                cubicTo(cx + 10f, eyeY - 10f, cx + 30f, eyeY - 32f, cx + headRadius * 0.75f, eyeY - 20f)
                cubicTo(cx + headRadius * 0.8f, eyeY + 28f, cx + 30f, eyeY + 26f, cx, eyeY + 16f)
                cubicTo(cx - 30f, eyeY + 26f, cx - headRadius * 0.8f, eyeY + 28f, cx - headRadius * 0.75f, eyeY - 20f)
                close()
            }
            drawPath(maskPath, Color(0xFFE53935).copy(alpha = 0.88f))
        }
        CostumeStyle.PAJAMAS -> {
            // Nightcap on head
            val capPath = Path().apply {
                moveTo(cx - headRadius * 0.5f, headCenterY - headRadius * 0.85f)
                quadraticTo(cx + 15f, headCenterY - headRadius * 1.4f, cx + headRadius * 0.95f, headCenterY - headRadius * 0.7f)
                lineTo(cx + headRadius * 0.4f, headCenterY - headRadius * 0.85f)
                close()
            }
            drawPath(capPath, Color(0xFF3949AB))
            drawCircle(Color(0xFFFFD54F), radius = 16f, center = Offset(cx + headRadius * 0.95f, headCenterY - headRadius * 0.7f))
        }
        else -> {}
    }
}

private fun DrawScope.drawAccessories(
    accessory: AccessoryStyle,
    cx: Float,
    cy: Float,
    headRadius: Float
) {
    val headCenterY = cy - headRadius * 0.25f
    val neckY = cy + headRadius * 0.38f

    when (accessory) {
        AccessoryStyle.BELL_COLLAR -> {
            // Red Collar
            drawRoundRect(
                color = Color(0xFFD32F2F),
                topLeft = Offset(cx - headRadius * 0.55f, neckY - 8f),
                size = Size(headRadius * 1.1f, 18f),
                cornerRadius = CornerRadius(9f, 9f)
            )
            // Golden Bell
            drawCircle(Color(0xFFFFD54F), radius = 15f, center = Offset(cx, neckY + 10f))
            drawCircle(Color(0xFFFFA000), radius = 4f, center = Offset(cx, neckY + 14f))
        }
        AccessoryStyle.STAR_CHOKER -> {
            // Black choker with Gold Star
            drawRoundRect(
                color = Color(0xFF212121),
                topLeft = Offset(cx - headRadius * 0.55f, neckY - 6f),
                size = Size(headRadius * 1.1f, 14f),
                cornerRadius = CornerRadius(7f, 7f)
            )
            drawCircle(Color(0xFFFFD54F), radius = 14f, center = Offset(cx, neckY + 8f))
        }
        AccessoryStyle.SUNGLASSES -> {
            val eyeY = headCenterY - headRadius * 0.05f
            // Cool retro black shades
            drawRoundRect(
                color = Color(0xFF1B1B1B),
                topLeft = Offset(cx - headRadius * 0.65f, eyeY - 20f),
                size = Size(headRadius * 0.55f, 40f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = Color(0xFF1B1B1B),
                topLeft = Offset(cx + headRadius * 0.1f, eyeY - 20f),
                size = Size(headRadius * 0.55f, 40f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            // Bridge
            drawRect(Color(0xFF1B1B1B), topLeft = Offset(cx - 15f, eyeY - 10f), size = Size(30f, 8f))
            // Lens glare
            drawLine(Color.White.copy(alpha = 0.4f), Offset(cx - headRadius * 0.55f, eyeY - 10f), Offset(cx - headRadius * 0.25f, eyeY + 10f), strokeWidth = 4f)
        }
        AccessoryStyle.ROUND_GLASSES -> {
            val eyeY = headCenterY - headRadius * 0.05f
            val r = headRadius * 0.28f
            drawCircle(Color(0xFF424242), radius = r, center = Offset(cx - headRadius * 0.4f, eyeY), style = Stroke(width = 5f))
            drawCircle(Color(0xFF424242), radius = r, center = Offset(cx + headRadius * 0.4f, eyeY), style = Stroke(width = 5f))
            drawLine(Color(0xFF424242), Offset(cx - headRadius * 0.12f, eyeY), Offset(cx + headRadius * 0.12f, eyeY), strokeWidth = 5f)
        }
        AccessoryStyle.FLOWER_PIN -> {
            val flowerCenter = Offset(cx + headRadius * 0.62f, headCenterY - headRadius * 0.72f)
            for (i in 0 until 5) {
                val angle = i * (Math.PI * 2 / 5)
                val petalOffset = Offset(
                    (flowerCenter.x + Math.cos(angle) * 16f).toFloat(),
                    (flowerCenter.y + Math.sin(angle) * 16f).toFloat()
                )
                drawCircle(Color(0xFFFF80AB), radius = 12f, center = petalOffset)
            }
            drawCircle(Color(0xFFFFEE58), radius = 9f, center = flowerCenter)
        }
        AccessoryStyle.ROYAL_CROWN -> {
            val crownBaseY = headCenterY - headRadius * 0.78f
            val crownW = headRadius * 0.7f
            val crownPath = Path().apply {
                moveTo(cx - crownW * 0.5f, crownBaseY)
                lineTo(cx - crownW * 0.58f, crownBaseY - headRadius * 0.35f)
                lineTo(cx - crownW * 0.25f, crownBaseY - headRadius * 0.16f)
                lineTo(cx, crownBaseY - headRadius * 0.44f)
                lineTo(cx + crownW * 0.25f, crownBaseY - headRadius * 0.16f)
                lineTo(cx + crownW * 0.58f, crownBaseY - headRadius * 0.35f)
                lineTo(cx + crownW * 0.5f, crownBaseY)
                close()
            }
            drawPath(crownPath, color = Color(0xFFFFD54F))
            drawCircle(Color(0xFFE53935), radius = 6f, center = Offset(cx, crownBaseY - headRadius * 0.12f))
        }
        AccessoryStyle.WIZARD_HAT -> {
            val hatBaseY = headCenterY - headRadius * 0.72f
            drawOval(
                color = Color(0xFF311B92),
                topLeft = Offset(cx - headRadius * 0.7f, hatBaseY - 12f),
                size = Size(headRadius * 1.4f, 26f)
            )
            val conePath = Path().apply {
                moveTo(cx - headRadius * 0.45f, hatBaseY)
                lineTo(cx + headRadius * 0.1f, hatBaseY - headRadius * 0.75f)
                lineTo(cx + headRadius * 0.45f, hatBaseY)
                close()
            }
            drawPath(conePath, color = Color(0xFF4527A0))
        }
        AccessoryStyle.GAMING_HEADSET -> {
            drawArc(
                color = Color(0xFF212121),
                startAngle = 195f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(cx - headRadius * 0.95f, headCenterY - headRadius * 1.0f),
                size = Size(headRadius * 1.9f, headRadius * 1.6f),
                style = Stroke(width = 14f)
            )
            drawRoundRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(cx - headRadius * 1.02f, headCenterY - headRadius * 0.2f),
                size = Size(20f, 44f),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(cx + headRadius * 1.02f - 20f, headCenterY - headRadius * 0.2f),
                size = Size(20f, 44f),
                cornerRadius = CornerRadius(10f, 10f)
            )
        }
        AccessoryStyle.RED_BOWTIE -> {
            val leftBow = Path().apply {
                moveTo(cx, neckY + 2f)
                lineTo(cx - 26f, neckY - 12f)
                lineTo(cx - 26f, neckY + 16f)
                close()
            }
            val rightBow = Path().apply {
                moveTo(cx, neckY + 2f)
                lineTo(cx + 26f, neckY - 12f)
                lineTo(cx + 26f, neckY + 16f)
                close()
            }
            drawPath(leftBow, color = Color(0xFFD32F2F))
            drawPath(rightBow, color = Color(0xFFD32F2F))
            drawCircle(Color(0xFFB71C1C), radius = 8f, center = Offset(cx, neckY + 2f))
        }
        AccessoryStyle.NONE -> {}
    }
}

private fun DrawScope.drawFloatingParticles(particles: List<FloatingParticle>) {
    particles.forEach { p ->
        drawCircle(
            color = p.color.copy(alpha = 0.85f),
            radius = 12f,
            center = Offset(p.startX, p.startY - 30f)
        )
    }
}

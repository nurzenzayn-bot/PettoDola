package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.model.BackgroundTheme
import com.example.model.TimeOfDay
import kotlin.math.sin
import kotlin.random.Random

private data class AmbientParticle(
    val initialXRatio: Float,
    val initialYRatio: Float,
    val radius: Float,
    val speed: Float,
    val phase: Float,
    val color: Color
)

@Composable
fun DynamicBackgroundCanvas(
    theme: BackgroundTheme,
    currentTimeOfDay: TimeOfDay,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambientProgress"
    )

    // Resolve ornate background image if present
    val ornateImageRes = theme.resolveDrawableRes(currentTimeOfDay)


    Box(modifier = modifier.fillMaxSize()) {
        if (ornateImageRes != null) {
            // 1. Full-bleed ornate background image covering entire phone screen
            Image(
                painter = painterResource(id = ornateImageRes),
                contentDescription = "Full-screen Ornate Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Subtle atmospheric vignette gradient to ensure top glass bar and bottom controls have optimum contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x35000000), // top bar readability
                                Color(0x05000000),
                                Color(0x35000000)  // bottom controls readability
                            )
                        )
                    )
            )

            // Clean room with vignette gradient (no rain particles)
        } else {
            // Procedural Canvas for styles without photographic ornate bitmaps (Cyberpunk, Sakura, Space)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val (topColor, midColor, botColor, floorColor) = when (theme) {
                    BackgroundTheme.CYBERPUNK_NEON -> listOf(
                        Color(0xFF09021F),
                        Color(0xFF1E0E3E),
                        Color(0xFF301B58),
                        Color(0xFF0A0217)
                    )
                    BackgroundTheme.SAKURA_GARDEN -> listOf(
                        Color(0xFFFDE2E4),
                        Color(0xFFFFCAD4),
                        Color(0xFFE2ECE9),
                        Color(0xFF8E9A82)
                    )
                    BackgroundTheme.DEEP_SPACE -> listOf(
                        Color(0xFF040209),
                        Color(0xFF0E0720),
                        Color(0xFF1A0B3B),
                        Color(0xFF020106)
                    )
                    else -> listOf(
                        Color(0xFF1E2A44),
                        Color(0xFF131A2B),
                        Color(0xFF0F1522),
                        Color(0xFF0F1522)
                    )
                }

                // 1. Sky / Wall Layer
                val skyBrush = Brush.verticalGradient(
                    colors = listOf(topColor, midColor, botColor),
                    startY = 0f,
                    endY = h * 0.72f
                )
                drawRect(brush = skyBrush, size = Size(w, h * 0.72f))

                // 2. Horizon Glow / Backlight
                val horizonGlow = Brush.radialGradient(
                    colors = listOf(
                        botColor.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.72f),
                    radius = w * 0.75f
                )
                drawRect(brush = horizonGlow, size = Size(w, h * 0.72f))

                // 3. Floor / Room Base
                val floorY = h * 0.70f
                val floorBrush = Brush.verticalGradient(
                    colors = listOf(
                        floorColor.copy(alpha = 0.95f),
                        floorColor
                    ),
                    startY = floorY,
                    endY = h
                )
                drawRect(
                    brush = floorBrush,
                    topLeft = Offset(0f, floorY),
                    size = Size(w, h - floorY)
                )

                // Perspective floor line
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(0f, floorY),
                    end = Offset(w, floorY),
                    strokeWidth = 2f
                )

                // Perspective grid lines if Cyberpunk
                if (theme == BackgroundTheme.CYBERPUNK_NEON) {
                    val gridColor = Color(0xFF00F5D4).copy(alpha = 0.35f)
                    val vanishingPoint = Offset(w * 0.5f, floorY)
                    val numLines = 8
                    for (i in 0..numLines) {
                        val endX = w * (i.toFloat() / numLines)
                        drawLine(
                            color = gridColor,
                            start = vanishingPoint,
                            end = Offset(endX, h),
                            strokeWidth = 1.5f
                        )
                    }
                    for (j in 1..4) {
                        val y = floorY + (h - floorY) * (j * j / 16f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.2f
                        )
                    }
                }


            }
        }
    }
}

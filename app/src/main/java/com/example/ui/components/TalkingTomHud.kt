package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CareOverlayType

/**
 * Top HUD identical to My Talking Tom reference screenshot:
 * - Level badge (LVL 12) with blue progress ring
 * - Gold star coins capsule (1,500)
 * - Blue diamond capsule (240)
 * - Settings gear (sky blue circle)
 * - Shopping cart (coral red circle)
 */
@Composable
fun TalkingTomTopHud(
    level: Int,
    levelProgress: Float,
    coins: Int,
    diamonds: Int,
    onLevelClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onShopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Level Circle Badge (LVL 12)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .testTag("top_level_badge")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 32.dp),
                    onClick = onLevelClick
                )
        ) {
            // Blue circular progress background ring
            Canvas(modifier = Modifier.size(64.dp)) {
                val strokeWidth = 5.dp.toPx()
                // Outer glow shadow
                drawCircle(
                    color = Color(0x33000000),
                    radius = size.minDimension / 2
                )
                // Background track
                drawArc(
                    color = Color(0x551E88E5),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                // Active level progress arc
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(0xFF42A5F5),
                            Color(0xFF00E5FF),
                            Color(0xFF1E88E5)
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = levelProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Inner gloss button surface
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF64B5F6),
                                Color(0xFF1E88E5),
                                Color(0xFF1565C0)
                            )
                        )
                    )
                    .border(2.dp, Color(0xFFBBDEFB), CircleShape)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "LVL",
                        color = Color(0xFFE3F2FD),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 11.sp
                    )
                    Text(
                        text = "$level",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // 2. Currency Capsules (Coins & Diamonds)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gold Star Coins Capsule
            CurrencyCapsule(
                amount = String.format("%,d", coins),
                iconEmoji = "⭐",
                isGold = true,
                testTag = "currency_coins"
            )

            // Blue Diamond / Stars Capsule
            CurrencyCapsule(
                amount = "$diamonds",
                iconEmoji = "💎",
                isGold = false,
                testTag = "currency_diamonds"
            )
        }

        // 3. Settings & Shop Circular Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Settings Gear (Sky Blue)
            GlossyIconButton(
                bgColor = Color(0xFF42A5F5),
                borderColor = Color(0xFFBBDEFB),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                testTag = "settings_button",
                onClick = onSettingsClick
            )

            // Shopping Cart (Coral Red)
            GlossyIconButton(
                bgColor = Color(0xFFEF5350),
                borderColor = Color(0xFFFFCDD2),
                icon = {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Toko",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                },
                testTag = "shop_button",
                onClick = onShopClick
            )
        }
    }
}

/**
 * Currency Capsule (Coins with gold star coin / Diamonds with blue star)
 */
@Composable
private fun CurrencyCapsule(
    amount: String,
    iconEmoji: String,
    isGold: Boolean,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val capsuleBg = Color(0xB51B2838)
    val capsuleBorder = if (isGold) Color(0xFFFFD54F) else Color(0xFF81D4FA)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .testTag(testTag)
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(capsuleBg)
            .border(1.5.dp, capsuleBorder.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
            .padding(start = 10.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
    ) {
        Text(
            text = amount,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(end = 6.dp)
        )

        // Glossy Badge for currency icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    if (isGold) {
                        Brush.verticalGradient(listOf(Color(0xFFFFEE58), Color(0xFFF57F17)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFF4FC3F7), Color(0xFF0277BD)))
                    }
                )
                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
        ) {
            Text(
                text = iconEmoji,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Circular Glossy Button with 3D bevel border & soft glow
 */
@Composable
fun GlossyIconButton(
    bgColor: Color,
    borderColor: Color,
    icon: @Composable () -> Unit,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 44
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .testTag(testTag)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        bgColor.copy(alpha = 0.95f),
                        bgColor,
                        bgColor.copy(alpha = 0.85f)
                    )
                )
            )
            .border(2.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = (size / 2).dp),
                onClick = onClick
            )
    ) {
        icon()
    }
}

/**
 * Side Floating Action Circles (Left and Right sides of screen)
 */
@Composable
fun TalkingTomSideButtons(
    isMimicModeActive: Boolean,
    onToggleMimic: () -> Unit,
    onOpenVoiceCall: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onOpenTabletGames: () -> Unit,
    onOpenRetroConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    Box(modifier = modifier.fillMaxWidth()) {
        // Left Column (Speech Mic, Live Conversation Mic, Wardrobe)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Pink Speech Mic Button (Tiru Suara)
            GlossyCircleActionButton(
                ringColor = Color(0xFFEC4899),
                centerEmoji = "🎙️",
                testTag = "side_mimic_mic_button",
                modifier = if (isMimicModeActive) Modifier.scale(micPulseScale) else Modifier,
                onClick = onToggleMimic
            )

            // 2. Cyan Live Call Mic Button (Ngobrol Santai)
            GlossyCircleActionButton(
                ringColor = Color(0xFF00B4D8),
                centerEmoji = "🎤",
                testTag = "side_conversation_mic_button",
                onClick = onOpenVoiceCall
            )

            // 3. Purple Wardrobe Button (Lemari Baju)
            GlossyCircleActionButton(
                ringColor = Color(0xFF8B5CF6),
                centerEmoji = "🚪",
                testTag = "side_wardrobe_button",
                onClick = onOpenWardrobe
            )
        }

        // Right Column (Tablet & Retro Gamepad)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Tablet Games Button
            GlossyCircleActionButton(
                ringColor = Color(0xFFA855F7),
                centerEmoji = "📱",
                testTag = "side_tablet_games_button",
                onClick = onOpenTabletGames
            )

            // 2. Retro Gamepad Button
            GlossyCircleActionButton(
                ringColor = Color(0xFF06B6D4),
                centerEmoji = "🎮",
                testTag = "side_retro_console_button",
                onClick = onOpenRetroConsole
            )
        }
    }
}

/**
 * High-definition glossy circular button for side actions
 */
@Composable
fun GlossyCircleActionButton(
    ringColor: Color,
    centerEmoji: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(54.dp)
            .testTag(testTag)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        Color(0xFFFFFFFF),
                        ringColor.copy(alpha = 0.35f),
                        ringColor
                    )
                )
            )
            .border(3.dp, ringColor.copy(alpha = 0.9f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 27.dp),
                onClick = onClick
            )
    ) {
        Text(
            text = centerEmoji,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Bottom Need Bar with the 4 iconic circular status gauges:
 * 1. Makan (Spoon & fork, green ring)
 * 2. Toilet (Toilet seat, cyan ring)
 * 3. Tidur (Moon & stars, yellow/gold ring)
 * 4. Bermain/Hiburan (Bouncy ball, green ring)
 */
@Composable
fun TalkingTomBottomNeedBar(
    hunger: Float,
    toilet: Float,
    energy: Float,
    funLevel: Float,
    onMakanClick: () -> Unit,
    onToiletClick: () -> Unit,
    onTidurClick: () -> Unit,
    onBermainClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Makan
        TalkingTomNeedGauge(
            label = "Makan",
            percentage = hunger,
            emoji = "🍴",
            ringColor = Color(0xFF22C55E),
            testTag = "need_gauge_makan",
            onClick = onMakanClick
        )

        // 2. Toilet
        TalkingTomNeedGauge(
            label = "Toilet",
            percentage = toilet,
            emoji = "🚽",
            ringColor = Color(0xFF00B4D8),
            testTag = "need_gauge_toilet",
            onClick = onToiletClick
        )

        // 3. Tidur
        TalkingTomNeedGauge(
            label = "Tidur",
            percentage = energy,
            emoji = "🌙",
            ringColor = Color(0xFFFFD166),
            testTag = "need_gauge_tidur",
            onClick = onTidurClick
        )

        // 4. Bermain/Hiburan
        TalkingTomNeedGauge(
            label = "Bermain/Hiburan",
            percentage = funLevel,
            emoji = "🏐",
            ringColor = Color(0xFF10B981),
            testTag = "need_gauge_bermain",
            onClick = onBermainClick
        )
    }
}

/**
 * Single Need Circular Gauge Item
 */
@Composable
fun TalkingTomNeedGauge(
    label: String,
    percentage: Float,
    emoji: String,
    ringColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = false, radius = 38.dp),
            onClick = onClick
        )
    ) {
        // Circular Gauge
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(68.dp)
                .testTag(testTag)
        ) {
            // Background circular progress arc
            Canvas(modifier = Modifier.size(68.dp)) {
                val strokeWidth = 5.5.dp.toPx()
                // Outer ring shadow
                drawCircle(
                    color = Color(0x33000000),
                    radius = size.minDimension / 2
                )
                // Dark background track
                drawArc(
                    color = Color(0x44000000),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                // Colored progress arc
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = percentage.coerceIn(0f, 1f) * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Inner gloss button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.95f),
                                Color(0xFFF1F5F9),
                                Color(0xFFCBD5E1)
                            )
                        )
                    )
                    .border(2.5.dp, ringColor, CircleShape)
            ) {
                Text(
                    text = emoji,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Label with shadow for high legibility
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black,
                    offset = Offset(1.5f, 1.5f),
                    blurRadius = 3f
                )
            )
        )
    }
}

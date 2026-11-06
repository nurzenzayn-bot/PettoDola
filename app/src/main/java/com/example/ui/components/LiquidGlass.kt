package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Standard Liquid Glass frosted styling constants.
 */
object LiquidGlassDefaults {
    val GlassCornerRadius = 24.dp
    val PillCornerRadius = 100.dp

    // Frosted glass layers
    val GlassFillDark = Color(0x40131B2E)
    val GlassFillLight = Color(0x35FFFFFF)
    val GlassFillAccent = Color(0x356366F1)

    // Specular highlight gradient borders
    val SpecularBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0x80FFFFFF),
            Color(0x20FFFFFF),
            Color(0x05FFFFFF),
            Color(0x45FFFFFF)
        )
    )

    val SpecularActiveBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF818CF8),
            Color(0x40A5B4FC),
            Color(0xFFC084FC)
        )
    )
}

/**
 * Liquid Glass Surface container for floating panels, dialogs, and overlays.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LiquidGlassDefaults.GlassCornerRadius),
    backgroundColor: Color = LiquidGlassDefaults.GlassFillDark,
    borderColor: Brush = LiquidGlassDefaults.SpecularBorderBrush,
    borderWidth: Dp = 1.2.dp,
    elevation: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, spotColor = Color(0x33000000))
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(borderWidth, borderColor), shape),
        content = content
    )
}

/**
 * Liquid Glass Card for list items, character selector cards, or speech bubble wrappers.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color(0x30151C30),
    borderBrush: Brush = LiquidGlassDefaults.SpecularBorderBrush,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .shadow(6.dp, shape = shape, spotColor = Color(0x2A000000))
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .then(clickModifier),
        content = content
    )
}

/**
 * Liquid Glass Pill Button with responsive press bounce and specular gradient edge.
 */
@Composable
fun LiquidGlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emoji: String? = null,
    isActive: Boolean = false,
    accentColor: Color = Color(0xFF6366F1),
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = tween(120),
        label = "pillScale"
    )

    val shape = RoundedCornerShape(LiquidGlassDefaults.PillCornerRadius)
    val fillBrush = if (isActive) {
        Brush.horizontalGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.55f),
                accentColor.copy(alpha = 0.35f)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0x38FFFFFF),
                Color(0x18FFFFFF)
            )
        )
    }

    val borderBrush = if (isActive) {
        LiquidGlassDefaults.SpecularActiveBorderBrush
    } else {
        LiquidGlassDefaults.SpecularBorderBrush
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .shadow(4.dp, shape, spotColor = if (isActive) accentColor.copy(alpha = 0.4f) else Color(0x22000000))
            .clip(shape)
            .background(fillBrush)
            .border(BorderStroke(if (isActive) 1.5.dp else 1.dp, borderBrush), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.defaultMinSize(minHeight = 24.dp)
        ) {
            if (emoji != null) {
                Text(
                    text = emoji,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) Color.White else Color(0xFFE2E8F0),
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 6.dp)
                )
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isActive) Color.White else Color(0xFFF1F5F9)
            )
        }
    }
}

/**
 * Liquid Glass circular action button for mic, send, close, and controls.
 * Ensures minimum 48.dp touch target.
 */
@Composable
fun LiquidGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isActive: Boolean = false,
    activeColor: Color = Color(0xFF6366F1),
    contentDescription: String? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = tween(100),
        label = "iconButtonScale"
    )

    val fill = if (isActive) activeColor.copy(alpha = 0.65f) else Color(0x351E293B)
    val borderBrush = if (isActive) {
        Brush.linearGradient(listOf(Color.White, activeColor))
    } else {
        LiquidGlassDefaults.SpecularBorderBrush
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(6.dp, CircleShape, spotColor = if (isActive) activeColor.copy(alpha = 0.5f) else Color(0x22000000))
            .clip(CircleShape)
            .background(fill)
            .border(BorderStroke(1.2.dp, borderBrush), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        content()
    }
}

/**
 * Liquid Glass status badge.
 */
@Composable
fun LiquidGlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    color: Color = Color(0xFF818CF8)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.22f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        if (icon != null) {
            Text(text = icon, fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
        }
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

package com.schedule.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * iOS-style pressable modifier simulating SwiftUI's PressableButtonStyle:
 * - Subtle opacity dip on press (0.6f)
 * - Slight spring scale compression (0.98f)
 * - Clean tactile touch with zero harsh rectangular ripples
 */
fun Modifier.iosPressable(
    enabled: Boolean = true,
    pressedAlpha: Float = 0.6f,
    pressedScale: Float = 0.98f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedAlpha else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "iosPressableAlpha"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "iosPressableScale"
    )

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            alpha = animatedAlpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

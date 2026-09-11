package com.schedule.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.ui.theme.AppTheme

/**
 * Authentic iOS-styled UISegmentedControl replica:
 * - Fluid capsule track with native iOS background tones
 * - Spring-animated sliding pill thumb with physical elevation shadow
 * - Subtle vertical segment separators
 * - Clean typography transitions with zero Material outlines
 */
@Composable
fun <T> AppSegmentedPicker(
    label: String = "",
    selection: T,
    onSelectionChange: (T) -> Unit,
    options: List<T>,
    title: (T) -> String,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return

    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current
    val selectedIndex = options.indexOf(selection).coerceAtLeast(0)

    val trackColor = if (isDark) Color(0x38767680) else Color(0x18767680)
    val thumbColor = if (isDark) AppTheme.colors.selectedFill else Color.White
    val dividerColor = if (isDark) Color(0x28FFFFFF) else Color(0x24000000)

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iosSegmentThumb"
    )

    var rowSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(trackColor)
            .padding(2.5.dp)
            .onSizeChanged { rowSize = it }
    ) {
        val segmentCount = options.size
        val availableWidthDp = with(density) {
            if (rowSize.width > 0) (rowSize.width).toDp() - 5.dp else 0.dp
        }
        val segmentWidthDp = if (segmentCount > 0 && availableWidthDp > 0.dp) availableWidthDp / segmentCount else 0.dp

        // Sliding Thumb Pill
        if (segmentWidthDp > 0.dp) {
            Box(
                modifier = Modifier
                    .offset(x = segmentWidthDp * animatedIndex)
                    .width(segmentWidthDp)
                    .fillMaxHeight()
                    .shadow(
                        elevation = if (isDark) 1.dp else 2.dp,
                        shape = RoundedCornerShape(7.5.dp),
                        spotColor = Color(0x33000000),
                        ambientColor = Color(0x1A000000)
                    )
                    .background(color = thumbColor, shape = RoundedCornerShape(7.5.dp))
            )
        }

        // Segments Row
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                val isNextSelected = index + 1 == selectedIndex
                val showDivider = index < options.size - 1 && !isSelected && !isNextSelected

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onSelectionChange(option)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title(option),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isSelected) {
                            AppTheme.colors.accent
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (showDivider) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(1.dp)
                                .height(15.dp)
                                .background(dividerColor)
                        )
                    }
                }
            }
        }
    }
}

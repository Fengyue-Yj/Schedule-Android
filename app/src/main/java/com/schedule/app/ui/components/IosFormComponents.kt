package com.schedule.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.app.ui.theme.AppTheme

/**
 * Native iOS Modal Bottom Sheet wrapper with:
 * - Rounded top corners (20.dp)
 * - iOS pill drag indicator
 * - Inset grouped background
 * - Dismiss on swipe down or outside tap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = AppTheme.colors.background,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(width = 36.dp, height = 5.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                ) {}
            }
        },
        scrimColor = Color.Black.copy(alpha = 0.45f),
        windowInsets = WindowInsets(0),
        modifier = modifier,
        content = content
    )
}

/**
 * iOS 3-part sheet navigation header:
 * - Left: "取消" / "关闭" (accent text button)
 * - Center: SemiBold 17sp Title
 * - Right: Action (e.g. "保存" / "完成" / "编辑", bold accent)
 */
@Composable
fun IosSheetHeader(
    title: String,
    leftActionText: String = "取消",
    onLeftAction: () -> Unit,
    rightActionText: String? = null,
    onRightAction: (() -> Unit)? = null,
    rightActionEnabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left action
            Text(
                text = leftActionText,
                color = AppTheme.colors.accent,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .iosPressable(onClick = onLeftAction)
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            )

            // Center Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Right action
            if (rightActionText != null && onRightAction != null) {
                Text(
                    text = rightActionText,
                    color = if (rightActionEnabled) AppTheme.colors.accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .then(if (rightActionEnabled) Modifier.iosPressable(onClick = onRightAction) else Modifier)
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = AppTheme.colors.border.copy(alpha = 0.5f)
        )
    }
}

/**
 * iOS Inset Grouped Section:
 * Rounded card container with white/dark surface, subtle 0.5dp border,
 * optional section header text above the card.
 */
@Composable
fun IosFormSection(
    modifier: Modifier = Modifier,
    headerText: String? = null,
    footerText: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!headerText.isNullOrBlank()) {
            Text(
                text = headerText.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surface,
            border = androidx.compose.foundation.BorderStroke(
                width = 0.5.dp,
                color = AppTheme.colors.border.copy(alpha = 0.4f)
            ),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }

        if (!footerText.isNullOrBlank()) {
            Text(
                text = footerText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 16.dp)
            )
        }
    }
}

/**
 * Inset Divider between rows in an iOS Form Section.
 * Indented 16.dp on the start by default.
 */
@Composable
fun IosFormDivider(modifier: Modifier = Modifier, startIndent: Boolean = true) {
    HorizontalDivider(
        modifier = modifier.then(if (startIndent) Modifier.padding(start = 16.dp) else Modifier),
        thickness = 0.5.dp,
        color = AppTheme.colors.border.copy(alpha = 0.35f)
    )
}

/**
 * iOS Form text field row:
 * Clean borderless inline text row with label on left and input on right.
 */
@Composable
fun IosFormTextFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    minLines: Int = 1,
    singleLine: Boolean = minLines == 1,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (minLines > 1) 12.dp else 11.dp),
        verticalAlignment = if (minLines > 1) Alignment.Top else Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(84.dp)
        )
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(AppTheme.colors.accent),
                singleLine = singleLine,
                minLines = minLines,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * iOS Form Clickable Row:
 * Used for Date/Time picker, Course selector, links, etc.
 * Has label on left, value + chevron arrow on right.
 */
@Composable
fun IosFormRow(
    label: String,
    value: String? = null,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    valueColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    leadingIcon: (@Composable () -> Unit)? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.iosPressable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            leadingIcon?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = labelColor
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = valueColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * iOS Switch Row:
 * Used for toggling settings/completion.
 */
@Composable
fun IosFormSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppTheme.colors.accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0x33787880),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

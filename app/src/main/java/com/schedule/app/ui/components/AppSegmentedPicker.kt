package com.schedule.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.schedule.app.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppSegmentedPicker(
    label: String,
    selection: T,
    onSelectionChange: (T) -> Unit,
    options: List<T>,
    title: (T) -> String,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.height(40.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selection
            SegmentedButton(
                selected = isSelected,
                onClick = { onSelectionChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = AppTheme.colors.selectedFill,
                    activeContentColor = AppTheme.colors.accent,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = title(option),
                    style = AppTheme.typography.labelLarge
                )
            }
        }
    }
}

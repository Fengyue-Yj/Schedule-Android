package com.schedule.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PeriodRangePicker(
    startPeriod: Int,
    endPeriod: Int,
    onRangeChanged: (start: Int, end: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val standardPresets = remember {
        listOf(
            Pair(1, 2),
            Pair(3, 4),
            Pair(5, 6),
            Pair(7, 8),
            Pair(7, 9),
            Pair(10, 11),
            Pair(10, 12)
        )
    }

    val span = maxOf(1, endPeriod - startPeriod + 1)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "上课节次",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            ) {
                Text(
                    text = if (span == 1) "单节课 (第 $startPeriod 节)" else "$span 节连堂 (第 $startPeriod-$endPeriod 节)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 1. Preset Chips (including custom chip if active)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val isStandardPreset = standardPresets.any { it.first == startPeriod && it.second == endPeriod }
            if (!isStandardPreset) {
                FilterChip(
                    selected = true,
                    onClick = { },
                    label = { Text("第 $startPeriod-$endPeriod 节 (自选)") }
                )
            }

            standardPresets.forEach { (start, end) ->
                val isSelected = startPeriod == start && endPeriod == end
                val label = when {
                    start == 7 && end == 9 -> "7-9 节 (3节)"
                    start == 10 && end == 12 -> "10-12 节 (3节)"
                    else -> "$start-$end 节"
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onRangeChanged(start, end) },
                    label = { Text(label) }
                )
            }
        }

        // 2. Custom Exact Selection Bar: Pick any period from 1 to 12
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "自由自选:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("第", style = MaterialTheme.typography.bodySmall)

                    // Start Period Dropdown
                    var startMenuOpen by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { startMenuOpen = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("$startPeriod ▾", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        DropdownMenu(
                            expanded = startMenuOpen,
                            onDismissRequest = { startMenuOpen = false }
                        ) {
                            (1..12).forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("第 $p 节") },
                                    onClick = {
                                        val newEnd = if (endPeriod < p) p else endPeriod
                                        onRangeChanged(p, newEnd)
                                        startMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Text("至", style = MaterialTheme.typography.bodySmall)

                    // End Period Dropdown
                    var endMenuOpen by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { endMenuOpen = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("$endPeriod ▾", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        DropdownMenu(
                            expanded = endMenuOpen,
                            onDismissRequest = { endMenuOpen = false }
                        ) {
                            (startPeriod..12).forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("第 $p 节") },
                                    onClick = {
                                        onRangeChanged(startPeriod, p)
                                        endMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Text("节", style = MaterialTheme.typography.bodySmall)
                }

                // Quick - / + span stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (endPeriod > startPeriod) {
                                onRangeChanged(startPeriod, endPeriod - 1)
                            }
                        },
                        enabled = endPeriod > startPeriod,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(width = 28.dp, height = 28.dp)
                    ) {
                        Text("-", fontSize = 14.sp)
                    }
                    FilledTonalButton(
                        onClick = {
                            if (endPeriod < 12) {
                                onRangeChanged(startPeriod, endPeriod + 1)
                            }
                        },
                        enabled = endPeriod < 12,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(width = 28.dp, height = 28.dp)
                    ) {
                        Text("+", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

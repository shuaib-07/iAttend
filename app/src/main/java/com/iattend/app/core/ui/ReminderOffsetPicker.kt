package com.iattend.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iattend.app.core.notifications.ReminderOffset

/**
 * Multi-select reminder offset picker. Shows only the currently selected offsets as chips (the
 * type's default is pre-populated by the caller's ViewModel - see Assessment.reminderOffsets /
 * Settings.classReminderDefaultOffsets/examReminderDefaultOffsets). Tapping a chip arms a trailing
 * remove (x) button that morphs in; tapping elsewhere - another chip, the "Add" chip, or empty
 * space - disarms it. More offsets are added via a duration-wheel dialog rather than a wall of
 * preset chips.
 */
@Composable
fun ReminderOffsetPicker(
    selectedMinutes: List<Int>,
    onChange: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    var armedOffset by remember { mutableStateOf<Int?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { armedOffset = null },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "How long before the scheduled time to notify you",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            selectedMinutes.sorted().forEach { minutes ->
                ReminderChip(
                    label = ReminderOffset.label(minutes),
                    armed = armedOffset == minutes,
                    onTap = { armedOffset = if (armedOffset == minutes) null else minutes },
                    onRemove = {
                        onChange(selectedMinutes - minutes)
                        armedOffset = null
                    }
                )
            }
            AssistChip(
                onClick = { armedOffset = null; showAddDialog = true },
                label = { Text("Add") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize)) }
            )
        }
        if (selectedMinutes.isEmpty()) {
            Text(
                "No reminders selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { minutes ->
                if (minutes !in selectedMinutes) onChange(selectedMinutes + minutes)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ReminderChip(label: String, armed: Boolean, onTap: () -> Unit, onRemove: () -> Unit) {
    FilterChip(
        selected = true,
        onClick = onTap,
        label = { Text(label) },
        trailingIcon = {
            AnimatedVisibility(
                visible = armed,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove reminder", modifier = Modifier.size(14.dp))
                }
            }
        }
    )
}

private enum class DurationUnit(val label: String, val minutesPerUnit: Int, val range: IntRange) {
    MINUTES("min", 1, 1..59), HOURS("hr", 60, 1..23), DAYS("day", 1440, 1..30)
}

@Composable
private fun AddReminderDialog(onDismiss: () -> Unit, onAdd: (Int) -> Unit) {
    var unit by remember { mutableStateOf(DurationUnit.MINUTES) }
    var amount by remember(unit) { mutableStateOf(unit.range.first) }

    SpringAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add reminder") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberWheel(range = unit.range, value = amount, onValueChange = { amount = it }, modifier = Modifier.width(64.dp))
                SingleChoiceSegmentedButtonRow {
                    DurationUnit.entries.forEachIndexed { index, u ->
                        SegmentedButton(
                            selected = unit == u,
                            onClick = { unit = u },
                            shape = SegmentedButtonDefaults.itemShape(index, DurationUnit.entries.size)
                        ) { Text(u.label) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(amount * unit.minutesPerUnit) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Scrollable number wheel (iOS-picker style) built on [VerticalPager]'s built-in snapping. */
@Composable
private fun NumberWheel(range: IntRange, value: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(initialPage = (value - range.first).coerceIn(0, range.count() - 1)) { range.count() }
    LaunchedEffect(pagerState.settledPage) { onValueChange(range.first + pagerState.settledPage) }

    VerticalPager(
        state = pagerState,
        modifier = modifier.height(120.dp),
        pageSize = PageSize.Fixed(40.dp),
        contentPadding = PaddingValues(vertical = 40.dp)
    ) { page ->
        val current = page == pagerState.currentPage
        Box(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${range.first + page}",
                style = if (current) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                color = if (current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

package com.iattend.app.core.ui.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.iattend.app.core.datastore.TimeFormat
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import java.time.LocalDate

private val RadiusXxl = 48.dp
private val RadiusXs = 4.dp
private val RadiusMd = 16.dp
private val RadiusSm = 8.dp
private val SpacingXl = 16.dp

/**
 * Exact Cashiro DatePicker visuals wrapped for iAttend.
 * Original: Reference_Repos/Cashiro/app/src/main/java/com/ritesh/cashiro/presentation/ui/components/DatePicker.kt
 * Changes: package, blurEffects removed (iAttend keeps haze only for capsule nav, not dialogs per spec #9).
 */
@Composable
fun CashiroDatePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    datePickerState: DatePickerState,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(
                            topStart = RadiusXxl,
                            topEnd = RadiusXs,
                            bottomStart = RadiusXxl,
                            bottomEnd = RadiusXs
                        ),
                        modifier = Modifier.padding(start = SpacingXl).weight(1f).fillMaxWidth()
                    ) {
                        Text(text = "Cancel", style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(
                            topStart = RadiusXs,
                            topEnd = RadiusXxl,
                            bottomStart = RadiusXs,
                            bottomEnd = RadiusXxl
                        ),
                        modifier = Modifier.padding(end = SpacingXl).weight(1f).fillMaxWidth()
                    ) {
                        Text(text = "OK", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
        dismissButton = {},
        colors = DatePickerDefaults.colors(containerColor = containerColor),
        modifier = Modifier.clip(RoundedCornerShape(RadiusMd)),
        shape = MaterialTheme.shapes.large,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadiusSm)
                .clip(RoundedCornerShape(RadiusMd))
                .background(color = MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface.copy(0.5f))
            )
        }
    }
}

/**
 * Exact Cashiro DateRangePicker visuals. Empty ranges allowed per spec #2.
 * Confirm enabled when at least one date selected (allows clearing).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashiroDateRangePickerDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onConfirm: (startDate: LocalDate?, endDate: LocalDate?) -> Unit,
    initialStartDate: LocalDate? = null,
    initialEndDate: LocalDate? = null,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    val initialStartMillis = initialStartDate?.toEpochDay()?.let { it * 86400000L }
    val initialEndMillis = initialEndDate?.toEpochDay()?.let { it * 86400000L }
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartMillis,
        initialSelectedEndDateMillis = initialEndMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(topStart = RadiusXxl, topEnd = RadiusXs, bottomStart = RadiusXxl, bottomEnd = RadiusXs),
                        modifier = Modifier.padding(start = SpacingXl).weight(1f).fillMaxWidth()
                    ) { Text(text = "Cancel", style = MaterialTheme.typography.titleMedium) }
                    Button(
                        onClick = {
                            val startMillis = dateRangePickerState.selectedStartDateMillis
                            val endMillis = dateRangePickerState.selectedEndDateMillis
                            val startDate = startMillis?.let { LocalDate.ofEpochDay(it / 86400000L) }
                            val endDate = endMillis?.let { LocalDate.ofEpochDay(it / 86400000L) }
                            // empty ranges allowed: pass nulls through; validate only when both present
                            if (startDate != null && endDate != null && startDate > endDate) return@Button
                            onConfirm(startDate, endDate)
                        },
                        // allow empty ranges: always enabled so user can clear, Cashiro required both but spec says allow empty
                        enabled = true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(topStart = RadiusXs, topEnd = RadiusXxl, bottomStart = RadiusXs, bottomEnd = RadiusXxl),
                        modifier = Modifier.padding(end = SpacingXl).weight(1f).fillMaxWidth()
                    ) { Text(text = "OK", style = MaterialTheme.typography.titleMedium) }
                }
            }
        },
        dismissButton = {},
        colors = DatePickerDefaults.colors(containerColor = containerColor),
        shape = RoundedCornerShape(RadiusMd),
        modifier = modifier.clip(RoundedCornerShape(RadiusMd))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RadiusSm)
                .clip(RoundedCornerShape(RadiusMd))
                .background(color = MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier,
                title = {
                    Text(
                        text = "Select Date Range",
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp)
                    )
                },
                headline = {
                    DateRangePickerDefaults.DateRangePickerHeadline(
                        selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis,
                        selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis,
                        displayMode = dateRangePickerState.displayMode,
                        dateFormatter = DatePickerDefaults.dateFormatter(),
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, bottom = 12.dp)
                    )
                },
                showModeToggle = true,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    headlineContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurface,
                    subheadContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    yearContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    currentYearContentColor = MaterialTheme.colorScheme.primary,
                    selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
                    selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                    dayContentColor = MaterialTheme.colorScheme.onSurface,
                    disabledDayContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledSelectedDayContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f),
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    disabledSelectedDayContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                    todayContentColor = MaterialTheme.colorScheme.primary,
                    todayDateBorderColor = MaterialTheme.colorScheme.primary,
                    dayInSelectionRangeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    dayInSelectionRangeContainerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}

/**
 * Exact Cashiro TimePicker visuals.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashiroTimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    timePickerState: TimePickerState,
    format: TimeFormat,
    onFormatChange: (TimeFormat) -> Unit,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select time") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SingleChoiceSegmentedButtonRow {
                    TimeFormat.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = format == option,
                            onClick = { onFormatChange(option) },
                            shape = SegmentedButtonDefaults.itemShape(index, TimeFormat.entries.size)
                        ) { Text(if (option == TimeFormat.TWELVE_HOUR) "12-hour" else "24-hour") }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.surface.copy(0.7f),
                            timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surface.copy(0.7f),
                        )
                    )
                }
            }
        },
        confirmButton = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface.copy(0.5f), contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(topStart = RadiusXxl, topEnd = RadiusXs, bottomStart = RadiusXxl, bottomEnd = RadiusXs),
                        modifier = Modifier.padding(start = SpacingXl).weight(1f).fillMaxWidth()
                    ) { Text(text = "Cancel", style = MaterialTheme.typography.titleMedium) }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        shape = RoundedCornerShape(topStart = RadiusXs, topEnd = RadiusXxl, bottomStart = RadiusXs, bottomEnd = RadiusXxl),
                        modifier = Modifier.padding(end = SpacingXl).weight(1f).fillMaxWidth()
                    ) { Text(text = "OK", style = MaterialTheme.typography.titleMedium) }
                }
            }
        },
        dismissButton = {},
        modifier = Modifier.clip(RoundedCornerShape(RadiusMd)),
        shape = MaterialTheme.shapes.large,
        containerColor = containerColor,
    )
}

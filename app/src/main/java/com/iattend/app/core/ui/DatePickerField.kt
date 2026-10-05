package com.iattend.app.core.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.iattend.app.core.datastore.TimeFormat
import com.iattend.app.core.ui.pickers.CashiroDatePickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val longDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy - EEEE")
val LocalTimeFormat = compositionLocalOf { TimeFormat.TWELVE_HOUR }

fun formatDateShort(date: LocalDate): String = date.format(shortDateFormat)

/** "20/08/2026 (20 Aug 2026 - Thursday)" - short form for scanning/sorting, long form in brackets for clarity. */
fun formatDateWithLong(date: LocalDate): String = "${date.format(shortDateFormat)} (${date.format(longDateFormat)})"

fun formatTime(time: LocalTime, format: TimeFormat = TimeFormat.TWELVE_HOUR): String =
    time.format(DateTimeFormatter.ofPattern(if (format == TimeFormat.TWENTY_FOUR_HOUR) "HH:mm" else "h:mm a", Locale.getDefault()))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    detailedLayout: Boolean = false,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, enabled) {
        interactionSource.interactions.collect { interaction ->
            if (enabled && interaction is PressInteraction.Release) showDialog = true
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (detailedLayout) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(if (enabled) "Target day" else "From class", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        OutlinedTextField(
            value = date?.let { if (detailedLayout) it.format(DateTimeFormatter.ofPattern("dd / MM / yyyy")) else formatDateWithLong(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = if (detailedLayout) null else { { Text(label) } },
            enabled = enabled,
            singleLine = true,
            textStyle = if (detailedLayout) MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyLarge,
            shape = if (detailedLayout) RoundedCornerShape(14.dp) else OutlinedTextFieldDefaults.shape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = if (detailedLayout) MaterialTheme.colorScheme.surfaceContainerLow else androidx.compose.ui.graphics.Color.Transparent,
                focusedContainerColor = if (detailedLayout) MaterialTheme.colorScheme.surfaceContainerLow else androidx.compose.ui.graphics.Color.Transparent
            ),
            trailingIcon = {
                IconButton(onClick = { showDialog = true }, enabled = enabled) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choose $label")
                }
            },
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth()
        )
        if (detailedLayout) {
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(date?.format(DateTimeFormatter.ofPattern("dd MMM yyyy — EEEE", Locale.getDefault())) ?: "Choose a date",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (enabled) {
                    AssistChip(modifier = Modifier.height(26.dp), onClick = { onDateChange(LocalDate.now()) },
                        label = { Text("Today", style = MaterialTheme.typography.labelSmall) })
                    AssistChip(modifier = Modifier.height(26.dp), onClick = { onDateChange((date ?: LocalDate.now()).plusDays(1)) },
                        label = { Text("+1d", style = MaterialTheme.typography.labelSmall) })
                    AssistChip(modifier = Modifier.height(26.dp), onClick = { onDateChange((date ?: LocalDate.now()).plusWeeks(1)) },
                        label = { Text("+1w", style = MaterialTheme.typography.labelSmall) })
                }
            }
        }
    }

    if (showDialog) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now())
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        CashiroDatePickerDialog(
            onDismiss = { showDialog = false },
            onConfirm = {
                state.selectedDateMillis?.let {
                    onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                showDialog = false
            },
            datePickerState = state
        )
    }
}

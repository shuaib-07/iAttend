package com.iattend.app.core.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.iattend.app.core.ui.pickers.CashiroDatePickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val longDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy - EEEE")
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

fun formatDateShort(date: LocalDate): String = date.format(shortDateFormat)

/** "20/08/2026 (20 Aug 2026 - Thursday)" - short form for scanning/sorting, long form in brackets for clarity. */
fun formatDateWithLong(date: LocalDate): String = "${date.format(shortDateFormat)} (${date.format(longDateFormat)})"

/** 12-hour clock everywhere in the app, no exceptions (design.md Forms & Inputs rule). */
fun formatTime(time: LocalTime): String = time.format(timeFormat)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) showDialog = true
        }
    }

    OutlinedTextField(
        value = date?.let(::formatDateWithLong) ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        interactionSource = interactionSource,
        modifier = modifier
    )

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

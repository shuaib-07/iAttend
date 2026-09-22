package com.iattend.app.core.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.iattend.app.core.ui.pickers.CashiroTimePickerDialog
import com.iattend.app.core.datastore.TimeFormat
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    label: String,
    time: LocalTime?,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var dialogFormat by remember { mutableStateOf(TimeFormat.TWELVE_HOUR) }
    var dialogHour by remember { mutableIntStateOf(9) }
    var dialogMinute by remember { mutableIntStateOf(0) }
    val savedFormat = LocalTimeFormat.current
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, savedFormat, time) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                dialogFormat = savedFormat
                dialogHour = time?.hour ?: 9
                dialogMinute = time?.minute ?: 0
                showDialog = true
            }
        }
    }

    OutlinedTextField(
        value = time?.let { formatTime(it, savedFormat) } ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        interactionSource = interactionSource,
        modifier = modifier
    )

    if (showDialog) {
        key(dialogFormat) {
            val state = rememberTimePickerState(initialHour = dialogHour, initialMinute = dialogMinute, is24Hour = dialogFormat == TimeFormat.TWENTY_FOUR_HOUR)
            CashiroTimePickerDialog(
                onDismiss = { showDialog = false },
                onConfirm = {
                    onTimeChange(LocalTime.of(state.hour, state.minute))
                    showDialog = false
                },
                timePickerState = state,
                format = dialogFormat,
                onFormatChange = {
                    dialogHour = state.hour
                    dialogMinute = state.minute
                    dialogFormat = it
                }
            )
        }
    }
}

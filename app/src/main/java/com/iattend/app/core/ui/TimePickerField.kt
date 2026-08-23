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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.iattend.app.core.ui.pickers.CashiroTimePickerDialog
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
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) showDialog = true
        }
    }

    OutlinedTextField(
        value = time?.let(::formatTime) ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        interactionSource = interactionSource,
        modifier = modifier
    )

    if (showDialog) {
        val initial = time ?: LocalTime.of(9, 0)
        // Forced 12h per spec #3
        val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
        CashiroTimePickerDialog(
            onDismiss = { showDialog = false },
            onConfirm = {
                onTimeChange(LocalTime.of(state.hour, state.minute))
                showDialog = false
            },
            timePickerState = state
        )
    }
}

package com.iattend.app.core.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.iattend.app.core.ui.pickers.CashiroDateRangePickerDialog
import java.time.LocalDate

@Composable
fun DateRangePickerField(
    label: String,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onRangeChange: (LocalDate?, LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) showDialog = true
        }
    }

    val displayValue = when {
        startDate != null && endDate != null -> "${formatDateShort(startDate)} - ${formatDateShort(endDate)}"
        startDate != null -> "${formatDateShort(startDate)} - …"
        endDate != null -> "… - ${formatDateShort(endDate)}"
        else -> ""
    }

    OutlinedTextField(
        value = displayValue,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        interactionSource = interactionSource,
        modifier = modifier
    )

    if (showDialog) {
        CashiroDateRangePickerDialog(
            onDismiss = { showDialog = false },
            onConfirm = { s, e ->
                onRangeChange(s, e)
                showDialog = false
            },
            initialStartDate = startDate,
            initialEndDate = endDate
        )
    }
}

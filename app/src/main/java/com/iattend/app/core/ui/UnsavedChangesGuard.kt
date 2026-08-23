package com.iattend.app.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Wires both the system back gesture and a screen's own back/close icon to a "discard changes?"
 * confirmation when [isDirty] is true, using [SpringAlertDialog] (animated entrance, platform
 * dialog scrim - already dims the background ~60%, no separate blur needed). Returns the guarded
 * callback to use in place of a raw onBack/onDismiss.
 */
@Composable
fun rememberUnsavedChangesGuard(isDirty: Boolean, onConfirmedBack: () -> Unit): () -> Unit {
    var showDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = isDirty) { showDialog = true }

    if (showDialog) {
        SpringAlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes that will be lost.") },
            confirmButton = {
                TextButton(onClick = { showDialog = false; onConfirmedBack() }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Keep editing") }
            }
        )
    }

    return { if (isDirty) showDialog = true else onConfirmedBack() }
}

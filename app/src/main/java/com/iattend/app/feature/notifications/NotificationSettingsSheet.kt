package com.iattend.app.feature.notifications

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.feature.settings.ReminderDefaultsFields
import com.iattend.app.feature.settings.SettingsViewModel

/** Bottom-sheet content (see ModalSheet), opened from the bell icon in [com.iattend.app.core.ui.BrandHeader]. */
@Composable
fun NotificationSettingsSheetContent(onDismiss: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.setRemindersEnabled(true)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Notifications", style = MaterialTheme.typography.titleLarge)
            SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = onDismiss)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Class reminders", modifier = Modifier.weight(1f))
            Switch(
                checked = remindersEnabled,
                onCheckedChange = { checked ->
                    if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setRemindersEnabled(checked)
                    }
                }
            )
        }
        if (remindersEnabled && !viewModel.canScheduleExactAlarms()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Exact alarm permission needed for reminders to fire on time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                    )
                }) { Text("Grant") }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        Text("Reminder timing", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
        ReminderDefaultsFields(viewModel)
    }
}

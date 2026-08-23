package com.iattend.app.feature.holidays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.Holiday
import com.iattend.app.core.ui.DatePickerField
import com.iattend.app.core.ui.DateRangePickerField
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.formatDateShort
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.core.ui.rememberUnsavedChangesGuard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayListScreen(
    onBack: () -> Unit,
    viewModel: HolidayListViewModel = hiltViewModel()
) {
    val holidays by viewModel.holidays.collectAsState()
    val form by viewModel.form.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()
    val guardedBack = rememberUnsavedChangesGuard(isDirty = isDirty, onConfirmedBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Holidays") },
                navigationIcon = {
                    SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = guardedBack)
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Date Range?", style = MaterialTheme.typography.titleMedium)
                        Switch(checked = form.isRange, onCheckedChange = viewModel::onRangeToggle)
                    }

                    if (form.isRange) {
                        DateRangePickerField(
                            label = "Holiday range",
                            startDate = form.fromDate,
                            endDate = form.toDate,
                            onRangeChange = { s, e ->
                                if (s != null) viewModel.onFromDateChange(s)
                                if (e != null) viewModel.onToDateChange(e)
                                // empty ranges allowed: if null, keep existing
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                        // Keep individual From/To pickers as fallback for precise single-date correction
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DatePickerField("From", form.fromDate, viewModel::onFromDateChange, Modifier.weight(1f))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                            DatePickerField("To", form.toDate, viewModel::onToDateChange, Modifier.weight(1f))
                        }
                    } else {
                        DatePickerField("Date", form.date, viewModel::onDateChange, Modifier.fillMaxWidth().padding(top = 8.dp))
                    }

                    OutlinedTextField(
                        value = form.reason,
                        onValueChange = viewModel::onReasonChange,
                        label = { Text("Reason (e.g. Christmas Break)") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )

                    Button(onClick = hapticClick(viewModel::save), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Text(
                            when {
                                form.editingId != null -> "Update Holiday"
                                form.isRange -> "Add Holiday Range"
                                else -> "Add Single Holiday"
                            }
                        )
                    }
                    if (form.editingId != null) {
                        OutlinedButton(onClick = viewModel::cancelEdit, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text("Cancel edit")
                        }
                    }
                }
            }
            items(holidays, key = { it.id }) { holiday ->
                HolidayRow(
                    holiday = holiday,
                    onClick = { viewModel.startEdit(holiday) },
                    onDelete = { viewModel.delete(holiday) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun HolidayRow(holiday: Holiday, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.fillMaxHeight().width(4.dp).background(MaterialTheme.colorScheme.error)
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(holiday.label, style = MaterialTheme.typography.bodyLarge)
            val dateText = if (holiday.startDate == holiday.endDate) {
                "${formatDateShort(holiday.startDate)} - ${holiday.startDate.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())}"
            } else {
                "${formatDateShort(holiday.startDate)} – ${formatDateShort(holiday.endDate)}"
            }
            Text(dateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete holiday", tint = MaterialTheme.colorScheme.error)
        }
    }
}

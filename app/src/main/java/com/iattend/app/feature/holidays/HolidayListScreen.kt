package com.iattend.app.feature.holidays

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.Holiday
import com.iattend.app.core.data.db.RecurringHolidayMode
import com.iattend.app.core.data.db.RecurringHolidayRule
import com.iattend.app.core.domain.occurrence.OccurrenceGenerator
import com.iattend.app.core.ui.DatePickerField
import com.iattend.app.core.ui.DateRangePickerField
import com.iattend.app.core.ui.MorphingSaveButton
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.formatDateShort
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.core.ui.rememberUnsavedChangesGuard
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayListScreen(
    onBack: () -> Unit,
    viewModel: HolidayListViewModel = hiltViewModel()
) {
    val holidays by viewModel.holidays.collectAsState()
    val recurringRules by viewModel.recurringRules.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val displayedMonth by viewModel.displayedMonth.collectAsState()
    val holidayInfo by viewModel.selectedDayHolidayInfo.collectAsState()
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Calendar View for Holidays
            item {
                HolidayCalendarView(
                    displayedMonth = displayedMonth,
                    selectedDate = selectedDate,
                    holidays = holidays,
                    recurringRules = recurringRules,
                    onDateSelected = viewModel::selectDate,
                    onPreviousMonth = viewModel::previousMonth,
                    onNextMonth = viewModel::nextMonth
                )
            }

            // Selected Date Info / Action Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            formatDateShort(selectedDate),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        when (val info = holidayInfo) {
                            is DayHolidayInfo.CustomHoliday -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            info.holiday.label.ifBlank { "Holiday" },
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            if (info.isRange) "${formatDateShort(info.holiday.startDate)} - ${formatDateShort(info.holiday.endDate)}"
                                            else "Single day holiday",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { viewModel.startEdit(info.holiday) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                        IconButton(onClick = { viewModel.delete(info.holiday) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                            is DayHolidayInfo.Recurring -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EventRepeat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "Recurring Off-Day (${info.rule.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            DayHolidayInfo.None -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "No holiday on this date.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(onClick = { viewModel.prefillForSelectedDate() }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text(" Add holiday")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Holiday Form
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            if (form.editingId != null) "Edit Holiday" else "Add New Holiday",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Date Range?", style = MaterialTheme.typography.bodyLarge)
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
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DatePickerField("From", form.fromDate, viewModel::onFromDateChange, Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                DatePickerField("To", form.toDate, viewModel::onToDateChange, Modifier.weight(1f))
                            }
                        } else {
                            DatePickerField("Date", form.date, viewModel::onDateChange, Modifier.fillMaxWidth())
                        }

                        OutlinedTextField(
                            value = form.reason,
                            onValueChange = viewModel::onReasonChange,
                            label = { Text("Reason (e.g. Festival Break)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        MorphingSaveButton(
                            onAction = {
                                viewModel.save()
                            },
                            label = when {
                                form.editingId != null -> "Update Holiday"
                                form.isRange -> "Add Holiday Range"
                                else -> "Add Single Holiday"
                            },
                            pendingLabel = when {
                                form.editingId != null -> "Updating Holiday..."
                                form.isRange -> "Adding Holiday Range..."
                                else -> "Adding Single Holiday..."
                            },
                            successLabel = when {
                                form.editingId != null -> "Holiday Updated"
                                form.isRange -> "Holiday Range Added"
                                else -> "Single Holiday Added"
                            },
                            resetAfterMs = 1500L,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )

                        if (form.editingId != null) {
                            OutlinedButton(onClick = viewModel::cancelEdit, modifier = Modifier.fillMaxWidth()) {
                                Text("Cancel edit")
                            }
                        }
                    }
                }
            }

            // All Configured Holidays List
            if (holidays.isNotEmpty()) {
                item {
                    Text(
                        "All Custom Holidays",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 8.dp)
                    )
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
}

@Composable
private fun HolidayCalendarView(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    holidays: List<Holiday>,
    recurringRules: List<RecurringHolidayRule>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weekdays = listOf(
        java.time.DayOfWeek.SUNDAY, java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY,
        java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY,
        java.time.DayOfWeek.SATURDAY
    )

    val firstDate = displayedMonth.atDay(1)
    val gridStart = firstDate.minusDays((firstDate.dayOfWeek.value % 7).toLong())
    val weekCount = ((firstDate.dayOfWeek.value % 7) + displayedMonth.lengthOfMonth() + 6) / 7
    val dates = (0 until weekCount * 7).map { gridStart.plusDays(it.toLong()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Row {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
                }
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp)) {
            weekdays.forEach { day ->
                Text(
                    day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        dates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val customHoliday = holidays.firstOrNull { date in it.startDate..it.endDate }
                    val isRange = customHoliday != null && customHoliday.startDate != customHoliday.endDate
                    val isSingle = customHoliday != null && customHoliday.startDate == customHoliday.endDate

                    val rule = recurringRules.firstOrNull { it.dayOfWeek == date.dayOfWeek }
                    val isRecurring = rule != null && when (rule.mode) {
                        RecurringHolidayMode.NONE -> false
                        RecurringHolidayMode.ALWAYS -> true
                        RecurringHolidayMode.PATTERN -> {
                            val bit = 1 shl (OccurrenceGenerator.nthWeekdayOfMonth(date) - 1)
                            rule.weeksOfMonth and bit != 0
                        }
                    }

                    val inMonth = date.month == displayedMonth.month && date.year == displayedMonth.year
                    val selected = date == selectedDate
                    val today = date == LocalDate.now()
                    val shape = MaterialTheme.shapes.medium

                    val bgColor = when {
                        isRange -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
                        isSingle -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        isRecurring -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        selected -> MaterialTheme.colorScheme.primaryContainer
                        else -> Color.Transparent
                    }

                    val numberColor = when {
                        isSingle || isRange -> MaterialTheme.colorScheme.onErrorContainer
                        selected -> MaterialTheme.colorScheme.onPrimaryContainer
                        today -> MaterialTheme.colorScheme.primary
                        inMonth -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .height(44.dp)
                            .clip(shape)
                            .background(bgColor)
                            .then(
                                if (selected) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape)
                                else if (today) Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), shape)
                                else Modifier
                            )
                            .clickable { onDateSelected(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(date.dayOfMonth.toString(), color = numberColor, style = MaterialTheme.typography.bodyMedium)
                            if (isSingle || isRange) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(MaterialTheme.colorScheme.error, CircleShape)
                                )
                            } else if (isRecurring) {
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HolidayRow(holiday: Holiday, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val isRange = holiday.startDate != holiday.endDate
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    holiday.label.ifBlank { "Holiday" },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    if (isRange) "${formatDateShort(holiday.startDate)} – ${formatDateShort(holiday.endDate)}"
                    else formatDateShort(holiday.startDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

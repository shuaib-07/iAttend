package com.iattend.app.feature.holidays

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.RecurringHolidayMode
import com.iattend.app.core.data.db.RecurringHolidayRule
import com.iattend.app.core.ui.SquircleIconButton
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringHolidayScreen(
    onBack: () -> Unit,
    viewModel: RecurringHolidayViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsState()
    var expandedDays by remember { mutableStateOf(setOf<DayOfWeek>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Standard Holidays") },
                navigationIcon = {
                    SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = onBack)
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Text(
                    "Select the days that are standard holidays (e.g. Weekends). Classes on these days won't be counted in attendance totals.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            items(DayOfWeek.entries) { day ->
                DayRuleRow(
                    day = day,
                    rule = rules[day],
                    expanded = day in expandedDays,
                    onToggleExpanded = { expandedDays = if (day in expandedDays) expandedDays - day else expandedDays + day },
                    onToggleOff = { on -> viewModel.setMode(day, if (on) RecurringHolidayMode.ALWAYS else RecurringHolidayMode.NONE) },
                    onToggleWeek = { week -> viewModel.toggleWeek(day, week) }
                )
            }
        }
    }
}

@Composable
private fun DayRuleRow(
    day: DayOfWeek,
    rule: RecurringHolidayRule?,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleOff: (Boolean) -> Unit,
    onToggleWeek: (Int) -> Unit
) {
    val mode = rule?.mode ?: RecurringHolidayMode.NONE
    val active = mode != RecurringHolidayMode.NONE
    val accentColor = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = if (active) BorderStroke(1.dp, accentColor) else null,
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggleOff(!active) }.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(day.name.lowercase().replaceFirstChar { it.uppercase() }, color = accentColor)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onToggleExpanded) { Text(if (expanded) "Hide" else "Customize") }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(if (active) accentColor else MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (active) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(16.dp))
                    }
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    Text("Off on specific weeks instead of every week:", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val weeksOfMonth = rule?.weeksOfMonth ?: 0
                        (1..5).forEach { week ->
                            val selected = mode == RecurringHolidayMode.PATTERN && weeksOfMonth and (1 shl (week - 1)) != 0
                            FilterChip(selected = selected, onClick = { onToggleWeek(week) }, label = { Text(ordinal(week)) })
                        }
                    }
                }
            }
        }
    }
}

private fun ordinal(n: Int) = when (n) {
    1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${n}th"
}

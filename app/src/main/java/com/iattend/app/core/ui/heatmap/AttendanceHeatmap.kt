package com.iattend.app.core.ui.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import java.time.LocalDate

@Composable
fun AttendanceHeatmap(
    presentCountByDate: Map<LocalDate, Int>,
    modifier: Modifier = Modifier,
    filterStartDate: LocalDate? = null,
    filterEndDate: LocalDate? = null,
    title: String = "Attendance Activity",
    subtitle: String? = null
) {
    val heatmapWeeks = remember(presentCountByDate, filterStartDate, filterEndDate) {
        generateHeatmapData(
            activeDates = presentCountByDate,
            customStartDate = filterStartDate,
            customEndDate = filterEndDate
        )
    }

    val listState = rememberLazyListState()
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    // Instant dismissal counter: bumps on scroll or screen departure to reset popup without lag
    var instantDismissals by remember { mutableIntStateOf(0) }
    val dismissNow = {
        if (selectedDate != null) {
            instantDismissals++
            selectedDate = null
        }
    }

    // Scroll to the latest weeks (right-aligned, showing recent dates)
    LaunchedEffect(heatmapWeeks.size) {
        if (heatmapWeeks.isNotEmpty()) {
            listState.scrollToItem(heatmapWeeks.size - 1)
        }
    }

    // Dismiss tooltip when user scrolls horizontally
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collect { dismissNow() }
    }

    // Dismiss when screen leaves foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                dismissNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Auto-dismiss tooltip after 3 seconds of inactivity
    LaunchedEffect(selectedDate) {
        if (selectedDate != null) {
            delay(3_000)
            selectedDate = null
        }
    }

    val lastCardY = remember { floatArrayOf(Float.NaN) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                val y = coordinates.positionInWindow().y
                if (!lastCardY[0].isNaN() && y != lastCardY[0]) dismissNow()
                lastCardY[0] = y
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )

                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Intensity Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Less",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HeatmapCell(intensity = 0)
                    HeatmapCell(intensity = 1)
                    HeatmapCell(intensity = 3)
                    HeatmapCell(intensity = 5)
                    Text(
                        text = "More",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row {
                // Day of Week Indicators: M, T, W, T, F, S, S
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 20.dp) // Offset to align with cells under month labels
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                LazyRow(
                    state = listState,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(heatmapWeeks, key = { it.weekIndex }) { week ->
                        Column {
                            Box(modifier = Modifier.height(20.dp)) {
                                if (week.firstDayOfMonth != null) {
                                    Text(
                                        text = week.firstDayOfMonth,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        softWrap = false
                                    )
                                }
                            }

                            HeatmapWeekColumn(
                                days = week.days,
                                selectedDate = selectedDate,
                                instantDismissals = instantDismissals,
                                onDayClick = { day ->
                                    selectedDate = if (selectedDate == day.date) null else day.date
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

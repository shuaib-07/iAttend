package com.iattend.app.feature.bunk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.ui.theme.Motion
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BunkSimulatorBottomSheet(
    initialSubjectId: Long? = null,
    onDismiss: () -> Unit,
    viewModel: BunkSimulatorViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(initialSubjectId) {
        if (initialSubjectId != null) {
            viewModel.selectSubject(initialSubjectId)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Smart Bunk Simulator",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Simulate attendance & plan safe bunks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SquircleIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = onDismiss
                )
            }

            // Subject Selector Horizontal Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedSubjectId == null,
                    onClick = { viewModel.selectSubject(null) },
                    label = { Text("Overall Semester") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                state.availableSubjects.forEach { subj ->
                    FilterChip(
                        selected = state.selectedSubjectId == subj.id,
                        onClick = { viewModel.selectSubject(subj.id) },
                        label = { Text(subj.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Hero Attendance & Projected Gauge Card
            HeroProjectionCard(
                state = state,
                onReset = viewModel::resetSimulation
            )

            // Primary Tabs: What-If Simulator vs Day-by-Day Planner
            PrimaryTabRow(
                selectedTabIndex = state.selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = { Text("What-If Stepper", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = { Text("Day-by-Day Planner", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Tab Content
            if (state.selectedTab == 0) {
                WhatIfStepperTab(
                    state = state,
                    onSimulateMissChange = viewModel::setSimulateMiss,
                    onSimulateAttendChange = viewModel::setSimulateAttend,
                    onTargetPercentageChange = viewModel::setTargetPercentage
                )
            } else {
                DayByDayPlannerTab(
                    state = state,
                    onToggleDate = viewModel::togglePlannedBunkDate,
                    onClearPlanned = viewModel::clearPlannedBunks
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroProjectionCard(
    state: BunkSimulatorUiState,
    onReset: () -> Unit
) {
    val displayResult = if (state.selectedTab == 1 && state.plannedBunkDates.isNotEmpty()) {
        state.plannedBunkImpact ?: state.result
    } else state.result

    val diff = displayResult.projectedPercentage - displayResult.currentPercentage
    val hasDiff = kotlin.math.abs(diff) >= 0.05f

    val statusColor = when (displayResult.status) {
        BunkStatus.COMFORTABLY_SAFE -> MaterialTheme.colorScheme.primary
        BunkStatus.CAUTION_BORDERLINE -> Color(0xFFD97706)
        BunkStatus.DANGER_ZONE -> MaterialTheme.colorScheme.error
    }
    val statusBg = when (displayResult.status) {
        BunkStatus.COMFORTABLY_SAFE -> MaterialTheme.colorScheme.primaryContainer
        BunkStatus.CAUTION_BORDERLINE -> Color(0xFFFFFBEB)
        BunkStatus.DANGER_ZONE -> MaterialTheme.colorScheme.errorContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "PROJECTED ATTENDANCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", displayResult.projectedPercentage),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        if (hasDiff) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (diff >= 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (diff >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = if (diff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%+.1f%%", diff),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (diff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "Current: ${String.format(Locale.getDefault(), "%.1f%%", displayResult.currentPercentage)} (${state.attendedClasses}/${state.totalClassesSoFar} classes)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.simulateMissCount > 0 || state.simulateAttendCount > 0 || state.plannedBunkDates.isNotEmpty()) {
                    IconButton(
                        onClick = onReset,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Simulation",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Animated Linear Wavy Progress Indicator
            LinearWavyProgressIndicator(
                progress = { (displayResult.projectedPercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            // Safe Bunk Margin Badge (design.md §2.5)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val badgeIcon = when (displayResult.status) {
                        BunkStatus.COMFORTABLY_SAFE -> Icons.Default.CheckCircle
                        BunkStatus.CAUTION_BORDERLINE -> Icons.Default.WarningAmber
                        BunkStatus.DANGER_ZONE -> Icons.Default.ErrorOutline
                    }
                    Icon(badgeIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                    Text(
                        text = when (displayResult.status) {
                            BunkStatus.COMFORTABLY_SAFE -> "Comfortably safe! You can safely skip ${displayResult.safeSkipsNow} class${if (displayResult.safeSkipsNow == 1) "" else "es"} and remain above your ${displayResult.targetPercentage.toInt()}% goal."
                            BunkStatus.CAUTION_BORDERLINE -> if (displayResult.safeSkipsNow == 0) {
                                "Caution: Next absent drops you below your ${displayResult.targetPercentage.toInt()}% goal!"
                            } else {
                                "Caution: You can only skip ${displayResult.safeSkipsNow} more class before falling below target."
                            }
                            BunkStatus.DANGER_ZONE -> "Debarment danger! Must attend next ${displayResult.classesNeededForTarget} consecutive class${if (displayResult.classesNeededForTarget == 1) "" else "es"} without absence to recover."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun WhatIfStepperTab(
    state: BunkSimulatorUiState,
    onSimulateMissChange: (Int) -> Unit,
    onSimulateAttendChange: (Int) -> Unit,
    onTargetPercentageChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "What-if Simulator",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Stepper: If I miss...
        StepperRow(
            label = "If I miss next classes",
            count = state.simulateMissCount,
            accentColor = MaterialTheme.colorScheme.error,
            onCountChange = onSimulateMissChange
        )

        // Stepper: If I attend...
        StepperRow(
            label = "If I attend next classes",
            count = state.simulateAttendCount,
            accentColor = MaterialTheme.colorScheme.primary,
            onCountChange = onSimulateAttendChange
        )

        Spacer(Modifier.height(4.dp))

        // Target Percentage Adjuster
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attendance Goal Target",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.targetPercentage.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = state.targetPercentage,
                    onValueChange = onTargetPercentageChange,
                    valueRange = 50f..95f,
                    steps = 8
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(75f, 80f, 85f).forEach { preset ->
                        FilterChip(
                            selected = state.targetPercentage == preset,
                            onClick = { onTargetPercentageChange(preset) },
                            label = { Text("${preset.toInt()}% Goal") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    count: Int,
    accentColor: Color,
    onCountChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { onCountChange((count - 1).coerceAtLeast(0)) },
                    enabled = count > 0,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                }

                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (count > 0) accentColor else MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { onCountChange(count + 1) },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun DayByDayPlannerTab(
    state: BunkSimulatorUiState,
    onToggleDate: (LocalDate) -> Unit,
    onClearPlanned: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Smart Day Bunk Planner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Upcoming 14-day schedule with bridge analysis",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.plannedBunkDates.isNotEmpty()) {
                TextButton(onClick = onClearPlanned) {
                    Text("Clear Selection (${state.plannedBunkDates.size})")
                }
            }
        }

        if (state.upcomingOpportunities.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No upcoming classes found in the next 14 days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            state.upcomingOpportunities.forEach { opp ->
                val isSelected = opp.date in state.plannedBunkDates
                DayBunkCard(
                    opportunity = opp,
                    isSelected = isSelected,
                    onToggle = { onToggleDate(opp.date) }
                )
            }
        }
    }
}

@Composable
private fun DayBunkCard(
    opportunity: DayBunkOpportunity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val (badgeText, badgeBg, badgeColor, badgeIcon) = when (opportunity.recommendationType) {
        BunkRecommendationType.RECOMMENDED -> Quadruple(
            if (opportunity.createsLongWeekend) "Long Weekend Bridge" else "Recommended Bridge",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            Icons.Default.BeachAccess
        )
        BunkRecommendationType.DO_NOT_BUNK -> Quadruple(
            if (opportunity.hasAssessment) "Assessment Scheduled" else "Do Not Bunk",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error,
            Icons.Default.ErrorOutline
        )
        BunkRecommendationType.HIGH_IMPACT -> Quadruple(
            "Heavy Class Load (${opportunity.classCount} classes)",
            Color(0xFFFFFBEB),
            Color(0xFFD97706),
            Icons.Default.WarningAmber
        )
        BunkRecommendationType.OPTIONAL_SAFE -> Quadruple(
            "Safe to Bunk",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.CheckCircle
        )
    }

    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        border = if (isSelected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = opportunity.date.format(dayFormatter),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "${opportunity.classCount} class${if (opportunity.classCount > 1) "es" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(badgeIcon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(12.dp))
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }

                Text(
                    text = opportunity.recommendationReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

package com.iattend.app.feature.onboarding

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.ui.DatePickerField
import com.iattend.app.core.ui.TimePickerField
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.formatTime
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.ui.theme.Motion
import java.time.DayOfWeek
import java.time.LocalTime
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onFinishedBackdated: (Long) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.finishEvents.collect { event ->
            when (event) {
                is OnboardingFinishEvent.ToHome -> onFinished()
                is OnboardingFinishEvent.ToBackdatedFill -> onFinishedBackdated(event.versionId)
            }
        }
    }
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    if (!state.hasChosenStart) {
        StartChoiceScreen(
            onStartFresh = hapticClick(viewModel::chooseStartFresh),
            onImportBackup = viewModel::importBackup
        )
        return
    }

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            com.iattend.app.core.ui.OnboardingAmbientBackground(modifier = Modifier.fillMaxSize())
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Step ${state.step + 1} of $ONBOARDING_STEP_COUNT", style = MaterialTheme.typography.labelLarge)
            val animatedProgress by animateFloatAsState(
                targetValue = (state.step + 1f) / ONBOARDING_STEP_COUNT,
                animationSpec = Motion.gentle(),
                label = "onboardingProgress"
            )
            LinearWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )

            AnimatedContent(
                targetState = state.step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(animationSpec = Motion.mediumTween()) { w -> if (forward) w else -w } + fadeIn(Motion.mediumTween())) togetherWith
                        (slideOutHorizontally(animationSpec = Motion.mediumTween()) { w -> if (forward) -w else w } + fadeOut(Motion.mediumTween()))
                },
                label = "onboardingStep"
            ) { step ->
                when (step) {
                    0 -> FeaturesStep()
                    1 -> NotificationPermissionStep(onGranted = viewModel::onNotificationsEnabled)
                    2 -> ThemeStep()
                    3 -> NameStep(state.name, viewModel::onNameChange)
                    4 -> SubjectsStep(state.subjects, viewModel::addSubject, viewModel::removeSubject)
                    5 -> TimetableStep(state.subjects, state.slots, viewModel::addSlot, viewModel::removeSlot)
                    6 -> DatesStep(state, viewModel)
                    7 -> HolidaysStep(state.holidayDays, viewModel::toggleHolidayDay)
                    8 -> ThresholdStep(state.requiredPercentage, viewModel::onRequiredPercentageChange)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (state.step > 0) {
                    OutlinedButton(onClick = hapticClick(viewModel::previousStep)) { Text("Back") }
                } else {
                    Box {}
                }
                if (state.step < ONBOARDING_STEP_COUNT - 1) {
                    Button(onClick = hapticClick(viewModel::nextStep)) { Text("Next") }
                } else {
                    Button(onClick = hapticClick(viewModel::finish)) { Text("Finish setup") }
                }
            }
            }
        }
    }
}

@Composable
private fun StartChoiceScreen(onStartFresh: () -> Unit, onImportBackup: (android.net.Uri) -> Unit) {
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportBackup)
    }
    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        com.iattend.app.core.ui.OnboardingAmbientBackground(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            com.iattend.app.core.ui.PulsingLogo(logoRes = com.iattend.app.R.drawable.img_logo_icon)
            Text(
                "iAttend",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(
                "Start fresh, or restore a backup you exported from another device.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Button(
                onClick = onStartFresh,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start fresh") }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json")) },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Import existing backup") }
        }
        }
    }
}

@Composable
private fun NameStep(name: String, onNameChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("What's your name?", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SubjectsStep(subjects: List<Subject>, onAdd: (String, String) -> Unit, onRemove: (Subject) -> Unit) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Add your subjects", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code") }, modifier = Modifier.weight(1f))
        }
        Button(
            onClick = hapticClick {
                onAdd(name.trim(), code.trim())
                name = ""; code = ""
            },
            enabled = name.isNotBlank() && code.isNotBlank()
        ) { Text("Add subject") }

        LazyColumn {
            items(subjects, key = { it.id }) { subject ->
                Row(modifier = Modifier.fillMaxWidth().animateItem(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${subject.name} (${subject.code})")
                    IconButton(onClick = { onRemove(subject) }) { Icon(Icons.Default.Close, contentDescription = "Remove") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableStep(
    subjects: List<Subject>,
    slots: List<OnboardingDraftSlot>,
    onAdd: (Subject, DayOfWeek, LocalTime, LocalTime, Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Build your weekly timetable", style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = hapticClick { showDialog = true }, enabled = subjects.isNotEmpty()) { Text("Add class slot") }
        LazyColumn {
            items(slots, key = { it.localId }) { slot ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).animateItem()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${slot.subjectName} — ${slot.dayOfWeek} ${formatTime(slot.startTime)}-${formatTime(slot.endTime)}")
                        IconButton(onClick = { onRemove(slot.localId) }) { Icon(Icons.Default.Close, contentDescription = "Remove") }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var subject by remember { mutableStateOf(subjects.firstOrNull()) }
        var menuOpen by remember { mutableStateOf(false) }
        var day by remember { mutableStateOf(DayOfWeek.MONDAY) }
        var dayMenuOpen by remember { mutableStateOf(false) }
        var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
        var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
        var classCount by remember { mutableStateOf("1") }

        SpringAlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add slot") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box {
                        OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text(subject?.name ?: "Select subject") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            subjects.forEach { s -> DropdownMenuItem(text = { Text(s.name) }, onClick = { subject = s; menuOpen = false }) }
                        }
                    }
                    Box {
                        OutlinedButton(onClick = { dayMenuOpen = true }, modifier = Modifier.fillMaxWidth()) { Text(day.toString()) }
                        DropdownMenu(expanded = dayMenuOpen, onDismissRequest = { dayMenuOpen = false }) {
                            DayOfWeek.entries.forEach { d -> DropdownMenuItem(text = { Text(d.toString()) }, onClick = { day = d; dayMenuOpen = false }) }
                        }
                    }
                    TimePickerField("Start time", startTime, { startTime = it }, Modifier.fillMaxWidth())
                    TimePickerField("End time", endTime, { endTime = it }, Modifier.fillMaxWidth())
                    if (!endTime.isAfter(startTime)) {
                        Text(
                            "End time must be after start time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    OutlinedTextField(value = classCount, onValueChange = { classCount = it }, label = { Text("Counts as N classes") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    onClick = hapticClick {
                        subject?.let { onAdd(it, day, startTime, endTime, classCount.toIntOrNull() ?: 1) }
                        showDialog = false
                    },
                    enabled = subject != null && endTime.isAfter(startTime)
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DatesStep(state: OnboardingState, viewModel: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("When do classes start?", style = MaterialTheme.typography.titleLarge)
        DatePickerField("Start date", state.startDate, viewModel::onStartDateChange, Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.hasEndDate, onCheckedChange = viewModel::onHasEndDateChange)
            Text("I know the end date")
        }
        if (state.hasEndDate) {
            DatePickerField("End date", state.endDate, viewModel::onEndDateChange, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HolidaysStep(holidayDays: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Weekly holidays", style = MaterialTheme.typography.titleLarge)
        LazyColumn {
            items(DayOfWeek.entries) { day ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(day.toString())
                    Switch(checked = day in holidayDays, onCheckedChange = { onToggle(day) })
                }
            }
        }
    }
}

@Composable
private fun ThresholdStep(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Minimum attendance required", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = value, onValueChange = onChange, label = { Text("Required %") }, modifier = Modifier.fillMaxWidth())
    }
}

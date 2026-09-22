package com.iattend.app.feature.timetable

import android.widget.Toast
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.iattend.app.core.data.db.ClassType
import com.iattend.app.core.tutorial.LocalTutorialController
import com.iattend.app.core.tutorial.TutorialSignal
import com.iattend.app.core.tutorial.tutorialTarget
import com.iattend.app.core.ui.DatePickerField
import com.iattend.app.core.ui.RoomComboBox
import com.iattend.app.core.ui.SpringAlertDialog
import com.iattend.app.core.ui.SquircleIconButton
import com.iattend.app.core.ui.SubjectChipRow
import com.iattend.app.core.ui.TimePickerField
import com.iattend.app.core.ui.LocalTimeFormat
import com.iattend.app.core.ui.formatTime
import com.iattend.app.core.ui.hapticClick
import com.iattend.app.core.ui.modalsheet.SideSheet
import com.iattend.app.core.ui.rememberUnsavedChangesGuard
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableVersionEditorScreen(
    onBack: () -> Unit,
    onBackdatedFill: (Long) -> Unit,
    viewModel: TimetableVersionEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedDay by remember { mutableStateOf(java.time.LocalDate.now().dayOfWeek) }
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var room by remember { mutableStateOf("") }
    var roomOverridden by remember { mutableStateOf(false) }
    var setAsDefaultRoom by remember { mutableStateOf(false) }
    var confirmClearDefault by remember { mutableStateOf(false) }
    var classCount by remember { mutableStateOf("1") }
    var classType by remember { mutableStateOf(ClassType.LECTURE) }
    var editingLocalId by remember { mutableStateOf<Int?>(null) }
    var formTouched by remember { mutableStateOf(false) }
    var showSlotsSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val viewModelDirty by viewModel.isDirty.collectAsState()
    val guardedBack = rememberUnsavedChangesGuard(isDirty = viewModelDirty || formTouched, onConfirmedBack = onBack)
    val tutorialController = LocalTutorialController.current
    fun findEffectiveRoom(subjectId: Long?, excludedLocalId: Int?): String? {
        val subject = state.subjects.firstOrNull { it.id == subjectId }
        val pending = state.slots.lastOrNull {
            it.localId != excludedLocalId && it.subjectId == subjectId && it.setSubjectDefaultRoom
        }
        return if (pending == null) subject?.defaultRoomNumber
        else if (pending.roomNumberOverridden) pending.roomNumber else subject?.defaultRoomNumber
    }
    val effectiveRoomForSubject: (Long?) -> String? = { subjectId ->
        findEffectiveRoom(subjectId, null)
    }

    LaunchedEffect(state.subjects) {
        if (selectedSubjectId == null) {
            val first = state.subjects.firstOrNull()
            selectedSubjectId = first?.id
            room = effectiveRoomForSubject(first?.id).orEmpty()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            tutorialController?.reportSignal(TutorialSignal.TIMETABLE_SAVED)
            when (event) {
                is EditorNavEvent.Back -> onBack()
                is EditorNavEvent.ToBackdatedFill -> onBackdatedFill(event.versionId)
            }
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit: From ${state.effectiveFrom}") },
                navigationIcon = {
                    SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = guardedBack)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = state.label,
                onValueChange = viewModel::onLabelChange,
                label = { Text("Label (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            DatePickerField(
                label = "Effective from",
                date = state.effectiveFrom,
                onDateChange = viewModel::onEffectiveFromChange,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            if (state.hasOverlap) {
                Text(
                    "Warning: some slots overlap on the same day.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Slots", style = MaterialTheme.typography.titleMedium)
                val daySlotCount = state.slots.count { it.dayOfWeek == selectedDay }
                OutlinedButton(onClick = { showSlotsSheet = true }, modifier = Modifier.tutorialTarget("view_slots_button")) { Text("View slots ($daySlotCount)") }
            }

            LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DayOfWeek.entries) { day ->
                    val isHoliday = day in state.holidayDays
                    FilterChip(
                        selected = selectedDay == day,
                        onClick = { if (isHoliday) viewModel.notifyHolidayDay() else selectedDay = day },
                        label = { Text(day.name.take(3).lowercase().replaceFirstChar { it.uppercase() }) },
                        colors = if (isHoliday) {
                            FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        } else {
                            FilterChipDefaults.filterChipColors()
                        }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).tutorialTarget("slot_form"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (editingLocalId != null) "EDIT SLOT" else "ADD SLOT", style = MaterialTheme.typography.labelLarge)
                        if (editingLocalId != null) {
                            TextButton(onClick = {
                                editingLocalId = null
                                room = effectiveRoomForSubject(selectedSubjectId).orEmpty()
                                roomOverridden = false
                                setAsDefaultRoom = false
                                classCount = "1"
                                classType = ClassType.LECTURE
                                formTouched = false
                            }) { Text("Cancel edit") }
                        }
                    }
                    SubjectChipRow(state.subjects, selectedSubjectId, onSelect = {
                        if (selectedSubjectId != it) {
                            selectedSubjectId = it
                            room = effectiveRoomForSubject(it).orEmpty()
                            roomOverridden = false
                            setAsDefaultRoom = false
                            formTouched = true
                        }
                    })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimePickerField("Start", startTime, { startTime = it; formTouched = true }, Modifier.weight(1f))
                        TimePickerField("End", endTime, { endTime = it; formTouched = true }, Modifier.weight(1f))
                    }
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ClassType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = classType == type,
                                onClick = { classType = type; formTouched = true },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = ClassType.entries.size)
                            ) { Text(type.label) }
                        }
                    }
                    RoomComboBox(
                        value = room,
                        onValueChange = { room = it; roomOverridden = true; formTouched = true },
                        options = state.knownRooms,
                        modifier = Modifier.fillMaxWidth(),
                        inherited = !roomOverridden,
                        supportingText = if (roomOverridden) "Overridden for this slot" else "Subject default: ${effectiveRoomForSubject(selectedSubjectId) ?: "Not set"}"
                    )
                    if (roomOverridden) TextButton(onClick = {
                        roomOverridden = false
                        room = effectiveRoomForSubject(selectedSubjectId).orEmpty()
                        formTouched = true
                    }) { Text("Reset to subject default") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = setAsDefaultRoom,
                            enabled = room.isNotBlank() || effectiveRoomForSubject(selectedSubjectId) != null,
                            onCheckedChange = { checked ->
                                if (checked && room.isBlank() && effectiveRoomForSubject(selectedSubjectId) != null) confirmClearDefault = true
                                else {
                                    setAsDefaultRoom = checked
                                    formTouched = true
                                }
                            }
                        )
                        Text(if (room.isBlank()) "Remove subject default room" else "Set as default for this subject")
                    }
                    OutlinedTextField(
                        value = classCount,
                        onValueChange = { classCount = it; formTouched = true },
                        label = { Text("Counts as N classes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = hapticClick {
                            selectedSubjectId?.let {
                                val editing = editingLocalId
                                val nextRoom = if (setAsDefaultRoom) room else findEffectiveRoom(selectedSubjectId, editing).orEmpty()
                                if (editing != null) {
                                    viewModel.updateSlot(editing, it, selectedDay, startTime, endTime, classCount.toIntOrNull() ?: 1, room, classType, roomOverridden, setAsDefaultRoom)
                                    editingLocalId = null
                                } else {
                                    viewModel.addSlot(it, selectedDay, startTime, endTime, classCount.toIntOrNull() ?: 1, room, classType, roomOverridden, setAsDefaultRoom)
                                    startTime = endTime
                                }
                                tutorialController?.reportSignal(TutorialSignal.SLOT_SAVED)
                                room = nextRoom
                                roomOverridden = false
                                setAsDefaultRoom = false
                                classCount = "1"
                                classType = ClassType.LECTURE
                                formTouched = false
                                showSlotsSheet = true
                            }
                        },
                        enabled = selectedSubjectId != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (editingLocalId != null) "Update Slot" else "Save Slot") }
                }
            }

            Button(
                onClick = hapticClick(viewModel::save),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).tutorialTarget("save_timetable_button")
            ) {
                Text("Save timetable")
            }
        }
    }

    SideSheet(visible = showSlotsSheet, onVisibleChange = { showSlotsSheet = it }) {
        SlotsSheetContent(
            day = selectedDay,
            slots = state.slots.filter { it.dayOfWeek == selectedDay }.sortedBy { it.startTime },
            subjectName = { id -> state.subjects.find { it.id == id }?.name ?: "?" },
            subjectRoom = effectiveRoomForSubject,
            onRemove = viewModel::removeSlot,
            onEdit = { slot ->
                editingLocalId = slot.localId
                selectedSubjectId = slot.subjectId
                selectedDay = slot.dayOfWeek
                startTime = slot.startTime
                endTime = slot.endTime
                roomOverridden = slot.roomNumberOverridden
                room = if (slot.roomNumberOverridden) slot.roomNumber.orEmpty() else effectiveRoomForSubject(slot.subjectId).orEmpty()
                setAsDefaultRoom = slot.setSubjectDefaultRoom
                classCount = slot.classCount.toString()
                classType = slot.classType
                formTouched = true
                showSlotsSheet = false
            },
            onDuplicateDay = { targets -> viewModel.duplicateDay(selectedDay, targets) },
            onDuplicateSlot = { slot, targets -> viewModel.duplicateSlot(slot, targets) },
            onDismiss = { showSlotsSheet = false }
        )
    }

    if (confirmClearDefault) {
        SpringAlertDialog(
            onDismissRequest = { confirmClearDefault = false },
            title = { Text("Remove subject default room?") },
            text = { Text("Slots that inherit this default will no longer show a room. Slot-specific overrides will stay unchanged.") },
            confirmButton = {
                TextButton(onClick = { setAsDefaultRoom = true; formTouched = true; confirmClearDefault = false }) { Text("Remove default") }
            },
            dismissButton = { TextButton(onClick = { confirmClearDefault = false }) { Text("Keep default") } }
        )
    }
}

@Composable
private fun DayMultiSelectDialog(
    excludeDay: DayOfWeek,
    onConfirm: (Set<DayOfWeek>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(setOf<DayOfWeek>()) }
    SpringAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Copy to which days?") },
        text = {
            Column {
                DayOfWeek.entries.filter { it != excludeDay }.forEach { day ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selected = if (day in selected) selected - day else selected + day
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = day in selected, onCheckedChange = null)
                        Text(day.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected); onDismiss() }, enabled = selected.isNotEmpty()) { Text("Copy") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SlotsSheetContent(
    day: DayOfWeek,
    slots: List<DraftSlot>,
    subjectName: (Long?) -> String,
    subjectRoom: (Long?) -> String?,
    onRemove: (Int) -> Unit,
    onEdit: (DraftSlot) -> Unit,
    onDuplicateDay: (Set<DayOfWeek>) -> Unit,
    onDuplicateSlot: (DraftSlot, Set<DayOfWeek>) -> Unit,
    onDismiss: () -> Unit
) {
    var showDuplicateDayDialog by remember { mutableStateOf(false) }
    var duplicateSlotTarget by remember { mutableStateOf<DraftSlot?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(day.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleLarge)
            SquircleIconButton(Icons.Default.Close, contentDescription = "Close", onClick = onDismiss)
        }
        if (slots.isNotEmpty()) {
            TextButton(onClick = { showDuplicateDayDialog = true }) { Text("Duplicate day to...") }
        }
        if (slots.isEmpty()) {
            Text(
                "No slots for this day yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(slots, key = { it.localId }) { slot ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).animateItem(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                subjectName(slot.subjectId),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    "${formatTime(slot.startTime, LocalTimeFormat.current)} - ${formatTime(slot.endTime, LocalTimeFormat.current)}" +
                                        " · ${slot.classType.label}" +
                                        (if (slot.classCount > 1) " · ${slot.classCount} classes" else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val room = (if (slot.roomNumberOverridden) slot.roomNumber else subjectRoom(slot.subjectId))?.takeIf { it.isNotBlank() }
                            Text(room?.let { "Room $it" } ?: "Room not set", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row {
                            IconButton(onClick = { onEdit(slot) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit slot")
                            }
                            IconButton(onClick = { duplicateSlotTarget = slot }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate slot")
                            }
                            IconButton(onClick = { onRemove(slot.localId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove slot", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDuplicateDayDialog) {
        DayMultiSelectDialog(
            excludeDay = day,
            onConfirm = onDuplicateDay,
            onDismiss = { showDuplicateDayDialog = false }
        )
    }
    duplicateSlotTarget?.let { slot ->
        DayMultiSelectDialog(
            excludeDay = day,
            onConfirm = { targets -> onDuplicateSlot(slot, targets) },
            onDismiss = { duplicateSlotTarget = null }
        )
    }
}

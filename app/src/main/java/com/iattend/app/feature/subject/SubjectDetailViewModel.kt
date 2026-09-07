package com.iattend.app.feature.subject

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.ClassOccurrenceDao
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import android.content.Context
import androidx.glance.appwidget.updateAll
import com.iattend.app.core.datastore.SettingsRepository
import com.iattend.app.core.domain.stats.AttendanceStatsCalculator
import com.iattend.app.core.navigation.SubjectDetailRoute
import com.iattend.app.core.notifications.ClassReminderScheduler
import com.iattend.app.widget.UpcomingClassesWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SubjectDetailState(
    val subject: Subject? = null,
    val stats: AttendanceStatsCalculator.SubjectStats? = null,
    val history: List<ClassOccurrence> = emptyList(),
    val requiredPercentage: Float = 75f
)

@HiltViewModel
class SubjectDetailViewModel @Inject constructor(
    private val subjectDao: SubjectDao,
    private val settingsRepository: SettingsRepository,
    private val classOccurrenceDao: ClassOccurrenceDao,
    private val reminderScheduler: ClassReminderScheduler,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val subjectId = savedStateHandle.toRoute<SubjectDetailRoute>().subjectId

    val state: StateFlow<SubjectDetailState> = combine(
        subjectDao.getById(subjectId),
        classOccurrenceDao.getForSubject(subjectId),
        settingsRepository.settings
    ) { subject, occurrences, settings ->
        val history = occurrences.sortedBy { it.date }
        if (subject == null) {
            SubjectDetailState(history = history)
        } else {
            val requiredPercentage = subject.requiredPercentageOverride ?: settings.requiredPercentageDefault
            val stats = AttendanceStatsCalculator.compute(subject, occurrences, requiredPercentage, settings.trackingEndDate)
            SubjectDetailState(subject, stats, history, requiredPercentage)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubjectDetailState())

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()

    fun delete() {
        viewModelScope.launch {
            state.value.subject?.let { subjectDao.delete(it) }
            _deleted.send(Unit)
        }
    }

    /** Unmarked -> Present -> Absent -> Cancelled -> Unmarked. Future-dated occurrences aren't markable (Product Plan §6.3). */
    fun cycleStatus(occurrence: ClassOccurrence) {
        if (occurrence.date.isAfter(LocalDate.now())) return
        val next = when (occurrence.status) {
            OccurrenceStatus.UNMARKED -> OccurrenceStatus.PRESENT
            OccurrenceStatus.PRESENT -> OccurrenceStatus.ABSENT
            OccurrenceStatus.ABSENT -> OccurrenceStatus.CANCELLED
            OccurrenceStatus.CANCELLED -> OccurrenceStatus.UNMARKED
        }
        val cancelReason = if (next == OccurrenceStatus.CANCELLED) occurrence.cancelReason else null
        viewModelScope.launch {
            classOccurrenceDao.update(occurrence.copy(status = next, cancelReason = cancelReason))
            if (next != OccurrenceStatus.UNMARKED) {
                reminderScheduler.cancelRemindersForOccurrence(occurrence.id)
            }
            UpcomingClassesWidget().updateAll(context)
        }
    }

    fun setCancelReason(occurrence: ClassOccurrence, reason: String?) {
        viewModelScope.launch {
            classOccurrenceDao.update(occurrence.copy(cancelReason = reason?.ifBlank { null }))
            UpcomingClassesWidget().updateAll(context)
        }
    }

    fun deleteOccurrence(occurrence: ClassOccurrence) {
        viewModelScope.launch {
            classOccurrenceDao.delete(occurrence)
            reminderScheduler.cancelRemindersForOccurrence(occurrence.id)
            UpcomingClassesWidget().updateAll(context)
        }
    }
}

package com.iattend.app.feature.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentDao
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.datastore.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AllAssessmentsRow(val assessment: Assessment, val subjectName: String)

@HiltViewModel
class AllAssessmentsViewModel @Inject constructor(
    assessmentDao: AssessmentDao,
    subjectDao: SubjectDao,
    settingsRepository: SettingsRepository
) : ViewModel() {
    val rows: StateFlow<List<AllAssessmentsRow>> = combine(
        assessmentDao.getAll(),
        subjectDao.getAll(),
        settingsRepository.settings
    ) { assessments, subjects, settings ->
        val activeSemId = settings.activeSemesterId
        val validSubjects = if (activeSemId != null) subjects.filter { it.semesterId == activeSemId } else subjects
        val validSubjectIds = validSubjects.map { it.id }.toSet()
        val subjectsById = validSubjects.associateBy { it.id }

        assessments
            .filter { it.subjectId in validSubjectIds }
            .map { a -> AllAssessmentsRow(a, subjectsById[a.subjectId]?.name ?: "?") }
            .sortedWith(compareBy({ it.assessment.date }, { it.assessment.startTime }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<Subject>> = combine(
        subjectDao.getAll(),
        settingsRepository.settings
    ) { all, settings ->
        val activeSemId = settings.activeSemesterId
        if (activeSemId != null) all.filter { it.semesterId == activeSemId } else all
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

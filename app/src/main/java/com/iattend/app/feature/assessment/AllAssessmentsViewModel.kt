package com.iattend.app.feature.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentDao
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
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
    subjectDao: SubjectDao
) : ViewModel() {
    val rows: StateFlow<List<AllAssessmentsRow>> = combine(assessmentDao.getAll(), subjectDao.getAll()) { assessments, subjects ->
        assessments
            .map { a -> AllAssessmentsRow(a, subjects.find { it.id == a.subjectId }?.name ?: "?") }
            .sortedWith(compareBy({ it.assessment.date }, { it.assessment.startTime }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<Subject>> = subjectDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

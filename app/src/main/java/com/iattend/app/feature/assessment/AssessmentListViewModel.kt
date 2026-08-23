package com.iattend.app.feature.assessment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.iattend.app.core.data.db.Assessment
import com.iattend.app.core.data.db.AssessmentDao
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.navigation.AssessmentListRoute
import com.iattend.app.core.notifications.AssessmentReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssessmentListViewModel @Inject constructor(
    private val assessmentDao: AssessmentDao,
    subjectDao: SubjectDao,
    private val reminderScheduler: AssessmentReminderScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AssessmentListRoute>()
    val subjectId: Long get() = route.subjectId

    val assessments: StateFlow<List<Assessment>> = assessmentDao.getForSubject(route.subjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjectName: StateFlow<String> = subjectDao.getById(route.subjectId)
        .map { it?.name ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun delete(assessment: Assessment) {
        viewModelScope.launch {
            assessmentDao.delete(assessment)
            reminderScheduler.cancel(assessment.id)
        }
    }
}

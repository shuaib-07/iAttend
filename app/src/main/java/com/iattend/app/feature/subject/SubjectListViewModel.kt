package com.iattend.app.feature.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubjectListViewModel @Inject constructor(
    private val subjectDao: SubjectDao
) : ViewModel() {
    val subjects: StateFlow<List<Subject>> = subjectDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(subject: Subject) {
        viewModelScope.launch { subjectDao.delete(subject) }
    }
}

package com.iattend.app.feature.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.datastore.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubjectListViewModel @Inject constructor(
    private val subjectDao: SubjectDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val subjects: StateFlow<List<Subject>> = combine(
        subjectDao.getAll(),
        settingsRepository.settings
    ) { all, settings ->
        val activeSemesterId = settings.activeSemesterId
        if (activeSemesterId != null) {
            all.filter { it.semesterId == activeSemesterId }
        } else {
            all
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(subject: Subject) {
        viewModelScope.launch { subjectDao.delete(subject) }
    }
}

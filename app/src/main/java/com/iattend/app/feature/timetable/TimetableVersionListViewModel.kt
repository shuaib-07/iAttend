package com.iattend.app.feature.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.TimetableVersion
import com.iattend.app.core.data.db.TimetableVersionDao
import com.iattend.app.core.domain.occurrence.OccurrenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimetableVersionListViewModel @Inject constructor(
    private val timetableVersionDao: TimetableVersionDao,
    private val occurrenceRepository: OccurrenceRepository
) : ViewModel() {
    val versions: StateFlow<List<TimetableVersion>> = timetableVersionDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(version: TimetableVersion) {
        viewModelScope.launch {
            timetableVersionDao.delete(version)
            occurrenceRepository.regenerateUnmarkedWindow()
        }
    }
}

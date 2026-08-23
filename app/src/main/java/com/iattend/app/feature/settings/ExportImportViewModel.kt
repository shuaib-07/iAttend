package com.iattend.app.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.data.db.SubjectDao
import com.iattend.app.core.data.export.ExportImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ExportImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exportImportRepository: ExportImportRepository,
    subjectDao: SubjectDao
) : ViewModel() {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val subjects = subjectDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _message.value = try {
                val json = exportImportRepository.export()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                "Backup saved."
            } catch (e: Exception) {
                "Export failed: ${e.message}"
            }
        }
    }

    fun exportTemplateTo(uri: Uri, selectedSubjectIds: Set<Long>?, includeExtraClasses: Boolean, includeAssessments: Boolean) {
        viewModelScope.launch {
            _message.value = try {
                val json = exportImportRepository.exportTemplate(selectedSubjectIds, includeExtraClasses, includeAssessments)
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                "Template saved."
            } catch (e: Exception) {
                "Export failed: ${e.message}"
            }
        }
    }

    fun shareTemplate(selectedSubjectIds: Set<Long>?, includeExtraClasses: Boolean, includeAssessments: Boolean) {
        viewModelScope.launch {
            try {
                val json = exportImportRepository.exportTemplate(selectedSubjectIds, includeExtraClasses, includeAssessments)
                val cacheDir = File(context.cacheDir, "share").apply { mkdirs() }
                val file = File(cacheDir, "iattend-template-${java.time.LocalDate.now()}.json")
                file.writeText(json)
                val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(intent, "Share timetable template").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                _message.value = "Share failed: ${e.message}"
            }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            _message.value = try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return@launch
                // auto-detect template vs full
                val isTemplate = try {
                    text.contains("\"exportType\"") && text.contains("TEMPLATE")
                } catch (_: Exception) { false }
                if (isTemplate) {
                    exportImportRepository.importAuto(text)
                    "Template imported. Attendance regenerated as unmarked."
                } else {
                    exportImportRepository.import(text)
                    "Data restored."
                }
            } catch (e: Exception) {
                "Import failed: ${e.message}"
            }
        }
    }

    fun clearMessage() { _message.value = null }
}

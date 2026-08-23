package com.iattend.app.feature.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iattend.app.core.datastore.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    val name: StateFlow<String> = profileRepository.profile
        .map { it.name }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val pictureUri: StateFlow<String?> = profileRepository.profile
        .map { it.pictureUri }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setName(value: String) {
        viewModelScope.launch { profileRepository.setName(value) }
    }

    /** Copies the picked image into app-private storage (Implementation Plan §3.8) so the path stays valid. */
    fun setPicture(uri: Uri) {
        viewModelScope.launch {
            val file = File(context.filesDir, "profile_picture.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            profileRepository.setPictureUri(file.absolutePath)
        }
    }

    /** Picks a generated DiceBear avatar - stores the URL directly, replacing any real photo (mutually exclusive). */
    fun setAvatarUrl(url: String) {
        viewModelScope.launch { profileRepository.setPictureUri(url) }
    }
}

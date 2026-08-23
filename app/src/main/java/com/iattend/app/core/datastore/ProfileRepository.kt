package com.iattend.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

private val Context.profileDataStore by preferencesDataStore(name = "profile")

@Serializable
data class Profile(
    val name: String = "",
    val pictureUri: String? = null
)

@Singleton
class ProfileRepository @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.profileDataStore

    private object Keys {
        val NAME = stringPreferencesKey("name")
        val PICTURE_URI = stringPreferencesKey("picture_uri")
    }

    val profile: Flow<Profile> = dataStore.data.map { prefs ->
        Profile(
            name = prefs[Keys.NAME] ?: "",
            pictureUri = prefs[Keys.PICTURE_URI]
        )
    }

    suspend fun setName(name: String) {
        dataStore.edit { it[Keys.NAME] = name }
    }

    suspend fun setPictureUri(uri: String?) {
        dataStore.edit {
            if (uri == null) it.remove(Keys.PICTURE_URI) else it[Keys.PICTURE_URI] = uri
        }
    }

    suspend fun replaceAll(profile: Profile) {
        dataStore.edit { prefs ->
            prefs[Keys.NAME] = profile.name
            if (profile.pictureUri == null) prefs.remove(Keys.PICTURE_URI) else prefs[Keys.PICTURE_URI] = profile.pictureUri
        }
    }
}

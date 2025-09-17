package com.vana.inspection.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val PREFS_NAME = "inspection_preferences"

private val Context.dataStore by preferencesDataStore(name = PREFS_NAME)

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val AUTO_UPLOAD = booleanPreferencesKey("auto_upload")
        val UPLOAD_TARGET = intPreferencesKey("upload_target")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val KEEP_LOCAL_COPY = booleanPreferencesKey("keep_local_copy")
        val INCLUDE_COMPASS = booleanPreferencesKey("include_compass")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences.toModel() }

    suspend fun updateAutoUpload(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_UPLOAD] = enabled
        }
    }

    suspend fun updateUploadTarget(target: UploadTarget) {
        context.dataStore.edit { prefs ->
            prefs[Keys.UPLOAD_TARGET] = target.ordinal
        }
    }

    suspend fun updateWifiOnlyUploads(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WIFI_ONLY] = enabled
        }
    }

    suspend fun updateKeepLocalCopy(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.KEEP_LOCAL_COPY] = enabled
        }
    }

    suspend fun updateCompass(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.INCLUDE_COMPASS] = enabled
        }
    }

    private fun Preferences.toModel(): AppPreferences {
        val uploadTargetIndex = this[Keys.UPLOAD_TARGET] ?: UploadTarget.MANUAL.ordinal
        val uploadTarget = UploadTarget.values().getOrElse(uploadTargetIndex) { UploadTarget.MANUAL }
        return AppPreferences(
            autoUploadEnabled = this[Keys.AUTO_UPLOAD] ?: false,
            uploadTarget = uploadTarget,
            wifiOnlyUploads = this[Keys.WIFI_ONLY] ?: true,
            keepLocalCopy = this[Keys.KEEP_LOCAL_COPY] ?: true,
            includeCompassDirection = this[Keys.INCLUDE_COMPASS] ?: false
        )
    }
}

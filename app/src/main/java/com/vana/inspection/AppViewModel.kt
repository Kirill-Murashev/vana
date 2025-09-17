package com.vana.inspection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vana.inspection.capture.PhotoCaptureManager
import com.vana.inspection.capture.PhotoUploader
import com.vana.inspection.data.AppPreferences
import com.vana.inspection.data.AppPreferencesRepository
import com.vana.inspection.data.PhotoRecord
import com.vana.inspection.data.ProjectInfo
import com.vana.inspection.data.UploadState
import com.vana.inspection.data.UploadTarget
import com.vana.inspection.location.LocationProvider
import com.vana.inspection.network.NetworkStatusProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.time.Instant

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = AppPreferencesRepository(application)
    private val locationProvider = LocationProvider(application)
    private val photoCaptureManager = PhotoCaptureManager(application)
    private val networkStatusProvider = NetworkStatusProvider(application)
    private val photoUploader = PhotoUploader(application, networkStatusProvider)

    private val _projectInfo = MutableStateFlow(ProjectInfo())
    private val _photos = MutableStateFlow<List<PhotoRecord>>(emptyList())
    private val _isCapturing = MutableStateFlow(false)
    private val _captureError = MutableStateFlow<String?>(null)

    private val preferencesState: StateFlow<AppPreferences> =
        preferencesRepository.preferencesFlow.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AppPreferences()
        )

    val uiState: StateFlow<AppUiState> = combine(
        _projectInfo,
        _photos,
        _isCapturing,
        _captureError,
        preferencesState
    ) { project, photos, capturing, error, preferences ->
        AppUiState(
            projectInfo = project,
            photos = photos,
            isCapturing = capturing,
            captureError = error,
            preferences = preferences
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppUiState()
    )

    init {
        viewModelScope.launch {
            val existing = photoCaptureManager.loadExistingPhotos()
            _photos.value = existing
        }
    }

    fun updateProjectInfo(block: ProjectInfo.() -> ProjectInfo) {
        _projectInfo.value = _projectInfo.value.block()
    }

    fun setProjectInfo(info: ProjectInfo) {
        _projectInfo.value = info
    }

    fun clearCaptureError() {
        _captureError.value = null
    }

    fun refreshGallery() {
        viewModelScope.launch {
            _photos.value = photoCaptureManager.loadExistingPhotos()
        }
    }

    suspend fun prepareCapture(): CapturePreparation? {
        val info = _projectInfo.value
        if (!info.isReady) {
            _captureError.value = "Please complete project and appraiser details before capturing."
            return null
        }
        _isCapturing.value = true

        return try {
            val timestamp = Instant.now()
            val file = photoCaptureManager.prepareOutputFile(timestamp)
            val location = locationProvider.currentLocation()
            val preferences = preferencesState.value
            val metadata = PhotoCaptureManager.PhotoMetadata(
                timestamp = timestamp,
                projectInfo = info,
                location = location,
                includeCompass = preferences.includeCompassDirection
            )
            val target = if (preferences.autoUploadEnabled) preferences.uploadTarget else UploadTarget.MANUAL
            CapturePreparation(file, metadata, target)
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to prepare capture")
            _captureError.value = exception.message ?: "Unable to prepare camera capture"
            _isCapturing.value = false
            null
        }
    }

    fun onCaptureFailed(message: String?) {
        _isCapturing.value = false
        _captureError.value = message ?: "Capture failed"
    }

    fun finalizeCapture(preparation: CapturePreparation) {
        viewModelScope.launch {
            val preferences = preferencesState.value
            try {
                var record = photoCaptureManager.processCapturedPhoto(
                    preparation.file,
                    preparation.metadata,
                    preferences,
                    preparation.uploadTarget
                )

                if (preferences.autoUploadEnabled && preparation.uploadTarget != UploadTarget.MANUAL) {
                    record = record.copy(uploadState = UploadState.UPLOADING)
                    record = photoUploader.upload(
                        record,
                        preparation.uploadTarget,
                        preferences.wifiOnlyUploads,
                        preferences.keepLocalCopy
                    )
                }

                _photos.value = listOf(record) + _photos.value.filterNot { it.id == record.id }
            } catch (exception: Exception) {
                Timber.e(exception, "Failed to process captured photo")
                _captureError.value = exception.message ?: "Failed to process captured photo"
                preparation.file.safeDelete()
            } finally {
                _isCapturing.value = false
            }
        }
    }

    fun retryUpload(recordId: String) {
        viewModelScope.launch {
            val preferences = preferencesState.value
            val record = _photos.value.find { it.id == recordId } ?: return@launch
            if (record.uploadTarget == UploadTarget.MANUAL) return@launch
            val updated = photoUploader.upload(
                record.copy(uploadState = UploadState.UPLOADING),
                record.uploadTarget,
                preferences.wifiOnlyUploads,
                preferences.keepLocalCopy
            )
            _photos.value = _photos.value.map { if (it.id == recordId) updated else it }
        }
    }

    fun removePhoto(recordId: String) {
        viewModelScope.launch {
            val existing = _photos.value.find { it.id == recordId } ?: return@launch
            runCatching { File(existing.filePath).delete() }
            _photos.value = _photos.value.filterNot { it.id == recordId }
        }
    }

    fun toggleAutoUpload(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateAutoUpload(enabled) }
    }

    fun setUploadTarget(target: UploadTarget) {
        viewModelScope.launch { preferencesRepository.updateUploadTarget(target) }
    }

    fun setWifiOnlyUploads(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateWifiOnlyUploads(enabled) }
    }

    fun setKeepLocalCopy(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateKeepLocalCopy(enabled) }
    }

    fun setIncludeCompass(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateCompass(enabled) }
    }

    data class CapturePreparation(
        val file: File,
        val metadata: PhotoCaptureManager.PhotoMetadata,
        val uploadTarget: UploadTarget
    )
}

private fun File.safeDelete() {
    runCatching { delete() }
}

data class AppUiState(
    val projectInfo: ProjectInfo = ProjectInfo(),
    val photos: List<PhotoRecord> = emptyList(),
    val isCapturing: Boolean = false,
    val captureError: String? = null,
    val preferences: AppPreferences = AppPreferences()
)

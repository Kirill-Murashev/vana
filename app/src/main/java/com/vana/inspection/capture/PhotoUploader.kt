package com.vana.inspection.capture

import android.content.Context
import com.vana.inspection.data.PhotoRecord
import com.vana.inspection.data.UploadState
import com.vana.inspection.data.UploadTarget
import com.vana.inspection.network.NetworkStatusProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class PhotoUploader(
    context: Context,
    private val networkStatusProvider: NetworkStatusProvider
) {

    private val appContext = context.applicationContext

    suspend fun upload(
        record: PhotoRecord,
        target: UploadTarget,
        wifiOnly: Boolean,
        keepLocalCopy: Boolean
    ): PhotoRecord = withContext(Dispatchers.IO) {
        if (wifiOnly && !networkStatusProvider.isWifiConnected()) {
            Timber.i("Deferring upload for %s: Wi-Fi required", record.fileName)
            return@withContext record.copy(uploadState = UploadState.PENDING)
        }

        val destinationDir = when (target) {
            UploadTarget.GOOGLE_DRIVE -> File(appContext.filesDir, "uploads/google-drive")
            UploadTarget.CORPORATE_SERVER -> File(appContext.filesDir, "uploads/corporate-server")
            UploadTarget.MANUAL -> File(appContext.filesDir, "uploads/manual")
        }
        if (!destinationDir.exists()) destinationDir.mkdirs()

        val sourceFile = File(record.filePath)
        val destinationFile = File(destinationDir, sourceFile.name)

        try {
            sourceFile.copyTo(destinationFile, overwrite = true)
            if (!keepLocalCopy && target != UploadTarget.MANUAL) {
                sourceFile.delete()
            }
            record.copy(uploadState = UploadState.UPLOADED)
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to copy file for upload")
            record.copy(uploadState = UploadState.FAILED)
        }
    }
}

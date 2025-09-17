package com.vana.inspection.data

enum class UploadTarget { MANUAL, GOOGLE_DRIVE, CORPORATE_SERVER }

enum class UploadState { PENDING, UPLOADING, UPLOADED, FAILED }

data class AppPreferences(
    val autoUploadEnabled: Boolean = false,
    val uploadTarget: UploadTarget = UploadTarget.MANUAL,
    val wifiOnlyUploads: Boolean = true,
    val keepLocalCopy: Boolean = true,
    val includeCompassDirection: Boolean = false
)

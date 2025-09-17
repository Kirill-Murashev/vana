package com.vana.inspection.data

import java.time.Instant

data class PhotoRecord(
    val id: String,
    val filePath: String,
    val timestamp: Instant,
    val latitude: Double?,
    val longitude: Double?,
    val altitudeMeters: Double?,
    val compassBearing: Float?,
    val uploadState: UploadState,
    val uploadTarget: UploadTarget,
    val projectInfo: ProjectInfo
) {
    val fileName: String = filePath.substringAfterLast('/')
}

package com.vana.inspection.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.location.Location
import android.os.Environment
import androidx.exifinterface.media.ExifInterface
import com.vana.inspection.data.AppPreferences
import com.vana.inspection.data.PhotoRecord
import com.vana.inspection.data.ProjectInfo
import com.vana.inspection.data.UploadState
import com.vana.inspection.data.UploadTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

private val DISPLAY_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
private val EXIF_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss").withZone(ZoneId.systemDefault())

class PhotoCaptureManager(private val context: Context) {

    data class PhotoMetadata(
        val timestamp: Instant,
        val projectInfo: ProjectInfo,
        val location: Location?,
        val includeCompass: Boolean
    )

    suspend fun prepareOutputFile(timestamp: Instant): File = withContext(Dispatchers.IO) {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: File(context.filesDir, "pictures").also { it.mkdirs() }
        if (!directory.exists()) directory.mkdirs()
        val fileName = "IMG_${timestamp.toEpochMilli()}.jpg"
        File(directory, fileName)
    }

    suspend fun processCapturedPhoto(
        file: File,
        metadata: PhotoMetadata,
        preferences: AppPreferences,
        uploadTarget: UploadTarget
    ): PhotoRecord = withContext(Dispatchers.IO) {
        applyOverlay(file, metadata)
        writeExif(file, metadata)

        PhotoRecord(
            id = file.nameWithoutExtension,
            filePath = file.absolutePath,
            timestamp = metadata.timestamp,
            latitude = metadata.location?.latitude,
            longitude = metadata.location?.longitude,
            altitudeMeters = metadata.location?.altitude,
            compassBearing = metadata.location?.takeIf { metadata.includeCompass && it.hasBearing() }?.bearing,
            uploadState = if (preferences.autoUploadEnabled) UploadState.PENDING else UploadState.PENDING,
            uploadTarget = uploadTarget,
            projectInfo = metadata.projectInfo
        )
    }

    suspend fun loadExistingPhotos(): List<PhotoRecord> = withContext(Dispatchers.IO) {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: File(context.filesDir, "pictures")
        if (!directory.exists()) return@withContext emptyList()

        directory.listFiles { file -> file.extension.equals("jpg", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { file -> readRecordFromFile(file) }
            ?: emptyList()
    }

    private fun readRecordFromFile(file: File): PhotoRecord? = try {
        val exif = ExifInterface(file)
        val timestamp = exif.getAttribute(ExifInterface.TAG_DATETIME)?.let {
            runCatching {
                LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"))
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            }.getOrNull()
        } ?: Instant.ofEpochMilli(file.lastModified())

        val latitude = exif.latLong?.getOrNull(0)
        val longitude = exif.latLong?.getOrNull(1)
        val altitude = exif.getAltitude(0.0)
        val bearing = exif.getAttributeDouble("GPSImgDirection", Double.NaN)
        val userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
        val projectInfo = parseProjectInfo(userComment)

        PhotoRecord(
            id = file.nameWithoutExtension,
            filePath = file.absolutePath,
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = if (altitude != 0.0) altitude else null,
            compassBearing = if (!bearing.isNaN()) bearing.toFloat() else null,
            uploadState = UploadState.PENDING,
            uploadTarget = UploadTarget.MANUAL,
            projectInfo = projectInfo
        )
    } catch (ioe: IOException) {
        Timber.e(ioe, "Failed to parse EXIF for %s", file.absolutePath)
        null
    }

    private fun applyOverlay(file: File, metadata: PhotoMetadata) {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)?.copy(Bitmap.Config.ARGB_8888, true)
        if (bitmap == null) {
            Timber.w("Failed to decode bitmap for overlay: %s", file.absolutePath)
            return
        }

        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = bitmap.width * 0.035f
        }
        val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DD000000")
        }
        val spacing = paint.textSize * 0.3f

        val lines = buildWatermarkLines(metadata)
        val maxTextWidth = lines.maxOfOrNull { paint.measureText(it) } ?: 0f
        val totalTextHeight = lines.size * paint.textSize + (lines.size - 1) * spacing
        val padding = paint.textSize * 0.5f

        val rect = RectF(
            bitmap.width - maxTextWidth - padding * 2,
            bitmap.height - totalTextHeight - padding * 2,
            bitmap.width.toFloat(),
            bitmap.height.toFloat()
        )
        canvas.drawRoundRect(rect, paint.textSize * 0.4f, paint.textSize * 0.4f, secondaryPaint)

        var textBaseline = rect.top + padding + paint.textSize
        for (line in lines) {
            canvas.drawText(line, rect.left + padding, textBaseline, paint)
            textBaseline += paint.textSize + spacing
        }

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
    }

    private fun buildWatermarkLines(metadata: PhotoMetadata): List<String> {
        val lines = mutableListOf<String>()
        lines += "Date: ${DISPLAY_FORMAT.format(metadata.timestamp)}"
        metadata.location?.let { location ->
            lines += "Coords: ${"%.6f".format(location.latitude)}, ${"%.6f".format(location.longitude)}"
            lines += "Altitude: ${"%.1f".format(location.altitude)} m"
            if (metadata.includeCompass && location.hasBearing()) {
                lines += "Bearing: ${"%.0f".format(location.bearing)}° ${bearingToCardinal(location.bearing)}"
            }
        } ?: lines.add("Coords: unavailable")

        lines += "Project: ${metadata.projectInfo.projectName}"
        lines += "Appraiser: ${metadata.projectInfo.valuerName}"
        if (metadata.projectInfo.companyName.isNotBlank()) {
            lines += metadata.projectInfo.companyName
        }
        return lines
    }

    private fun bearingToCardinal(bearing: Float): String {
        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((bearing % 360 + 360) % 360 / 45f).roundToInt() % directions.size
        return directions[index]
    }

    private fun writeExif(file: File, metadata: PhotoMetadata) {
        val exif = ExifInterface(file)
        exif.setAttribute(ExifInterface.TAG_DATETIME, EXIF_FORMAT.format(metadata.timestamp))
        exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, buildDescription(metadata))
        exif.setAttribute(ExifInterface.TAG_USER_COMMENT, buildUserComment(metadata))

        metadata.location?.let { location ->
            exif.setGpsLatitude(location.latitude)
            exif.setGpsLongitude(location.longitude)
            if (location.hasAltitude()) {
                exif.setAltitude(location.altitude)
            }
            if (metadata.includeCompass && location.hasBearing()) {
                exif.setAttribute("GPSImgDirection", location.bearing.toString())
                exif.setAttribute("GPSImgDirectionRef", "T")
            }
        }

        exif.saveAttributes()
    }

    private fun buildDescription(metadata: PhotoMetadata): String = buildString {
        append(metadata.projectInfo.projectName)
        append(" | ")
        append(metadata.projectInfo.valuerName)
        if (metadata.projectInfo.companyName.isNotBlank()) {
            append(" | ")
            append(metadata.projectInfo.companyName)
        }
    }

    private fun buildUserComment(metadata: PhotoMetadata): String = listOf(
        "project=${metadata.projectInfo.projectName}",
        "client=${metadata.projectInfo.clientName}",
        "valuer=${metadata.projectInfo.valuerName}",
        "company=${metadata.projectInfo.companyName}"
    ).joinToString(";")

    private fun parseProjectInfo(comment: String?): ProjectInfo {
        if (comment.isNullOrBlank()) return ProjectInfo()
        val values = comment.split(';').mapNotNull {
            val (key, value) = it.split('=', limit = 2).let { parts ->
                parts.getOrNull(0) to parts.getOrNull(1)
            }
            if (key != null && value != null) key to value else null
        }.toMap()
        return ProjectInfo(
            projectName = values["project"].orEmpty(),
            clientName = values["client"].orEmpty(),
            valuerName = values["valuer"].orEmpty(),
            companyName = values["company"].orEmpty()
        )
    }

    private fun ExifInterface.setGpsLatitude(latitude: Double) {
        val (deg, min, sec) = toDms(latitude)
        setAttribute(ExifInterface.TAG_GPS_LATITUDE, formatDms(deg, min, sec))
        setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (latitude >= 0) "N" else "S")
    }

    private fun ExifInterface.setGpsLongitude(longitude: Double) {
        val (deg, min, sec) = toDms(longitude)
        setAttribute(ExifInterface.TAG_GPS_LONGITUDE, formatDms(deg, min, sec))
        setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (longitude >= 0) "E" else "W")
    }

    private fun ExifInterface.setAltitude(altitude: Double) {
        val ref = if (altitude >= 0) "0" else "1"
        setAttribute(ExifInterface.TAG_GPS_ALTITUDE, formatRational(abs(altitude)))
        setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, ref)
    }

    private fun toDms(coordinate: Double): Triple<Int, Int, Double> {
        val absolute = abs(coordinate)
        val degrees = floor(absolute).toInt()
        val minutesFull = (absolute - degrees) * 60
        val minutes = floor(minutesFull).toInt()
        val seconds = (minutesFull - minutes) * 60
        return Triple(degrees, minutes, seconds)
    }

    private fun formatDms(degrees: Int, minutes: Int, seconds: Double): String {
        val sec = (seconds * 10000).roundToInt()
        return "$degrees/1,$minutes/1,${sec}/10000"
    }

    private fun formatRational(value: Double): String {
        val scaled = (value * 100).roundToInt()
        return "${scaled}/100"
    }
}

package com.wootan.ghostcamera.camera

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CaptureTarget(
    val options: ImageCapture.OutputFileOptions,
    val fallbackFile: File? = null,
)

object CaptureStore {
    fun createTarget(context: Context): CaptureTarget {
        val fileName = "GHOST_${timestamp()}.jpg"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/GhostCamera",
                )
            }
            CaptureTarget(
                options = ImageCapture.OutputFileOptions.Builder(
                    context.contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values,
                ).build(),
            )
        } else {
            val directory = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "GhostCamera",
            ).apply { mkdirs() }
            val file = File(directory, fileName)
            CaptureTarget(
                options = ImageCapture.OutputFileOptions.Builder(file).build(),
                fallbackFile = file,
            )
        }
    }

    fun resolveSavedUri(
        context: Context,
        result: ImageCapture.OutputFileResults,
        target: CaptureTarget,
    ): Uri? {
        result.savedUri?.let { return it }
        val file = target.fallbackFile ?: return null
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("image/jpeg"),
            null,
        )
        return Uri.fromFile(file)
    }

    fun applyAdditionalRotation(
        context: Context,
        uri: Uri,
        quarterTurns: Int,
    ): Boolean {
        val normalizedTurns = CameraRotation.normalize(quarterTurns)
        if (normalizedTurns == 0) return true

        return runCatching {
            if (uri.scheme == "file") {
                val path = requireNotNull(uri.path)
                rotateExif(ExifInterface(path), normalizedTurns)
            } else {
                context.contentResolver.openFileDescriptor(uri, "rw")?.use { descriptor ->
                    rotateExif(ExifInterface(descriptor.fileDescriptor), normalizedTurns)
                } ?: error("Unable to open saved image")
            }
        }.isSuccess
    }

    private fun rotateExif(exif: ExifInterface, quarterTurns: Int) {
        val currentOrientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val rotatedOrientation = rotateExifOrientation(currentOrientation, quarterTurns)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, rotatedOrientation.toString())
        exif.saveAttributes()
    }

    private fun timestamp(): String = SimpleDateFormat(
        "yyyyMMdd_HHmmss_SSS",
        Locale.US,
    ).format(Date())
}

internal fun rotateExifOrientation(orientation: Int, quarterTurns: Int): Int {
    var result = when (orientation) {
        in ExifInterface.ORIENTATION_NORMAL..ExifInterface.ORIENTATION_ROTATE_270 -> orientation
        else -> ExifInterface.ORIENTATION_NORMAL
    }
    repeat(CameraRotation.normalize(quarterTurns)) {
        result = when (result) {
            ExifInterface.ORIENTATION_NORMAL -> ExifInterface.ORIENTATION_ROTATE_90
            ExifInterface.ORIENTATION_ROTATE_90 -> ExifInterface.ORIENTATION_ROTATE_180
            ExifInterface.ORIENTATION_ROTATE_180 -> ExifInterface.ORIENTATION_ROTATE_270
            ExifInterface.ORIENTATION_ROTATE_270 -> ExifInterface.ORIENTATION_NORMAL
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> ExifInterface.ORIENTATION_TRANSPOSE
            ExifInterface.ORIENTATION_TRANSPOSE -> ExifInterface.ORIENTATION_FLIP_VERTICAL
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> ExifInterface.ORIENTATION_TRANSVERSE
            ExifInterface.ORIENTATION_TRANSVERSE -> ExifInterface.ORIENTATION_FLIP_HORIZONTAL
            else -> ExifInterface.ORIENTATION_ROTATE_90
        }
    }
    return result
}

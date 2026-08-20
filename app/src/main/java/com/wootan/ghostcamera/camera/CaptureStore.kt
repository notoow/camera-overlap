package com.wootan.ghostcamera.camera

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
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

    private fun timestamp(): String = SimpleDateFormat(
        "yyyyMMdd_HHmmss_SSS",
        Locale.US,
    ).format(Date())
}

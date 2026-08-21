package com.wootan.ghostcamera.camera

import android.content.ContentValues
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

object ComparisonStore {
    private const val OUTPUT_WIDTH = 2400
    private const val OUTPUT_HEIGHT = 1600
    private const val PANEL_WIDTH = OUTPUT_WIDTH / 2
    private const val JPEG_QUALITY = 94

    fun save(
        context: Context,
        beforeFile: File,
        afterUri: Uri,
        beforeRotationQuarterTurns: Int,
    ): Uri {
        val beforeBitmap = decodeForPanel(
            context = context,
            uri = Uri.fromFile(beforeFile),
            additionalRotationQuarterTurns = beforeRotationQuarterTurns,
        )
        try {
            val afterBitmap = decodeForPanel(
                context = context,
                uri = afterUri,
                additionalRotationQuarterTurns = 0,
            )
            try {
                val collage = Bitmap.createBitmap(
                    OUTPUT_WIDTH,
                    OUTPUT_HEIGHT,
                    Bitmap.Config.ARGB_8888,
                )
                try {
                    val canvas = Canvas(collage)
                    canvas.drawColor(Color.rgb(17, 23, 25))
                    drawPanel(canvas, beforeBitmap, panelIndex = 0, label = "BEFORE")
                    drawPanel(canvas, afterBitmap, panelIndex = 1, label = "AFTER")
                    canvas.drawRect(
                        PANEL_WIDTH - 2f,
                        0f,
                        PANEL_WIDTH + 2f,
                        OUTPUT_HEIGHT.toFloat(),
                        Paint().apply { color = Color.WHITE },
                    )
                    return writeToGallery(context, collage)
                } finally {
                    collage.recycle()
                }
            } finally {
                afterBitmap.recycle()
            }
        } finally {
            beforeBitmap.recycle()
        }
    }

    private fun drawPanel(
        canvas: Canvas,
        bitmap: Bitmap,
        panelIndex: Int,
        label: String,
    ) {
        val panelLeft = panelIndex * PANEL_WIDTH
        val target = fitRect(
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height,
            targetLeft = panelLeft,
            targetTop = 0,
            targetWidth = PANEL_WIDTH,
            targetHeight = OUTPUT_HEIGHT,
        )
        val destination = Rect(target.left, target.top, target.right, target.bottom)
        canvas.drawBitmap(
            bitmap,
            null,
            destination,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
        drawLabel(canvas, panelLeft, label)
    }

    private fun drawLabel(canvas: Canvas, panelLeft: Int, label: String) {
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(205, 0, 0, 0)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textWidth = textPaint.measureText(label)
        val left = panelLeft + 38f
        val top = 38f
        canvas.drawRoundRect(
            left,
            top,
            left + textWidth + 48f,
            top + 86f,
            8f,
            8f,
            backgroundPaint,
        )
        canvas.drawText(label, left + 24f, top + 61f, textPaint)
    }

    private fun decodeForPanel(
        context: Context,
        uri: Uri,
        additionalRotationQuarterTurns: Int,
    ): Bitmap {
        val normalizedRotation = CameraRotation.normalize(additionalRotationQuarterTurns)
        val decodeBounds = preRotationDecodeBounds(
            targetWidth = PANEL_WIDTH,
            targetHeight = OUTPUT_HEIGHT,
            rotationQuarterTurns = normalizedRotation,
        )
        val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(context, uri, decodeBounds.width, decodeBounds.height)
        } else {
            decodeLegacy(context, uri, decodeBounds.width, decodeBounds.height)
        }
        return try {
            applyQuarterTurn(decoded, normalizedRotation)
        } catch (error: Throwable) {
            decoded.recycle()
            throw error
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(
        context: Context,
        uri: Uri,
        maxWidth: Int,
        maxHeight: Int,
    ): Bitmap {
        val source = if (uri.scheme == ContentResolver.SCHEME_FILE) {
            ImageDecoder.createSource(File(requireNotNull(uri.path)))
        } else {
            ImageDecoder.createSource(context.contentResolver, uri)
        }
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val target = boundedSize(
                info.size.width,
                info.size.height,
                maxWidth,
                maxHeight,
            )
            decoder.setTargetSize(target.width, target.height)
        }
    }

    private fun decodeLegacy(
        context: Context,
        uri: Uri,
        maxWidth: Int,
        maxHeight: Int,
    ): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: error("Unable to open comparison image")
        check(bounds.outWidth > 0 && bounds.outHeight > 0) {
            "Unable to read comparison image dimensions"
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(
                bounds.outWidth,
                bounds.outHeight,
                maxWidth,
                maxHeight,
            )
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: error("Unable to decode comparison image")

        val orientation = context.contentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
        val oriented = applyExifOrientation(decoded, orientation)
        val target = boundedSize(
            oriented.width,
            oriented.height,
            maxWidth,
            maxHeight,
        )
        if (target.width == oriented.width && target.height == oriented.height) return oriented

        val scaled = Bitmap.createScaledBitmap(oriented, target.width, target.height, true)
        if (scaled !== oriented) oriented.recycle()
        return scaled
    }

    private fun applyQuarterTurn(bitmap: Bitmap, quarterTurns: Int): Bitmap {
        val normalizedRotation = CameraRotation.normalize(quarterTurns)
        if (normalizedRotation == 0) return bitmap

        val rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            Matrix().apply {
                setRotate(CameraRotation.degrees(normalizedRotation).toFloat())
            },
            true,
        )
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    setRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    setRotate(-90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        if (matrix.isIdentity) return bitmap

        val oriented = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true,
        )
        if (oriented !== bitmap) bitmap.recycle()
        return oriented
    }

    private fun calculateSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        maxWidth: Int,
        maxHeight: Int,
    ): Int {
        val target = boundedSize(sourceWidth, sourceHeight, maxWidth, maxHeight)
        var sampleSize = 1
        while (
            sourceWidth / (sampleSize * 2) >= target.width &&
            sourceHeight / (sampleSize * 2) >= target.height
        ) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun writeToGallery(context: Context, bitmap: Bitmap): Uri {
        val fileName = "BA_${timestamp()}.jpg"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeToMediaStore(context, bitmap, fileName)
        } else {
            writeToExternalFiles(context, bitmap, fileName)
        }
    }

    private fun writeToMediaStore(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
    ): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/GhostCamera",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create comparison image")
        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    "Unable to encode comparison image"
                }
            } ?: error("Unable to write comparison image")
            check(
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null,
                ) == 1,
            ) { "Unable to publish comparison image" }
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun writeToExternalFiles(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
    ): Uri {
        val picturesDirectory = requireNotNull(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        ) { "External pictures directory is unavailable" }
        val directory = File(picturesDirectory, "GhostCamera").apply { mkdirs() }
        val file = File(directory, fileName)
        try {
            file.outputStream().buffered().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    "Unable to encode comparison image"
                }
            }
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
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

internal data class PixelSize(val width: Int, val height: Int)

internal data class PixelRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

internal fun preRotationDecodeBounds(
    targetWidth: Int,
    targetHeight: Int,
    rotationQuarterTurns: Int,
): PixelSize {
    require(targetWidth > 0 && targetHeight > 0)
    return if (CameraRotation.normalize(rotationQuarterTurns) % 2 == 1) {
        PixelSize(width = targetHeight, height = targetWidth)
    } else {
        PixelSize(width = targetWidth, height = targetHeight)
    }
}

internal fun boundedSize(
    sourceWidth: Int,
    sourceHeight: Int,
    maxWidth: Int,
    maxHeight: Int,
): PixelSize {
    require(sourceWidth > 0 && sourceHeight > 0)
    require(maxWidth > 0 && maxHeight > 0)
    val scale = min(
        1f,
        min(maxWidth.toFloat() / sourceWidth, maxHeight.toFloat() / sourceHeight),
    )
    return PixelSize(
        width = (sourceWidth * scale).roundToInt().coerceAtLeast(1),
        height = (sourceHeight * scale).roundToInt().coerceAtLeast(1),
    )
}

internal fun fitRect(
    sourceWidth: Int,
    sourceHeight: Int,
    targetLeft: Int,
    targetTop: Int,
    targetWidth: Int,
    targetHeight: Int,
): PixelRect {
    require(sourceWidth > 0 && sourceHeight > 0)
    require(targetWidth > 0 && targetHeight > 0)
    val scale = min(
        targetWidth.toFloat() / sourceWidth,
        targetHeight.toFloat() / sourceHeight,
    )
    val width = (sourceWidth * scale).roundToInt().coerceAtLeast(1)
    val height = (sourceHeight * scale).roundToInt().coerceAtLeast(1)
    val left = targetLeft + (targetWidth - width) / 2
    val top = targetTop + (targetHeight - height) / 2
    return PixelRect(left, top, left + width, top + height)
}

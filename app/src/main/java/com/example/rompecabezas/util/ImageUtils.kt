package com.example.rompecabezas.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import com.example.rompecabezas.data.model.PuzzlePiece
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageUtils {

    private const val MAX_IMAGE_DIMENSION = 1200

    /**
     * Loads a Bitmap from a content Uri, adjusting for EXIF rotation and scaling down
     * to a maximum dimension to prevent OOM and maximize rendering speed.
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            // First, decode bounds to calculate sample size
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val rawWidth = options.outWidth
            val rawHeight = options.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) return null

            var inSampleSize = 1
            val maxDim = max(rawWidth, rawHeight)
            if (maxDim > MAX_IMAGE_DIMENSION) {
                inSampleSize = (maxDim / MAX_IMAGE_DIMENSION.toFloat()).toInt().coerceAtLeast(1)
            }

            // Decode the actual downsampled bitmap
            inputStream = context.contentResolver.openInputStream(uri)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            var bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (bitmap == null) return null

            // Check EXIF orientation
            try {
                context.contentResolver.openInputStream(uri)?.use { exifStream ->
                    val exif = ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    bitmap = rotateBitmapIfNeeded(bitmap!!, orientation)
                }
            } catch (_: Exception) {
                // Ignore EXIF errors if not available
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads a Bitmap from an internal storage file path.
     */
    fun loadBitmapFromFile(filePath: String): Bitmap? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            BitmapFactory.decodeFile(filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves a Bitmap as a JPEG into the app's internal files directory.
     */
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, fileName: String): File? {
        return try {
            val directory = File(context.filesDir, "puzzles")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Slices the source bitmap into a grid of rows x cols pieces.
     */
    fun sliceBitmap(source: Bitmap, rows: Int, cols: Int): List<PuzzlePiece> {
        val pieces = mutableListOf<PuzzlePiece>()
        if (rows <= 0 || cols <= 0) return pieces

        // Ensure dimensions are cleanly divisible
        val usableWidth = source.width - (source.width % cols)
        val usableHeight = source.height - (source.height % rows)
        val pieceWidth = usableWidth / cols
        val pieceHeight = usableHeight / rows

        var idCounter = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val startX = c * pieceWidth
                val startY = r * pieceHeight
                val pieceBitmap = Bitmap.createBitmap(
                    source,
                    startX,
                    startY,
                    pieceWidth,
                    pieceHeight
                )
                pieces.add(
                    PuzzlePiece(
                        id = idCounter++,
                        row = r,
                        col = c,
                        bitmap = pieceBitmap,
                        isPlaced = false
                    )
                )
            }
        }
        return pieces
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }
}

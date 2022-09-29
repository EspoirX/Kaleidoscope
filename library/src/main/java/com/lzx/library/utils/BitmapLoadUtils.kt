package com.lzx.library.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Point
import android.net.Uri
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.exifinterface.media.ExifInterface
import com.lzx.library.utils.FileUtils.safeClose
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.ceil

object BitmapLoadUtils {

    private const val MAX_BITMAP_SIZE = 100 * 1024 * 1024 // 100 MB

    @JvmStatic
    fun calculateMaxBitmapSize(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        val display: Display
        val width: Int
        val height: Int
        val size = Point()
        if (wm != null) {
            display = wm.defaultDisplay
            display.getSize(size)
        }
        width = size.x
        height = size.y

        // Twice the device screen diagonal as default
        var maxBitmapSize =
            Math.sqrt(Math.pow(width.toDouble(), 2.0) + Math.pow(height.toDouble(), 2.0)).toInt()

        // Check for max texture size via Canvas
        val canvas = Canvas()
        val maxCanvasSize = canvas.maximumBitmapWidth.coerceAtMost(canvas.maximumBitmapHeight)
        if (maxCanvasSize > 0) {
            maxBitmapSize = maxBitmapSize.coerceAtMost(maxCanvasSize)
        }

        // Check for max texture size via GL
        val maxTextureSize = EglUtils.getMaxTextureSize()
        if (maxTextureSize > 0) {
            maxBitmapSize = maxBitmapSize.coerceAtMost(maxTextureSize)
        }
        Log.d("BitmapLoadUtils", "maxBitmapSize: $maxBitmapSize")
        return maxBitmapSize
    }

    /**
     * 计算图片合适压缩比较
     *
     * @param srcWidth  src width
     * @param srcHeight src height
     * @return
     */
    fun computeSize(_srcWidth: Int, _srcHeight: Int): Int {
        var srcWidth = _srcWidth
        var srcHeight = _srcHeight
        srcWidth = if (srcWidth % 2 == 1) srcWidth + 1 else srcWidth
        srcHeight = if (srcHeight % 2 == 1) srcHeight + 1 else srcHeight
        val longSide = Math.max(srcWidth, srcHeight)
        val shortSide = Math.min(srcWidth, srcHeight)
        val scale = shortSide.toFloat() / longSide
        return if (scale <= 1 && scale > 0.5625) {
            if (longSide < 1664) {
                1
            } else if (longSide < 4990) {
                2
            } else if (longSide in 4991..10239) {
                4
            } else {
                longSide / 1280
            }
        } else if (scale <= 0.5625 && scale > 0.5) {
            if (longSide / 1280 == 0) 1 else longSide / 1280
        } else {
            ceil(longSide / (1280.0 / scale)).toInt()
        }
    }

    /**
     * 判断拍照 图片是否旋转
     *
     * @param context
     * @param path    资源路径
     */
    fun rotateImage(context: Context?, path: String) {
        if (context == null) return
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        var bitmap: Bitmap? = null
        try {
            val degree: Int = readPictureDegree(context, path)
            if (degree > 0) {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                if (FileUtils.isContent(path)) {
                    inputStream = context.contentResolver.openInputStream(Uri.parse(path))
                    BitmapFactory.decodeStream(inputStream, null, options)
                } else {
                    BitmapFactory.decodeFile(path, options)
                }
                options.inSampleSize = computeSize(options.outWidth, options.outHeight)
                options.inJustDecodeBounds = false
                if (FileUtils.isContent(path)) {
                    inputStream = context.contentResolver.openInputStream(Uri.parse(path))
                    bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                } else {
                    bitmap = BitmapFactory.decodeFile(path, options)
                }
                if (bitmap != null) {
                    bitmap = rotatingImage(bitmap, degree)
                    outputStream = if (FileUtils.isContent(path)) {
                        context.contentResolver.openOutputStream(Uri.parse(path)) as FileOutputStream?
                    } else {
                        FileOutputStream(path)
                    }
                    saveBitmapFile(bitmap, outputStream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream.safeClose()
            outputStream.safeClose()
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    /**
     * 读取图片属性：旋转的角度
     *
     * @param context
     * @param filePath 图片绝对路径
     * @return degree旋转的角度
     */
    fun readPictureDegree(context: Context, filePath: String?): Int {
        val exifInterface: ExifInterface
        var inputStream: InputStream? = null
        return try {
            if (FileUtils.isContent(filePath)) {
                inputStream = context.contentResolver.openInputStream(Uri.parse(filePath))
                if (inputStream == null) {
                    return 0
                }
                exifInterface = ExifInterface(inputStream)
            } else {
                if (filePath.isNullOrEmpty()) {
                    return 0
                }
                exifInterface = ExifInterface(filePath)
            }
            when (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        } finally {
            inputStream.safeClose()
        }
    }

    /**
     * 旋转Bitmap
     *
     * @param bitmap
     * @param angle
     * @return
     */
    fun rotatingImage(bitmap: Bitmap, angle: Int): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 保存Bitmap至本地
     *
     * @param bitmap
     * @param fos
     */
    private fun saveBitmapFile(bitmap: Bitmap?, fos: FileOutputStream?) {
        if (bitmap == null) return
        if (fos == null) return
        var stream: ByteArrayOutputStream? = null
        try {
            stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, fos)
            fos.write(stream.toByteArray())
            fos.flush()
            fos.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            fos.safeClose()
            stream.safeClose()
        }
    }
}
package com.lzx.library.crop

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lzx.library.crop.callback.BitmapCropCallback
import com.lzx.library.utils.FileUtils
import com.lzx.library.utils.FileUtils.safeClose
import com.lzx.library.utils.getUriPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 裁剪
 */
class BitmapCropTask(
    private var mViewBitmap: Bitmap?,
    private val mCropRect: RectF,
    private val mCurrentImageRect: RectF,
    private var mCurrentScale: Float,
    private val mCurrentAngle: Float,
    private val mMaxResultImageSizeX: Int,
    private val mMaxResultImageSizeY: Int,
    private val mCompressFormat: Bitmap.CompressFormat,
    private val mCompressQuality: Int,
    private val mImageInputPath: String?,
    private var mImageOutputPath: String?,
    private val mImageInputUri: Uri?,
    private var mImageOutputUri: Uri?,
    private val mCropCallback: BitmapCropCallback?
) {

    private var mContext: WeakReference<AppCompatActivity>? = null
    private var cropOffsetX: Int = 0
    private var cropOffsetY: Int = 0
    private var mCroppedImageWidth: Int = 0
    private var mCroppedImageHeight: Int = 0

    fun execute(activity: AppCompatActivity) {
        mContext = WeakReference(activity)
        if (mViewBitmap == null) {
            mCropCallback?.onCropFailure(NullPointerException("ViewBitmap is null"))
            return
        }
        if (mViewBitmap?.isRecycled == true) {
            mCropCallback?.onCropFailure(NullPointerException("ViewBitmap is recycled"))
            return
        }
        if (mCurrentImageRect.isEmpty) {
            mCropCallback?.onCropFailure(NullPointerException("CurrentImageRect is empty"))
            return
        }
        if (mImageOutputUri == null || mImageOutputPath == null) {
            val outputCropPath = FileUtils.getImageSandboxPath(activity)
            val fileName = FileUtils.getCreateFileName("CROP_") + ".jpeg"
            mImageOutputUri = Uri.fromFile(File(outputCropPath, fileName))
            mImageOutputPath = mImageOutputUri!!.getUriPath()
        }
        activity.lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                if (crop()) {
                    val uri = if (hasContentScheme(mImageOutputUri)) {
                        mImageOutputUri!!
                    } else {
                        Uri.fromFile(File(mImageOutputPath!!))
                    }
                    mCropCallback?.onBitmapCropped(
                        uri,
                        mImageOutputPath,
                        cropOffsetX,
                        cropOffsetY,
                        mCroppedImageWidth,
                        mCroppedImageHeight
                    )
                }
                mViewBitmap = null
            }
        }
    }

    private fun crop(): Boolean {
        val context = mContext?.get() ?: return false
        // Downsize if needed
        if (mMaxResultImageSizeX > 0 && mMaxResultImageSizeY > 0) {
            val cropWidth = mCropRect.width() / mCurrentScale
            val cropHeight = mCropRect.height() / mCurrentScale
            if (cropWidth > mMaxResultImageSizeX || cropHeight > mMaxResultImageSizeY) {
                val scaleX = mMaxResultImageSizeX / cropWidth
                val scaleY = mMaxResultImageSizeY / cropHeight
                val resizeScale = Math.min(scaleX, scaleY)
                val resizedBitmap = Bitmap.createScaledBitmap(
                    mViewBitmap!!,
                    (mViewBitmap!!.width * resizeScale).roundToInt(),
                    (mViewBitmap!!.height * resizeScale).roundToInt(), false
                )
                if (mViewBitmap != resizedBitmap) {
                    mViewBitmap!!.recycle()
                }
                mViewBitmap = resizedBitmap
                mCurrentScale /= resizeScale
            }
        }
        // Rotate if needed
        if (mCurrentAngle != 0f) {
            val tempMatrix = Matrix()
            tempMatrix.setRotate(
                mCurrentAngle,
                (mViewBitmap!!.width / 2).toFloat(),
                (mViewBitmap!!.height / 2).toFloat()
            )
            val rotatedBitmap = Bitmap.createBitmap(
                mViewBitmap!!, 0, 0, mViewBitmap!!.width, mViewBitmap!!.height,
                tempMatrix, true
            )
            if (mViewBitmap != rotatedBitmap) {
                mViewBitmap!!.recycle()
            }
            mViewBitmap = rotatedBitmap
        }

        cropOffsetX = ((mCropRect.left - mCurrentImageRect.left) / mCurrentScale).roundToInt()
        cropOffsetY = ((mCropRect.top - mCurrentImageRect.top) / mCurrentScale).roundToInt()
        mCroppedImageWidth = (mCropRect.width() / mCurrentScale).roundToInt()
        mCroppedImageHeight = (mCropRect.height() / mCurrentScale).roundToInt()
        val shouldCrop: Boolean = shouldCrop(mCroppedImageWidth, mCroppedImageHeight)
        if (shouldCrop) {
            checkValidityCropBounds()
            saveImage(
                Bitmap.createBitmap(
                    mViewBitmap!!,
                    cropOffsetX,
                    cropOffsetY,
                    mCroppedImageWidth,
                    mCroppedImageHeight
                )
            )
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && FileUtils.isContent(mImageInputPath)) {
                val inputStream = context.contentResolver.openInputStream(Uri.parse(mImageInputPath))
                FileUtils.writeFileFromIS(inputStream, FileOutputStream(mImageOutputPath))
            } else {
                FileUtils.copyFile(mImageInputPath!!, mImageOutputPath!!)
            }
        }
        return true
    }

    private fun shouldCrop(width: Int, height: Int): Boolean {
        var pixelError = 1
        pixelError += (width.coerceAtLeast(height) / 1000f).roundToInt()
        return (mMaxResultImageSizeX > 0 && mMaxResultImageSizeY > 0
            || abs(mCropRect.left - mCurrentImageRect.left) > pixelError
            || abs(mCropRect.top - mCurrentImageRect.top) > pixelError
            || abs(
            mCropRect.bottom - mCurrentImageRect.bottom
        ) > pixelError
            || abs(mCropRect.right - mCurrentImageRect.right) > pixelError
            || mCurrentAngle != 0f)
    }

    private fun checkValidityCropBounds() {
        if (cropOffsetX < 0) {
            cropOffsetX = 0
            mCroppedImageWidth = mViewBitmap!!.width
        }
        if (cropOffsetY < 0) {
            cropOffsetY = 0
            mCroppedImageHeight = mViewBitmap!!.height
        }
    }

    private fun saveImage(croppedBitmap: Bitmap): Boolean {
        val context = mContext?.get() ?: return false
        var outputStream: OutputStream? = null
        var outStream: ByteArrayOutputStream? = null
        try {
            outputStream = context.contentResolver.openOutputStream(mImageOutputUri!!)
            outStream = ByteArrayOutputStream()
            croppedBitmap.compress(mCompressFormat, mCompressQuality, outStream)
            outputStream?.write(outStream.toByteArray())
            croppedBitmap.recycle()
        } catch (exc: IOException) {
            exc.printStackTrace()
            context.runOnUiThread {
                mCropCallback?.onCropFailure(exc)
            }
            return false
        } finally {
            outputStream?.safeClose()
            outStream?.safeClose()
        }
        return true
    }

    private fun hasContentScheme(uri: Uri?): Boolean {
        return uri != null && "content" == uri.scheme
    }
}
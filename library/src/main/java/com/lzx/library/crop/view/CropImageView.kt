package com.lzx.library.crop.view

import android.content.Context
import android.graphics.Bitmap.CompressFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import androidx.annotation.IntRange
import androidx.appcompat.app.AppCompatActivity
import com.lzx.library.crop.BitmapCropTask
import com.lzx.library.crop.callback.BitmapCropCallback
import com.lzx.library.crop.callback.CropBoundsChangeListener
import com.lzx.library.crop.utils.CubicEasing
import com.lzx.library.crop.utils.RectUtils
import java.lang.ref.WeakReference
import java.util.Arrays

open class CropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TransformImageView(context, attrs, defStyle) {

    private val mCropRect = RectF()
    private val mTempMatrix = Matrix()
    private var mTargetAspectRatio = 0f
    private var mMaxScaleMultiplier = 10.0f
    var cropBoundsChangeListener: CropBoundsChangeListener? = null
    private var mWrapCropBoundsRunnable: Runnable? = null
    private var mZoomImageToPositionRunnable: Runnable? = null

    /**
     * 当前图像和裁剪比例的最大比例值
     */
    var maxScale = 0f

    /**
     * 当前图像和裁剪比例的最小比例值
     */
    var minScale = 0f
    private var mMaxResultImageSizeX = 0
    private var mMaxResultImageSizeY = 0
    private var mImageToWrapCropBoundsAnimDuration: Long = 500

    fun cropAndSaveImage(
        compressFormat: CompressFormat, compressQuality: Int,
        cropCallback: BitmapCropCallback?
    ) {
        cancelAllAnimations()
        setImageToWrapCropBounds(false)
        if (context != null && context is AppCompatActivity) {
            BitmapCropTask(
                viewBitmap,
                mCropRect,
                RectUtils.trapToRect(mCurrentImageCorners),
                currentScale,
                currentAngle,
                mMaxResultImageSizeX,
                mMaxResultImageSizeY,
                compressFormat,
                compressQuality,
                imageInputPath,
                imageOutputPath,
                imageInputUri,
                imageOutputUri,
                cropCallback
            ).execute(context as AppCompatActivity)
        }
    }

    /**
     * 裁剪边界的纵横比
     */
    var targetAspectRatio: Float
        get() = mTargetAspectRatio
        set(targetAspectRatio) {
            val drawable = drawable
            if (drawable == null) {
                mTargetAspectRatio = targetAspectRatio
                return
            }
            mTargetAspectRatio = if (targetAspectRatio == 0f) {
                drawable.intrinsicWidth / drawable.intrinsicHeight.toFloat()
            } else {
                targetAspectRatio
            }
            cropBoundsChangeListener?.onCropAspectRatioChanged(mTargetAspectRatio)
        }

    /**
     * 更新裁剪范围
     */
    fun setCropRect(cropRect: RectF) {
        mTargetAspectRatio = cropRect.width() / cropRect.height()
        mCropRect.set(
            cropRect.left - paddingLeft,
            cropRect.top - paddingTop,
            cropRect.right - paddingRight,
            cropRect.bottom - paddingBottom
        )
        calculateImageScaleBounds()
        setImageToWrapCropBounds()
    }

    /**
     * 设置生成的裁剪图像的最大宽度
     */
    fun setMaxResultImageSizeX(@IntRange(from = 10) maxResultImageSizeX: Int) {
        mMaxResultImageSizeX = maxResultImageSizeX
    }

    /**
     * 设置生成的裁剪图像的最大宽度
     */
    fun setMaxResultImageSizeY(@IntRange(from = 10) maxResultImageSizeY: Int) {
        mMaxResultImageSizeY = maxResultImageSizeY
    }

    /**
     * 平移缩放动画时间
     */
    fun setImageToWrapCropBoundsAnimDuration(@IntRange(from = 100) duration: Long) {
        if (duration > 0) {
            mImageToWrapCropBoundsAnimDuration = duration
        }
    }

    /**
     * 设置乘数，用于从最小图像比例计算最大图像比例。.
     * (minScale * maxScaleMultiplier) = maxScale
     */
    fun setMaxScaleMultiplier(maxScaleMultiplier: Float) {
        mMaxScaleMultiplier = maxScaleMultiplier
    }

    /**
     * 基于图像中心缩小图像。
     */
    fun zoomOutImage(deltaScale: Float) {
        zoomOutImage(deltaScale, mCropRect.centerX(), mCropRect.centerY())
    }

    /**
     * 基于x,y坐标缩小图像。
     */
    fun zoomOutImage(scale: Float, centerX: Float, centerY: Float) {
        if (scale >= minScale) {
            postScale(scale / currentScale, centerX, centerY)
        }
    }

    /**
     * 基于图像中心放大图像。
     */
    fun zoomInImage(deltaScale: Float) {
        zoomInImage(deltaScale, mCropRect.centerX(), mCropRect.centerY())
    }

    /**
     * 基于x,y坐标放大图像。
     */
    fun zoomInImage(scale: Float, centerX: Float, centerY: Float) {
        if (scale <= maxScale) {
            postScale(scale / currentScale, centerX, centerY)
        }
    }

    /**
     * 缩小/放大图像实现
     */
    override fun postScale(deltaScale: Float, px: Float, py: Float) {
        if (deltaScale > 1 && currentScale * deltaScale <= maxScale) {
            super.postScale(deltaScale, px, py)
        } else if (deltaScale < 1 && currentScale * deltaScale >= minScale) {
            super.postScale(deltaScale, px, py)
        }
    }

    /**
     * 基于图像中心旋转图像。
     */
    fun postRotate(deltaAngle: Float) {
        postRotate(deltaAngle, mCropRect.centerX(), mCropRect.centerY())
    }

    fun cancelAllAnimations() {
        removeCallbacks(mWrapCropBoundsRunnable)
        removeCallbacks(mZoomImageToPositionRunnable)
    }

    fun setImageToWrapCropBounds() {
        setImageToWrapCropBounds(true)
    }

    /**
     * 如果图像没有填充裁剪边界，则必须正确平移和缩放以填充它们。
     */
    fun setImageToWrapCropBounds(animate: Boolean) {
        if (mBitmapLaidOut && !isImageWrapCropBounds) {
            val currentX = mCurrentImageCenter[0]
            val currentY = mCurrentImageCenter[1]
            val currentScale = currentScale
            var deltaX = mCropRect.centerX() - currentX
            var deltaY = mCropRect.centerY() - currentY
            var deltaScale = 0f
            mTempMatrix.reset()
            mTempMatrix.setTranslate(deltaX, deltaY)
            val tempCurrentImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.size)
            mTempMatrix.mapPoints(tempCurrentImageCorners)
            val willImageWrapCropBoundsAfterTranslate = isImageWrapCropBounds(tempCurrentImageCorners)
            if (willImageWrapCropBoundsAfterTranslate) {
                val imageIndents = calculateImageIndents()
                deltaX = -(imageIndents[0] + imageIndents[2])
                deltaY = -(imageIndents[1] + imageIndents[3])
            } else {
                val tempCropRect = RectF(mCropRect)
                mTempMatrix.reset()
                mTempMatrix.setRotate(currentAngle)
                mTempMatrix.mapRect(tempCropRect)
                val currentImageSides = RectUtils.getRectSidesFromCorners(mCurrentImageCorners)
                deltaScale = Math.max(
                    tempCropRect.width() / currentImageSides[0],
                    tempCropRect.height() / currentImageSides[1]
                )
                deltaScale = deltaScale * currentScale - currentScale
            }
            if (animate) {
                post(WrapCropBoundsRunnable(
                    this@CropImageView, mImageToWrapCropBoundsAnimDuration, currentX, currentY, deltaX, deltaY,
                    currentScale, deltaScale, willImageWrapCropBoundsAfterTranslate
                ).also { mWrapCropBoundsRunnable = it })
            } else {
                postTranslate(deltaX, deltaY)
                if (!willImageWrapCropBoundsAfterTranslate) {
                    zoomInImage(currentScale + deltaScale, mCropRect.centerX(), mCropRect.centerY())
                }
            }
        }
    }

    /**
     * 首先，取消旋转图像并裁剪矩形（使图像矩形轴对齐）。
     * 其次，计算这些矩形边之间的增量。
     * 第三，根据 delta（其符号）将它们或零放入数组中。
     * 第四，使用 Matrix，将这些点（缩进）向后旋转。
     *
     * @return - 图片缩进的浮点数组（4 个浮点数） - 按此顺序 [左、上、右、下]
     */
    private fun calculateImageIndents(): FloatArray {
        mTempMatrix.reset()
        mTempMatrix.setRotate(-currentAngle)
        val unrotatedImageCorners = Arrays.copyOf(mCurrentImageCorners, mCurrentImageCorners.size)
        val unrotatedCropBoundsCorners = RectUtils.getCornersFromRect(mCropRect)
        mTempMatrix.mapPoints(unrotatedImageCorners)
        mTempMatrix.mapPoints(unrotatedCropBoundsCorners)
        val unrotatedImageRect = RectUtils.trapToRect(unrotatedImageCorners)
        val unrotatedCropRect = RectUtils.trapToRect(unrotatedCropBoundsCorners)
        val deltaLeft = unrotatedImageRect.left - unrotatedCropRect.left
        val deltaTop = unrotatedImageRect.top - unrotatedCropRect.top
        val deltaRight = unrotatedImageRect.right - unrotatedCropRect.right
        val deltaBottom = unrotatedImageRect.bottom - unrotatedCropRect.bottom
        val indents = FloatArray(4)
        indents[0] = if (deltaLeft > 0) deltaLeft else 0f
        indents[1] = if (deltaTop > 0) deltaTop else 0f
        indents[2] = if (deltaRight < 0) deltaRight else 0f
        indents[3] = if (deltaBottom < 0) deltaBottom else 0f
        mTempMatrix.reset()
        mTempMatrix.setRotate(currentAngle)
        mTempMatrix.mapPoints(indents)
        return indents
    }

    /**
     * 布局图像时，它必须正确居中以适合当前裁剪范围。
     */
    override fun onImageLaidOut() {
        super.onImageLaidOut()
        val drawable = drawable ?: return
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()
        if (mTargetAspectRatio == 0f) {
            mTargetAspectRatio = drawableWidth / drawableHeight
        }
        val height = (mThisWidth / mTargetAspectRatio).toInt()
        if (height > mThisHeight) {
            val width = (mThisHeight * mTargetAspectRatio).toInt()
            val halfDiff = (mThisWidth - width) / 2
            mCropRect[halfDiff.toFloat(), 0f, (width + halfDiff).toFloat()] = mThisHeight.toFloat()
        } else {
            val halfDiff = (mThisHeight - height) / 2
            mCropRect[0f, halfDiff.toFloat(), mThisWidth.toFloat()] = (height + halfDiff).toFloat()
        }
        calculateImageScaleBounds(drawableWidth, drawableHeight)
        setupInitialImagePosition(drawableWidth, drawableHeight)
        cropBoundsChangeListener?.onCropAspectRatioChanged(mTargetAspectRatio)
        mTransformImageListener?.onScale(currentScale)
        mTransformImageListener?.onRotate(currentAngle)
    }

    /**
     * 检查当前图像是否填充裁剪边界。
     */
    protected val isImageWrapCropBounds: Boolean
        get() = isImageWrapCropBounds(mCurrentImageCorners)

    /**
     * 检查当前图像是否填充裁剪边界实现
     */
    fun isImageWrapCropBounds(imageCorners: FloatArray): Boolean {
        mTempMatrix.reset()
        mTempMatrix.setRotate(-currentAngle)
        val unrotatedImageCorners = Arrays.copyOf(imageCorners, imageCorners.size)
        mTempMatrix.mapPoints(unrotatedImageCorners)
        val unrotatedCropBoundsCorners = RectUtils.getCornersFromRect(mCropRect)
        mTempMatrix.mapPoints(unrotatedCropBoundsCorners)
        return RectUtils.trapToRect(unrotatedImageCorners).contains(RectUtils.trapToRect(unrotatedCropBoundsCorners))
    }

    /**
     * 持续缩放
     */
    protected fun zoomImageToPosition(_scale: Float, centerX: Float, centerY: Float, durationMs: Long) {
        var scale = _scale
        if (scale > maxScale) {
            scale = maxScale
        }
        val oldScale = currentScale
        val deltaScale = scale - oldScale
        post(ZoomImageToPosition(
            this@CropImageView,
            durationMs, oldScale, deltaScale, centerX, centerY
        ).also { mZoomImageToPositionRunnable = it })
    }

    private fun calculateImageScaleBounds() {
        val drawable = drawable ?: return
        calculateImageScaleBounds(drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
    }

    /**
     * 此方法计算当前图像最小和最大比例值。
     */
    private fun calculateImageScaleBounds(drawableWidth: Float, drawableHeight: Float) {
        val widthScale = Math.min(mCropRect.width() / drawableWidth, mCropRect.width() / drawableHeight)
        val heightScale = Math.min(mCropRect.height() / drawableHeight, mCropRect.height() / drawableWidth)
        minScale = Math.min(widthScale, heightScale)
        maxScale = minScale * mMaxScaleMultiplier
    }

    /**
     * 此方法计算初始图像位置，以便正确定位。
     * 然后它将这些值设置为当前图像矩阵。
     */
    private fun setupInitialImagePosition(drawableWidth: Float, drawableHeight: Float) {
        val cropRectWidth = mCropRect.width()
        val cropRectHeight = mCropRect.height()
        val widthScale = mCropRect.width() / drawableWidth
        val heightScale = mCropRect.height() / drawableHeight
        val initialMinScale = Math.max(widthScale, heightScale)
        val tw = (cropRectWidth - drawableWidth * initialMinScale) / 2.0f + mCropRect.left
        val th = (cropRectHeight - drawableHeight * initialMinScale) / 2.0f + mCropRect.top
        mCurrentImageMatrix.reset()
        mCurrentImageMatrix.postScale(initialMinScale, initialMinScale)
        mCurrentImageMatrix.postTranslate(tw, th)
        imageMatrix = mCurrentImageMatrix
    }

    private class WrapCropBoundsRunnable(
        cropImageView: CropImageView,
        durationMs: Long,
        oldX: Float, oldY: Float,
        centerDiffX: Float, centerDiffY: Float,
        oldScale: Float, deltaScale: Float,
        willBeImageInBoundsAfterTranslate: Boolean
    ) : Runnable {
        private val mCropImageView: WeakReference<CropImageView>
        private val mDurationMs: Long
        private val mStartTime: Long
        private val mOldX: Float
        private val mOldY: Float
        private val mCenterDiffX: Float
        private val mCenterDiffY: Float
        private val mOldScale: Float
        private val mDeltaScale: Float
        private val mWillBeImageInBoundsAfterTranslate: Boolean

        init {
            mCropImageView = WeakReference(cropImageView)
            mDurationMs = durationMs
            mStartTime = System.currentTimeMillis()
            mOldX = oldX
            mOldY = oldY
            mCenterDiffX = centerDiffX
            mCenterDiffY = centerDiffY
            mOldScale = oldScale
            mDeltaScale = deltaScale
            mWillBeImageInBoundsAfterTranslate = willBeImageInBoundsAfterTranslate
        }

        override fun run() {
            val cropImageView = mCropImageView.get() ?: return
            val now = System.currentTimeMillis()
            val currentMs = Math.min(mDurationMs, now - mStartTime).toFloat()
            val newX = CubicEasing.easeOut(currentMs, 0f, mCenterDiffX, mDurationMs.toFloat())
            val newY = CubicEasing.easeOut(currentMs, 0f, mCenterDiffY, mDurationMs.toFloat())
            val newScale = CubicEasing.easeInOut(currentMs, 0f, mDeltaScale, mDurationMs.toFloat())
            if (currentMs < mDurationMs) {
                cropImageView.postTranslate(
                    newX - (cropImageView.mCurrentImageCenter[0] - mOldX),
                    newY - (cropImageView.mCurrentImageCenter[1] - mOldY)
                )
                if (!mWillBeImageInBoundsAfterTranslate) {
                    cropImageView.zoomInImage(
                        mOldScale + newScale,
                        cropImageView.mCropRect.centerX(),
                        cropImageView.mCropRect.centerY()
                    )
                }
                if (!cropImageView.isImageWrapCropBounds) {
                    cropImageView.post(this)
                }
            }
        }
    }

    private class ZoomImageToPosition(
        cropImageView: CropImageView,
        durationMs: Long,
        oldScale: Float, deltaScale: Float,
        destX: Float, destY: Float
    ) : Runnable {
        private val mCropImageView: WeakReference<CropImageView>
        private val mDurationMs: Long
        private val mStartTime: Long
        private val mOldScale: Float
        private val mDeltaScale: Float
        private val mDestX: Float
        private val mDestY: Float

        init {
            mCropImageView = WeakReference(cropImageView)
            mStartTime = System.currentTimeMillis()
            mDurationMs = durationMs
            mOldScale = oldScale
            mDeltaScale = deltaScale
            mDestX = destX
            mDestY = destY
        }

        override fun run() {
            val cropImageView = mCropImageView.get() ?: return
            val now = System.currentTimeMillis()
            val currentMs = Math.min(mDurationMs, now - mStartTime).toFloat()
            val newScale = CubicEasing.easeInOut(currentMs, 0f, mDeltaScale, mDurationMs.toFloat())
            if (currentMs < mDurationMs) {
                cropImageView.zoomInImage(mOldScale + newScale, mDestX, mDestY)
                cropImageView.post(this)
            } else {
                cropImageView.setImageToWrapCropBounds()
            }
        }
    }
}
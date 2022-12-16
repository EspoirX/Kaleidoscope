package com.lzx.library.crop.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.IntRange
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.lzx.library.crop.FastBitmapDrawable
import com.lzx.library.crop.utils.RectUtils
import com.lzx.library.utils.BitmapLoadUtils.calculateMaxBitmapSize
import com.lzx.library.utils.FileUtils
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

open class TransformImageView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(
    context!!, attrs, defStyle
) {
    //直角点坐标
    protected val mCurrentImageCorners = FloatArray(8)

    //矩形中心点坐标
    protected val mCurrentImageCenter = FloatArray(2)

    //矩阵数
    private val mMatrixValues = FloatArray(9)

    //当前图片的Matrix
    protected var mCurrentImageMatrix = Matrix()
    protected var mThisWidth = 0
    protected var mThisHeight = 0
    protected var mTransformImageListener: TransformImageListener? = null

    //最初的坐标
    private var mInitialImageCorners: FloatArray? = null
    private var mInitialImageCenter: FloatArray? = null

    protected var mBitmapDecoded = false
    protected var mBitmapLaidOut = false

    private var mMaxBitmapSize = 0

    var imageInputPath: String? = null
    var imageOutputPath: String? = null
    var imageInputUri: Uri? = null
    var imageOutputUri: Uri? = null

    interface TransformImageListener {
        fun onLoadComplete()
        fun onLoadFailure(e: Exception)
        fun onRotate(currentAngle: Float)
        fun onScale(currentScale: Float)
    }

    init {
        initView()
    }

    fun setTransformImageListener(transformImageListener: TransformImageListener?) {
        mTransformImageListener = transformImageListener
    }

    override fun setScaleType(scaleType: ScaleType) {
        if (scaleType == ScaleType.MATRIX) {
            super.setScaleType(scaleType)
        }
    }

    /**
     * 需要在setImageURI前调用
     */
    var maxBitmapSize: Int
        get() {
            if (mMaxBitmapSize <= 0) {
                mMaxBitmapSize = calculateMaxBitmapSize(context)
            }
            return mMaxBitmapSize
        }
        set(maxBitmapSize) {
            mMaxBitmapSize = maxBitmapSize
        }

    override fun setImageBitmap(bitmap: Bitmap) {
        setImageDrawable(FastBitmapDrawable(bitmap))
    }

    /**
     * 加载图片
     */
    fun setImageUri(imageUri: Uri, outputUri: Uri?) {
        Glide.with(context).asBitmap().load(imageUri).into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                val copyBitmap = resource.copy(resource.config, true)
                setBitmapLoadedResult(copyBitmap, imageUri, outputUri)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                mTransformImageListener?.onLoadFailure(IllegalStateException("图片加载失败"))
            }
        })
    }

    /**
     * 图片加载成功后赋值
     */
    fun setBitmapLoadedResult(
        bitmap: Bitmap, imageInputUri: Uri,
        imageOutputUri: Uri?
    ) {
        this.imageInputUri = imageInputUri
        this.imageOutputUri = imageOutputUri
        imageInputPath = if (FileUtils.isContent(imageInputUri.toString())) {
            imageInputUri.toString()
        } else {
            imageInputUri.path
        }
        imageOutputPath = if (imageOutputUri != null) {
            if (FileUtils.isContent(imageOutputUri.toString())) {
                imageOutputUri.toString()
            } else {
                imageOutputUri.path
            }
        } else null
        mBitmapDecoded = true
        setImageBitmap(bitmap)
    }

    /**
     * 当前图像比例值。
     * [1.0f - 原始图像，2.0f - 200% 缩放图像等]
     */
    val currentScale: Float
        get() = getMatrixScale(mCurrentImageMatrix)

    /**
     * 计算指定 Matrix 对象的比例值。
     */
    fun getMatrixScale(matrix: Matrix): Float {
        return sqrt(
            getMatrixValue(matrix, Matrix.MSCALE_X).toDouble().pow(2.0)
                    + getMatrixValue(matrix, Matrix.MSKEW_Y).toDouble().pow(2.0)
        ).toFloat()
    }

    /**
     * 当前图像旋转角度。.
     */
    val currentAngle: Float
        get() = getMatrixAngle(mCurrentImageMatrix)

    /**
     * 计算指定 Matrix 对象的旋转角度。
     */
    fun getMatrixAngle(matrix: Matrix): Float {
        return -(atan2(
            getMatrixValue(matrix, Matrix.MSKEW_X).toDouble(),
            getMatrixValue(matrix, Matrix.MSCALE_X).toDouble()
        ) * (180 / Math.PI)).toFloat()
    }

    override fun setImageMatrix(matrix: Matrix) {
        super.setImageMatrix(matrix)
        mCurrentImageMatrix.set(matrix)
        updateCurrentImagePoints()
    }

    val viewBitmap: Bitmap?
        get() = if (drawable == null || drawable !is FastBitmapDrawable) {
            null
        } else {
            (drawable as FastBitmapDrawable).bitmap
        }

    /**
     * 平移
     */
    fun postTranslate(deltaX: Float, deltaY: Float) {
        if (deltaX != 0f || deltaY != 0f) {
            mCurrentImageMatrix.postTranslate(deltaX, deltaY)
            imageMatrix = mCurrentImageMatrix
        }
    }

    /**
     * 缩放
     */
    open fun postScale(deltaScale: Float, px: Float, py: Float) {
        if (deltaScale != 0f) {
            mCurrentImageMatrix.postScale(deltaScale, deltaScale, px, py)
            imageMatrix = mCurrentImageMatrix
            mTransformImageListener?.onScale(getMatrixScale(mCurrentImageMatrix))
        }
    }

    /**
     * 选转
     */
    fun postRotate(deltaAngle: Float, px: Float, py: Float) {
        if (deltaAngle != 0f) {
            mCurrentImageMatrix.postRotate(deltaAngle, px, py)
            imageMatrix = mCurrentImageMatrix
            mTransformImageListener?.onRotate(getMatrixAngle(mCurrentImageMatrix))
        }
    }

    protected open fun initView() {
        scaleType = ScaleType.MATRIX
    }

    override fun onLayout(changed: Boolean, _left: Int, _top: Int, _right: Int, _bottom: Int) {
        var left = _left
        var top = _top
        var right = _right
        var bottom = _bottom
        super.onLayout(changed, left, top, right, bottom)
        if (changed || mBitmapDecoded && !mBitmapLaidOut) {
            left = paddingLeft
            top = paddingTop
            right = width - paddingRight
            bottom = height - paddingBottom
            mThisWidth = right - left
            mThisHeight = bottom - top
            onImageLaidOut()
        }
    }

    protected open fun onImageLaidOut() {
        val drawable = drawable ?: return
        val w = drawable.intrinsicWidth.toFloat()
        val h = drawable.intrinsicHeight.toFloat()
        Log.d(TAG, String.format("Image size: [%d:%d]", w.toInt(), h.toInt()))
        val initialImageRect = RectF(0f, 0f, w, h)
        mInitialImageCorners = RectUtils.getCornersFromRect(initialImageRect)
        mInitialImageCenter = RectUtils.getCenterFromRect(initialImageRect)
        mBitmapLaidOut = true
        mTransformImageListener?.onLoadComplete()
    }

    /**
     * 返回对应索引的矩阵值
     */
    protected fun getMatrixValue(matrix: Matrix, @IntRange(from = 0, to = 9) valueIndex: Int): Float {
        matrix.getValues(mMatrixValues)
        return mMatrixValues[valueIndex]
    }

    /**
     * 此方法更新存储在中的当前图像角点和中心点
     */
    private fun updateCurrentImagePoints() {
        if (mInitialImageCorners != null) {
            mCurrentImageMatrix.mapPoints(mCurrentImageCorners, mInitialImageCorners)
        }
        if (mInitialImageCenter != null) {
            mCurrentImageMatrix.mapPoints(mCurrentImageCenter, mInitialImageCenter)
        }
    }

    companion object {
        private const val TAG = "TransformImageView"
    }
}
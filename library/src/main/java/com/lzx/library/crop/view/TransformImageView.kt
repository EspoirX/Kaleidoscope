package com.lzx.library.crop.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.lzx.library.crop.FastBitmapDrawable;
import com.lzx.library.crop.utils.RectUtils;
import com.lzx.library.utils.BitmapLoadUtils;
import com.lzx.library.utils.FileUtils;


public class TransformImageView extends AppCompatImageView {

    private static final String TAG = "TransformImageView";

    //直角点坐标
    protected final float[] mCurrentImageCorners = new float[8];
    //矩形中心点坐标
    protected final float[] mCurrentImageCenter = new float[2];

    //矩阵数
    private final float[] mMatrixValues = new float[9];

    //当前图片的Matrix
    protected Matrix mCurrentImageMatrix = new Matrix();
    protected int mThisWidth, mThisHeight;

    protected TransformImageListener mTransformImageListener;

    //最初的坐标
    private float[] mInitialImageCorners;
    private float[] mInitialImageCenter;

    protected boolean mBitmapDecoded = false;
    protected boolean mBitmapLaidOut = false;

    private int mMaxBitmapSize = 0;

    private String mImageInputPath, mImageOutputPath;
    private Uri mImageInputUri, mImageOutputUri;

    public interface TransformImageListener {

        void onLoadComplete();

        void onLoadFailure(@NonNull Exception e);

        void onRotate(float currentAngle);

        void onScale(float currentScale);
    }

    public TransformImageView(Context context) {
        this(context, null);
    }

    public TransformImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransformImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void setTransformImageListener(TransformImageListener transformImageListener) {
        mTransformImageListener = transformImageListener;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        if (scaleType == ScaleType.MATRIX) {
            super.setScaleType(scaleType);
        }
    }

    /**
     * 需要在setImageURI前调用
     */
    public void setMaxBitmapSize(int maxBitmapSize) {
        mMaxBitmapSize = maxBitmapSize;
    }

    public int getMaxBitmapSize() {
        if (mMaxBitmapSize <= 0) {
            mMaxBitmapSize = BitmapLoadUtils.calculateMaxBitmapSize(getContext());
        }
        return mMaxBitmapSize;
    }

    @Override
    public void setImageBitmap(final Bitmap bitmap) {
        setImageDrawable(new FastBitmapDrawable(bitmap));
    }

    public String getImageInputPath() {
        return mImageInputPath;
    }

    public String getImageOutputPath() {
        return mImageOutputPath;
    }

    public Uri getImageInputUri() {
        return mImageInputUri;
    }

    public Uri getImageOutputUri() {
        return mImageOutputUri;
    }

    /**
     * 加载图片
     */
    public void setImageUri(@NonNull Uri imageUri, @Nullable Uri outputUri) {
        Glide.with(getContext()).asBitmap().load(imageUri).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                Bitmap copyBitmap = resource.copy(resource.getConfig(), true);
                setBitmapLoadedResult(copyBitmap, imageUri, outputUri);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
                if (mTransformImageListener != null) {
                    mTransformImageListener.onLoadFailure(new IllegalStateException("图片加载失败"));
                }
            }
        });
    }

    /**
     * 图片加载成功后赋值
     */
    public void setBitmapLoadedResult(@NonNull Bitmap bitmap, @NonNull Uri imageInputUri,
                                      @Nullable Uri imageOutputUri) {
        mImageInputUri = imageInputUri;
        mImageOutputUri = imageOutputUri;
        mImageInputPath = FileUtils.isContent(imageInputUri.toString())
                ? imageInputUri.toString() : imageInputUri.getPath();
        mImageOutputPath = imageOutputUri != null
                ? FileUtils.isContent(imageOutputUri.toString()) ? imageOutputUri.toString()
                : imageOutputUri.getPath() : null;

        mBitmapDecoded = true;
        setImageBitmap(bitmap);
    }

    /**
     * 当前图像比例值。
     * [1.0f - 原始图像，2.0f - 200% 缩放图像等]
     */
    public float getCurrentScale() {
        return getMatrixScale(mCurrentImageMatrix);
    }

    /**
     * 计算指定 Matrix 对象的比例值。
     */
    public float getMatrixScale(@NonNull Matrix matrix) {
        return (float) Math.sqrt(Math.pow(getMatrixValue(matrix, Matrix.MSCALE_X), 2)
                + Math.pow(getMatrixValue(matrix, Matrix.MSKEW_Y), 2));
    }

    /**
     * 当前图像旋转角度。.
     */
    public float getCurrentAngle() {
        return getMatrixAngle(mCurrentImageMatrix);
    }

    /**
     * 计算指定 Matrix 对象的旋转角度。
     */
    public float getMatrixAngle(@NonNull Matrix matrix) {
        return (float) -(Math.atan2(getMatrixValue(matrix, Matrix.MSKEW_X),
                getMatrixValue(matrix, Matrix.MSCALE_X)) * (180 / Math.PI));
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        super.setImageMatrix(matrix);
        mCurrentImageMatrix.set(matrix);
        updateCurrentImagePoints();
    }

    @Nullable
    public Bitmap getViewBitmap() {
        if (getDrawable() == null || !(getDrawable() instanceof FastBitmapDrawable)) {
            return null;
        } else {
            return ((FastBitmapDrawable) getDrawable()).getBitmap();
        }
    }

    /**
     * 平移
     */
    public void postTranslate(float deltaX, float deltaY) {
        if (deltaX != 0 || deltaY != 0) {
            mCurrentImageMatrix.postTranslate(deltaX, deltaY);
            setImageMatrix(mCurrentImageMatrix);
        }
    }

    /**
     * 缩放
     */
    public void postScale(float deltaScale, float px, float py) {
        if (deltaScale != 0) {
            mCurrentImageMatrix.postScale(deltaScale, deltaScale, px, py);
            setImageMatrix(mCurrentImageMatrix);
            if (mTransformImageListener != null) {
                mTransformImageListener.onScale(getMatrixScale(mCurrentImageMatrix));
            }
        }
    }

    /**
     * 选转
     */
    public void postRotate(float deltaAngle, float px, float py) {
        if (deltaAngle != 0) {
            mCurrentImageMatrix.postRotate(deltaAngle, px, py);
            setImageMatrix(mCurrentImageMatrix);
            if (mTransformImageListener != null) {
                mTransformImageListener.onRotate(getMatrixAngle(mCurrentImageMatrix));
            }
        }
    }

    protected void init() {
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed || (mBitmapDecoded && !mBitmapLaidOut)) {

            left = getPaddingLeft();
            top = getPaddingTop();
            right = getWidth() - getPaddingRight();
            bottom = getHeight() - getPaddingBottom();
            mThisWidth = right - left;
            mThisHeight = bottom - top;

            onImageLaidOut();
        }
    }

    protected void onImageLaidOut() {
        final Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }

        float w = drawable.getIntrinsicWidth();
        float h = drawable.getIntrinsicHeight();

        Log.d(TAG, String.format("Image size: [%d:%d]", (int) w, (int) h));

        RectF initialImageRect = new RectF(0, 0, w, h);
        mInitialImageCorners = RectUtils.getCornersFromRect(initialImageRect);
        mInitialImageCenter = RectUtils.getCenterFromRect(initialImageRect);

        mBitmapLaidOut = true;

        if (mTransformImageListener != null) {
            mTransformImageListener.onLoadComplete();
        }
    }

    /**
     * 返回对应索引的矩阵值
     */
    protected float getMatrixValue(@NonNull Matrix matrix, @IntRange(from = 0, to = 9) int valueIndex) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[valueIndex];
    }

    /**
     * 此方法更新存储在中的当前图像角点和中心点
     */
    private void updateCurrentImagePoints() {
        if (mInitialImageCorners != null) {
            mCurrentImageMatrix.mapPoints(mCurrentImageCorners, mInitialImageCorners);
        }
        if (mInitialImageCenter != null) {
            mCurrentImageMatrix.mapPoints(mCurrentImageCenter, mInitialImageCenter);
        }
    }
}

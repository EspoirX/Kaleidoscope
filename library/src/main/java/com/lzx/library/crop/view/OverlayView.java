package com.lzx.library.crop.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import com.lzx.library.crop.callback.OverlayViewChangeListener;
import com.lzx.library.crop.utils.RectUtils;
import com.lzx.library.utils.DisplayUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class OverlayView extends View {
    private static final long SMOOTH_CENTER_DURATION = 1000;
    public static final int FREESTYLE_CROP_MODE_DISABLE = 0;
    public static final int FREESTYLE_CROP_MODE_ENABLE = 1;
    public static final int FREESTYLE_CROP_MODE_ENABLE_WITH_PASS_THROUGH = 2;

    public static final boolean DEFAULT_SHOW_CROP_FRAME = true;
    public static final boolean DEFAULT_SHOW_CROP_GRID = true;
    public static final int DEFAULT_FREESTYLE_CROP_MODE = FREESTYLE_CROP_MODE_DISABLE;
    public static final int DEFAULT_CROP_GRID_ROW_COUNT = 2;
    public static final int DEFAULT_CROP_GRID_COLUMN_COUNT = 2;

    private final RectF mCropViewRect = new RectF();
    private final RectF mTempRect = new RectF();

    protected int mThisWidth, mThisHeight;
    protected float[] mCropGridCorners;
    protected float[] mCropGridCenter;

    private int mCropGridRowCount, mCropGridColumnCount;
    private float mTargetAspectRatio;
    private float[] mGridPoints = null;
    private boolean mShowCropFrame, mShowCropGrid;
    //暗淡区域
    private boolean mCircleDimmedLayer;
    private int mDimmedColor = Color.parseColor("#8c000000");
    private final Path mCircularPath = new Path();
    private final Paint mDimmedStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mCropGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mCropFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mCropFrameCornersPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @FreestyleMode
    private int mFreestyleCropMode = DEFAULT_FREESTYLE_CROP_MODE;
    private float mPreviousTouchX = -1, mPreviousTouchY = -1;
    private int mCurrentTouchCornerIndex = -1;
    private final int mTouchPointThreshold = DisplayUtil.dp2px(30);
    private final int mCropRectMinSize = DisplayUtil.dp2px(100);
    private final int mCropRectCornerTouchAreaLineLength = DisplayUtil.dp2px(10);
    private boolean isDragCenter;  //裁剪和拖动自动居中
    private OverlayViewChangeListener mCallback;
    private ValueAnimator smoothAnimator;
    private boolean mShouldSetupCropBounds;


    public OverlayView(Context context) {
        this(context, null);
    }

    public OverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public OverlayViewChangeListener getOverlayViewChangeListener() {
        return mCallback;
    }

    public void setOverlayViewChangeListener(OverlayViewChangeListener callback) {
        mCallback = callback;
    }

    @NonNull
    public RectF getCropViewRect() {
        return mCropViewRect;
    }

    /**
     * 裁剪和拖动自动居中
     */
    public void setDragSmoothToCenter(boolean isDragCenter) {
        this.isDragCenter = isDragCenter;
    }

    @FreestyleMode
    public int getFreestyleCropMode() {
        return mFreestyleCropMode;
    }

    public void setFreestyleCropMode(@FreestyleMode int mFreestyleCropMode) {
        this.mFreestyleCropMode = mFreestyleCropMode;
        postInvalidate();
    }

    /**
     * 圆形暗淡区域
     */
    public void setCircleDimmedLayer(boolean circleDimmedLayer) {
        mCircleDimmedLayer = circleDimmedLayer;
    }

    /**
     * 设置裁剪网格行
     */
    public void setCropGridRowCount(@IntRange(from = 0) int cropGridRowCount) {
        mCropGridRowCount = cropGridRowCount;
        mGridPoints = null;
    }

    /**
     * 设置裁剪网格列
     */
    public void setCropGridColumnCount(@IntRange(from = 0) int cropGridColumnCount) {
        mCropGridColumnCount = cropGridColumnCount;
        mGridPoints = null;
    }

    /**
     * Setter for {@link #mShowCropFrame} variable.
     *
     * @param showCropFrame - set to true if you want to see a crop frame rectangle on top of an image
     */
    public void setShowCropFrame(boolean showCropFrame) {
        mShowCropFrame = showCropFrame;
    }

    /**
     * 是否展示裁剪网格
     */
    public void setShowCropGrid(boolean showCropGrid) {
        mShowCropGrid = showCropGrid;
    }

    /**
     * 暗淡区域颜色
     */
    public void setDimmedColor(@ColorInt int dimmedColor) {
        mDimmedColor = dimmedColor;
    }

    public void setCircleStrokeColor(@ColorInt int circleStrokeColor) {
        mDimmedStrokePaint.setColor(circleStrokeColor);
    }

    public void setCropFrameStrokeWidth(@IntRange(from = 0) int width) {
        mCropFramePaint.setStrokeWidth(width);
    }

    public void setCropGridStrokeWidth(@IntRange(from = 0) int width) {
        mCropGridPaint.setStrokeWidth(width);
    }

    public void setDimmedStrokeWidth(@IntRange(from = 0) int width) {
        mDimmedStrokePaint.setStrokeWidth(width);
    }

    public void setCropFrameColor(@ColorInt int color) {
        mCropFramePaint.setColor(color);
    }

    public void setCropGridColor(@ColorInt int color) {
        mCropGridPaint.setColor(color);
    }

    /**
     * 设置裁剪比例
     */
    public void setTargetAspectRatio(final float targetAspectRatio) {
        mTargetAspectRatio = targetAspectRatio;
        if (mThisWidth > 0) {
            setupCropBounds();
            postInvalidate();
        } else {
            mShouldSetupCropBounds = true;
        }
    }

    public void setupCropBounds() {
        int height = (int) (mThisWidth / mTargetAspectRatio);
        if (height > mThisHeight) {
            int width = (int) (mThisHeight * mTargetAspectRatio);
            int halfDiff = (mThisWidth - width) / 2;
            mCropViewRect.set(getPaddingLeft() + halfDiff, getPaddingTop(),
                    getPaddingLeft() + width + halfDiff, getPaddingTop() + mThisHeight);
        } else {
            int halfDiff = (mThisHeight - height) / 2;
            mCropViewRect.set(getPaddingLeft(), getPaddingTop() + halfDiff,
                    getPaddingLeft() + mThisWidth, getPaddingTop() + height + halfDiff);
        }

        if (mCallback != null) {
            mCallback.onCropRectUpdated(mCropViewRect);
        }

        updateGridPoints();
    }

    private void updateGridPoints() {
        mCropGridCorners = RectUtils.getCornersFromRect(mCropViewRect);
        mCropGridCenter = RectUtils.getCenterFromRect(mCropViewRect);

        mGridPoints = null;
        mCircularPath.reset();
        mCircularPath.addCircle(mCropViewRect.centerX(), mCropViewRect.centerY(),
                Math.min(mCropViewRect.width(), mCropViewRect.height()) / 2.f, Path.Direction.CW);
    }

    protected void init() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
        mDimmedStrokePaint.setColor(mDimmedColor);
        mDimmedStrokePaint.setStyle(Paint.Style.STROKE);
        mDimmedStrokePaint.setStrokeWidth(DisplayUtil.dp2px(1));

        initCropFrameStyle();
        mShowCropFrame = DEFAULT_SHOW_CROP_FRAME;

        initCropGridStyle();
        mShowCropGrid = DEFAULT_SHOW_CROP_GRID;
    }

    @SuppressWarnings("deprecation")
    private void initCropFrameStyle() {
        int cropFrameStrokeSize = DisplayUtil.dp2px(1);
        int cropFrameColor = Color.WHITE;
        mCropFramePaint.setStrokeWidth(cropFrameStrokeSize);
        mCropFramePaint.setColor(cropFrameColor);
        mCropFramePaint.setStyle(Paint.Style.STROKE);

        mCropFrameCornersPaint.setStrokeWidth(cropFrameStrokeSize * 3);
        mCropFrameCornersPaint.setColor(cropFrameColor);
        mCropFrameCornersPaint.setStyle(Paint.Style.STROKE);
    }

    @SuppressWarnings("deprecation")
    private void initCropGridStyle() {
        int cropGridStrokeSize = DisplayUtil.dp2px(1);
        int cropGridColor = Color.parseColor("#80ffffff");
        mCropGridPaint.setStrokeWidth(cropGridStrokeSize);
        mCropGridPaint.setColor(cropGridColor);

        mCropGridRowCount = DEFAULT_CROP_GRID_ROW_COUNT;
        mCropGridColumnCount = DEFAULT_CROP_GRID_COLUMN_COUNT;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            left = getPaddingLeft();
            top = getPaddingTop();
            right = getWidth() - getPaddingRight();
            bottom = getHeight() - getPaddingBottom();
            mThisWidth = right - left;
            mThisHeight = bottom - top;

            if (mShouldSetupCropBounds) {
                mShouldSetupCropBounds = false;
                setTargetAspectRatio(mTargetAspectRatio);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawDimmedLayer(canvas);
        drawCropGrid(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mCropViewRect.isEmpty() || mFreestyleCropMode == FREESTYLE_CROP_MODE_DISABLE) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
            mCurrentTouchCornerIndex = getCurrentTouchIndex(x, y);
            boolean shouldHandle = mCurrentTouchCornerIndex != -1;
            if (!shouldHandle) {
                mPreviousTouchX = -1;
                mPreviousTouchY = -1;
            } else if (mPreviousTouchX < 0) {
                mPreviousTouchX = x;
                mPreviousTouchY = y;
            }
            return shouldHandle;
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE) {
            if (event.getPointerCount() == 1 && mCurrentTouchCornerIndex != -1) {

                x = Math.min(Math.max(x, getPaddingLeft()), getWidth() - getPaddingRight());
                y = Math.min(Math.max(y, getPaddingTop()), getHeight() - getPaddingBottom());

                updateCropViewRect(x, y);

                mPreviousTouchX = x;
                mPreviousTouchY = y;

                return true;
            }
        }

        if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            mPreviousTouchX = -1;
            mPreviousTouchY = -1;
            mCurrentTouchCornerIndex = -1;

            if (mCallback != null) {
                mCallback.onCropRectUpdated(mCropViewRect);
            }

            if (isDragCenter) {
                smoothToCenter();
            }
        }

        return false;
    }

    private void updateCropViewRect(float touchX, float touchY) {
        mTempRect.set(mCropViewRect);
        switch (mCurrentTouchCornerIndex) {
            // resize rectangle
            case 0:
                mTempRect.set(touchX, touchY, mCropViewRect.right, mCropViewRect.bottom);
                break;
            case 1:
                mTempRect.set(mCropViewRect.left, touchY, touchX, mCropViewRect.bottom);
                break;
            case 2:
                mTempRect.set(mCropViewRect.left, mCropViewRect.top, touchX, touchY);
                break;
            case 3:
                mTempRect.set(touchX, mCropViewRect.top, mCropViewRect.right, touchY);
                break;
            // move rectangle
            case 4:
                mTempRect.offset(touchX - mPreviousTouchX, touchY - mPreviousTouchY);
                if (mTempRect.left > getLeft() && mTempRect.top > getTop()
                        && mTempRect.right < getRight() && mTempRect.bottom < getBottom()) {
                    mCropViewRect.set(mTempRect);
                    updateGridPoints();
                    postInvalidate();
                }
                return;
            default:
                return;
        }

        boolean changeHeight = mTempRect.height() >= mCropRectMinSize;
        boolean changeWidth = mTempRect.width() >= mCropRectMinSize;
        mCropViewRect.set(
                changeWidth ? mTempRect.left : mCropViewRect.left,
                changeHeight ? mTempRect.top : mCropViewRect.top,
                changeWidth ? mTempRect.right : mCropViewRect.right,
                changeHeight ? mTempRect.bottom : mCropViewRect.bottom);

        if (changeHeight || changeWidth) {
            updateGridPoints();
            postInvalidate();
        }
    }

    private int getCurrentTouchIndex(float touchX, float touchY) {
        int closestPointIndex = -1;
        double closestPointDistance = mTouchPointThreshold;
        for (int i = 0; i < 8; i += 2) {
            double distanceToCorner = Math.sqrt(Math.pow(touchX - mCropGridCorners[i], 2)
                    + Math.pow(touchY - mCropGridCorners[i + 1], 2));
            if (distanceToCorner < closestPointDistance) {
                closestPointDistance = distanceToCorner;
                closestPointIndex = i / 2;
            }
        }

        if (mFreestyleCropMode == FREESTYLE_CROP_MODE_ENABLE && closestPointIndex < 0
                && mCropViewRect.contains(touchX, touchY)) {
            return 4;
        }
        return closestPointIndex;
    }

    protected void drawDimmedLayer(@NonNull Canvas canvas) {
        canvas.save();
        if (mCircleDimmedLayer) {
            canvas.clipPath(mCircularPath, Region.Op.DIFFERENCE);
        } else {
            canvas.clipRect(mCropViewRect, Region.Op.DIFFERENCE);
        }
        canvas.drawColor(mDimmedColor);
        canvas.restore();

        if (mCircleDimmedLayer) { // Draw 1px stroke to fix antialias
            canvas.drawCircle(mCropViewRect.centerX(), mCropViewRect.centerY(),
                    Math.min(mCropViewRect.width(), mCropViewRect.height()) / 2.f, mDimmedStrokePaint);
        }
    }

    protected void drawCropGrid(@NonNull Canvas canvas) {
        if (mShowCropGrid) {
            if (mGridPoints == null && !mCropViewRect.isEmpty()) {

                mGridPoints = new float[(mCropGridRowCount) * 4 + (mCropGridColumnCount) * 4];

                int index = 0;
                for (int i = 0; i < mCropGridRowCount; i++) {
                    mGridPoints[index++] = mCropViewRect.left;
                    mGridPoints[index++] = (mCropViewRect.height() * (((float) i + 1.0f) /
                            (float) (mCropGridRowCount + 1))) + mCropViewRect.top;
                    mGridPoints[index++] = mCropViewRect.right;
                    mGridPoints[index++] = (mCropViewRect.height() * (((float) i + 1.0f) /
                            (float) (mCropGridRowCount + 1))) + mCropViewRect.top;
                }

                for (int i = 0; i < mCropGridColumnCount; i++) {
                    mGridPoints[index++] = (mCropViewRect.width() * (((float) i + 1.0f) /
                            (float) (mCropGridColumnCount + 1))) + mCropViewRect.left;
                    mGridPoints[index++] = mCropViewRect.top;
                    mGridPoints[index++] = (mCropViewRect.width() * (((float) i + 1.0f) /
                            (float) (mCropGridColumnCount + 1))) + mCropViewRect.left;
                    mGridPoints[index++] = mCropViewRect.bottom;
                }
            }

            if (mGridPoints != null) {
                canvas.drawLines(mGridPoints, mCropGridPaint);
            }
        }

        if (mShowCropFrame) {
            canvas.drawRect(mCropViewRect, mCropFramePaint);
        }

        if (mFreestyleCropMode != FREESTYLE_CROP_MODE_DISABLE) {
            canvas.save();

            mTempRect.set(mCropViewRect);
            mTempRect.inset(mCropRectCornerTouchAreaLineLength, -mCropRectCornerTouchAreaLineLength);
            canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);

            mTempRect.set(mCropViewRect);
            mTempRect.inset(-mCropRectCornerTouchAreaLineLength, mCropRectCornerTouchAreaLineLength);
            canvas.clipRect(mTempRect, Region.Op.DIFFERENCE);

            canvas.drawRect(mCropViewRect, mCropFrameCornersPaint);

            canvas.restore();
        }
    }


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FREESTYLE_CROP_MODE_DISABLE, FREESTYLE_CROP_MODE_ENABLE, FREESTYLE_CROP_MODE_ENABLE_WITH_PASS_THROUGH})
    public @interface FreestyleMode {
    }

    /**
     * 平滑移动至中心
     */
    private void smoothToCenter() {
        Point centerPoint = new Point((getRight() + getLeft()) / 2, (getTop() + getBottom()) / 2);
        final int offsetY = (int) (centerPoint.y - mCropViewRect.centerY());
        final int offsetX = (int) (centerPoint.x - mCropViewRect.centerX());
        final RectF before = new RectF(mCropViewRect);
        RectF after = new RectF(mCropViewRect);
        after.offset(offsetX, offsetY);
        if (smoothAnimator != null) {
            smoothAnimator.cancel();
        }
        smoothAnimator = ValueAnimator.ofFloat(0, 1);
        smoothAnimator.setDuration(SMOOTH_CENTER_DURATION);
        smoothAnimator.setInterpolator(new OvershootInterpolator(1.0F));
        smoothAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mCallback != null) {
                    mCallback.onCropRectUpdated(mCropViewRect);
                }
            }
        });

        smoothAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            float lastAnimationValue = 0f;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float x = offsetX * (float) animation.getAnimatedValue();
                float y = offsetY * (float) animation.getAnimatedValue();
                mCropViewRect.set(new RectF(
                        before.left + x,
                        before.top + y,
                        before.right + x,
                        before.bottom + y
                ));
                updateGridPoints();
                postInvalidate();
                if (mCallback != null) {
                    mCallback.postTranslate(
                            offsetX * ((float) animation.getAnimatedValue() - lastAnimationValue),
                            offsetY * ((float) animation.getAnimatedValue() - lastAnimationValue)
                    );
                }
                lastAnimationValue = (float) animation.getAnimatedValue();
            }
        });
        smoothAnimator.start();
    }
}

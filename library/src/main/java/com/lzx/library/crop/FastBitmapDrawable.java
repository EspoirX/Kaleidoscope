package com.lzx.library.crop;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import com.lzx.library.crop.utils.BitmapUtils;


public class FastBitmapDrawable extends Drawable {

    private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private Bitmap mBitmap;
    private int mAlpha;
    private int mWidth, mHeight;

    public FastBitmapDrawable(Bitmap b) {
        mAlpha = 255;
        setBitmap(b);
    }

    @Override
    public void draw(Canvas canvas) {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            canvas.drawBitmap(mBitmap, null, getBounds(), mPaint);
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void setFilterBitmap(boolean filterBitmap) {
        mPaint.setFilterBitmap(filterBitmap);
    }

    public int getAlpha() {
        return mAlpha;
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
        mPaint.setAlpha(alpha);
    }

    @Override
    public int getIntrinsicWidth() {
        return mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mHeight;
    }

    @Override
    public int getMinimumWidth() {
        return mWidth;
    }

    @Override
    public int getMinimumHeight() {
        return mHeight;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap b) {
        mBitmap = b;
        if (b != null) {
            mWidth = mBitmap.getWidth();
            mHeight = mBitmap.getHeight();
        } else {
            mWidth = mHeight = 0;
        }

        //fix Bitmap too large to be uploaded into a texture
        if (mHeight > BitmapUtils.getOpenglRenderLimitValue()) {
            mWidth = mWidth * BitmapUtils.getOpenglRenderLimitValue() / mHeight;
            mHeight = BitmapUtils.getOpenglRenderLimitValue();
            mBitmap = BitmapUtils.zoomImg(mBitmap, mWidth, mHeight);
        }
    }
}

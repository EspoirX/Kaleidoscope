package com.lzx.library.view.photoview

import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView

interface OnMatrixChangedListener {
    fun onMatrixChanged(rect: RectF?)
}

interface OnPhotoTapListener {
    fun onPhotoTap(view: ImageView?, x: Float, y: Float)
}

interface OnOutsidePhotoTapListener {
    fun onOutsidePhotoTap(imageView: ImageView?)
}

interface OnViewTapListener {
    fun onViewTap(view: View?, x: Float, y: Float)
}

interface OnViewDragListener {
    fun onDrag(dx: Float, dy: Float)
}

interface OnScaleChangedListener {
    fun onScaleChange(scaleFactor: Float, focusX: Float, focusY: Float)
}

interface OnSingleFlingListener {
    fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean
}

internal interface OnGestureListener {
    fun onDrag(dx: Float, dy: Float)
    fun onFling(
        startX: Float, startY: Float, velocityX: Float,
        velocityY: Float
    )

    fun onScale(scaleFactor: Float, focusX: Float, focusY: Float)
    fun onScale(scaleFactor: Float, focusX: Float, focusY: Float, dx: Float, dy: Float)
}
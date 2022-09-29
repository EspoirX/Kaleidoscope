package com.lzx.library.crop.callback

import android.graphics.RectF
import android.net.Uri

interface BitmapCropCallback {
    fun onBitmapCropped(
        resultUri: Uri,
        resultPath: String?,
        offsetX: Int, offsetY: Int, imageWidth: Int, imageHeight: Int
    )

    fun onCropFailure(t: Throwable)
}

interface CropBoundsChangeListener {
    fun onCropAspectRatioChanged(cropRatio: Float)
}

interface OverlayViewChangeListener {
    fun onCropRectUpdated(cropRect: RectF?)
    fun postTranslate(deltaX: Float, deltaY: Float)
}
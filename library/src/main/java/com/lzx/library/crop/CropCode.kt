package com.lzx.library.crop

import com.lzx.library.BuildConfig


/**
 * 裁剪相关参数
 */
object CropCode {
    private const val EXTRA_PREFIX: String = BuildConfig.LIBRARY_PACKAGE_NAME

    //打开裁剪界面相关参数
    const val EXTRA_INPUT_URI: String = "$EXTRA_PREFIX.InputUri"
    const val CROP_REQUEST_CODE = 9922

    //裁剪完成后相关key
    const val EXTRA_OUTPUT_URI: String = "$EXTRA_PREFIX.OutputUri"
    const val EXTRA_OUTPUT_PATH: String = "$EXTRA_PREFIX.OutputPath"
    const val EXTRA_OUTPUT_CROP_ASPECT_RATIO: String = "$EXTRA_PREFIX.CropAspectRatio"
    const val EXTRA_OUTPUT_IMAGE_WIDTH: String = "$EXTRA_PREFIX.ImageWidth"
    const val EXTRA_OUTPUT_IMAGE_HEIGHT: String = "$EXTRA_PREFIX.ImageHeight"
    const val EXTRA_OUTPUT_OFFSET_X: String = "$EXTRA_PREFIX.OffsetX"
    const val EXTRA_OUTPUT_OFFSET_Y: String = "$EXTRA_PREFIX.OffsetY"
}
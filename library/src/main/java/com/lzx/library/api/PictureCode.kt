package com.lzx.library.api

import com.lzx.library.BuildConfig

object PictureCode {
    private const val EXTRA_PREFIX: String = BuildConfig.LIBRARY_PACKAGE_NAME

    const val PIC_SELECTOR_REQUEST = 1320
    const val PIC_DETAIL_REQUEST = 1321
    const val REQUEST_CAMERA = 909

    const val EXTRA_MODE = "extra_result_mode"
    const val MODE_CAMERA = "extra_result_mode_camera"
    const val MODE_SELECTION = "extra_result_mode_selection"

    const val EXTRA_OUTPUT_URI: String = "$EXTRA_PREFIX.OutputUri"
    const val EXTRA_OUTPUT_PATH: String = "$EXTRA_PREFIX.OutputPath"
    const val EXTRA_OUTPUT_PATHS: String = "$EXTRA_PREFIX.OutputPaths"
    const val EXTRA_FAMILY_OUTPUT_PATH: String = "IMG_PATH"
    const val EXTRA_FAMILY_CROP_OUTPUT_PATH: String = "path"
    const val EXTRA_RESULT_FLASH_PIC_CHECKED = "FLASH_PIC_CHECKED" //是否选中闪照快照

    //下面这些只有裁剪才会有值
    const val EXTRA_OUTPUT_CROP_ASPECT_RATIO: String = "$EXTRA_PREFIX.CropAspectRatio"
    const val EXTRA_OUTPUT_IMAGE_WIDTH: String = "$EXTRA_PREFIX.ImageWidth"
    const val EXTRA_OUTPUT_IMAGE_HEIGHT: String = "$EXTRA_PREFIX.ImageHeight"
    const val EXTRA_OUTPUT_OFFSET_X: String = "$EXTRA_PREFIX.OffsetX"
    const val EXTRA_OUTPUT_OFFSET_Y: String = "$EXTRA_PREFIX.OffsetY"
}
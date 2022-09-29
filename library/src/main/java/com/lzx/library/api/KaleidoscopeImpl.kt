package com.lzx.library.api

import android.Manifest
import android.content.Intent
import android.net.Uri
import com.hjq.permissions.XXPermissions
import com.lzx.library.crop.CropActivity
import com.lzx.library.crop.CropCode
import com.lzx.library.selector.PictureSelectorActivity
import com.lzx.library.utils.FileUtils
import com.lzx.library.utils.navigationToForResult
import java.io.File

class KaleidoscopeImpl {

    private var kaleidoscope: Kaleidoscope? = null

    fun create(kaleidoscope: Kaleidoscope): KaleidoscopeImpl {
        this.kaleidoscope = kaleidoscope
        KaleConfig.reset()
        return this
    }

    /**
     *
     */
    fun chooseMode(mode: ChooseMode) = apply {
        KaleConfig.chooseMode = mode
    }

    fun maxSelectNum(maxNum: Int) = apply {
        KaleConfig.maxSelectNum = maxNum
    }

    fun filterMaxFileSize(size: Long) = apply {
        KaleConfig.filterMaxFileSize = size
    }

    fun filterMinFileSize(size: Long) = apply {
        KaleConfig.filterMinFileSize = size
    }

    fun isLoadGif(gif: Boolean) = apply {
        KaleConfig.isLoadGif = gif
    }

    fun isOnlyLoadGif(gif: Boolean) = apply {
        KaleConfig.isOnlyLoadGif = gif
    }

    fun isLoadVideo(load: Boolean) = apply {
        KaleConfig.isLoadVideo = load
    }

    fun isLoadAudio(load: Boolean) = apply {
        KaleConfig.isLoadAudio = load
    }

    fun isDisplayCamera(display: Boolean) = apply {
        KaleConfig.isDisplayCamera = display
    }

    fun isNeedCut(need: Boolean) = apply {
        KaleConfig.isNeedCut = need
    }

    fun isNeedPreview(need: Boolean) = apply {
        KaleConfig.isNeedPreview = need
    }

    fun filterVideoMaxSecond(second: Int) = apply {
        KaleConfig.filterVideoMaxSecond = second
    }

    fun filterVideoMinSecond(second: Int) = apply {
        KaleConfig.filterVideoMinSecond = second
    }

    fun isFilterSizeDuration(isFilter: Boolean) = apply {
        KaleConfig.isFilterSizeDuration = isFilter
    }

    fun savePicFolderName(name: String) = apply {
        KaleConfig.savePicFolderName = name
    }

    fun jumpForIntent(callback: (result: Intent?) -> Unit) {
        val act = kaleidoscope?.getActivity() ?: kaleidoscope?.getFragment()?.activity
        act?.let {
            XXPermissions.with(it).permission(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE).request { permissions, all ->
                KaleActivityForResult.startPictureSelectorActivity(it, callback)
            }
        }
    }

    fun jump(callback: ((path: String?) -> Unit)) {
        jumpForIntent() { result ->
            val path = result?.getStringExtra(PictureCode.EXTRA_OUTPUT_PATH)
            callback.invoke(path)
        }
    }

    fun jumpToCamera(callback: ((path: String?) -> Unit)) {
        val act = kaleidoscope?.getActivity() ?: kaleidoscope?.getFragment()?.activity
        act?.let {
            XXPermissions.with(it).permission(Manifest.permission.CAMERA).request { permissions, all ->
                KaleActivityForResult.startCamera(it) { _, result ->
                    val path = result?.getStringExtra(PictureCode.EXTRA_OUTPUT_PATH)
                    callback.invoke(path)
                }
            }
        }
    }

    fun jumpForResult(requestCode: Int) {
        val act = kaleidoscope?.getActivity() ?: kaleidoscope?.getFragment()?.activity
        act?.let {
            XXPermissions.with(it).permission(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE).request { permissions, all ->
                if (kaleidoscope?.getActivity() != null) {
                    kaleidoscope?.getActivity()?.navigationToForResult<PictureSelectorActivity>(requestCode)
                } else if (kaleidoscope?.getFragment() != null) {
                    kaleidoscope?.getFragment()?.navigationToForResult<PictureSelectorActivity>(requestCode)
                }
            }
        }
    }

    fun jumpCropForResult(requestCode: Int, inputPath: String) {
        val act = kaleidoscope?.getActivity() ?: kaleidoscope?.getFragment()?.activity
        act?.let {
            val inputUri = if (FileUtils.isContent(inputPath)) {
                Uri.parse(inputPath)
            } else Uri.fromFile(File(inputPath))
            if (kaleidoscope?.getActivity() != null) {
                kaleidoscope?.getActivity()
                    ?.navigationToForResult<CropActivity>(requestCode, CropCode.EXTRA_INPUT_URI to inputUri)
            } else if (kaleidoscope?.getFragment() != null) {
                kaleidoscope?.getFragment()
                    ?.navigationToForResult<CropActivity>(requestCode, CropCode.EXTRA_INPUT_URI to inputUri)
            } else { //
            }
        }
    }
}
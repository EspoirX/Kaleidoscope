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
     * 可选中展示图片，视频等
     */
    fun chooseMode(mode: ChooseMode) = apply {
        KaleConfig.chooseMode = mode
    }

    /**
     * 最大可以选中多少张
     */
    fun maxSelectNum(maxNum: Int) = apply {
        KaleConfig.maxSelectNum = maxNum
    }

    /**
     * 过滤最大文件大小
     */
    fun filterMaxFileSize(size: Long) = apply {
        KaleConfig.filterMaxFileSize = size
    }

    /**
     * 过滤最小文件大小
     */
    fun filterMinFileSize(size: Long) = apply {
        KaleConfig.filterMinFileSize = size
    }

    /**
     * 是否加载gif图
     */
    fun isLoadGif(gif: Boolean) = apply {
        KaleConfig.isLoadGif = gif
    }

    /**
     * 是否只加载gif图
     */
    fun isOnlyLoadGif(gif: Boolean) = apply {
        KaleConfig.isOnlyLoadGif = gif
    }

    /**
     * 是否加载视频
     */
    fun isLoadVideo(load: Boolean) = apply {
        KaleConfig.isLoadVideo = load
    }

    /**
     * 是否加载音频
     */
    fun isLoadAudio(load: Boolean) = apply {
        KaleConfig.isLoadAudio = load
    }

    /**
     * 是否展示拍照按钮
     */
    fun isDisplayCamera(display: Boolean) = apply {
        KaleConfig.isDisplayCamera = display
    }

    /**
     * 是否需要裁剪
     */
    fun isNeedCut(need: Boolean) = apply {
        KaleConfig.isNeedCut = need
    }

    /**
     * 是否需要预览
     */
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

    /**
     * 保存图片（拍照后）的文件夹名字
     */
    fun savePicFolderName(name: String) = apply {
        KaleConfig.savePicFolderName = name
    }

    /**
     * 是否展示单选界面
     */
    fun isFolderView(isFolder: Boolean) = apply {
        KaleConfig.isPicFolderView = isFolder
    }

    /**
     * 开始转跳相册，返回 Intent
     *
     */
    fun jumpForIntent(callback: (result: Intent?) -> Unit) {
        val act = kaleidoscope?.getActivity() ?: kaleidoscope?.getFragment()?.activity
        act?.let {
            XXPermissions.with(it).permission(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE).request { permissions, all ->
                KaleActivityForResult.startPictureSelectorActivity(it, callback)
            }
        }
    }

    /**
     * 开始转跳相册，返回 图片绝对路径
     */
    fun jump(callback: ((path: String?) -> Unit)) {
        jumpForIntent() { result ->
            val path = result?.getStringExtra(PictureCode.EXTRA_OUTPUT_PATH)
            callback.invoke(path)
        }
    }

    /**
     * 开始转跳相机，返回 图片绝对路径
     */
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

    /**
     * 开始转跳相册，需要在 onActivityResult 中获取值
     */
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

    /**
     * 开始转跳裁剪界面，需要在 onActivityResult 中获取值
     */
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
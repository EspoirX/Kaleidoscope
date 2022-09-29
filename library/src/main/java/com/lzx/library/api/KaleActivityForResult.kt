package com.lzx.library.api

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.lzx.library.crop.CropActivity
import com.lzx.library.crop.CropCode
import com.lzx.library.selector.PictureSelectorActivity
import com.lzx.library.utils.FileUtils
import com.lzx.library.utils.MediaUtils
import com.lzx.library.utils.SdkVersionUtils
import com.lzx.library.utils.showToast
import java.io.File

object KaleActivityForResult {
    var sRequestCode = 0
        set(value) {
            field = if (value >= Integer.MAX_VALUE) 1 else value
        }

    inline fun startPictureSelectorActivity(activity: FragmentActivity,
                                           crossinline callback: ((result: Intent?) -> Unit)) {
        startActivityForResultImpl(activity,
            Intent(activity, PictureSelectorActivity::class.java),
            PictureCode.PIC_SELECTOR_REQUEST) { _, result ->
            if (result != null) {
                callback.invoke(result)
            }
        }
    }

    inline fun startCamera(activity: FragmentActivity,
                           crossinline callback: ((resultCode: Int, result: Intent?) -> Unit)) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(activity.packageManager) == null) return
        val data = MediaUtils.createCameraOutImageUri(activity)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, data.first)

        startActivityForResultImpl(activity, intent, PictureCode.REQUEST_CAMERA) { resultCode, result ->
            var outPutUri = result?.getParcelableExtra<Uri?>(MediaStore.EXTRA_OUTPUT)
            if (outPutUri == null) {
                outPutUri = data.first
            }
            var path = FileUtils.getPath(activity, outPutUri)
            if (path == null && !SdkVersionUtils.isQ() && data.second != null) {
                path = data.second!!.absolutePath
            }

            if (path.isNullOrEmpty() || !File(path).exists()) {
                activity.showToast("相机发生错误，拍照失败")
                return@startActivityForResultImpl
            }
            if (KaleConfig.isNeedCut) {
                startActivityForResultImpl(activity,
                    Intent(activity, CropActivity::class.java).apply { putExtra(CropCode.EXTRA_INPUT_URI, outPutUri) },
                    CropCode.CROP_REQUEST_CODE) { resultCode2, data ->
                    data?.let {
                        val resultUri = data.getStringExtra(CropCode.EXTRA_OUTPUT_URI)
                        val resultPath = data.getStringExtra(CropCode.EXTRA_OUTPUT_PATH)
                        callback.invoke(resultCode2, Intent().apply {
                            putExtra(PictureCode.EXTRA_OUTPUT_URI, resultUri)
                            putExtra(PictureCode.EXTRA_OUTPUT_PATH, resultPath)
                        })
                    }
                }
            } else {
                callback.invoke(resultCode, Intent().apply {
                    putExtra(PictureCode.EXTRA_OUTPUT_URI, outPutUri)
                    putExtra(PictureCode.EXTRA_OUTPUT_PATH, path)
                })
            }
        }
    }

    inline fun startActivityForResultImpl(activity: FragmentActivity?,
                                          intent: Intent,
                                          requestCode: Int = -1,
                                          crossinline callback: ((resultCode: Int, result: Intent?) -> Unit)) {
        activity?.let {
            val fragment = GhostFragment()
            val code = if (requestCode == -1) {
                sRequestCode += 1
                sRequestCode
            } else {
                requestCode
            }
            fragment.init(code, intent) { resultCode, result ->
                callback(resultCode, result)
                it.supportFragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss()
            }
            it.supportFragmentManager.beginTransaction().add(fragment, GhostFragment::class.java.simpleName)
                .commitAllowingStateLoss()
        }
    }

    class GhostFragment : Fragment() {
        private var requestCode = -1
        private var intent: Intent? = null
        private var callback: ((resultCode: Int, result: Intent?) -> Unit)? = null

        fun init(requestCode: Int, intent: Intent, callback: ((resultCode: Int, result: Intent?) -> Unit)) {
            this.requestCode = requestCode
            this.intent = intent
            this.callback = callback
        }

        private var activityStarted = false

        override fun onAttach(activity: Activity) {
            super.onAttach(activity)
            if (!activityStarted) {
                activityStarted = true
                intent?.let { startActivityForResult(it, requestCode) }
            }
        }

        override fun onAttach(context: Context) {
            super.onAttach(context)
            if (!activityStarted) {
                activityStarted = true
                intent?.let { startActivityForResult(it, requestCode) }
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == this.requestCode) {
                callback?.let { it(resultCode, data) }
            }
        }

        override fun onDetach() {
            super.onDetach()
            intent = null
            callback = null
        }
    }
}
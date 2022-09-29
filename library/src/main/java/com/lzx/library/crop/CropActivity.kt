package com.lzx.library.crop

import android.content.Intent
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lzx.library.R
import com.lzx.library.crop.callback.BitmapCropCallback
import com.lzx.library.crop.callback.CropBoundsChangeListener
import com.lzx.library.crop.view.GestureCropImageView
import com.lzx.library.crop.view.OverlayView
import com.lzx.library.utils.DisplayUtil
import com.lzx.library.utils.dp2px

/**
 * 裁剪界面
 */
class CropActivity : AppCompatActivity() {

    //纵横比
    private val aspectRatioX = 1
    private val aspectRatioY = 1
    private var cropSizeWidth = 200
    private var cropSizeHeight = 200

    private val mCompressFormat: CompressFormat = CompressFormat.JPEG
    private val mCompressQuality: Int = 100

    private var viewOverlay: OverlayView? = null
    private var gestureCropImageView: GestureCropImageView? = null
    private var btnFinish: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)

        viewOverlay = findViewById(R.id.viewOverlay)
        gestureCropImageView = findViewById(R.id.gestureCropImageView)
        btnFinish = findViewById(R.id.btnFinish)

        val targetAspectRatio = (aspectRatioX / aspectRatioY).toFloat()
        cropSizeWidth = DisplayUtil.getPhoneWidth() - 30.dp2px
        cropSizeHeight = (cropSizeWidth * targetAspectRatio).toInt()

        viewOverlay?.setShowCropGrid(false)
        gestureCropImageView?.cropBoundsChangeListener = object : CropBoundsChangeListener {
            override fun onCropAspectRatioChanged(cropRatio: Float) {
                viewOverlay?.setTargetAspectRatio(cropRatio)
            }
        }
        gestureCropImageView?.targetAspectRatio = 1f
        gestureCropImageView?.setImageToWrapCropBounds()

        setImageData(intent)

        btnFinish?.setOnClickListener {
            gestureCropImageView?.cropAndSaveImage(mCompressFormat,
                mCompressQuality,
                object : BitmapCropCallback {
                    override fun onBitmapCropped(resultUri: Uri,
                                                 resultPath: String?,
                                                 offsetX: Int,
                                                 offsetY: Int,
                                                 imageWidth: Int,
                                                 imageHeight: Int) {
                        setResult(RESULT_OK, Intent().apply {
                            putExtra(CropCode.EXTRA_OUTPUT_URI, resultUri)
                            putExtra(CropCode.EXTRA_OUTPUT_PATH, resultPath)
                            putExtra(CropCode.EXTRA_OUTPUT_CROP_ASPECT_RATIO,
                                gestureCropImageView?.targetAspectRatio)
                            putExtra(CropCode.EXTRA_OUTPUT_IMAGE_WIDTH, imageWidth)
                            putExtra(CropCode.EXTRA_OUTPUT_IMAGE_HEIGHT, imageHeight)
                            putExtra(CropCode.EXTRA_OUTPUT_OFFSET_X, offsetX)
                            putExtra(CropCode.EXTRA_OUTPUT_OFFSET_Y, offsetY)
                        })
                        finish()
                    }

                    override fun onCropFailure(t: Throwable) {
                    }
                })
        }
    }

    private fun setImageData(intent: Intent) {
        val inputUri = intent.getParcelableExtra<Uri?>(CropCode.EXTRA_INPUT_URI)
        if (inputUri == null) {
            finish()
            return
        }
        gestureCropImageView?.setImageUri(inputUri, null)
    }

    override fun onStop() {
        super.onStop()
        gestureCropImageView?.cancelAllAnimations()
    }
}
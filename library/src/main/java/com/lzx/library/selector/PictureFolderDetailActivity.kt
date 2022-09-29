package com.lzx.library.selector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.annotaion.DividerOrientation
import com.lzx.library.R
import com.lzx.library.api.KaleConfig
import com.lzx.library.api.PictureCode
import com.lzx.library.bean.LocalMedia
import com.lzx.library.bean.LocalMediaFolder
import com.lzx.library.crop.CropActivity
import com.lzx.library.crop.CropCode
import com.lzx.library.loader.LocalMediaLoader
import com.lzx.library.utils.FileUtils
import com.lzx.library.utils.divider
import com.lzx.library.utils.dp2px
import com.lzx.library.utils.grid
import com.lzx.library.utils.loadImage
import com.lzx.library.utils.navigationToForResult
import com.lzx.library.utils.setup
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.io.File

class PictureFolderDetailActivity : AppCompatActivity() {

    companion object {

        const val TAG = "PictureFolderDetailFragment"
    }

    private var folderName: String? = null
    private var firstImagePath: String? = null

    private val loader = LocalMediaLoader()

    private var recycleView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture_folder_detail)
        recycleView = findViewById(R.id.recycleView)
        folderName = intent.getStringExtra("folderName")
        firstImagePath = intent.getStringExtra("firstImagePath")
        lifecycleScope.launch {
            flow {
                emit(loader.loadAllAlbum(this@PictureFolderDetailActivity))
            }.catch { it.printStackTrace() }.collect { it ->
                val list = it.filter { it.folderName == folderName && it.firstImagePath == firstImagePath }
                handleAllAlbumData(list.toMutableList())
            }
        }
    }

    private fun handleAllAlbumData(list: MutableList<LocalMediaFolder>) {
        if (list.size == 1) {
            recycleView?.grid(3)?.divider {
                setDivider(2.dp2px)
                orientation = DividerOrientation.GRID
                startVisible = true
                endVisible = true
            }?.setup {
                addType<LocalMedia>(R.layout.item_picture_detail)
                onBind {
                    loadImage(R.id.image, getModel<LocalMedia>().realPath)
                }
                R.id.image.onClick {
                    val media = getModel<LocalMedia>()
                    val currentEditPath = media.realPath
                    val inputUri = if (FileUtils.isContent(currentEditPath)) {
                        Uri.parse(currentEditPath)
                    } else Uri.fromFile(File(currentEditPath))
                    if (KaleConfig.isNeedCut) {
                        navigationToForResult<CropActivity>(CropCode.CROP_REQUEST_CODE,
                            CropCode.EXTRA_INPUT_URI to inputUri)
                    } else {
                        if (KaleConfig.isNeedPreview) {
                            navigationToForResult<PictureFolderDetailPreviewActivity>(
                                PictureFolderDetailPreviewActivity.REQUEST_CODE,
                                "list" to list[0].data,
                                "position" to modelPosition)
                        } else {
                            setResult(RESULT_OK, Intent().apply {
                                putExtra(PictureCode.EXTRA_OUTPUT_URI, inputUri.toString())
                                putExtra(PictureCode.EXTRA_OUTPUT_PATH, media.realPath)
                            })
                            finish()
                        }
                    }
                }
            }?.models = list[0].data
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == CropCode.CROP_REQUEST_CODE && data != null) {
                val resultUri = data.getStringExtra(CropCode.EXTRA_OUTPUT_URI)
                val resultPath = data.getStringExtra(CropCode.EXTRA_OUTPUT_PATH)
                val targetAspectRatio = data.getFloatExtra(CropCode.EXTRA_OUTPUT_CROP_ASPECT_RATIO, 1f)
                val imageWidth = data.getIntExtra(CropCode.EXTRA_OUTPUT_IMAGE_WIDTH, 0)
                val imageHeight = data.getIntExtra(CropCode.EXTRA_OUTPUT_IMAGE_HEIGHT, 0)
                val offsetX = data.getIntExtra(CropCode.EXTRA_OUTPUT_OFFSET_X, 0)
                val offsetY = data.getIntExtra(CropCode.EXTRA_OUTPUT_OFFSET_Y, 0)
                setResult(RESULT_OK, Intent().apply {
                    putExtra(PictureCode.EXTRA_OUTPUT_URI, resultUri)
                    putExtra(PictureCode.EXTRA_OUTPUT_PATH, resultPath)
                    putExtra(PictureCode.EXTRA_OUTPUT_CROP_ASPECT_RATIO, targetAspectRatio)
                    putExtra(PictureCode.EXTRA_OUTPUT_IMAGE_WIDTH, imageWidth)
                    putExtra(PictureCode.EXTRA_OUTPUT_IMAGE_HEIGHT, imageHeight)
                    putExtra(PictureCode.EXTRA_OUTPUT_OFFSET_X, offsetX)
                    putExtra(PictureCode.EXTRA_OUTPUT_OFFSET_Y, offsetY)
                })
                finish()
            } else if (requestCode == PictureFolderDetailPreviewActivity.REQUEST_CODE && data != null) {
                setResult(RESULT_OK, Intent().apply {
                    putExtra(PictureCode.EXTRA_OUTPUT_URI, data.getStringExtra(PictureCode.EXTRA_OUTPUT_URI))
                    putExtra(PictureCode.EXTRA_OUTPUT_PATH,
                        data.getStringExtra(PictureCode.EXTRA_OUTPUT_PATH))
                })
                finish()
            }
        }
    }
}
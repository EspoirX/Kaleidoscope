package com.lzx.library.selector

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.lzx.library.R
import com.lzx.library.api.KaleConfig
import com.lzx.library.api.PictureCode
import com.lzx.library.bean.LocalMediaFolder
import com.lzx.library.loader.LocalMediaLoader
import com.lzx.library.utils.itemClicked
import com.lzx.library.utils.linear
import com.lzx.library.utils.loadImage
import com.lzx.library.utils.navigationToForResult
import com.lzx.library.utils.setText
import com.lzx.library.utils.setup
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * 没有拍照，单选的相册
 */
class PictureFoldersFragment : Fragment() {

    companion object {
        const val TAG = "PictureFoldersFragment"

        fun newInstance(): PictureFoldersFragment {
            val fragment = PictureFoldersFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_medias_folder, container, false)
    }

    private var btnBack: ImageView? = null
    private var recycleView: RecyclerView? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnBack = view.findViewById(R.id.btnBack)
        recycleView = view.findViewById(R.id.recycleView)

        btnBack?.setOnClickListener { activity?.finish() }
        initData()
    }

    private fun initData() {
        val loader = LocalMediaLoader()
        lifecycleScope.launch {
            flow {
                emit(loader.loadAllAlbum(requireActivity()))
            }.catch { it.printStackTrace() }.collect {
                handleAllAlbumData(it)
            }
        }
    }

    /**
     * 数据处理
     */
    private fun handleAllAlbumData(list: MutableList<LocalMediaFolder>) {
        recycleView?.linear()?.setup {
            addType<LocalMediaFolder>(R.layout.item_selectpic_images_folder)
            onBind {
                val info = getModel<LocalMediaFolder>()
                loadImage(R.id.image, info.firstImagePath)
                setText(R.id.tv_folder, info.folderName)
                setText(R.id.tv_num, info.data.size.toString() + "张")
                itemClicked {
                    navigationToForResult<PictureFolderDetailActivity>(PictureCode.PIC_DETAIL_REQUEST,
                        "folderName" to info.folderName,
                        "firstImagePath" to info.firstImagePath)
                }
            }
        }?.models = list
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == PictureCode.PIC_DETAIL_REQUEST && data != null) {
            val resultUri = data.getStringExtra(PictureCode.EXTRA_OUTPUT_URI)
            val resultPath = data.getStringExtra(PictureCode.EXTRA_OUTPUT_PATH)
            activity?.setResult(AppCompatActivity.RESULT_OK, Intent().apply {
                putExtra(PictureCode.EXTRA_OUTPUT_URI, resultUri)
                putExtra(PictureCode.EXTRA_OUTPUT_PATH, resultPath)
                if (KaleConfig.isNeedCut) {
                    val targetAspectRatio = data.getFloatExtra(PictureCode.EXTRA_OUTPUT_CROP_ASPECT_RATIO, 1f)
                    val imageWidth = data.getIntExtra(PictureCode.EXTRA_OUTPUT_IMAGE_WIDTH, 0)
                    val imageHeight = data.getIntExtra(PictureCode.EXTRA_OUTPUT_IMAGE_HEIGHT, 0)
                    val offsetX = data.getIntExtra(PictureCode.EXTRA_OUTPUT_OFFSET_X, 0)
                    val offsetY = data.getIntExtra(PictureCode.EXTRA_OUTPUT_OFFSET_Y, 0)
                    putExtra(PictureCode.EXTRA_OUTPUT_CROP_ASPECT_RATIO, targetAspectRatio)
                    putExtra(PictureCode.EXTRA_OUTPUT_IMAGE_WIDTH, imageWidth)
                    putExtra(PictureCode.EXTRA_OUTPUT_IMAGE_HEIGHT, imageHeight)
                    putExtra(PictureCode.EXTRA_OUTPUT_OFFSET_X, offsetX)
                    putExtra(PictureCode.EXTRA_OUTPUT_OFFSET_Y, offsetY)
                }
            })
            activity?.finish()
        }
    }
}
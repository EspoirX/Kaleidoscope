package com.lzx.library.selector

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.lzx.library.R
import com.lzx.library.api.KaleConfig
import com.lzx.library.bean.LocalMediaFolder
import com.lzx.library.utils.SelectedManager
import com.lzx.library.utils.put
import com.lzx.library.utils.showToast

class PictureSelectorPreviewFragment : Fragment() {
    companion object {
        const val TAG = "PictureSelectorPreviewFragment"

        /**
         * isPreviewAll ：点图片预览为true,点左下角预览为false
         */
        fun newInstance(albumIndex: Int,
                        position: Int,
                        isPreviewAll: Boolean): PictureSelectorPreviewFragment {
            val fragment = PictureSelectorPreviewFragment()
            fragment.arguments = Bundle().apply {
                put("albumIndex", albumIndex)
                put("position", position)
                put("isPreviewAll", isPreviewAll)
            }
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_picture_selector_preview, container, false)
    }

    private var viewPager: ViewPager? = null
    private var btnBack: ImageView? = null
    private var previewStatus: ImageView? = null
    private var textNums: TextView? = null
    private var previewComplete: TextView? = null

    private var mAlbumIndex: Int = 0
    private var mPosition: Int = 1
    private var isPreviewAll: Boolean = true
    private var mediaFolders = mutableListOf<LocalMediaFolder>()

    private var adapter: PicturePreviewAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = view.findViewById(R.id.viewPager)
        btnBack = view.findViewById(R.id.btnBack)
        previewStatus = view.findViewById(R.id.previewStatus)
        textNums = view.findViewById(R.id.textNums)
        previewComplete = view.findViewById(R.id.previewComplete)

        viewPager?.isVerticalFadingEdgeEnabled = false // 取消竖直渐变边框
        viewPager?.isHorizontalFadingEdgeEnabled = false // 取消水平渐变边框
        runCatching {
            adapter = PicturePreviewAdapter(requireActivity())
            viewPager?.adapter = adapter
        }
        mAlbumIndex = arguments?.getInt("albumIndex") ?: 0
        mPosition = arguments?.getInt("position") ?: 1
        isPreviewAll = arguments?.getBoolean("isPreviewAll") ?: true
        updateBtnFinish()
        if (mediaFolders.size > 0) {
            val medias = getList() ?: mutableListOf()
            adapter?.setData(medias.toMutableList())
            adapter?.notifyDataSetChanged()
            viewPager?.currentItem = mPosition

            val media = medias.getOrNull(mPosition)
            media?.let {
                previewStatus?.isSelected = it.isChecked
            }
        }
        btnBack?.setOnClickListener {
            onBackToSelectorFragment()
        }
        previewStatus?.setOnClickListener {
            if (SelectedManager.selectCount >= KaleConfig.maxSelectNum) {
                showToast("你最多选中" + KaleConfig.maxSelectNum + "张图片")
                return@setOnClickListener
            }
            val media = getList()?.getOrNull(mPosition)

            if (media?.isChecked == true) {
                media.isChecked = false
                previewStatus?.isSelected = false
                SelectedManager.removeSelectResult(media)
                val unCheckedPos = media.checkedCount
                SelectedManager.selectedResult.forEach {
                    if (it.checkedCount >= unCheckedPos) {
                        var checkedCount = it.checkedCount
                        checkedCount -= 1
                        it.checkedCount = checkedCount
                    }
                    mediaFolders.getOrNull(mAlbumIndex)?.data?.set(media.position, media)
                }
            } else {
                media?.isChecked = true
                previewStatus?.isSelected = true
                var selectCount = 0
                mediaFolders.forEach { it ->
                    selectCount += it.data.filter { !it.isDisplayCamera && it.isChecked }.size
                }
                media?.checkedCount = selectCount
                SelectedManager.addSelectResult(media)
            }
            updateBtnFinish()
        }
        viewPager?.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                mPosition = position
                val media = getList()?.getOrNull(mPosition)
                previewStatus?.isSelected = media?.isChecked == true
            }
        })
        previewComplete?.setOnClickListener {
            onBackToSelectorFragment()
        }
    }

    fun getList() = mediaFolders.getOrNull(mAlbumIndex)?.data?.filter {
        if (isPreviewAll) {
            !it.isDisplayCamera
        } else {
            !it.isDisplayCamera && it.isChecked
        }
    }

    /**
     * 返回上一个界面
     */
    private fun onBackToSelectorFragment() {
        activity?.onBackPressed()
    }

    /**
     * 设置数据
     */
    fun setPreviewData(mediaFolders: MutableList<LocalMediaFolder>) {
        this.mediaFolders.clear()
        this.mediaFolders.addAll(mediaFolders)
    }

    /**
     * 更新完成按钮
     */
    @SuppressLint("SetTextI18n")
    private fun updateBtnFinish() {
        if (SelectedManager.selectCount == 0) {
            previewComplete?.alpha = 0.3f
            previewComplete?.text = "完成"
            previewComplete?.isEnabled = false
            textNums?.text = ""
        } else {
            previewComplete?.alpha = 1f
            previewComplete?.text = "完成(" + SelectedManager.selectCount + ")"
            previewComplete?.isEnabled = true
            textNums?.text = "已选(${SelectedManager.selectCount})"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaFolders.getOrNull(mAlbumIndex)?.data?.forEach {
            if (it.isChecked) {
                SelectedManager.addSelectResult(it)
            }
        }
        val fragments = activity?.supportFragmentManager?.fragments
        fragments?.forEach {
            if (it is PictureSelectorFragment) {
                it.updateWhenPreviewBack(mediaFolders, isPreviewAll)
            }
        }
    }
}
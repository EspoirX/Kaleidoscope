package com.lzx.library.selector

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.drake.brv.annotaion.DividerOrientation
import com.hjq.permissions.XXPermissions
import com.lzx.library.R
import com.lzx.library.api.KaleConfig
import com.lzx.library.api.PictureCode
import com.lzx.library.bean.LocalMedia
import com.lzx.library.bean.LocalMediaFolder
import com.lzx.library.loader.LocalMediaLoader
import com.lzx.library.utils.BitmapLoadUtils
import com.lzx.library.utils.FileUtils
import com.lzx.library.utils.MediaUtils
import com.lzx.library.utils.PictureMediaScannerConnection
import com.lzx.library.utils.SdkVersionUtils
import com.lzx.library.utils.SelectedManager
import com.lzx.library.utils.divider
import com.lzx.library.utils.dp2px
import com.lzx.library.utils.getUriPath
import com.lzx.library.utils.grid
import com.lzx.library.utils.itemClicked
import com.lzx.library.utils.models
import com.lzx.library.utils.navigationToForResult
import com.lzx.library.utils.notifyItemChanged
import com.lzx.library.utils.setup
import com.lzx.library.utils.showToast
import com.lzx.library.view.AlbumFolderLayout
import com.lzx.library.view.rclayout.RCImageView
import com.lzx.library.crop.CropActivity
import com.lzx.library.crop.CropCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PictureSelectorFragment : Fragment() {

    companion object {
        const val TAG = "PictureSelectorFragment"

        fun newInstance(): PictureSelectorFragment {
            val fragment = PictureSelectorFragment()
            fragment.arguments = Bundle()
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_photos_select, container, false)
    }

    private var mCurrentAlbumIndex = 0
    private var mediaFolders = mutableListOf<LocalMediaFolder>()

    private var btnBack: ImageView? = null
    private var barTitleStatus: ImageView? = null
    private var barTitle: TextView? = null
    private var recycleView: RecyclerView? = null
    private var selectAlbumView: AlbumFolderLayout? = null
    private var albumSpace: View? = null
    private var albumRecyclerView: RecyclerView? = null
    private var btnFinish: TextView? = null
    private var btnPreview: TextView? = null
    private var barTitleLayout: LinearLayout? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnBack = view.findViewById(R.id.btnBack)
        barTitleStatus = view.findViewById(R.id.barTitleStatus)
        barTitle = view.findViewById(R.id.barTitle)
        recycleView = view.findViewById(R.id.recycleView)
        selectAlbumView = view.findViewById(R.id.selectAlbumView)
        albumSpace = view.findViewById(R.id.albumSpace)
        albumRecyclerView = view.findViewById(R.id.albumRecyclerView)
        btnPreview = view.findViewById(R.id.btnPreview)
        btnFinish = view.findViewById(R.id.btnFinish)
        barTitleLayout = view.findViewById(R.id.barTitleLayout)

        selectAlbumView?.listener = object : AlbumFolderLayout.OnAlbumItemClick {
            override fun onItemClick(position: Int, info: LocalMediaFolder) {
                mCurrentAlbumIndex = position
                mediaFolders.getOrNull(mCurrentAlbumIndex)?.let {
                    barTitle?.text = it.folderName
                    recycleView?.models = getRealLocalMedias(it.data)
                }
                selectAlbumView?.dismiss()
                selectAlbumView?.startRotateDownAnim(barTitleStatus)
            }
        }
        albumSpace?.setOnClickListener {
            selectAlbumView?.dismiss()
            selectAlbumView?.startRotateDownAnim(barTitleStatus)
        }
        btnBack?.setOnClickListener {
            activity?.finish()
        }

        initPictureRecycleView()

        btnPreview?.setOnClickListener {
            if (SelectedManager.selectCount > 0) {
                onStartPreview(0, false)
            }
        }
        btnFinish?.setOnClickListener {
            if (SelectedManager.selectCount > 0) {
                val list = arrayListOf<String>()
                SelectedManager.selectedResult.forEach {
                    list.add(it.realPath.orEmpty())
                }
                val resultIntent = Intent()
                resultIntent.putExtra(PictureCode.EXTRA_MODE, PictureCode.MODE_SELECTION)
                resultIntent.putStringArrayListExtra(PictureCode.EXTRA_OUTPUT_PATHS, list)
                if (list.size == 1) {
                    resultIntent.putExtra(PictureCode.EXTRA_OUTPUT_PATH, list[0])
                }
                activity?.setResult(Activity.RESULT_OK, resultIntent)
                activity?.finish()
            }
        }

        initData()
    }

    /**
     * 数据加载
     */
    private fun initData() {
        val loader = LocalMediaLoader()
        lifecycleScope.launch {
            flow {
                emit(loader.loadAllAlbum(requireActivity()))
            }.catch { it.printStackTrace() }.collect { list ->
                if (list.size > 0) {
                    mediaFolders.clear()
                    mediaFolders.addAll(list)
                    barTitleLayout?.isVisible = true
                    barTitleLayout?.setOnClickListener {
                        if (selectAlbumView?.visibility == View.VISIBLE) {
                            selectAlbumView?.dismiss()
                            selectAlbumView?.startRotateDownAnim(barTitleStatus)
                        } else {
                            selectAlbumView?.setData(list)
                            selectAlbumView?.show(list.size)
                            selectAlbumView?.startRotateUpAnim(barTitleStatus)
                        }
                    }
                    val firstMedia = list[mCurrentAlbumIndex]
                    barTitle?.text = firstMedia.folderName
                    recycleView?.models = getRealLocalMedias(firstMedia.data)
                }
            }
        }
    }

    /**
     * 图片列表
     */
    private fun initPictureRecycleView() {
        recycleView?.setHasFixedSize(true)
        recycleView?.grid(3)?.divider {
            setDivider(12.dp2px)
            orientation = DividerOrientation.GRID
            startVisible = true
            endVisible = true
        }?.setup {
            addType<LocalMedia> {
                if (this.isDisplayCamera) {
                    R.layout.item_photo_select_camera
                } else {
                    R.layout.item_select_photo
                }
            }
            onPayload {
                if (itemViewType == R.layout.item_select_photo) {
                    val info = getModel<LocalMedia>()
                    val itemSelect = findView<TextView>(R.id.item_select)
                    itemSelect.isSelected = info.isChecked
                    if (info.isChecked) {
                        itemSelect.text = info.checkedCount.toString()
                    } else {
                        itemSelect.text = ""
                    }
                }
            }
            onBind {
                if (itemViewType == R.layout.item_photo_select_camera) {
                    itemClicked { openSelectedCamera() }
                } else {
                    val info = getModel<LocalMedia>()
                    val imageView = findView<RCImageView>(R.id.item_image_view_photo)
                    Glide.with(requireActivity()).load(info.realPath).override(getItemSize(), getItemSize())
                        .into(imageView)
                    val itemSelect = findView<TextView>(R.id.item_select)
                    itemSelect.isSelected = info.isChecked
                    if (info.isChecked) {
                        itemSelect.text = info.checkedCount.toString()
                    } else {
                        itemSelect.text = ""
                    }
                    itemSelect.setOnClickListener { view ->
                        if (info.isChecked) {
                            info.isChecked = false
                            itemSelect.isSelected = false
                            itemSelect.text = ""
                            SelectedManager.removeSelectResult(info)
                            updateBtnFinish()
                            val unCheckedPos = info.checkedCount
                            SelectedManager.selectedResult.forEach {
                                if (it.checkedCount >= unCheckedPos) {
                                    var checkedCount = it.checkedCount
                                    checkedCount -= 1
                                    it.checkedCount = checkedCount
                                }
                                notifyItemChanged(it.position, it)
                            }
                            notifyItemChanged(modelPosition, info)
                        } else {
                            if (SelectedManager.selectCount < KaleConfig.maxSelectNum) {
                                info.isChecked = true
                                var selectCount = 0
                                mediaFolders.forEach { it ->
                                    selectCount += it.data.filter { it.isChecked }.size
                                }
                                info.checkedCount = selectCount
                                itemSelect.isSelected = true
                                SelectedManager.addSelectResult(info)
                                itemSelect.text = info.checkedCount.toString()
                                updateBtnFinish()
                            } else {
                                showToast("你最多选中" + KaleConfig.maxSelectNum + "张图片")
                            }
                        }
                    }
                }
            }
            R.id.item_image_view_photo.onClick {
                val pos = if (KaleConfig.isDisplayCamera) modelPosition - 1 else modelPosition
                onStartPreview(pos, true)
            }
        }
    }

    /**
     * 更新完成按钮
     */
    private fun updateBtnFinish() {
        if (SelectedManager.selectCount == 0) {
            btnFinish?.alpha = 0.3f
            btnFinish?.isEnabled = false
        } else {
            btnFinish?.alpha = 1f
            btnFinish?.isEnabled = true
        }
    }

    /**
     * 打开相机
     */
    private var cameraImageUri: Uri? = null
    private var cameraFile: File? = null
    private fun openSelectedCamera() {
        XXPermissions.with(this).permission(Manifest.permission.CAMERA).request { permissions, all ->
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (cameraIntent.resolveActivity(requireActivity().packageManager) != null) {
                val data = MediaUtils.createCameraOutImageUri(requireContext())
                cameraImageUri = data.first
                cameraFile = data.second
                if (cameraImageUri != null) {
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                    startActivityForResult(cameraIntent, PictureCode.REQUEST_CAMERA)
                }
            }
        }
    }

    /**
     * 打开预览
     * modelPosition 图片位置 0 开始
     */
    private fun onStartPreview(modelPosition: Int, isPreviewAll: Boolean) {
        val albumIndex = mCurrentAlbumIndex
        val previewFragment =
            PictureSelectorPreviewFragment.newInstance(albumIndex, modelPosition, isPreviewAll)
        previewFragment.setPreviewData(mediaFolders)
        val ft = activity?.supportFragmentManager?.beginTransaction()
        ft?.setCustomAnimations(R.anim.photo_for_big_pic_enter,
            R.anim.photo_for_big_pic_finish,
            R.anim.photo_for_big_pic_enter,
            R.anim.photo_for_big_pic_finish)
        ft?.add(R.id.fragment_container, previewFragment)
        ft?.addToBackStack(null)
        ft?.commitAllowingStateLoss()
    }

    /**
     * 预览返回后更新数据
     */
    fun updateWhenPreviewBack(mediaFolders: MutableList<LocalMediaFolder>, isPreviewAll: Boolean) {
        if (isPreviewAll) {
            this.mediaFolders.clear()
            this.mediaFolders.addAll(mediaFolders)
        } else {
            this.mediaFolders.forEach { old ->
                mediaFolders.forEach { new ->
                    if (old.folderName == new.folderName && old.firstImagePath == new.firstImagePath && old.firstMimeType == new.firstMimeType) {
                        old.data.filter { !it.isDisplayCamera }.forEach { oldInfo ->
                            new.data.filter { !it.isDisplayCamera }.forEach { newInfo ->
                                if (oldInfo.realPath == newInfo.realPath && oldInfo.isChecked != newInfo.isChecked) {
                                    oldInfo.isChecked = newInfo.isChecked
                                }
                            }
                        }
                    }
                }
            }
        }
        this.mediaFolders.getOrNull(mCurrentAlbumIndex)?.data?.filter { !it.isDisplayCamera }?.forEach {
            recycleView?.notifyItemChanged(it.position, it, true)
        }
        updateBtnFinish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PictureCode.REQUEST_CAMERA) {
                dispatchHandleCamera(data)
            } else if (requestCode == CropCode.CROP_REQUEST_CODE && data != null) {
                val resultUri = data.getStringExtra(CropCode.EXTRA_OUTPUT_URI)
                val resultPath = data.getStringExtra(CropCode.EXTRA_OUTPUT_PATH)
                val targetAspectRatio = data.getFloatExtra(CropCode.EXTRA_OUTPUT_CROP_ASPECT_RATIO, 1f)
                val imageWidth = data.getIntExtra(CropCode.EXTRA_OUTPUT_IMAGE_WIDTH, 0)
                val imageHeight = data.getIntExtra(CropCode.EXTRA_OUTPUT_IMAGE_HEIGHT, 0)
                val offsetX = data.getIntExtra(CropCode.EXTRA_OUTPUT_OFFSET_X, 0)
                val offsetY = data.getIntExtra(CropCode.EXTRA_OUTPUT_OFFSET_Y, 0)
                activity?.setResult(AppCompatActivity.RESULT_OK, Intent().apply {
                    putExtra(PictureCode.EXTRA_OUTPUT_URI, resultUri)
                    putExtra(PictureCode.EXTRA_OUTPUT_PATH, resultPath)
                    putExtra(PictureCode.EXTRA_OUTPUT_CROP_ASPECT_RATIO, targetAspectRatio)
                    putExtra(PictureCode.EXTRA_OUTPUT_IMAGE_WIDTH, imageWidth)
                    putExtra(PictureCode.EXTRA_OUTPUT_IMAGE_HEIGHT, imageHeight)
                    putExtra(PictureCode.EXTRA_OUTPUT_OFFSET_X, offsetX)
                    putExtra(PictureCode.EXTRA_OUTPUT_OFFSET_Y, offsetY)
                })
                activity?.finish()
            }
        }
    }

    /**
     * 相机事件回调处理
     */
    private fun dispatchHandleCamera(intent: Intent?) {
        lifecycleScope.launch(Dispatchers.IO) {
            var outPutUri = intent?.getParcelableExtra<Uri?>(MediaStore.EXTRA_OUTPUT)
            if (intent == null || outPutUri == null) {
                outPutUri = cameraImageUri
            }
            var outputPath: String?
            if (SdkVersionUtils.isQ() && cameraFile == null) {
                outputPath = outPutUri?.getUriPath()
            } else {
                outputPath = FileUtils.getPath(activity, outPutUri)
                if (outputPath == null && cameraFile != null) {
                    outputPath = cameraFile!!.absolutePath
                }
            }
            if (!outputPath.isNullOrEmpty()) {
                val media = LocalMedia.generateLocalMedia(context, outputPath)
                if (SdkVersionUtils.isQ() && !FileUtils.isContent(outputPath)) {
                    media.path = cameraImageUri.toString()
                    media.realPath = outputPath
                } else {
                    if (cameraFile != null) {
                        media.realPath = outputPath
                        media.path = outputPath
                    }
                }
                BitmapLoadUtils.rotateImage(context, outputPath)
                withContext(Dispatchers.Main) {
                    onScannerScanFile(media, outputPath)
                    if (KaleConfig.isNeedCut) {
                        if (!media.realPath.isNullOrEmpty()) {
                            val inputUri = if (FileUtils.isContent(media.realPath)) {
                                Uri.parse(media.realPath)
                            } else Uri.fromFile(File(media.realPath))
                            navigationToForResult<CropActivity>(CropCode.CROP_REQUEST_CODE,
                                CropCode.EXTRA_INPUT_URI to inputUri)
                        } else {
                        }
                    } else {
                        val resultIntent = Intent()
                        resultIntent.putExtra(PictureCode.EXTRA_MODE, PictureCode.MODE_CAMERA)
                        resultIntent.putExtra(PictureCode.EXTRA_OUTPUT_PATH, media.realPath)
                        activity?.setResult(Activity.RESULT_OK, resultIntent)
                        activity?.finish()
                    }
                }
            } else {
                showToast("相机发生错误，拍照失败")
            }
        }
    }

    /**
     * 刷新相册
     *
     * @param media 要刷新的对象
     */
    private fun onScannerScanFile(media: LocalMedia, outputPath: String) {
        if (SdkVersionUtils.isQ()) {
            if (FileUtils.isHasVideo(media.mimeType) && FileUtils.isContent(outputPath)) {
                PictureMediaScannerConnection(activity, media.realPath)
            }
        } else {
            val path = if (FileUtils.isContent(outputPath)) media.realPath else outputPath
            PictureMediaScannerConnection(activity, path)
            if (FileUtils.isHasImage(media.mimeType)) {
                var lastImageId: Int = -1
                val dirFile = File(path)
                dirFile.parent?.let {
                    lastImageId = MediaUtils.getDCIMLastImageId(context, it)
                }
                if (lastImageId != -1) {
                    MediaUtils.removeMedia(context, lastImageId)
                }
            }
        }
    }

    /**
     * 添加相机按钮
     */
    private fun getRealLocalMedias(curr: MutableList<LocalMedia>): MutableList<LocalMedia> {
        return if (KaleConfig.isDisplayCamera) {
            val camera = LocalMedia().apply { this.isDisplayCamera = true }
            if (curr.getOrNull(0)?.isDisplayCamera == false) {
                curr.add(0, camera)
            }
            curr
        } else {
            curr
        }
    }

    private fun getItemSize(): Int { //屏幕宽
        var screenWidth = resources.displayMetrics.widthPixels //扣除左右间距
        screenWidth -= 12.dp2px * 2
        screenWidth -= 3.dp2px * 2
        return screenWidth / 3
    }

    override fun onDestroyView() {
        super.onDestroyView()
        SelectedManager.clearSelectResult()
    }
}
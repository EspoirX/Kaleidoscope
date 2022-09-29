package com.lzx.library.selector

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.lzx.library.R
import com.lzx.library.api.PictureCode
import com.lzx.library.bean.LocalMedia
import com.lzx.library.utils.FileUtils
import java.io.File

class PictureFolderDetailPreviewActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE = 543
    }

    private var adapter: PicturePreviewAdapter? = null
    private var list = arrayListOf<LocalMedia>()
    private var currPagePosition = 0

    private var btnBack: ImageView? = null
    private var titleName: TextView? = null
    private var btnSelect: TextView? = null
    private var viewPager: ViewPager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder_detail_preview)

        btnBack = findViewById(R.id.btnBack)
        titleName = findViewById(R.id.titleName)
        viewPager = findViewById(R.id.viewPager)
        btnSelect = findViewById(R.id.btnSelect)

        btnBack?.setOnClickListener { finish() }
        intent?.let { it ->
            list = it.getParcelableArrayListExtra<LocalMedia>("list") ?: arrayListOf()
            val position = it.getIntExtra("position", 0)
            currPagePosition = position
            val media = list.getOrNull(position)
            titleName?.text = media?.parentFolderName
            viewPager?.isVerticalFadingEdgeEnabled = false // 取消竖直渐变边框
            viewPager?.isHorizontalFadingEdgeEnabled = false // 取消水平渐变边框
            runCatching {
                adapter = PicturePreviewAdapter(this@PictureFolderDetailPreviewActivity)
                viewPager?.adapter = adapter

                adapter?.setData(list.toMutableList())
                adapter?.notifyDataSetChanged()
                viewPager?.setCurrentItem(position)
            }
        }
        viewPager?.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currPagePosition = position
            }
        })

        btnSelect?.setOnClickListener {
            val media = list.getOrNull(currPagePosition)
            media?.let {
                val currentEditPath = media.realPath
                val inputUri = if (FileUtils.isContent(currentEditPath)) {
                    Uri.parse(currentEditPath)
                } else Uri.fromFile(File(currentEditPath))
                setResult(RESULT_OK, Intent().apply {
                    putExtra(PictureCode.EXTRA_OUTPUT_URI, inputUri.toString())
                    putExtra(PictureCode.EXTRA_OUTPUT_PATH, media.realPath)
                })
            }
            finish()
        }
    }
}
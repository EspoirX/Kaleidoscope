package com.lzx.library.selector

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.lzx.library.R
import com.lzx.library.bean.LocalMedia
import com.lzx.library.view.photoview.PhotoView

class PicturePreviewAdapter(private val context: Context) : PagerAdapter() {
    private var mData = mutableListOf<LocalMedia>()

    fun setData(data: MutableList<LocalMedia>) {
        mData.clear()
        mData.addAll(data.filter { !it.isDisplayCamera })
    }

    override fun getCount(): Int = mData.size

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        val layout = obj as View
        container.removeView(layout)
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return view == obj
    }

    private fun getItem(position: Int): LocalMedia? {
        return mData.getOrNull(position)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layout = LayoutInflater.from(context).inflate(R.layout.item_preview_image, null)
        val photoView = layout.findViewById<PhotoView>(R.id.photoView)
        val media = getItem(position)
        media?.let {
            Glide.with(photoView.context).load(it.realPath).into(photoView)
        }
        container.addView(layout)
        layout.tag = position
        return layout
    }
}
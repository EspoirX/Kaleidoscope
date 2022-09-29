package com.lzx.library.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.view.animation.TranslateAnimation
import android.widget.RelativeLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.lzx.library.R
import com.lzx.library.bean.LocalMediaFolder
import com.lzx.library.utils.dp2px
import com.lzx.library.utils.itemClicked
import com.lzx.library.utils.loadImage
import com.lzx.library.utils.setText


class AlbumFolderLayout @JvmOverloads constructor(context: Context?,
                                                  attrs: AttributeSet? = null,
                                                  defStyleAttr: Int = 0) :
    RelativeLayout(context, attrs, defStyleAttr) {

    private var inAlphaAnim: Animation? = null
    private var outAlphaAnim: Animation? = null
    private var inTransAnim: Animation? = null
    private var outTransAnim: Animation? = null
    private var mRotateUpAnim: RotateAnimation? = null
    private var mRotateDownAnim: RotateAnimation? = null
    private var recyclerView: RecyclerView? = null
    private var albumSpace: View? = null

    private var mItemHeight = 65.dp2px
    private var maxHeight = 340.dp2px
    var listener: OnAlbumItemClick? = null

    init {
        initAnimate()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        recyclerView = findViewById(R.id.albumRecyclerView)
        albumSpace = findViewById(R.id.albumSpace)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        recyclerView?.setHasFixedSize(true)
        recyclerView?.setup {
            addType<LocalMediaFolder>(R.layout.item_album)
            onBind {
                val info = getModel<LocalMediaFolder>()
                loadImage(R.id.item_album_image, info.firstImagePath)
                setText(R.id.item_album_name, info.folderName)
                setText(R.id.item_album_num, info.data.size.toString())
                itemClicked {
                    listener?.onItemClick(modelPosition, info)
                }
                if (modelPosition % 2 == 1) {
                    itemView.setBackgroundColor(-0x1)
                } else {
                    itemView.setBackgroundColor(-0x40405)
                }
            }
        }?.models = mutableListOf()
    }

    fun setData(list: MutableList<LocalMediaFolder>) {
        recyclerView?.models = list
    }

    private fun initAnimate() {
        mRotateDownAnim = RotateAnimation((-180).toFloat(),
            0f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f)
        mRotateDownAnim?.fillAfter = true
        mRotateDownAnim?.duration = 200
        mRotateUpAnim = RotateAnimation(0f,
            (-180).toFloat(),
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f)
        mRotateUpAnim?.fillAfter = true
        mRotateUpAnim?.duration = 200

        inAlphaAnim = AlphaAnimation(0.0f, 1.0f)
        inAlphaAnim?.duration = 200
        inAlphaAnim?.fillAfter = true

        outAlphaAnim = AlphaAnimation(1.0f, 0.0f)
        outAlphaAnim?.duration = 200
        outAlphaAnim?.fillAfter = true

        inTransAnim = TranslateAnimation(Animation.RELATIVE_TO_SELF,
            0f,
            Animation.RELATIVE_TO_SELF,
            0f,
            Animation.RELATIVE_TO_SELF,
            -1.0f,
            Animation.RELATIVE_TO_SELF,
            0f)
        inTransAnim?.duration = 200
        inTransAnim?.fillAfter = true

        outTransAnim = TranslateAnimation(Animation.RELATIVE_TO_SELF,
            0f,
            Animation.RELATIVE_TO_SELF,
            0f,
            Animation.RELATIVE_TO_SELF,
            0f,
            Animation.RELATIVE_TO_SELF,
            -1.0f)
        outTransAnim?.duration = 200
        outTransAnim?.fillAfter = true

        outTransAnim?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                recyclerView?.clearAnimation()
                clearAnimation()
                visibility = GONE
            }
        })
    }

    fun show(itemCount: Int) {
        var recyclerHeight: Int = mItemHeight * itemCount
        if (recyclerHeight > maxHeight) {
            recyclerHeight = maxHeight
        }
        if (recyclerView?.layoutParams?.height != recyclerHeight) {
            recyclerView?.layoutParams?.height = recyclerHeight
            recyclerView?.postInvalidate()
        }
        visibility = VISIBLE
        albumSpace?.startAnimation(inAlphaAnim)
        recyclerView?.startAnimation(inTransAnim)
    }

    fun dismiss() {
        albumSpace?.startAnimation(outAlphaAnim)
        recyclerView?.startAnimation(outTransAnim)
    }

    fun startRotateDownAnim(view: View?) {
        view?.startAnimation(mRotateDownAnim)
    }

    fun startRotateUpAnim(view: View?) {
        view?.startAnimation(mRotateUpAnim)
    }

    interface OnAlbumItemClick {
        fun onItemClick(position: Int, info: LocalMediaFolder)
    }
}
package com.lzx.kaleidoscope

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lzx.library.api.Kaleidoscope
import com.lzx.library.api.PictureCode

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val adapter = PhotoAdapter(this)
        val recycleView = findViewById<RecyclerView>(R.id.recycleView)
        recycleView.layoutManager = GridLayoutManager(this, 3)
        recycleView.adapter = adapter

        //相册多选
        findViewById<TextView>(R.id.xaingce).setOnClickListener {
            Kaleidoscope.from(this).jumpForIntent { data ->
                val mode = data?.getStringExtra(PictureCode.EXTRA_MODE)
                val photoList = mutableListOf<String>()
                if (mode == PictureCode.MODE_CAMERA) {
                    data.getStringExtra(PictureCode.EXTRA_OUTPUT_PATH)?.let {
                        photoList.add(it)
                    }
                } else {
                    val list = data?.getStringArrayListExtra(PictureCode.EXTRA_OUTPUT_PATHS)
                    list?.forEach {
                        photoList.add(it)
                    }
                }
                adapter.clearImageList()
                adapter.setImageList(photoList)
            }
        }

        //相册单选
        findViewById<TextView>(R.id.xaingce2).setOnClickListener {
            Kaleidoscope.from(this).isFolderView(true).jump {
                adapter.clearImageList()
                adapter.setImageList(mutableListOf<String>().apply { add(it.orEmpty()) })
            }
        }

        //相册单选带裁剪
        findViewById<TextView>(R.id.xaingce3).setOnClickListener {
            Kaleidoscope.from(this).isNeedCut(true).isFolderView(true).jump {
                adapter.clearImageList()
                adapter.setImageList(mutableListOf<String>().apply { add(it.orEmpty()) })
            }
        }

        //拍照
        findViewById<TextView>(R.id.paizhao).setOnClickListener {
            Kaleidoscope.from(this).jumpToCamera {
                adapter.clearImageList()
                adapter.setImageList(mutableListOf<String>().apply { add(it.orEmpty()) })
            }
        }

        //拍照带裁剪
        findViewById<TextView>(R.id.paizhao2).setOnClickListener {
            Kaleidoscope.from(this).isNeedCut(true).jumpToCamera {
                adapter.clearImageList()
                adapter.setImageList(mutableListOf<String>().apply { add(it.orEmpty()) })
            }
        }
    }
}
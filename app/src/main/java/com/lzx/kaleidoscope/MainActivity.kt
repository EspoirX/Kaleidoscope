package com.lzx.kaleidoscope

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.lzx.library.api.Kaleidoscope

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.xaingce).setOnClickListener {
            Kaleidoscope.from(this)
                .isNeedCut(true)
                .isNeedPreview(true)
                .jump {
                    Glide.with(this).load(it).into(findViewById(R.id.result))
                }
        }

        findViewById<TextView>(R.id.paizhao).setOnClickListener {
            Kaleidoscope.from(this)
                .isNeedCut(true)
                .isNeedPreview(true)
                .jumpToCamera {
                    Glide.with(this).load(it).into(findViewById(R.id.result))
                }
        }
    }
}
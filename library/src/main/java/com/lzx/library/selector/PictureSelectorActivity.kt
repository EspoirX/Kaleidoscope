package com.lzx.library.selector

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lzx.library.R
import com.lzx.library.api.KaleConfig

class PictureSelectorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture_selector)

        val fragment = if (KaleConfig.isPicFolderView) {
            PictureFoldersFragment.newInstance()
        } else {
            PictureSelectorFragment.newInstance()
        }
        val ft = supportFragmentManager.beginTransaction()
        ft.add(R.id.fragment_container, fragment)
        ft.commitAllowingStateLoss()
    }
}
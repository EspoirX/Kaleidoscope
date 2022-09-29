package com.lzx.library.utils

import android.os.Build

object SdkVersionUtils {

    private const val R = 30

    @JvmStatic
    fun isR(): Boolean {
        return Build.VERSION.SDK_INT >= R
    }

    @JvmStatic
    fun isQ(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }
}
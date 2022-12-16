package com.lzx.library.crop.utils

import android.os.Build
import com.lzx.library.crop.utils.BitmapUtils
import android.opengl.GLES10
import javax.microedition.khronos.egl.EGL10
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.lzx.library.utils.DisplayUtil
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import kotlin.math.ceil

object BitmapUtils {
    @JvmStatic
    val openglRenderLimitValue: Int
        get() {
            val maxsize: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                gLESTextureLimitEqualAboveLollipop
            } else {
                openglRenderLimitBelowLollipop
            }
            return if (maxsize == 0) 4096 else maxsize
        }
    private val openglRenderLimitBelowLollipop: Int
        get() {
            val maxSize = IntArray(1)
            GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0)
            return maxSize[0]
        }

    // TROUBLE! No config found.
    // missing in EGL10
    private val gLESTextureLimitEqualAboveLollipop: Int
        get() {
            val egl = EGLContext.getEGL() as EGL10
            val dpy = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
            val vers = IntArray(2)
            egl.eglInitialize(dpy, vers)
            val configAttr = intArrayOf(
                EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RGB_BUFFER, EGL10.EGL_LEVEL, 0,
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT, EGL10.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfig = IntArray(1)
            egl.eglChooseConfig(dpy, configAttr, configs, 1, numConfig)
            if (numConfig[0] == 0) {
                // TROUBLE! No config found.
            }
            val config = configs[0]
            val surfAttr = intArrayOf(EGL10.EGL_WIDTH, 64, EGL10.EGL_HEIGHT, 64, EGL10.EGL_NONE)
            val surf = egl.eglCreatePbufferSurface(dpy, config, surfAttr)
            val eglContextClientVersion = 0x3098
            // missing in EGL10
            val ctxAttrib = intArrayOf(eglContextClientVersion, 1, EGL10.EGL_NONE)
            val ctx = egl.eglCreateContext(dpy, config, EGL10.EGL_NO_CONTEXT, ctxAttrib)
            egl.eglMakeCurrent(dpy, surf, surf, ctx)
            val maxSize = IntArray(1)
            GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0)
            egl.eglMakeCurrent(dpy, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
            egl.eglDestroySurface(dpy, surf)
            egl.eglDestroyContext(dpy, ctx)
            egl.eglTerminate(dpy)
            return maxSize[0]
        }

    @JvmStatic
    fun zoomImg(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        // 获得图片的宽高
        val width = bm.width
        val height = bm.height
        // 计算缩放比例
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        // 取得想要缩放的matrix参数
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        // 得到新的图片
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true)
    }


    @JvmStatic
    fun bitmapFromPath(path: String?, maxWidth: Int, maxHeight: Int): Bitmap? =
        BitmapFactory.decodeFile(path, getOpt(maxHeight, maxWidth, object : ImageFilter {
            override fun filter(bmpFactoryOptions: BitmapFactory.Options?) {
                BitmapFactory.decodeFile(path, bmpFactoryOptions)
            }
        }))

    internal interface ImageFilter {
        fun filter(bmpFactoryOptions: BitmapFactory.Options?)
    }

    private fun getOpt(
        maxHeight: Int, maxWidth: Int,
        filter: ImageFilter
    ): BitmapFactory.Options {
        val bmpFactoryOptions = BitmapFactory.Options()
        val heightRadio: Int
        val widthRadio: Int
        if (maxHeight > 0) {  // 使用人工限定大小
            bmpFactoryOptions.inJustDecodeBounds = true
            filter.filter(bmpFactoryOptions)
            heightRadio = ceil((bmpFactoryOptions.outHeight / maxHeight.toFloat()).toDouble()).toInt()
            widthRadio = ceil((bmpFactoryOptions.outWidth / maxWidth.toFloat()).toDouble()).toInt()
        } else {  // 使用屏幕限制大小
            bmpFactoryOptions.inJustDecodeBounds = true
            filter.filter(bmpFactoryOptions)
            heightRadio = ceil(
                (bmpFactoryOptions.outHeight / DisplayUtil.getPhoneHeight().toFloat())
                    .toDouble()
            ).toInt()
            widthRadio = ceil((bmpFactoryOptions.outWidth / DisplayUtil.getPhoneWidth().toFloat()).toDouble()).toInt()
        }
        if (heightRadio > widthRadio * 2.5) {  // 长图
            bmpFactoryOptions.inSampleSize = (heightRadio / 2)
        } else if (heightRadio * 2.5 < widthRadio) {  // 宽图
            bmpFactoryOptions.inSampleSize = (widthRadio / 2)
        } else if (heightRadio > 1 || widthRadio > 1) {
            if (heightRadio > widthRadio) {
                bmpFactoryOptions.inSampleSize = heightRadio
            } else {
                bmpFactoryOptions.inSampleSize = widthRadio
            }
        }
        bmpFactoryOptions.inJustDecodeBounds = false
        bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888
        return bmpFactoryOptions
    }
}
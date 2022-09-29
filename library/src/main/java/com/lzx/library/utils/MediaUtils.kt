package com.lzx.library.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.lzx.library.api.KaleConfig
import com.lzx.library.utils.FileUtils.getCreateFileName
import com.lzx.library.utils.FileUtils.safeClose
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.net.URLConnection
import java.util.Locale

object MediaUtils {

    private const val MIME_TYPE_JPEG = "image/jpeg"

    /**
     * 获取mimeType
     */
    @JvmStatic
    fun getMimeTypeFromMediaUrl(path: String?): String? {
        if (path.isNullOrEmpty()) return MIME_TYPE_JPEG
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(path)
        var mimeType =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase(Locale.getDefault()))
        if (mimeType.isNullOrEmpty()) {
            mimeType = getMimeType(File(path))
        }
        return if (mimeType.isNullOrEmpty()) MIME_TYPE_JPEG else mimeType
    }

    private fun getMimeType(file: File): String? {
        val fileNameMap = URLConnection.getFileNameMap()
        return fileNameMap.getContentTypeFor(file.name)
    }

    @SuppressLint("Range")
    @JvmStatic
    fun getPathMediaBucketId(context: Context, absolutePath: String?): Array<Long> {
        val mediaBucketId = arrayOf(0L, 0L)
        var data: Cursor? = null
        try { //selection: 指定查询条件
            val selection = MediaStore.Files.FileColumns.DATA + " like ?" //定义selectionArgs：
            val selectionArgs = arrayOf("%$absolutePath%")
            data = if (SdkVersionUtils.isR()) {
                val queryArgs = createQueryArgsBundle(selection,
                    selectionArgs,
                    1,
                    0,
                    MediaStore.Files.FileColumns._ID + " DESC")
                context.contentResolver.query(MediaStore.Files.getContentUri("external"),
                    null,
                    queryArgs,
                    null)
            } else {
                val orderBy = MediaStore.Files.FileColumns._ID + " DESC limit 1 offset 0"
                context.contentResolver.query(MediaStore.Files.getContentUri("external"),
                    null,
                    selection,
                    selectionArgs,
                    orderBy)
            }
            if (data != null && data.count > 0 && data.moveToFirst()) {
                mediaBucketId[0] = data.getLong(data.getColumnIndex(MediaStore.Files.FileColumns._ID))
                mediaBucketId[1] = data.getLong(data.getColumnIndex("bucket_id"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            data?.close()
        }
        return mediaBucketId
    }

    private fun createQueryArgsBundle(selection: String?,
                                      selectionArgs: Array<String>?,
                                      limitCount: Int,
                                      offset: Int,
                                      orderBy: String?): Bundle {
        val queryArgs = Bundle()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            queryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, orderBy)
            if (SdkVersionUtils.isR()) {
                queryArgs.putString(ContentResolver.QUERY_ARG_SQL_LIMIT, "$limitCount offset $offset")
            }
        }
        return queryArgs
    }

    @JvmStatic
    fun getImageSize(context: Context, url: String?): Pair<Int, Int> {
        var inputStream: InputStream? = null
        var pair: Pair<Int, Int> = Pair(0, 0)
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            inputStream = if (FileUtils.isContent(url)) {
                context.contentResolver?.openInputStream(Uri.parse(url))
            } else {
                FileInputStream(url)
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            pair = Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream.safeClose()
        }
        return pair
    }

    fun getRealPathUri(id: Long, mimeType: String?): String {
        val contentUri: Uri = if (FileUtils.isHasImage(mimeType)) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else if (FileUtils.isHasVideo(mimeType)) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else if (FileUtils.isHasAudio(mimeType)) {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }
        return ContentUris.withAppendedId(contentUri, id).toString()
    }

    /**
     * 创建拍照uri
     */
    fun createCameraOutImageUri(context: Context): Pair<Uri?, File?> {
        val cameraFileName: String = System.currentTimeMillis().toString()
        if (SdkVersionUtils.isQ()) {
            val uri = createImageUri(context, cameraFileName, "image/jpeg")
            return Pair(uri, null)
        } else {
            val cameraFile = createCameraFile(context) ?: return Pair(null, null)
            val uri = parUri(context, cameraFile)
            return Pair(uri, cameraFile)
        }
    }

    @JvmStatic
    fun createImageUri(ctx: Context, cameraFileName: String, mimeType: String): Uri? {
        val context = ctx.applicationContext
        val imageFilePath = arrayOf<Uri?>(null)

        val contentValues: ContentValues = buildImageContentValues(cameraFileName, mimeType)
        val uri = getImageContentUri()
        imageFilePath[0] = context.contentResolver.insert(uri, contentValues) //插入图片
        return imageFilePath[0]
    }

    @JvmStatic
    private fun getImageContentUri(): Uri {
        val status = Environment.getExternalStorageState()
        return if (status == Environment.MEDIA_MOUNTED) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.INTERNAL_CONTENT_URI
        }
    }

    private fun buildImageContentValues(customFileName: String, mimeType: String): ContentValues {
        val time: String = System.currentTimeMillis().toString()
        val values = ContentValues()
        if (customFileName.isEmpty()) {
            values.put(MediaStore.Images.Media.DISPLAY_NAME, getCreateFileName("IMG_"))
        } else {
            if (customFileName.lastIndexOf(".") == -1) {
                values.put(MediaStore.Images.Media.DISPLAY_NAME, getCreateFileName("IMG_"))
            } else {
                val suffix = customFileName.substring(customFileName.lastIndexOf("."))
                val fileName = customFileName.replace(suffix.toRegex(), "")
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            }
        }
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        if (SdkVersionUtils.isQ()) {
            values.put(MediaStore.Images.Media.DATE_TAKEN, time)
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/" + KaleConfig.savePicFolderName)
        }
        return values
    }

    /**
     * 创建文件
     */
    private fun createCameraFile(ctx: Context): File? {
        val path = FileUtils.getImageSandboxPath(ctx) + File.separator
        val folderDir = File(path)
        if (!folderDir.exists()) {
            folderDir.mkdirs()
        }
        val suffix = ".jpg"
        val newFileImageName = getCreateFileName("IMG_") + suffix
        return File(folderDir, newFileImageName)
    }

    private fun parUri(context: Context, cameraFile: File): Uri? {
        val authority = context.packageName + ".fileprovider"
        return FileProvider.getUriForFile(context, authority, cameraFile)
    }

    /**
     * 获取DCIM文件下最新一条拍照记录
     *
     * @return
     */
    @SuppressLint("Range")
    fun getDCIMLastImageId(context: Context?, absoluteDir: String): Int {
        if (context == null) return -1
        var data: Cursor? = null
        return try { //selection: 指定查询条件
            val selection = MediaStore.Images.Media.DATA + " like ?" //定义selectionArgs：
            val selectionArgs = arrayOf("%$absoluteDir%")
            data = if (SdkVersionUtils.isR()) {
                val queryArgs = createQueryArgsBundle(selection,
                    selectionArgs,
                    1,
                    0,
                    MediaStore.Files.FileColumns._ID + " DESC")
                context.applicationContext.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null,
                    queryArgs,
                    null)
            } else {
                val orderBy = MediaStore.Files.FileColumns._ID + " DESC limit 1 offset 0"
                context.applicationContext.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null,
                    selection,
                    selectionArgs,
                    orderBy)
            }
            if (data != null && data.count > 0 && data.moveToFirst()) {
                val id = data.getInt(data.getColumnIndex(MediaStore.Images.Media._ID))
                val date = data.getLong(data.getColumnIndex(MediaStore.Images.Media.DATE_ADDED))
                val duration: Int = dateDiffer(date) // 最近时间1s以内的图片，可以判定是最新生成的重复照片
                if (duration <= 1) id else -1
            } else {
                -1
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            -1
        } finally {
            data?.close()
        }
    }

    /**
     * 删除部分手机 拍照在DCIM也生成一张的问题
     *
     * @param id
     */
    fun removeMedia(context: Context?, id: Int) {
        if (context == null) return
        try {
            val cr = context.applicationContext.contentResolver
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val selection = MediaStore.Images.Media._ID + "=?"
            cr.delete(uri, selection, arrayOf(id.toLong().toString()))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 判断两个时间戳相差多少秒
     *
     * @param d
     * @return
     */
    private fun dateDiffer(d: Long): Int {
        return try {
            val l1: Long = getCurrentTimeMillis()
            val interval = l1 - d
            Math.abs(interval).toInt()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            -1
        }
    }

    private fun getCurrentTimeMillis(): Long {
        val timeToString = System.currentTimeMillis().toString()
        return (if (timeToString.length > 10) timeToString.substring(0, 10) else timeToString).toLong()
    }
}
package com.lzx.library.utils

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Locale

object FileUtils {

    private const val TAG = "Kaleidoscope"

    private val SF = SimpleDateFormat("yyyyMMddHHmmssSSS")

    const val KB: Long = 1024
    const val MB = (1024 * 1024).toLong()
    const val GB = (1024 * 1024 * 1024).toLong()

    const val ACCURATE_GB = 1000 * 1000 * 1000
    const val ACCURATE_MB = 1000 * 1000
    const val ACCURATE_KB = 1000

    /**
     * 根据时间戳创建文件名
     */
    @JvmStatic
    fun getCreateFileName(prefix: String): String {
        val millis = System.currentTimeMillis()
        return prefix + SF.format(millis)
    }

    /**
     * is content://
     */
    @JvmStatic
    fun isContent(url: String?): Boolean {
        return if (url.isNullOrEmpty()) false else url.startsWith("content://")
    }

    @JvmStatic
    fun generateCameraFolderName(absolutePath: String): String {
        val cameraFile = File(absolutePath)
        return cameraFile.parentFile?.name ?: "Camera"
    }

    /**
     * 根据uri获取绝对路径
     */
    @JvmStatic
    @SuppressLint("NewApi")
    fun getPath(context: Context?, uri: Uri?): String? {
        if (context == null || uri == null) return null
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {    // DownloadsProvider
                val id = DocumentsContract.getDocumentId(uri)
                if (!id.isNullOrEmpty()) {
                    return try {
                        val downloadUri = Uri.parse("content://downloads/public_downloads")
                        val contentUri = ContentUris.withAppendedId(downloadUri, id.toLong())
                        getDataColumn(context, contentUri, null, null)
                    } catch (e: NumberFormatException) {
                        Log.i(TAG, e.message.toString())
                        null
                    }
                }
            } else if (isMediaDocument(uri)) {     // MediaProvider
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                when (type) {
                    "image" -> {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    "video" -> {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }
                    "audio" -> {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {  // MediaStore (and general)
            return if (isGooglePhotosUri(uri)) {         // Return the remote address
                uri.lastPathSegment
            } else getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {    // File
            return uri.path
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    private fun getDataColumn(context: Context,
                              uri: Uri?,
                              selection: String?,
                              selectionArgs: Array<String>?): String? {
        if (uri == null) return null
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } catch (ex: Exception) {
            Log.i(TAG, String.format(Locale.getDefault(), "getDataColumn: _data - [%s]", ex.message))
        } finally {
            cursor?.close()
        }
        return null
    }

    fun Closeable?.safeClose() {
        try {
            this?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getUrlToFileName(path: String): String? {
        var result: String? = ""
        try {
            val lastIndexOf = path.lastIndexOf("/")
            if (lastIndexOf != -1) {
                result = path.substring(lastIndexOf + 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * isGif
     */
    fun isHasGif(mimeType: String?): Boolean {
        return mimeType != null && (mimeType == "image/gif" || mimeType == "image/GIF")
    }

    /**
     * isVideo
     */
    fun isHasVideo(mimeType: String?): Boolean {
        return mimeType != null && mimeType.startsWith("video")
    }


    /**
     * isAudio
     */
    fun isHasAudio(mimeType: String?): Boolean {
        return mimeType != null && mimeType.startsWith("audio")
    }


    /**
     * isImage
     */
    fun isHasImage(mimeType: String?): Boolean {
        return mimeType != null && mimeType.startsWith("image")
    }

    /**
     * isHasBmp
     */
    fun isHasBmp(mimeType: String): Boolean {
        return if (TextUtils.isEmpty(mimeType)) {
            false
        } else mimeType.startsWith("image/bmp") || mimeType.startsWith("image/x-ms-bmp") || mimeType.startsWith(
            "image/vnd.wap.wbmp")
    }

    /**
     * 图片存储的沙盒路径
     */
    fun getImageSandboxPath(context: Context): String {
        val dir = File(context.filesDir.absolutePath, "Sandbox")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir.absolutePath + File.separator + "image")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file.absolutePath
    }

    /**
     * 复制文件
     *
     * @param is 文件输入流
     * @param os 文件输出流
     * @return
     */
    fun writeFileFromIS(`is`: InputStream?, os: OutputStream): Boolean {
        var osBuffer: OutputStream? = null
        var isBuffer: BufferedInputStream? = null
        return try {
            isBuffer = BufferedInputStream(`is`)
            osBuffer = BufferedOutputStream(os)
            val data = ByteArray(1024)
            var len: Int
            while (isBuffer.read(data).also { len = it } != -1) {
                os.write(data, 0, len)
            }
            os.flush()
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        } finally {
            isBuffer.safeClose()
            osBuffer.safeClose()
        }
    }

    @Throws(IOException::class)
    fun copyFile(pathFrom: String, pathTo: String) {
        if (pathFrom.equals(pathTo, ignoreCase = true)) {
            return
        }
        var outputChannel: FileChannel? = null
        var inputChannel: FileChannel? = null
        try {
            inputChannel = FileInputStream(pathFrom).channel
            outputChannel = FileOutputStream(pathTo).channel
            inputChannel.transferTo(0, inputChannel.size(), outputChannel)
        } finally {
            inputChannel?.close()
            outputChannel?.close()
        }
    }
}
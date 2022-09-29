package com.lzx.library.loader

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.text.TextUtils
import com.lzx.library.api.ChooseMode
import com.lzx.library.api.KaleConfig
import com.lzx.library.bean.LocalMedia
import com.lzx.library.bean.LocalMediaFolder
import com.lzx.library.utils.FileUtils
import com.lzx.library.utils.MediaUtils
import com.lzx.library.utils.SdkVersionUtils
import java.util.Collections
import java.util.Locale

class LocalMediaLoader {

    private val projection = arrayOf(MediaStore.Files.FileColumns._ID,
        MediaStore.MediaColumns.DATA,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.WIDTH,
        MediaStore.MediaColumns.HEIGHT,
        "duration",
        MediaStore.MediaColumns.SIZE,
        "bucket_display_name",
        MediaStore.MediaColumns.DISPLAY_NAME,
        "bucket_id",
        MediaStore.MediaColumns.DATE_ADDED,
        "orientation")

    /**
     * 要在线程中执行该方法
     */
    fun loadAllAlbum(context: Context): MutableList<LocalMediaFolder> {
        val imageFolders = mutableListOf<LocalMediaFolder>()
        val uri = MediaStore.Files.getContentUri("external")
        val data =
            context.contentResolver.query(uri, projection, getSelection(), getSelectionArgs(), getSortOrder())
        try {
            if (data != null) {
                val allImageFolder = LocalMediaFolder()
                val latelyImages = mutableListOf<LocalMedia>()
                val count = data.count
                if (count > 0) {
                    data.moveToFirst()
                    do {
                        val media = parseLocalMedia(data) ?: continue
                        val folder =
                            getImageFolder(media.path, media.mimeType, media.parentFolderName, imageFolders)
                        folder.data.add(media)
                        folder.folderTotalNum = folder.folderTotalNum + 1
                        latelyImages.add(media)
                        val imageNum: Int = allImageFolder.folderTotalNum
                        allImageFolder.folderTotalNum = imageNum + 1
                    } while (data.moveToNext())

                    if (latelyImages.size > 0) {
                        sortFolder(imageFolders)
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            data?.close()
        }
        imageFolders.forEach { it ->
            var index = if (KaleConfig.isDisplayCamera) 0 else -1
            it.data.forEach {
                index += 1
                it.position = index
            }
        }
        return imageFolders
    }

    private fun parseLocalMedia(data: Cursor): LocalMedia? {
        val idColumn = data.getColumnIndexOrThrow(projection[0])
        val dataColumn = data.getColumnIndexOrThrow(projection[1])
        val mimeTypeColumn = data.getColumnIndexOrThrow(projection[2])
        val widthColumn = data.getColumnIndexOrThrow(projection[3])
        val heightColumn = data.getColumnIndexOrThrow(projection[4])
        val durationColumn = data.getColumnIndexOrThrow(projection[5])
        val sizeColumn = data.getColumnIndexOrThrow(projection[6])
        val folderNameColumn = data.getColumnIndexOrThrow(projection[7])
        val fileNameColumn = data.getColumnIndexOrThrow(projection[8])
        val dateAddedColumn = data.getColumnIndexOrThrow(projection[10])
        val orientationColumn = data.getColumnIndexOrThrow(projection[11])
        val id = data.getLong(idColumn)
        val dateAdded = data.getLong(dateAddedColumn)
        var mimeType = data.getString(mimeTypeColumn)
        val absolutePath = data.getString(dataColumn)
        val url = if (SdkVersionUtils.isQ()) MediaUtils.getRealPathUri(id, mimeType) else absolutePath
        mimeType = if (mimeType.isNullOrEmpty()) "image/jpeg" else mimeType

        if (mimeType.endsWith("image/*")) {
            mimeType = MediaUtils.getMimeTypeFromMediaUrl(absolutePath)
            if (!KaleConfig.isLoadGif) {
                if (FileUtils.isHasGif(mimeType)) {
                    return null
                }
            }
        }

        if (mimeType.endsWith("image/*")) {
            return null
        }

        if (mimeType.startsWith("image/webp")) {
            return null
        }

        if (FileUtils.isHasBmp(mimeType)) {
            return null
        }

        if (KaleConfig.isLoadGif && KaleConfig.isOnlyLoadGif) {
            if (!FileUtils.isHasGif(mimeType)) {
                return null
            }
        }

        if (!KaleConfig.isLoadVideo) {
            if (FileUtils.isHasVideo(mimeType)) {
                return null
            }
        }

        if (!KaleConfig.isLoadAudio) {
            if (FileUtils.isHasAudio(mimeType)) {
                return null
            }
        }

        var width = data.getInt(widthColumn)
        var height = data.getInt(heightColumn)
        val orientation = data.getInt(orientationColumn)
        if (orientation == 90 || orientation == 270) {
            width = data.getInt(heightColumn)
            height = data.getInt(widthColumn)
        }
        val duration = data.getLong(durationColumn)
        val size = data.getLong(sizeColumn)
        val folderName = data.getString(folderNameColumn)
        var fileName = data.getString(fileNameColumn)
        if (fileName.isNullOrEmpty()) {
            fileName = FileUtils.getUrlToFileName(absolutePath)
        }

        if (KaleConfig.isFilterSizeDuration && size > 0 && size < FileUtils.KB) {
            return null
        }
        if (KaleConfig.isLoadVideo || KaleConfig.isLoadAudio) {
            if (FileUtils.isHasVideo(mimeType) || FileUtils.isHasAudio(mimeType)) {
                if (KaleConfig.filterVideoMinSecond > 0 && duration < KaleConfig.filterVideoMinSecond) {
                    return null
                }
                if (KaleConfig.filterVideoMaxSecond in 1 until duration) {
                    return null
                }
                if (KaleConfig.isFilterSizeDuration && duration <= 0) {
                    return null
                }
            }
        }

        val media = LocalMedia.create()
        media.id = id
        media.path = url
        media.realPath = absolutePath
        media.fileName = fileName
        media.parentFolderName = folderName
        media.mimeType = mimeType
        media.width = width
        media.height = height
        media.size = size
        media.dateAddedTime = dateAdded
        return media
    }

    private fun getImageFolder(firstPath: String?,
                               firstMimeType: String?,
                               folderName: String?,
                               imageFolders: MutableList<LocalMediaFolder>): LocalMediaFolder {
        for (folder in imageFolders) {
            val name = folder.folderName
            if (name.isNullOrEmpty()) {
                continue
            }
            if (TextUtils.equals(name, folderName)) {
                return folder
            }
        }
        val newFolder = LocalMediaFolder()
        newFolder.folderName = folderName
        newFolder.firstImagePath = firstPath
        newFolder.firstMimeType = firstMimeType
        imageFolders.add(newFolder)
        return newFolder
    }

    private fun sortFolder(imageFolders: List<LocalMediaFolder>) {
        Collections.sort(imageFolders) { lhs: LocalMediaFolder, rhs: LocalMediaFolder ->
            if (lhs.data.isEmpty() || rhs.data.isEmpty()) {
                return@sort 0
            }
            val lSize: Int = lhs.folderTotalNum
            val rSize: Int = rhs.folderTotalNum
            rSize.compareTo(lSize)
        }
    }

    private fun getSelection(): String {
        val durationCondition: String = getDurationCondition()
        val fileSizeCondition: String = getFileSizeCondition()
        val queryMimeCondition: String = getQueryMimeCondition()
        return when (KaleConfig.chooseMode) {
            ChooseMode.TYPE_ALL -> getSelectionArgsForAllMediaCondition(durationCondition,
                fileSizeCondition,
                queryMimeCondition)
            ChooseMode.TYPE_IMAGE ->                 // Gets the image
                getSelectionArgsForImageMediaCondition(fileSizeCondition, queryMimeCondition)
            ChooseMode.TYPE_VIDEO ->                 // Access to video
                getSelectionArgsForVideoMediaCondition(durationCondition, queryMimeCondition)
            ChooseMode.TYPE_AUDIO ->                 // Access to the audio
                getSelectionArgsForAudioMediaCondition(durationCondition, queryMimeCondition)
        }
    }

    private fun getSelectionArgs(): Array<String>? {
        return when (KaleConfig.chooseMode) {
            ChooseMode.TYPE_ALL ->                 // Get all
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
            ChooseMode.TYPE_IMAGE ->                 // Get photo
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
            ChooseMode.TYPE_VIDEO ->                 // Get video
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
            ChooseMode.TYPE_AUDIO ->                 // Get audio
                arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO.toString())
        }
    }

    private fun getSortOrder(): String {
        return MediaStore.Images.Media.DATE_MODIFIED + " desc"
    }

    private fun getDurationCondition(): String {
        val maxS = if (KaleConfig.filterVideoMaxSecond == 0) {
            Long.MAX_VALUE
        } else {
            KaleConfig.filterVideoMaxSecond.toLong()
        }
        return String.format(Locale.CHINA,
            "%d <%s duration and duration <= %d",
            0.coerceAtLeast(KaleConfig.filterVideoMinSecond),
            "=",
            maxS)
    }

    private fun getFileSizeCondition(): String {
        val maxS = if (KaleConfig.filterMaxFileSize == 0L) Long.MAX_VALUE else KaleConfig.filterMaxFileSize
        return String.format(Locale.CHINA,
            "%d <%s " + MediaStore.MediaColumns.SIZE + " and " + MediaStore.MediaColumns.SIZE + " <= %d",
            0L.coerceAtLeast(KaleConfig.filterMinFileSize),
            "=",
            maxS)
    }

    private fun getQueryMimeCondition(): String {
        val stringBuilder = StringBuilder()
        if (KaleConfig.chooseMode != ChooseMode.TYPE_VIDEO) {
            if (!KaleConfig.isLoadGif) {
                stringBuilder.append(" AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/gif')")
            }
        }
        return stringBuilder.toString()
    }

    private fun getSelectionArgsForAllMediaCondition(timeCondition: String,
                                                     sizeCondition: String,
                                                     queryMimeCondition: String): String {
        return "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?" + queryMimeCondition + " OR " + MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " + timeCondition + ") AND " + sizeCondition
    }

    private fun getSelectionArgsForImageMediaCondition(fileSizeCondition: String,
                                                       queryMimeCondition: String): String {
        return MediaStore.Files.FileColumns.MEDIA_TYPE + "=?" + queryMimeCondition + " AND " + fileSizeCondition
    }

    private fun getSelectionArgsForVideoMediaCondition(durationCondition: String,
                                                       queryMimeCondition: String): String {
        return MediaStore.Files.FileColumns.MEDIA_TYPE + "=?" + queryMimeCondition + " AND " + durationCondition
    }

    private fun getSelectionArgsForAudioMediaCondition(durationCondition: String,
                                                       queryMimeCondition: String): String {
        return MediaStore.Files.FileColumns.MEDIA_TYPE + "=?" + queryMimeCondition + " AND " + durationCondition
    }
}
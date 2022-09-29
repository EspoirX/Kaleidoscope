package com.lzx.library.bean

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import com.lzx.library.utils.FileUtils.generateCameraFolderName
import com.lzx.library.utils.FileUtils.getPath
import com.lzx.library.utils.FileUtils.isContent
import com.lzx.library.utils.MediaUtils.getImageSize
import com.lzx.library.utils.MediaUtils.getMimeTypeFromMediaUrl
import com.lzx.library.utils.MediaUtils.getPathMediaBucketId
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
class LocalMedia(var id: Long = 0,
                 var path: String? = null,       //原始路径，可能是uri
                 var realPath: String? = null,   //真正的路径，即绝对路径
                 var isChecked: Boolean = false, //是否选中
                 var position: Int = 0,          //序号
                 var checkedCount: Int = 0,      //选中序号
                 var mimeType: String? = null,   //媒体资源类型
                 var width: Int = 0,             //图片宽度
                 var height: Int = 0,            //图片高度
                 var size: Long = 0,             //文件大小
                 var fileName: String? = null,   //文件名
                 var parentFolderName: String? = null, //父文件夹名称
                 var dateAddedTime: Long = 0,    //媒体创建时间
                 var isDisplayCamera: Boolean = false) : Parcelable {

    companion object {

        /**
         * 构造本地资源下的LocalMedia
         *
         * @param context 上下文
         * @param path    本地路径
         * @return
         */
        fun generateLocalMedia(context: Context?, path: String?): LocalMedia {
            val media = create()
            val uriPath = getPath(context!!, Uri.parse(path)) ?: return media
            val cameraFile = if (isContent(path)) File(uriPath) else File(path)
            media.path = path
            media.realPath = cameraFile.absolutePath
            media.fileName = cameraFile.name
            media.parentFolderName = generateCameraFolderName(cameraFile.absolutePath)
            media.mimeType = getMimeTypeFromMediaUrl(cameraFile.absolutePath)
            media.size = cameraFile.length()
            media.dateAddedTime = cameraFile.lastModified() / 1000
            val realPath = cameraFile.absolutePath
            if (realPath.contains("Android/data/") || realPath.contains("data/user/")) {
                media.id = System.currentTimeMillis()
            } else {
                val mediaBucketId = getPathMediaBucketId(context, media.realPath)
                media.id = if (mediaBucketId[0] == 0L) System.currentTimeMillis() else mediaBucketId[0]
            }
            val (first, second) = getImageSize(context, path)
            media.width = first
            media.height = second
            return media
        }

        /**
         * 创建LocalMedia对象
         */
        fun create(): LocalMedia {
            return LocalMedia()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalMedia

        if (id != other.id) return false
        if (path != other.path) return false
        if (realPath != other.realPath) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (path?.hashCode() ?: 0)
        result = 31 * result + (realPath?.hashCode() ?: 0)
        result = 31 * result + isChecked.hashCode()
        return result
    }
}
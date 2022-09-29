package com.lzx.library.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class LocalMediaFolder : Parcelable {
    var folderName: String? = null //名称
    var firstImagePath: String? = null  //封面
    var firstMimeType: String? = null
    var folderTotalNum = 0 //数量
    var isSelectTag = false
    var data = mutableListOf<LocalMedia>() //图片
    var currentDataPage = 1
    var isHasMore = false
}
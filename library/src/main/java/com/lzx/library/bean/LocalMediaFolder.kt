package com.lzx.library.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class LocalMediaFolder(var folderName: String? = null, //名称
                       var firstImagePath: String? = null,  //封面
                       var firstMimeType: String? = null,
                       var folderTotalNum: Int = 0,
                       var data: MutableList<LocalMedia> = mutableListOf()) : Parcelable
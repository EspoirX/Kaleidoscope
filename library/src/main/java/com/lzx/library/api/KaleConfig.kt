package com.lzx.library.api

object KaleConfig {
    var chooseMode: ChooseMode = ChooseMode.TYPE_IMAGE
    var maxSelectNum = 9

    var isLoadGif = false
    var isOnlyLoadGif = false

    var isLoadVideo = false
    var isLoadAudio = false

    var isDisplayCamera = true
    var isNeedCut = false
    var isNeedPreview = true

    //
    var filterVideoMaxSecond = 0
    var filterVideoMinSecond = 0

    var filterMaxFileSize = 0L
    var filterMinFileSize = 0L

    var isFilterSizeDuration = false

    var savePicFolderName = "kaleidoscope"

    fun reset() {
        chooseMode = ChooseMode.TYPE_IMAGE
        maxSelectNum = 9
        isLoadGif = false
        isLoadVideo = false
        isLoadAudio = false
        isOnlyLoadGif = false
        isDisplayCamera = true
        isNeedCut = false
        isNeedPreview = true

        filterVideoMaxSecond = 0
        filterVideoMinSecond = 0

        filterMaxFileSize = 0L
        filterMinFileSize = 0L

        isFilterSizeDuration = false
        savePicFolderName = "kaleidoscope"
    }
}

enum class ChooseMode {
    TYPE_ALL, TYPE_IMAGE, TYPE_VIDEO, TYPE_AUDIO
}
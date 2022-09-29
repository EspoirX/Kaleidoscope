package com.lzx.library.utils

import com.lzx.library.bean.LocalMedia

object SelectedManager {
    /**
     * selected result
     */
    @get:Synchronized
    val selectedResult = ArrayList<LocalMedia>()

    @Synchronized
    fun addSelectResult(media: LocalMedia?) {
        if (media == null) return
        if (selectedResult.contains(media)) {
            return
        }
        selectedResult.add(media)
    }

    val selectCount: Int
        get() = selectedResult.size

    @Synchronized
    fun removeSelectResult(media: LocalMedia) {
        if (selectedResult.size > 0) {
            selectedResult.remove(media)
        }
    }

    @Synchronized
    fun clearSelectResult() {
        if (selectedResult.size > 0) {
            selectedResult.clear()
        }
    }
}
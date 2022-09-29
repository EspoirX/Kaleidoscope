package com.lzx.kaleidoscope

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lzx.library.view.rclayout.RCImageView


class PhotoAdapter(val context: Context) : RecyclerView.Adapter<PhotoHolder>() {

    private val imageList = mutableListOf<String>()

    fun clearImageList() {
        imageList.clear()
        notifyDataSetChanged()
    }

    fun setImageList(list: MutableList<String>) {
        if (list.isEmpty()) {
            return
        }
        list.forEach { imageList.add(it) }
        if (imageList.size == 10) {
            imageList.removeAt(0)
        }
        notifyDataSetChanged()
    }

    fun swapList(fromPosition: Int, targetPosition: Int) {
        val fromValue = imageList.removeAt(fromPosition)
        imageList.add(targetPosition, fromValue)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
        return PhotoHolder(LayoutInflater.from(context).inflate(R.layout.item_image, parent, false))
    }

    override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
        val url = imageList[position]
        Glide.with(context).load(url).into(holder.image)
    }
}

class PhotoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val image: RCImageView = itemView.findViewById(R.id.image)
}

package com.lzx.library.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.drake.brv.BindingAdapter
import com.drake.brv.DefaultDecoration
import java.io.Serializable

object DisplayUtil {
    @JvmStatic
    fun dp2px(dp: Int): Int {
        return dp.dp2px
    }

    @JvmStatic
    fun getPhoneWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }
}

val Int.dp2px: Int
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics).toInt()

fun Int?.orDef(default: Int = 0) = this ?: default

val RecyclerView.bindingAdapter
    get() = adapter as? BindingAdapter ?: throw NullPointerException("RecyclerView without BindingAdapter")

fun RecyclerView.setup(block: BindingAdapter.(RecyclerView) -> Unit): BindingAdapter {
    val adapter = BindingAdapter()
    adapter.block(this)
    this.adapter = adapter
    return adapter
}

fun RecyclerView.linear(@RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
                        reverseLayout: Boolean = false,
                        stackFromEnd: Boolean = false,
                        recycleChildrenOnDetach: Boolean = false): RecyclerView {
    layoutManager = LinearLayoutManager(context, orientation, reverseLayout).apply {
        this.stackFromEnd = stackFromEnd
        this.recycleChildrenOnDetach = recycleChildrenOnDetach
    }
    return this
}

fun RecyclerView.grid(spanCount: Int = 1,
                      @RecyclerView.Orientation orientation: Int = RecyclerView.VERTICAL,
                      reverseLayout: Boolean = false,
                      spanSizeLookup: GridLayoutManager.SpanSizeLookup? = null): RecyclerView {
    layoutManager = GridLayoutManager(context, spanCount, orientation, reverseLayout).apply {
        if (spanSizeLookup != null) {
            setSpanSizeLookup(spanSizeLookup)
        }
    }
    return this
}

var RecyclerView.models
    get() = bindingAdapter.models
    set(value) {
        bindingAdapter.models = value
    }

fun RecyclerView.notifyItemChanged(position: Int, data: Any?, payload: Boolean = false) {
    runCatching {
        if (position >= 0 && position <= bindingAdapter.mutable.lastIndex.orDef()) {
            bindingAdapter.mutable[position] = data
            if (payload) {
                bindingAdapter.notifyItemChanged(position, data)
            } else {
                bindingAdapter.notifyItemChanged(position)
            }
        }
    }
}

fun RecyclerView.divider(block: DefaultDecoration.() -> Unit): RecyclerView {
    val itemDecoration = DefaultDecoration(context).apply(block)
    addItemDecoration(itemDecoration)
    return this
}

fun BindingAdapter.BindingViewHolder.setText(id: Int, text: CharSequence?) {
    findView<TextView>(id).text = text
}

fun BindingAdapter.BindingViewHolder.setImageResource(id: Int, @DrawableRes resId: Int) {
    findView<ImageView>(id).setImageResource(resId)
}

fun BindingAdapter.BindingViewHolder.setBackgroundResource(id: Int, @DrawableRes resId: Int) {
    findView<View>(id).setBackgroundResource(resId)
}

fun BindingAdapter.BindingViewHolder.setTextColor(id: Int, @ColorInt color: Int) {
    findView<TextView>(id).setTextColor(color)
}

fun BindingAdapter.BindingViewHolder.visible(id: Int) {
    findView<View>(id).isVisible = true
}

fun BindingAdapter.BindingViewHolder.gone(id: Int) {
    findView<View>(id).isVisible = false
}

fun BindingAdapter.BindingViewHolder.visibilityBy(id: Int, isVisible: Boolean) {
    findView<View>(id).isVisible = isVisible
}

fun BindingAdapter.BindingViewHolder.loadImage(id: Int, url: String?) {
    Glide.with(context).load(url).into(findView(id))
}

fun BindingAdapter.BindingViewHolder.itemClicked(block: View.OnClickListener.(v: View?) -> Unit) {
    itemView.setOnClickListener(object : View.OnClickListener {
        override fun onClick(v: View?) {
            block.invoke(this, v)
        }
    })
}

fun BindingAdapter.BindingViewHolder.onClick(id: Int, block: View.OnClickListener.(v: View?) -> Unit) {
    findView<View>(id).setOnClickListener(object : View.OnClickListener {
        override fun onClick(v: View?) {
            block.invoke(this, v)
        }
    })
}

fun <T> Bundle.put(key: String, value: T) {
    when (value) {
        null -> putSerializable(key, null as Serializable?)
        is Boolean -> putBoolean(key, value)
        is String -> putString(key, value)
        is Int -> putInt(key, value)
        is Short -> putShort(key, value)
        is Long -> putLong(key, value)
        is Byte -> putByte(key, value)
        is ByteArray -> putByteArray(key, value)
        is Char -> putChar(key, value)
        is CharArray -> putCharArray(key, value)
        is CharSequence -> putCharSequence(key, value)
        is Float -> putFloat(key, value)
        is Bundle -> putBundle(key, value)
        is Parcelable -> putParcelable(key, value)
        is Serializable -> putSerializable(key, value)
        else -> throw IllegalStateException("Type of property $key is not supported")
    }
}

fun Uri.getUriPath(): String? {
    return if (FileUtils.isContent(this.toString())) this.toString() else this.path
}

fun Fragment.showToast(msg: String) {
    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}

fun Activity.showToast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

inline fun <reified T : Activity> Activity.navigationToForResult(requestCode: Int,
                                                                 vararg params: Pair<String, Any?>) =
    internalStartActivityForResult(this, T::class.java, requestCode, params)

inline fun <reified T : Activity> Fragment.navigationToForResult(requestCode: Int,
                                                                 vararg params: Pair<String, Any?>) =
    internalStartActivityForResultFragment(this, T::class.java, requestCode, params)

fun internalStartActivityForResult(act: Activity,
                                   activity: Class<out Activity>,
                                   requestCode: Int,
                                   params: Array<out Pair<String, Any?>>) {
    act.startActivityForResult(createIntent(act, activity, params), requestCode)
}

fun internalStartActivityForResultFragment(frag: Fragment,
                                           activity: Class<out Activity>,
                                           requestCode: Int,
                                           params: Array<out Pair<String, Any?>>) {
    frag.startActivityForResult(createIntent(frag.context, activity, params), requestCode)
}

fun <T> createIntent(ctx: Context? = null,
                     clazz: Class<out T>? = null,
                     params: Array<out Pair<String, Any?>>): Intent {
    val intent = if (clazz == null) Intent() else Intent(ctx, clazz)
    if (params.isNotEmpty()) fillIntentArguments(intent, params)
    return intent
}

fun fillIntentArguments(intent: Intent, params: Array<out Pair<String, Any?>>) {
    params.forEach {
        when (val value = it.second) {
            null -> intent.putExtra(it.first, null as Serializable?)
            is Int -> intent.putExtra(it.first, value)
            is Long -> intent.putExtra(it.first, value)
            is CharSequence -> intent.putExtra(it.first, value)
            is String -> intent.putExtra(it.first, value)
            is Float -> intent.putExtra(it.first, value)
            is Double -> intent.putExtra(it.first, value)
            is Char -> intent.putExtra(it.first, value)
            is Short -> intent.putExtra(it.first, value)
            is Boolean -> intent.putExtra(it.first, value)
            is Serializable -> intent.putExtra(it.first, value)
            is Bundle -> intent.putExtra(it.first, value)
            is Parcelable -> intent.putExtra(it.first, value)
            is Array<*> -> when {
                value.isArrayOf<CharSequence>() -> intent.putExtra(it.first, value)
                value.isArrayOf<String>() -> intent.putExtra(it.first, value)
                value.isArrayOf<Parcelable>() -> intent.putExtra(it.first, value)
                else -> throw RuntimeException("Intent extra ${it.first} has wrong type ${value.javaClass.name}")
            }
            is IntArray -> intent.putExtra(it.first, value)
            is LongArray -> intent.putExtra(it.first, value)
            is FloatArray -> intent.putExtra(it.first, value)
            is DoubleArray -> intent.putExtra(it.first, value)
            is CharArray -> intent.putExtra(it.first, value)
            is ShortArray -> intent.putExtra(it.first, value)
            is BooleanArray -> intent.putExtra(it.first, value)
            else -> throw RuntimeException("Intent extra ${it.first} has wrong type ${value.javaClass.name}")
        }
        return@forEach
    }
}
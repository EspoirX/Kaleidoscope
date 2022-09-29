package com.lzx.library.api

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

class Kaleidoscope {

    private var mActivity: WeakReference<FragmentActivity>? = null
    private var mFragment: WeakReference<Fragment>? = null

    private constructor(activity: FragmentActivity?) {
        mActivity = WeakReference<FragmentActivity>(activity)
    }

    private constructor(fragment: Fragment?) {
        mFragment = WeakReference<Fragment>(fragment)
    }

    companion object {

        @JvmStatic
        fun from(activity: FragmentActivity?): KaleidoscopeImpl {
            return KaleidoscopeImpl().create(Kaleidoscope(activity))
        }

        @JvmStatic
        fun from(fragment: Fragment?): KaleidoscopeImpl {
            return KaleidoscopeImpl().create(Kaleidoscope(fragment))
        }
    }

    fun getActivity(): FragmentActivity? {
        return mActivity?.get()
    }

    fun getFragment(): Fragment? {
        return mFragment?.get()
    }
}
package com.gueg.tclwatcher

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.github.ybq.android.spinkit.SpinKitView
import com.github.ybq.android.spinkit.style.Wave


class LoadingView(context: Context, attrs: AttributeSet?= null) : FrameLayout(context, attrs) {

    private var progressBar = SpinKitView(context)

    var loading: Boolean = false
    set(value) {
        if(value)
            progressBar.visibility = VISIBLE
        else
            progressBar.visibility = GONE
        field = value
    }

    init {
        addView(progressBar)
        val wave = Wave()
        wave.color = context.resources.getColor(R.color.colorAccent)
        progressBar.setIndeterminateDrawable(wave)
    }
}
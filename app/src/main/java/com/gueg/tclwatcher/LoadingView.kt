package com.gueg.tclwatcher

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.github.ybq.android.spinkit.SpinKitView

class LoadingView(context: Context, attrs: AttributeSet?= null) : FrameLayout(context, attrs) {

    private var progressBar: SpinKitView
    private var layout: LinearLayout
    private var departAt: TextView
    private var arriveAt: TextView

    init {
        addView(View.inflate(context, R.layout.view_loading, null))

        progressBar = findViewById(R.id.view_loading_progressbar)
        layout = findViewById(R.id.view_loading_layout)
        departAt = findViewById(R.id.view_loading_departat)
        arriveAt = findViewById(R.id.view_loading_arriveat)
    }

    fun setTimes(departAt: String, arriveAt: String) {
        progressBar.visibility = GONE
        layout.visibility  = VISIBLE
        this.departAt.text = departAt
        this.arriveAt.text = arriveAt
    }

    fun setLoading() {
        layout.visibility  = GONE
        progressBar.visibility = VISIBLE
    }
}
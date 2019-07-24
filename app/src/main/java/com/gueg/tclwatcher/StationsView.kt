package com.gueg.tclwatcher

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView

class StationsView(context: Context, attrs: AttributeSet ?= null) : FrameLayout(context, attrs) {

    private var from: TextView
    private var to: TextView
    private var swap: ImageButton

    init {
        addView(View.inflate(context, R.layout.view_stations, null))

        from = findViewById(R.id.view_stationpicker_from)
        to = findViewById(R.id.view_stationpicker_to)
        swap = findViewById(R.id.view_stationpicker_swap)
    }

    fun setStations(from: String, to: String) {
        this.from.text = from
        this.to.text = to
    }
}
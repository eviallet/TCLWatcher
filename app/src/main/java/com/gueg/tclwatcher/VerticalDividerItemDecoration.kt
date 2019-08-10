package com.gueg.tclwatcher

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.View

class VerticalDividerItemDecoration(context: Context, private val spaceHeight: Int) : DividerItemDecoration(context, VERTICAL) {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.top = spaceHeight
        outRect.bottom = spaceHeight
    }
}
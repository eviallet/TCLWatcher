package com.gueg.tclwatcher

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View


class VerticalItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
    private var first = true

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (first) {
            outRect.top = verticalSpaceHeight
            first = false
        }
        if (parent.getChildAdapterPosition(view) != parent.adapter!!.itemCount - 1) {
            outRect.bottom = verticalSpaceHeight
        }
    }
}

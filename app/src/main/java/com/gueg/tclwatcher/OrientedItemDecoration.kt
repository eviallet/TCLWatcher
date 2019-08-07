package com.gueg.tclwatcher

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View


class OrientedItemDecoration(private val spaceHeight: Int, private val orientation: Int = VERTICAL) : RecyclerView.ItemDecoration() {

    companion object {
        const val VERTICAL = 0
        const val HORIZONTAL = 1
    }

    private var first = true

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (first) {
            when(orientation) {
                VERTICAL -> outRect.top = spaceHeight
                else -> outRect.left = spaceHeight
            }
            first = false
        }
        when(orientation) {
            VERTICAL -> outRect.bottom = spaceHeight
            else -> outRect.right = spaceHeight
        }
    }
}

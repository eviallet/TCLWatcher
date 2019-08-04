package com.gueg.tclwatcher.bookmarks

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.gueg.tclwatcher.HomepageFragment
import com.gueg.tclwatcher.R
import com.gueg.tclwatcher.bookmarks.bookmarks_db.BookmarkDatabase

class BookmarkAdapter internal constructor(
    private val context: Context,
    private val bookmarkSelectedListener: HomepageFragment.BookmarkSelectedListener
) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

    private val bookmarks = ArrayList<Bookmark>()

    init {
        Thread {
            bookmarks.addAll(BookmarkDatabase.getDatabase(context).bookmarkDao().all)
        }.start()
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var isExpanded = false
        var isAnimatingExpandArrow = false
        var container: View = v.findViewById(R.id.row_bookmark_container)
        var from: TextView = v.findViewById(R.id.row_bookmark_from)
        var to: TextView = v.findViewById(R.id.row_bookmark_to)
        var swap: ImageButton = v.findViewById(R.id.row_bookmark_swap)
        var delete: ImageButton = v.findViewById(R.id.row_bookmark_remove)
        var expand: LinearLayout = v.findViewById(R.id.row_bookmark_expand)
        var expandArrow: ImageView = v.findViewById(R.id.row_bookmark_expand_arrow)
        var recyclerView: RecyclerView = v.findViewById(R.id.row_bookmark_recyclerview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_bookmark, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmark = bookmarks[position]

        holder.from.text = bookmark.from
        holder.to.text = bookmark.to

        holder.container.setOnClickListener { bookmarkSelectedListener.onBookmarkSelected(bookmark) }

        holder.swap.setOnClickListener {
            val fromText = holder.from.text
            val toText = holder.to.text

            val fromAnim = AlphaAnimation(1f, 0f)
            fromAnim.duration = 200
            fromAnim.repeatCount = 1
            fromAnim.repeatMode = Animation.REVERSE
            val toAnim = AlphaAnimation(1f, 0f)
            toAnim.duration = 200
            toAnim.repeatCount = 1
            toAnim.repeatMode = Animation.REVERSE

            fromAnim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) { }
                override fun onAnimationRepeat(animation: Animation?) { holder.from.text = toText }
                override fun onAnimationEnd(animation: Animation?) { }
            })
            holder.from.startAnimation(fromAnim)

            toAnim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) { }
                override fun onAnimationRepeat(animation: Animation?) { holder.to.text = fromText }
                override fun onAnimationEnd(animation: Animation?) { }
            })
            holder.to.startAnimation(toAnim)

            val index = holder.adapterPosition
            var temp = bookmark.from
            bookmark.from = bookmark.to
            bookmark.to = temp
            if(bookmark.hasBeenRefined()) {
                temp = bookmark.refinedFrom
                bookmark.refinedFrom = bookmark.refinedTo
                bookmark.refinedTo = temp
            }
            bookmarks.removeAt(index)
            bookmarks.add(index, bookmark)
        }

        holder.delete.setOnClickListener { bookmarkSelectedListener.onBookmarkDeleted(bookmark) }

        holder.expand.setOnClickListener {
            if(holder.isExpanded) {
                holder.recyclerView.visibility = GONE
                holder.delete.animate().alpha(0f).scaleX(0.3f).scaleY(0.3f).withEndAction { holder.delete.visibility = GONE }
            } else {
                holder.recyclerView.visibility = VISIBLE
                holder.delete.visibility = VISIBLE
                holder.delete.animate().alpha(1f).scaleX(1f).scaleY(1f)
            }

            if(!holder.isAnimatingExpandArrow) {
                holder.isAnimatingExpandArrow = true
                holder.expandArrow.animate().rotationBy(180f * if(holder.isExpanded) 1 else -1)
                    .withEndAction { holder.isAnimatingExpandArrow = false }
            }

            holder.isExpanded = !holder.isExpanded
        }
    }

    fun add(bookmark: Bookmark) {
        bookmarks.add(bookmark)
    }

    fun remove(bookmark: Bookmark) {
        bookmarks.remove(bookmark)
    }

    override fun getItemCount(): Int {
        return bookmarks.size
    }


}
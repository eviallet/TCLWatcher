package com.gueg.tclwatcher.bookmarks

import android.app.Activity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.gueg.tclwatcher.HomepageFragment
import com.gueg.tclwatcher.OrientedItemDecoration
import com.gueg.tclwatcher.R
import com.gueg.tclwatcher.bookmarks.bookmarks_db.BookmarkDatabase

class BookmarkAdapter internal constructor(
    private val activity: Activity,
    private val recyclerView: RecyclerView,
    private val bookmarkSelectedListener: HomepageFragment.BookmarkSelectedListener
) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

    private val bookmarks = ArrayList<Bookmark>()

    companion object {
        const val HORIZONTAL_MARGIN = 10
    }

    init {
        Thread {
            bookmarks.addAll(BookmarkDatabase.getDatabase(activity).bookmarkDao().all)
        }.start()
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var isExpanded = false
        var isFirstExpanding = true
        var isAnimatingExpandArrow = false
        var container: View = v.findViewById(R.id.row_bookmark_container)
        var from: TextView = v.findViewById(R.id.row_bookmark_from)
        var to: TextView = v.findViewById(R.id.row_bookmark_to)
        var swap: ImageButton = v.findViewById(R.id.row_bookmark_swap)
        var delete: ImageButton = v.findViewById(R.id.row_bookmark_remove)
        var moveup: ImageButton = v.findViewById(R.id.row_bookmark_moveup)
        var movedown: ImageButton = v.findViewById(R.id.row_bookmark_movedown)
        var expand: LinearLayout = v.findViewById(R.id.row_bookmark_expand)
        var expandArrow: ImageView = v.findViewById(R.id.row_bookmark_expand_arrow)
        var recyclerView1: RecyclerView = v.findViewById(R.id.row_bookmark_recyclerview_1)
        var depart1: TextView = v.findViewById(R.id.row_bookmark_recyclerview1_depart)
        var arrival1: TextView = v.findViewById(R.id.row_bookmark_recyclerview1_arrival)
        var container1: LinearLayout = v.findViewById(R.id.row_bookmark_recyclerview1_container)
        var recyclerView2: RecyclerView = v.findViewById(R.id.row_bookmark_recyclerview_2)
        var depart2: TextView = v.findViewById(R.id.row_bookmark_recyclerview2_depart)
        var arrival2: TextView = v.findViewById(R.id.row_bookmark_recyclerview2_arrival)
        var container2: LinearLayout = v.findViewById(R.id.row_bookmark_recyclerview2_container)
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

        holder.moveup.setOnClickListener {
            val pos = holder.adapterPosition
            val from = bookmarks[pos]
            from.rank = pos-1
            val dest = bookmarks[pos-1]
            dest.rank = pos
            bookmarks.remove(from)
            bookmarks.remove(dest)
            bookmarks.add(pos-1, from)
            bookmarks.add(pos, dest)
            notifyItemMoved(pos, pos-1)
            Log.d(":-:","moving : $pos -> ${pos-1}")
            refreshMoveArrows(pos-1)
            refreshMoveArrows(pos)

            Thread {
                BookmarkDatabase.getDatabase(activity).bookmarkDao().update(from)
                BookmarkDatabase.getDatabase(activity).bookmarkDao().update(dest)
            }.start()
        }

        holder.movedown.setOnClickListener {
            val pos = holder.adapterPosition
            val from = bookmarks[pos]
            from.rank = pos+1
            val dest = bookmarks[pos+1]
            dest.rank = pos
            bookmarks.remove(from)
            bookmarks.remove(dest)
            bookmarks.add(pos, dest)
            bookmarks.add(pos, from)
            notifyItemMoved(pos, pos+1)
            Log.d(":-:","moving : $pos -> ${pos+1}")
            refreshMoveArrows(pos)
            refreshMoveArrows(pos+1)

            Thread {
                BookmarkDatabase.getDatabase(activity).bookmarkDao().update(from)
                BookmarkDatabase.getDatabase(activity).bookmarkDao().update(dest)
            }.start()
        }

        holder.expand.setOnClickListener {
            if(holder.isExpanded) {
                holder.container1.visibility = GONE
                holder.container2.visibility = GONE
                holder.delete.animate().alpha(0f).scaleX(0.3f).scaleY(0.3f).withEndAction { holder.delete.visibility = GONE }
                holder.moveup.animate().alpha(0f).scaleX(0.3f).scaleY(0.3f).withEndAction { holder.moveup.visibility = GONE }
                holder.movedown.animate().alpha(0f).scaleX(0.3f).scaleY(0.3f).withEndAction { holder.movedown.visibility = GONE }
            } else {
                if(holder.isFirstExpanding) {
                    holder.recyclerView1.setHasFixedSize(true)
                    holder.recyclerView1.overScrollMode = OVER_SCROLL_NEVER
                    holder.recyclerView1.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
                    holder.recyclerView1.addItemDecoration(OrientedItemDecoration(HORIZONTAL_MARGIN, orientation = OrientedItemDecoration.HORIZONTAL))
                    holder.recyclerView1.adapter = BookmarkRouteAdapter(activity, bookmark, onFinished = { nextUrl: String ->
                        activity.runOnUiThread {
                            holder.recyclerView2.setHasFixedSize(true)
                            holder.recyclerView2.overScrollMode = OVER_SCROLL_NEVER
                            holder.recyclerView2.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
                            holder.recyclerView2.addItemDecoration(OrientedItemDecoration(HORIZONTAL_MARGIN, orientation = OrientedItemDecoration.HORIZONTAL))
                            holder.recyclerView2.adapter = BookmarkRouteAdapter(activity, bookmark, nextUrl = nextUrl, onTimesGot = { depart, arrival ->
                                activity.runOnUiThread {
                                    holder.depart2.text = depart
                                    holder.arrival2.text = arrival
                                }
                            })
                        }
                    }, onTimesGot = { depart, arrival ->
                        activity.runOnUiThread {
                            holder.depart1.text = depart
                            holder.arrival1.text = arrival
                        }
                    })
                    holder.isFirstExpanding = false
                }

                holder.container1.visibility = VISIBLE
                holder.container2.visibility = VISIBLE
                holder.delete.visibility = VISIBLE
                holder.delete.animate().alpha(1f).scaleX(1f).scaleY(1f)
                if(holder.adapterPosition != 0) {
                    holder.moveup.visibility = VISIBLE
                    holder.moveup.animate().alpha(1f).scaleX(1f).scaleY(1f)
                }
                if(holder.adapterPosition != bookmarks.lastIndex) {
                    holder.movedown.visibility = VISIBLE
                    holder.movedown.animate().alpha(1f).scaleX(1f).scaleY(1f)
                }
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
        bookmark.rank = bookmarks.lastIndex

        Thread {
            BookmarkDatabase.getDatabase(activity).bookmarkDao().update(bookmark)
        }.start()
    }

    private fun viewAt(position: Int): ViewHolder? {
        return try {
            recyclerView.findViewHolderForAdapterPosition(position) as ViewHolder
        } catch (err: TypeCastException) {
            null
        }
    }

    fun refresh() {
        for(i in 0..bookmarks.lastIndex)
            viewAt(i)?.isFirstExpanding = true
        notifyDataSetChanged()
    }

    private fun refreshMoveArrows(position: Int) {
        if(viewAt(position) == null)
            return

        val holder = viewAt(position)!!

        if(!holder.isExpanded)
            return

        when(position) {
            0 -> holder.moveup.visibility = GONE
            bookmarks.lastIndex -> holder.movedown.visibility = GONE
            else -> {
                if(holder.moveup.visibility != VISIBLE) {
                    holder.moveup.visibility = VISIBLE
                    holder.moveup.animate().alpha(1f).scaleX(1f).scaleY(1f)
                }
                if(holder.movedown.visibility != VISIBLE) {
                    holder.movedown.visibility = VISIBLE
                    holder.movedown.animate().alpha(1f).scaleX(1f).scaleY(1f)
                }
            }
        }

    }

    fun remove(bookmark: Bookmark) {
        bookmarks.remove(bookmark)
    }

    override fun getItemCount(): Int {
        return bookmarks.size
    }


}
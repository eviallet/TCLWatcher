package com.gueg.tclwatcher.bookmarks

import android.app.Activity
import android.graphics.drawable.AnimatedVectorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
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
        var isArrowExpanded = false
        var areSettingsVisible = false
        var isFirstExpanding = true
        var isAnimatingExpandArrow = false
        var details: View = v.findViewById(R.id.row_bookmark_details)
        var container: View = v.findViewById(R.id.row_bookmark_container)
        var routeLayout: LinearLayout = v.findViewById(R.id.row_bookmark_details_routes_layout)
        var from: TextView = v.findViewById(R.id.row_bookmark_from)
        var to: TextView = v.findViewById(R.id.row_bookmark_to)
        var swap: ImageButton = v.findViewById(R.id.row_bookmark_swap)
        var delete: ImageButton = v.findViewById(R.id.row_bookmark_remove)
        var settings: ImageButton = v.findViewById(R.id.row_bookmark_settings)
        var moveLayout: RelativeLayout = v.findViewById(R.id.row_bookmark_settings_move_layout)
        var moveup: ImageButton = v.findViewById(R.id.row_bookmark_moveup)
        var movedown: ImageButton = v.findViewById(R.id.row_bookmark_movedown)
        var expandArrow: ImageView = v.findViewById(R.id.row_bookmark_expand_arrow)
        var recyclerView1: RecyclerView = v.findViewById(R.id.row_bookmark_recyclerview_1)
        var depart1: TextView = v.findViewById(R.id.row_bookmark_recyclerview1_depart)
        var arrival1: TextView = v.findViewById(R.id.row_bookmark_recyclerview1_arrival)
        var recyclerView2: RecyclerView = v.findViewById(R.id.row_bookmark_recyclerview_2)
        var depart2: TextView = v.findViewById(R.id.row_bookmark_recyclerview2_depart)
        var arrival2: TextView = v.findViewById(R.id.row_bookmark_recyclerview2_arrival)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_bookmark, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bookmark = bookmarks[position]

        holder.from.text = bookmark.fromName
        holder.to.text = bookmark.toName

        holder.container.setOnClickListener {
            holder.expandArrow.performClick()
        }

        holder.container.setOnLongClickListener {
            bookmarkSelectedListener.onBookmarkSelected(bookmark)
            true
        }

        holder.swap.setOnClickListener {
            val d = (holder.swap.drawable as AnimatedVectorDrawable)

            if(!d.isRunning)
                d.start()

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
            val temp = bookmark.from
            bookmark.from = bookmark.to
            bookmark.to = temp
            bookmarks.removeAt(index)
            bookmarks.add(index, bookmark)

            refresh(index)
        }

        holder.settings.setOnClickListener {
            val d = (holder.settings.drawable as AnimatedVectorDrawable)

            if(!d.isRunning)
                d.start()

            if(!holder.areSettingsVisible) {
                holder.moveLayout.visibility = VISIBLE
                refreshMoveArrows(position)
                holder.delete.visibility = VISIBLE
                holder.routeLayout.animate().alpha(0f).setDuration(300).setInterpolator(AccelerateDecelerateInterpolator()).start()
            } else {
                holder.moveLayout.visibility = GONE
                holder.moveup.visibility = GONE
                holder.movedown.visibility = GONE
                holder.delete.visibility = GONE
                holder.routeLayout.animate().alpha(1f).setDuration(300).setInterpolator(AccelerateDecelerateInterpolator()).start()
            }

            holder.areSettingsVisible = !holder.areSettingsVisible
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
            refreshMoveArrows(pos)
            refreshMoveArrows(pos+1)

            Thread {
                BookmarkDatabase.getDatabase(activity).bookmarkDao().update(from)
                BookmarkDatabase.getDatabase(activity).bookmarkDao().update(dest)
            }.start()
        }

        holder.expandArrow.setOnClickListener {
            TransitionManager.beginDelayedTransition(recyclerView)
            if(holder.isExpanded) {
                holder.details.visibility = GONE
            } else {
                if(holder.isFirstExpanding) {
                    setRecyclerViews(holder, bookmark)
                    holder.isFirstExpanding = false
                }

                holder.details.visibility = VISIBLE
            }

            if(!holder.isAnimatingExpandArrow) {
                holder.isAnimatingExpandArrow = true
                holder.expandArrow.animate().rotationBy(180f * if(holder.isExpanded) 1 else -1)
                    .withStartAction {
                        holder.isArrowExpanded = !holder.isExpanded
                    }
                    .withEndAction {
                        holder.isAnimatingExpandArrow = false
                        if(holder.isExpanded!=holder.isArrowExpanded)
                            holder.expandArrow.rotation = if(holder.isExpanded) 90f else -90f
                    }.start()
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

    fun refresh(pos: Int = -1) {
        if(pos != -1) {
            setRecyclerViews(viewAt(pos)!!, bookmarks[pos])
        } else {
            for(i in 0..bookmarks.lastIndex) {
                if(viewAt(i)!=null) {
                    if(viewAt(i)!!.isExpanded)
                        setRecyclerViews(viewAt(i)!!, bookmarks[i])
                    else
                        viewAt(i)!!.isFirstExpanding = true
                }
            }
            notifyDataSetChanged()
        }
    }

    private fun setRecyclerViews(holder: ViewHolder, bookmark: Bookmark) {
        holder.recyclerView1.setHasFixedSize(true)
        holder.recyclerView1.overScrollMode = OVER_SCROLL_NEVER
        holder.recyclerView1.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        holder.recyclerView1.addItemDecoration(OrientedItemDecoration(HORIZONTAL_MARGIN, orientation = OrientedItemDecoration.HORIZONTAL))
        holder.recyclerView1.adapter = BookmarkRouteAdapter(activity, bookmark, onFinished = { nextDatetime: String ->
            activity.runOnUiThread {
                holder.recyclerView2.setHasFixedSize(true)
                holder.recyclerView2.overScrollMode = OVER_SCROLL_NEVER
                holder.recyclerView2.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
                holder.recyclerView2.addItemDecoration(OrientedItemDecoration(HORIZONTAL_MARGIN, orientation = OrientedItemDecoration.HORIZONTAL))
                holder.recyclerView2.adapter = BookmarkRouteAdapter(activity, bookmark, next = nextDatetime, onTimesGot = { depart, arrival ->
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
    }

    private fun refreshMoveArrows(position: Int) {
        if(viewAt(position) == null)
            return

        val holder = viewAt(position)!!

        if(!holder.isExpanded)
            return

        if(position==0)
            holder.moveup.visibility = GONE
        else if(holder.moveup.visibility != VISIBLE) {
            holder.moveup.visibility = VISIBLE
        }

        if(position==bookmarks.lastIndex)
            holder.movedown.visibility = GONE
        else if(holder.movedown.visibility != VISIBLE) {
            holder.movedown.visibility = VISIBLE
        }

    }

    fun remove(bookmark: Bookmark) {
        bookmarks.remove(bookmark)
    }

    override fun getItemCount(): Int {
        return bookmarks.size
    }


}
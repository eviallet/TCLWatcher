package com.gueg.tclwatcher.stations

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnticipateOvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.github.ybq.android.spinkit.SpinKitView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.gueg.tclwatcher.R
import com.gueg.tclwatcher.bookmarks.Bookmark
import com.gueg.tclwatcher.routes.RouteRequest
import com.gueg.tclwatcher.routes.RouteRequestBuilder
import java.util.*
import kotlin.collections.ArrayList


class StationPicker(context: Context, attrs: AttributeSet ?= null) : FrameLayout(context, attrs) {

    private var loading: SpinKitView
    private var from: AutoCompleteTextView
    private var to: AutoCompleteTextView
    private var fab: ExtendedFloatingActionButton
    private var swap: ImageButton
    private var bookmark: ImageButton
    private var depArrLayout: RelativeLayout
    private var depArr: Spinner
    private val depArrText: TextView
    private var dateLayout: RelativeLayout
    private var dateText: TextView
    private var dateSpinner: Spinner
    private var timeLayout: RelativeLayout
    private var timeText: TextView
    var listener: StationPickerListener?= null
    private var year: Int = Calendar.getInstance().get(Calendar.YEAR)
    private lateinit var autoCompleteAdapter: ArrayAdapter<String>

    private var firstDateNotification = true

    private var refinedFrom : String ?= null
    private var refinedTo : String ?= null

    private var swapAnimationRunning = false

    init {
        addView(View.inflate(context, R.layout.view_stationpicker, null))

        from = findViewById(R.id.view_stationpicker_from)
        to = findViewById(R.id.view_stationpicker_to)

        loading = findViewById(R.id.view_stationpicker_loading)

        bookmark = findViewById(R.id.view_stationpicker_bookmark)
        bookmark.setOnClickListener { listener!!.onBookmarkAdded(from.text.toString(), to.text.toString()) }

        depArrText = findViewById(R.id.view_stationpicker_param_dep_arr_text)

        depArr = findViewById(R.id.view_stationpicker_param_dep_arr)
        val depArrAdapter = ArrayAdapter.createFromResource(context,
            R.array.spinner_dep_arr,
            R.layout.view_spinner
        )
        depArrLayout = findViewById(R.id.view_stationpicker_param_dep_arr_layout)
        depArrLayout.setOnClickListener { depArr.performClick() }
        depArr.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                depArrText.text = depArrAdapter.getItem(position)
            }
        }

        depArrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        depArr.adapter = depArrAdapter
        depArr.setSelection(0)

        dateLayout = findViewById(R.id.view_stationpicker_param_date_layout)
        dateText = findViewById(R.id.view_stationpicker_param_date)
        dateText.text = context.resources.getStringArray(R.array.spinner_date)[0]

        dateSpinner = findViewById(R.id.view_stationpicker_param_date_spinner)
        val dateAdapter = ArrayAdapter.createFromResource(context,
            R.array.spinner_date,
            R.layout.view_spinner_date
        )
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dateSpinner.adapter = dateAdapter
        dateSpinner.setSelection(0)
        dateLayout.setOnClickListener { dateSpinner.performClick() }
        dateSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                if(firstDateNotification) {
                    firstDateNotification = false
                    return
                }

                if(position==2) {
                    val pickerDialog = DatePickerDialog(
                        context, DatePickerDialog.OnDateSetListener { _, y, month, day ->
                            year = y
                            val m = month+1
                            dateText.text = ("le ${
                                if(day<10) "0$day" else day
                            }/${
                                if(m<10) "0$m" else m
                            }")
                        },
                        Calendar.getInstance().get(Calendar.YEAR),
                        Calendar.getInstance().get(Calendar.MONTH),
                        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                    )
                    pickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
                    pickerDialog.show()
                } else {
                    dateText.text = context.resources.getStringArray(R.array.spinner_date)[position]
                }
            }
        }

        timeLayout = findViewById(R.id.view_stationpicker_param_time_layout)
        timeText = findViewById(R.id.view_stationpicker_param_time)
        timeText.text = now()
        timeLayout.setOnClickListener {
            val hour = timeText.text.split(":")[0].toInt()
            val min = timeText.text.split(":")[1].toInt()
            TimePickerDialog(context,
                TimePickerDialog.OnTimeSetListener { _, selectedHour, selectedMinute ->
                    timeText.text = ("${
                        if(selectedHour<10) "0$selectedHour" else selectedHour
                    }:${
                        if(selectedMinute<10) "0$selectedMinute" else selectedMinute
                    }")
                }, hour, min, true
            ).show()
        }

        fab = findViewById(R.id.view_stationpicker_fab)
        fab.setOnClickListener {
            if(checkText()) {
                setLoading(true)
                RouteRequestBuilder.with(context).from(from.text.toString()).to(to.text.toString()).build { fromStr, toStr ->
                    if(fromStr.isEmpty()) {
                        setLoading(false)
                    } else {
                        refinedFrom = fromStr
                        refinedTo = toStr
                        val request = RouteRequest(
                            from = fromStr,
                            to = toStr,
                            timeMode = if (depArr.selectedItemPosition == 0) RouteRequest.TimeMode.DEPART_AT else RouteRequest.TimeMode.ARRIVE_AT,
                            year = year,
                            month = getMonth(),
                            day = getDay(),
                            hour = timeText.text.split(":")[0].toInt(),
                            minute = timeText.text.split(":")[1].toInt()
                        )
                        listener!!.onRequestEmitted(request)
                    }
                }

            }
        }

        from.setOnEditorActionListener { _, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_NEXT)
                to.performClick()
            false
        }
        to.setOnEditorActionListener { _, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_DONE)
                fab.performClick()
            false
        }

        from.addTextChangedListener(object: TextWatcher {
            var oldLength = 0
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, before: Int, p3: Int) {
                if(s==null) return
                if(s.isNotEmpty())
                    from.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                if(s.length < oldLength && refinedTo != null) {
                    refinedFrom = null
                    refinedTo = null
                }
                oldLength = s.length

                if(s.isNotEmpty()&&to.text.isNotEmpty()&&bookmark.visibility == GONE)
                    bookmark.visibility = View.VISIBLE
                if(s.isEmpty()&&bookmark.visibility == View.VISIBLE)
                    bookmark.visibility = View.GONE
            }
        })

        to.addTextChangedListener(object: TextWatcher {
            var oldLength = 0
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, before: Int, p3: Int) {
                if(s==null) return
                if(s.isNotEmpty())
                    to.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                if(s.length < oldLength && refinedTo != null) {
                    refinedFrom = null
                    refinedTo = null
                }
                oldLength = s.length

                if(s.isNotEmpty()&&from.text.isNotEmpty()&&bookmark.visibility == GONE)
                    bookmark.visibility = View.VISIBLE
                if(s.isEmpty()&&bookmark.visibility == View.VISIBLE)
                    bookmark.visibility = View.GONE
            }
        })

        swap = findViewById(R.id.view_stationpicker_swap)
        swap.setOnClickListener {
            if(!swapAnimationRunning) {
                swap.animate()
                    .rotationBy(180f)
                    .setInterpolator(AnticipateOvershootInterpolator(2f))
                    .setDuration(450)
                    .withEndAction { swapAnimationRunning = false }
                    .start()
                swapAnimationRunning = true
            }

            val fromText = from.text
            val toText = to.text

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
                override fun onAnimationRepeat(animation: Animation?) { from.text = toText }
                override fun onAnimationEnd(animation: Animation?) { from.clearFocus() }
            })
            from.startAnimation(fromAnim)

            toAnim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) { }
                override fun onAnimationRepeat(animation: Animation?) { to.text = fromText }
                override fun onAnimationEnd(animation: Animation?) { to.clearFocus() }
            })
            to.startAnimation(toAnim)
        }
    }

    fun initFrom(picker: StationPicker) {
        from.text = picker.from.text
        to.text = picker.to.text
        refinedFrom = picker.refinedFrom
        refinedTo = picker.refinedTo
        depArr.setSelection(picker.depArr.selectedItemPosition)
        dateText.text = picker.dateText.text
        timeText.text = picker.timeText.text
        listener = picker.listener
        year = picker.year
        autoCompleteAdapter = picker.autoCompleteAdapter
        from.setAdapter(autoCompleteAdapter)
        to.setAdapter(autoCompleteAdapter)
    }

    fun setLoading(loading: Boolean) {
        if(loading) {
            // TODO morph search icon into loading animation
            this.loading.visibility = View.VISIBLE
            fab.text = "Chargement..."
        } else {
            this.loading.visibility = View.GONE
            fab.text = "Rechercher"
        }
    }

    fun checkText(): Boolean {
        var isTextNotEmpty = true

        if(from.text.isEmpty()) {
            isTextNotEmpty = false
            from.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_empty_text, 0)
        }
        if(to.text.isEmpty()) {
            isTextNotEmpty = false
            to.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_empty_text, 0)
        }

        return isTextNotEmpty
    }

    fun from() = from.text.toString()
    fun to() = to.text.toString()

    fun fill(from: String, fromName: String= "", to: String, toName: String= "") {
        refinedFrom = from
        refinedTo = to
        CharacterAnimator(this.from, fromName).start()
        CharacterAnimator(this.to, toName).start()
    }

    fun fillWithBookmark(bookmark: Bookmark) {
        fill(
            from = bookmark.from,
            fromName = bookmark.fromName,
            to = bookmark.to,
            toName = bookmark.toName
        )
    }

    fun fillNow(from: String, to: String) {
        this.from.setText(from)
        this.to.setText(to)
    }

    private inner class CharacterAnimator(val view: EditText, val text: String, val delay: Long = 4, val onFinish: () -> Unit = {}) : Runnable {
        private var index = 0
        private val handler = Handler()

        fun start() {
            view.clearFocus()
            view.text.clear()
            handler.postDelayed(this, delay)
        }

        override fun run() {
            view.setText(text.subSequence(0, index++))
            if(index != text.length + 1)
                handler.postDelayed(this, delay)
            else
                onFinish()
        }
    }


    // =============== DATE UTILS ===============

    private fun now(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val min = Calendar.getInstance().get(Calendar.MINUTE)
        return ("${if(hour<10) "0$hour" else hour}:${if(min<10) "0$min" else min}")
    }

    private fun getMonth(): Int = when {
        dateText.text == "aujourd'hui" -> Calendar.getInstance().get(Calendar.MONTH) + 1
        dateText.text == "demain" -> {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.get(Calendar.MONTH) + 1
        }
        else -> dateText.text.split("/")[1].toInt()
    }

    private fun getDay(): Int = when {
        dateText.text == "aujourd'hui" -> Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        dateText.text == "demain" -> {
            val calendar = Calendar.getInstance().clone() as Calendar
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            year = calendar.get(Calendar.YEAR)
            calendar.get(Calendar.DAY_OF_MONTH)
        }
        else -> dateText.text.split("/")[0].replace("le ","").toInt()
    }



    // =============== INIT ===============

    fun setStations(stations: List<Station>) {
        val stationsStrings = ArrayList<String>(stations.size)

        for(station in stations)
            stationsStrings.add(station.name)

        val adapter = ArrayAdapter(context,
            android.R.layout.simple_dropdown_item_1line,
            stationsStrings
        )

        autoCompleteAdapter = adapter

        from.setAdapter(adapter)
        to.setAdapter(adapter)
    }

    fun onPause() {
        dateSpinner.setSelection(0)
    }

    interface StationPickerListener {
        fun onRequestEmitted(request: RouteRequest)
        fun onBookmarkAdded(from: String, to: String)
    }
}
package com.gueg.tclwatcher.stations

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.support.design.widget.FloatingActionButton
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.gueg.tclwatcher.R
import com.gueg.tclwatcher.routes.Request
import java.util.*
import kotlin.collections.ArrayList






class StationPicker(context: Context, attrs: AttributeSet ?= null) : FrameLayout(context, attrs) {

    private var from: AutoCompleteTextView
    private var to: AutoCompleteTextView
    private var fab: FloatingActionButton
    private var swap: Button
    private var depArr: Spinner
    private var dateLayout: LinearLayout
    private var dateText: TextView
    private var dateSpinner: Spinner
    private var timeLayout: LinearLayout
    private var timeText: TextView
    var listener: StationPickerListener?= null
    private var year: Int = Calendar.getInstance().get(Calendar.YEAR)
    private lateinit var autoCompleteAdapter: ArrayAdapter<String>

    init {
        addView(View.inflate(context, R.layout.view_stationpicker, null))

        from = findViewById(R.id.view_stationpicker_from)
        to = findViewById(R.id.view_stationpicker_to)

        depArr = findViewById(R.id.view_stationpicker_param_dep_arr)
        val depArrAdapter = ArrayAdapter.createFromResource(context,
            R.array.spinner_dep_arr,
            R.layout.view_spinner
        )
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
            var canDoRequest = true

            if(from.text.isEmpty()) {
                canDoRequest = false
                from.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_empty_text, 0)
                from.addTextChangedListener(object: TextWatcher{
                    override fun afterTextChanged(p0: Editable?) {}
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        if(p0 == null) return
                        if(p0.isNotEmpty())
                            from.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    }
                })
            }
            if(to.text.isEmpty()) {
                canDoRequest = false
                to.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_empty_text, 0)
                to.addTextChangedListener(object: TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {}
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        if (p0 == null) return
                        if (p0.isNotEmpty())
                            to.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    }
                })
            }

            if(canDoRequest) {
                val request = Request(
                    from = from.text.toString(),
                    to = to.text.toString(),
                    timeMode = if (depArr.selectedItemPosition == 0) Request.TimeMode.DEPART_AT else Request.TimeMode.ARRIVE_AT,
                    year = year,
                    month = getMonth(),
                    day = getDay(),
                    hour = timeText.text.split(":")[0].toInt(),
                    minute = timeText.text.split(":")[1].toInt()
                )
                listener!!.onRequestEmitted(request)
            }
        }

        from.setOnEditorActionListener { _, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_NEXT)
                to.requestFocus()
            false
        }
        to.setOnEditorActionListener { _, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_DONE)
                fab.performClick()
            false
        }

        swap = findViewById(R.id.view_stationpicker_swap)
        swap.setOnClickListener {
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
        depArr.setSelection(picker.depArr.selectedItemPosition)
        dateText.text = picker.dateText.text
        timeText.text = picker.timeText.text
        listener = picker.listener
        year = picker.year
        autoCompleteAdapter = picker.autoCompleteAdapter
        from.setAdapter(autoCompleteAdapter)
        to.setAdapter(autoCompleteAdapter)
    }

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

    interface StationPickerListener {
        fun onRequestEmitted(request: Request)
    }
}
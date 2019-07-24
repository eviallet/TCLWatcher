package com.gueg.tclwatcher

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.support.design.widget.FloatingActionButton
import android.util.AttributeSet
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
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
    var listener: StationPickerListener ?= null

    init {
        addView(View.inflate(context, R.layout.view_stationpicker, null))

        from = findViewById(R.id.view_stationpicker_from)
        to = findViewById(R.id.view_stationpicker_to)
        depArr = findViewById(R.id.view_stationpicker_param_dep_arr)
        val depArrAdapter = ArrayAdapter.createFromResource(context, R.array.spinner_dep_arr, R.layout.view_spinner)
        depArrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        depArr.adapter = depArrAdapter
        depArr.setSelection(0)

        dateLayout = findViewById(R.id.view_stationpicker_param_date_layout)
        dateText = findViewById(R.id.view_stationpicker_param_date)
        dateText.text = context.resources.getStringArray(R.array.spinner_date)[0]

        dateSpinner = findViewById(R.id.view_stationpicker_param_date_spinner)
        val dateAdapter = ArrayAdapter.createFromResource(context, R.array.spinner_date, R.layout.view_spinner_date)
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dateSpinner.adapter = dateAdapter
        dateSpinner.setSelection(0)
        dateLayout.setOnClickListener { dateSpinner.performClick() }
        dateSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                if(position==2) {
                    val pickerDialog = DatePickerDialog(
                        context, DatePickerDialog.OnDateSetListener { _, _, month, day ->
                            dateText.text = ("le ${
                                if(day<10) "0$day" else day
                            }/${
                                if(month<10) "0$month" else month
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
        fab.setOnClickListener { listener!!.onSearchClicked(from.text.toString(), to.text.toString()) }

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

    private fun now(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val min = Calendar.getInstance().get(Calendar.MINUTE)
        return ("${if(hour<10) "0$hour" else hour}:${if(min<10) "0$min" else min}")
    }

    fun setStations(stations: List<Station>) {
        val stationsStrings = ArrayList<String>(stations.size)

        for(station in stations)
            stationsStrings.add(station.name)

        val adapter = ArrayAdapter(context,
            android.R.layout.simple_dropdown_item_1line,
            stationsStrings
        )

        from.setAdapter(adapter)
        to.setAdapter(adapter)
    }

    interface StationPickerListener {
        fun onSearchClicked(from: String, to: String)
    }
}
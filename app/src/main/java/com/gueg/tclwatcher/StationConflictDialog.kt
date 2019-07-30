package com.gueg.tclwatcher

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView

class StationConflictDialog(context: Context, private val listener: StationConflictListener, private val from: Boolean,
                            private val choices: ArrayList<String>, private val values: ArrayList<String>) : Dialog(context) {

    private lateinit var list: RadioGroup

    @SuppressLint("UseSparseArrays")
    private val buttonToPosition = HashMap<Int, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_stationconflict)
        window!!.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        findViewById<Button>(R.id.dialog_stationconflict_cancel).setOnClickListener {
            listener.onCancelled()
            dismiss()
        }
        val okButton = findViewById<Button>(R.id.dialog_stationconflict_ok)
        okButton.setOnClickListener {
            listener.onValidated(values[buttonToPosition[list.checkedRadioButtonId]!!])
            dismiss()
        }

        findViewById<TextView>(R.id.dialog_stationconflict_title).text = (findViewById<TextView>(R.id.dialog_stationconflict_title).text.toString() + if(from) " de départ" else " d'arrivée")

        list = findViewById(R.id.dialog_stationconflict_checkbox)
        list.setOnCheckedChangeListener {_,_ ->
            if(!okButton.isEnabled) {
                okButton.setTextColor(context.resources.getColor(android.R.color.holo_green_light))
                okButton.isEnabled = true
            }
        }

        for(i in 0 until choices.size) {
            val checkbox = RadioButton(context)
            checkbox.text = choices[i]
            checkbox.id = View.generateViewId()
            buttonToPosition[checkbox.id] = i
            list.addView(checkbox)
        }
    }

    interface StationConflictListener {
        fun onCancelled()
        fun onValidated(value: String)
    }
}
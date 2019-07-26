package com.gueg.tclwatcher

import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView



class HomepageFragment : Fragment() {

    private lateinit var rootView: ViewGroup
    private lateinit var stationPicker: StationPicker

    private lateinit var errorLayout: LinearLayout
    private lateinit var errorText: TextView
    private var errorShown: Boolean = false

    private lateinit var stationPickerListener: StationPicker.StationPickerListener
    private lateinit var stations: List<Station>
    var tempStationPickerData: StationPicker ?= null
        set(value) {
            if(::stationPicker.isInitialized)
                stationPicker.initFrom(value!!)
            field = value
        }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_homepage, container, false) as ViewGroup

        stationPicker = rootView.findViewById(R.id.fragment_homepage_stationpicker)
        stationPicker.setStations(stations)
        stationPicker.listener = stationPickerListener
        if(tempStationPickerData != null)
            stationPicker.initFrom(tempStationPickerData!!)

        errorLayout = rootView.findViewById(R.id.fragment_homepage_error_layout)
        errorText = rootView.findViewById(R.id.fragment_homepage_error_text)

        return rootView
    }

    fun setStations(stations: List<Station>) {
        this.stations = stations
    }

    fun setStationPickerListener(stationPickerListener: StationPicker.StationPickerListener) {
        this.stationPickerListener = stationPickerListener
    }

    fun setError(text: String) {
        errorText.text = text
        if(!errorShown) {
            errorShown = true
            errorLayout.animate().translationYBy(-errorLayout.height.toFloat()+1)
            Handler().postDelayed({
                errorLayout.animate().translationY(0f).withEndAction { errorShown = false }
            }, 3000)
        }
    }

}
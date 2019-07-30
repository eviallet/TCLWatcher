package com.gueg.tclwatcher

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton



class HomepageFragment : Fragment() {

    private lateinit var rootView: ViewGroup
    private lateinit var stationPicker: StationPicker

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

        rootView.findViewById<ImageButton>(R.id.fragment_homepage_menu_map).setOnClickListener {
            activity!!.startActivity(Intent(activity, MapActivity::class.java))
        }

        return rootView
    }

    fun setStations(stations: List<Station>) {
        this.stations = stations
    }

    fun setStationPickerListener(stationPickerListener: StationPicker.StationPickerListener) {
        this.stationPickerListener = stationPickerListener
    }


}
package com.gueg.tclwatcher

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class HomepageFragment : Fragment() {

    private lateinit var rootView: View
    private lateinit var stationPicker: StationPicker
    private lateinit var button: Button

    private lateinit var homepageListener: HomepageListener
    private lateinit var stations: List<Station>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_homepage, container, false)

        stationPicker = rootView.findViewById(R.id.fragment_homepage_stationpicker)
        stationPicker.setStations(stations)
        stationPicker.listener = object: StationPicker.StationPickerListener {
            override fun onSearchClicked(from:String, to:String) {
                homepageListener.onRequestEmitted(from, to)
            }
        }

        return rootView
    }

    fun setStations(stations: List<Station>) {
        this.stations = stations
    }

    fun setHomepageListener(homepageListener: HomepageListener) {
        this.homepageListener = homepageListener
    }


    interface HomepageListener {
        fun onRequestEmitted(from:String, to:String)
    }
}
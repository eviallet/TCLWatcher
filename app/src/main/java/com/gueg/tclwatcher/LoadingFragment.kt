package com.gueg.tclwatcher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class LoadingFragment : Fragment() {

    private lateinit var rootView: View
    private lateinit var loadingText: TextView
    private var isInit = false
    private var pendingText = LoadingText.LOAD_DB

    enum class LoadingText {
        LOAD_DB,
        LOAD_ONLINE,
        INSERT_DB
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_loading, container, false)
        loadingText = rootView.findViewById(R.id.fragment_loading_text)
        isInit = true
        updateText(pendingText)
        return rootView
    }

    fun updateText(id: LoadingText) {
        if(isInit) {
            when (id) {
                LoadingText.LOAD_DB -> loadingText.text = resources.getText(R.string.loading_database)
                LoadingText.LOAD_ONLINE -> loadingText.text = resources.getText(R.string.loading_online)
                LoadingText.INSERT_DB -> loadingText.text = resources.getText(R.string.inserting_database)
            }
        } else
            pendingText = id
    }
}
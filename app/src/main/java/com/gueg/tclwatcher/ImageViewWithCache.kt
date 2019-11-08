package com.gueg.tclwatcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView


class ImageViewWithCache(context: Context, attrs : AttributeSet?= null) : FrameLayout(context, attrs) {

    private val webView : WebView = WebView(context)
    private val imageView : ImageView = ImageView(context)

    private var listener: ImageLoadedListener ?= null

    init {
        addView(webView)
        addView(imageView)

        setSelected(Selected.WEBVIEW)
    }

    fun setBitmap(bmp: Bitmap) {
        setSelected(Selected.IMAGEVIEW)

        imageView.setImageBitmap(bmp)
    }

    fun setDrawable(id: Int) {
        setSelected(Selected.IMAGEVIEW)

        imageView.setImageDrawable(context.getDrawable(id))
    }

    fun setSVG(url: String) {
        setSelected(Selected.WEBVIEW)

        webView.loadUrl(url)

        webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                listener?.onImageLoaded(drawViewToBitmap(webView))
                listener = null
            }
        }

    }

    // https://dev.to/pranavpandey/android-create-bitmap-from-a-view-3lck
    fun drawViewToBitmap(view: View) : Bitmap? {
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    fun setListener(listener: ImageLoadedListener) : ImageViewWithCache {
        this.listener = listener
        return this
    }

    private fun setSelected(what: Selected) = when(what) {
        Selected.WEBVIEW -> {
            webView.visibility = VISIBLE
            imageView.visibility = GONE
        }
        Selected.IMAGEVIEW -> {
            webView.visibility = GONE
            imageView.visibility = VISIBLE
        }
    }

    interface ImageLoadedListener {
        fun onImageLoaded(bmp: Bitmap?)
    }

    private enum class Selected {
        WEBVIEW,
        IMAGEVIEW
    }
}
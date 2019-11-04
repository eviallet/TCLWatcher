package com.gueg.tclwatcher

import android.content.Context
import android.webkit.WebView


// https://stackoverflow.com/a/8987637

class ImageLoader {
    companion object {
        fun load(context: Context, url: String, webView: WebView) {
            /*
            Thread {
                val html = Jsoup.connect(url).ignoreContentType(true).maxBodySize(Integer.MAX_VALUE).get().text()
                Log.d(":-:", "html : $html")
            }.start()


            val imgName = url.substring(url.lastIndexOf('/')+1).replace(".svg","")
            val fullPath = "${context.filesDir.absolutePath}/$imgName.xml"
            val file = File(fullPath)


            if(file.exists()) {
                val raw = file.readLines()[0]
                webView.loadDataWithBaseURL(url, raw, "multipart/related", "UTF-8", null)
                Log.d(":-:","Loaded from cache")
            } else {
                Log.d(":-:","Loaded from url")
            }*/
            webView.loadUrl(url)
        }
    }
}
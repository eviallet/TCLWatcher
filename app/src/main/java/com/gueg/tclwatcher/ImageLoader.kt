package com.gueg.tclwatcher

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream


class ImageLoader {
    companion object {
        fun load(activity: Activity, url: String, imageView: ImageViewWithCache) {
            val name = url.substring(url.lastIndexOf('/')+1).replace(".svg","")
            Thread {
                val bmp = ImageCache.load(activity, name)

                activity.runOnUiThread {
                    if (bmp != null) {
                        imageView.setBitmap(bmp)
                    } else {
                        imageView.setListener(object : ImageViewWithCache.ImageLoadedListener {
                            override fun onImageLoaded(bmp: Bitmap?) {
                                Thread {
                                    if(bmp != null)
                                        ImageCache.save(activity, name, bmp)
                                }.start()
                            }
                        }).setSVG(url)
                    }
                }
            }.start()
        }
    }

    object ImageCache {

        fun save(context: Context, name: String, bmp: Bitmap) {
            val cw = ContextWrapper(context)
            val directory = cw.getDir("bmp_cache", Context.MODE_PRIVATE)
            if (!directory.exists())
                directory.mkdir()

            val path = File(directory, "$name.bmp")
            val fos = FileOutputStream(path)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
        }

        fun load(context: Context, name: String): Bitmap? {
            val cw = ContextWrapper(context)
            val directory = cw.getDir("bmp_cache", Context.MODE_PRIVATE)

            if(!directory.exists())
                return null

            val file = File(directory.absolutePath+"/$name.bmp")

            if(!file.exists())
                return null

            return BitmapFactory.decodeFile(file.absolutePath)
        }
    }
}
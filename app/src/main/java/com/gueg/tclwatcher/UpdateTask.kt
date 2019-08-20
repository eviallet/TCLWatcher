package com.gueg.tclwatcher

import android.app.Activity
import android.app.PendingIntent
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.widget.Toast
import org.jsoup.Jsoup


class UpdateTask(private val app: Activity, private val userAsked: Boolean) : AsyncTask<Void, Void, Boolean>() {

    private lateinit var mPrefs: SharedPreferences

    companion object {
       private const val CURRENT_VERSION_TOKEN = "com.gueg.tclwatcher.updatetask.current_version_token"
       const val GITHUB_RELEASES_LINK = "https://github.com/eviallet/TCLWatcher/releases/latest/"
       private const val GITHUB_RELEASES_DOWNLOAD = "https://github.com/eviallet/TCLWatcher/releases/latest/download/tclwatcher.apk"
    }

    private val isConnected: Boolean
        get() {
            val cm = app.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            return cm.activeNetworkInfo != null && cm.activeNetworkInfo.isConnected
        }

    override fun onPreExecute() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(app)
    }

    override fun doInBackground(vararg voids: Void): Boolean? {
        if (!isConnected) return null

        val latestVersion = Jsoup.connect(GITHUB_RELEASES_LINK).get().title() ?: return null

        if(mPrefs.getString(CURRENT_VERSION_TOKEN, "") == "") {
            mPrefs.edit().putString(CURRENT_VERSION_TOKEN, latestVersion).apply()
            return false
        }

        return mPrefs.getString(CURRENT_VERSION_TOKEN, "") != latestVersion
    }

    override fun onPostExecute(response: Boolean?) {
        if(response == null) return

        if(response) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_RELEASES_DOWNLOAD))
            val pendingIntent = PendingIntent.getActivity(app, 0, intent, 0)

            val notificationBuilder =
                NotificationCompat.Builder(app, "TCLWatcher")
                    .setSmallIcon(R.drawable.ic_update)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setContentTitle("Mise à jour disponible")

            val notificationManager = NotificationManagerCompat.from(app)
            notificationManager.notify(1599 , notificationBuilder.build())
        } else if(userAsked) {
            Toast.makeText(app, "Aucune mise à jour disponible.", Toast.LENGTH_SHORT).show()
        }
    }

}
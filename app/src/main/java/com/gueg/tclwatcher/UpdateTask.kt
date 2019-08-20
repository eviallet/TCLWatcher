package com.gueg.tclwatcher

import android.app.Activity
import android.app.PendingIntent
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import android.widget.Toast
import org.jsoup.Jsoup


class UpdateTask(private val app: Activity, private val userAsked: Boolean) : AsyncTask<Void, Void, String>() {

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

    override fun doInBackground(vararg voids: Void): String? {
        if (!isConnected) return null

        val latestVersionUrl = Jsoup.connect(GITHUB_RELEASES_LINK).followRedirects(true).execute().url().toString()
        val latestVersionString = latestVersionUrl.subSequence(
            latestVersionUrl.lastIndexOf("/")+1,
            latestVersionUrl.length
        ).toString()

        val currentVersionString = app.packageManager.getPackageInfo(app.packageName, 0).versionName

        Log.d(":-:","Current version : $currentVersionString - Latest version : $latestVersionString")

        return if(currentVersionString == latestVersionString) "" else latestVersionString
    }

    override fun onPostExecute(response: String?) {
        if(response == null && userAsked)
            Toast.makeText(app, "Pas de connexion internet.", Toast.LENGTH_SHORT).show()
        if(response == null)
            return

        if(response.isNotEmpty()) {
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
package com.androosio.jamesdsptweaks

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.androosio.jamesdsptweaks.utils.JdspUtils
import com.androosio.jamesdsptweaks.utils.RootUtils

fun getApplicationName(context: Context): String {
    val applicationInfo = context.applicationInfo
    val stringId = applicationInfo.labelRes
    return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else context.getString(stringId)
}

class BootReceiver : BroadcastReceiver() {

    private val tag = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        // Temporary root (re-applied each boot) requires the manufacturer's PServer binder.
        // On a permanently-rooted device the JamesDSP Magisk module handles this instead.
        if (!RootUtils.hasPServer() || RootUtils.isDeviceRooted)
            return

        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED)
            return

        Log.d(tag, "Boot completed received")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d(tag, "POST_NOTIFICATIONS not granted; skipping boot tweaks")
                return
            }
        }

        val sharedPrefs = AppSettings.getSharedPrefs(context)
        if (AppSettings.getJdspEnabled(sharedPrefs)) {
            createNotificationChannel(context)
            showNotification(context)
            Log.d(tag, "Enabling JamesDSP at boot...")
            JdspUtils.enableJdsp(context)
        }
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            "boot_channel",
            "Boot Notification Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Channel for boot notifications" }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun showNotification(context: Context) {
        val builder = NotificationCompat.Builder(context, "boot_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getApplicationName(context))
            .setContentText("JamesDSP enabled at startup")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            with(NotificationManagerCompat.from(context)) {
                notify(123, builder.build())
            }
        }
    }
}

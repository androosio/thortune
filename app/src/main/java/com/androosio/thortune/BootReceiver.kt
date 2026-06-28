package com.androosio.thortune

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
import com.androosio.thortune.utils.JdspUtils
import com.androosio.thortune.utils.RootUtils
import com.androosio.thortune.utils.SaturationUtils

fun getApplicationName(context: Context): String {
    val applicationInfo = context.applicationInfo
    val stringId = applicationInfo.labelRes
    return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else context.getString(stringId)
}

class BootReceiver : BroadcastReceiver() {

    private val tag = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        // The tweaks are re-applied each boot through the manufacturer's PServer binder.
        if (!RootUtils.hasPServer())
            return

        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED)
            return

        Log.d(tag, "Boot completed received")

        val sharedPrefs = AppSettings.getSharedPrefs(context)

        // Display saturation persists via its system property, but re-issue the runtime
        // SurfaceFlinger call as a belt-and-braces fallback once PServer is reachable. This needs
        // no notification, so it runs before the POST_NOTIFICATIONS gate below.
        val saturation = AppSettings.getSaturation(sharedPrefs)
        if (saturation != AppSettings.DEFAULT_SATURATION) {
            Log.d(tag, "Re-applying display saturation at boot: $saturation")
            SaturationUtils.apply(context, saturation)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d(tag, "POST_NOTIFICATIONS not granted; skipping boot tweaks")
                return
            }
        }

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

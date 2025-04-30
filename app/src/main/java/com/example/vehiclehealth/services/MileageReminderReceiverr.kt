package com.example.vehiclehealth.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.vehiclehealth.MyApp
import com.example.vehiclehealth.R

class MileageReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID       = "mileage_reminder_channel"
        private const val CHANNEL_NAME     = "Mileage Reminders"
        private const val NOTIFICATION_ID  = 1001
        private const val ACTION_MILEAGE   = "com.example.vehiclehealth.ACTION_MILEAGE_REMINDER"
    }

    override fun onReceive(ctx: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_MILEAGE -> showMileageNotification(ctx)
            else           -> {

            }
        }
    }

    private fun showMileageNotification(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminder to update your vehicles mileage."
            }
            ctx.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val launchIntent = Intent(ctx, MyApp::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = ACTION_MILEAGE
        }
        val pi = PendingIntent.getActivity(
            ctx,  0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to log mileage")
            .setContentText("Tap here to update your vehicle’s current mileage.")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(ctx)
            .notify(NOTIFICATION_ID, notif)
    }
}


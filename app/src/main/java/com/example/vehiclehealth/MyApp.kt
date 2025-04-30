package com.example.vehiclehealth

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.vehiclehealth.services.ReminderAlarmReceiver
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

//        val dailyCheck = PeriodicWorkRequestBuilder<RemindersWorker>(
//            15, TimeUnit.MINUTES
//        )
//            .setInitialDelay(0, TimeUnit.MINUTES)
//            .build()
//
//        WorkManager.getInstance(this)
//            .enqueueUniquePeriodicWork(
//                "service_reminders",
//                ExistingPeriodicWorkPolicy.KEEP,
//                dailyCheck
//            )

        /**
         *  Service Reminder Notifications
         */
        val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val pi = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, ReminderAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmMgr.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis(),
            15 * 60 * 1_000L,
            pi
        )


        /**
         *  Mileage Update Reminder Notifications
         */
        val mileageIntent = Intent(this, ReminderAlarmReceiver::class.java).apply {
            action = "com.example.vehiclehealth.ACTION_MILEAGE_REMINDER"
        }
        val mileagePi = PendingIntent.getBroadcast(
            this,
            1,
            mileageIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
        }
        alarmMgr.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            mileagePi
        )
    }
}


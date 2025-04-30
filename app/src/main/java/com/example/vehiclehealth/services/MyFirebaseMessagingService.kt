package com.example.vehiclehealth.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.vehiclehealth.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val usersCol = FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)

        val data = mapOf(
            "fcmToken"        to token,
            "tokenUpdatedAt"  to FieldValue.serverTimestamp()
        )

        usersCol.set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("FCM", "Token saved.")
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Failed to save token.", e)
            }

        FirebaseMessaging.getInstance().subscribeToTopic("announcements")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Subscribed to Announcements.")
                }
            }

        FirebaseMessaging.getInstance().subscribeToTopic("feature_updates")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Subscribed to Feature Updates.")
                }
            }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val title = data["title"]
            ?: remoteMessage.notification?.title
            ?: "VehicleHealth"
        val body = data["body"]
            ?: remoteMessage.notification?.body
            ?: ""

        val type = data["type"] ?: "announcement"
        val (channelId, channelName) = when (type) {
            "service_reminder" ->
                "service_reminders_channel" to "Service Reminders"
            "feature_update"  ->
                "feature_updates_channel" to "Feature Updates"
            else              ->
                "announcements_channel" to "System Announcements"
        }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(chan)
        }

        val notif = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notif)
    }
}

package com.example.vehiclehealth.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.vehiclehealth.models.NotificationRecord
import com.example.vehiclehealth.models.ServiceHistory
import com.example.vehiclehealth.models.Vehicle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ReminderAlarmReceiver : BroadcastReceiver() {

    private val recommendedIntervals = mapOf(
        "oil change"       to Pair(31_557_600_000L, 10_000),      // 1 year, 10 k
        "fuel filter"      to Pair(63_115_200_000L, 30_000),      // 2 years, 30 k
        "car inspection"   to Pair(31_557_600_000L, Int.MAX_VALUE), // 1 year
        "brake inspection" to Pair(31_557_600_000L, 15_000)       // 1 year, 15 k
    )

    override fun onReceive(ctx: Context, intent: Intent) {
        GlobalScope.launch {
            val db   = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            val uid  = auth.currentUser?.uid ?: return@launch
            val notificationHelper = NotificationHelper(ctx)
            val now  = System.currentTimeMillis()

            val vehiclesSnap = db.collection("users")
                .document(uid)
                .collection("vehicles")
                .get()
                .await()

            val vehicles = vehiclesSnap.documents
                .mapNotNull { it.toObject(Vehicle::class.java)?.apply { id = it.id } }

            vehicles.forEach { vehicle ->
                val vid            = vehicle.id
                val vehicleMileage = vehicle.mileage

                val historySnap = db.collection("ServiceHistory")
                    .whereEqualTo("vehicleId", vid)
                    .get()
                    .await()

                val records = historySnap.documents
                    .mapNotNull {
                        it.toObject(ServiceHistory::class.java)
                            ?.apply { id = it.id }
                    }

                records
                    .groupBy { it.serviceType.trim().lowercase() }
                    .forEach { (type, recs) ->
                        val interval = recommendedIntervals[type] ?: return@forEach
                        val last     = recs.maxByOrNull { parseDateToMillis(it.date) }
                            ?: return@forEach

                        val timeDiff    = now - parseDateToMillis(last.date)
                        val mileageDiff = vehicleMileage - last.mileage

                        val timeStatus = when {
                            timeDiff    >= interval.first       -> "overdue"
                            timeDiff    >= interval.first / 2   -> "upcoming"
                            else                                -> "none"
                        }
                        val mileageStatus = when {
                            mileageDiff >= interval.second      -> "overdue"
                            mileageDiff >= interval.second / 2  -> "upcoming"
                            else                                -> "none"
                        }

                        val overall = when {
                            timeStatus    == "overdue"  || mileageStatus    == "overdue"  -> "overdue"
                            timeStatus    == "upcoming" || mileageStatus    == "upcoming" -> "upcoming"
                            else                                                           -> "none"
                        }

                        if (overall != "none") {
                            val title   = "${type.replaceFirstChar { it.uppercase() }} $overall"
                            val message = "Last done on ${last.date}, ${last.mileage} km ago"
                            val notifId = "$vid|$type|$overall".hashCode()

                            val existing = db.collection("Notifications")
                                .whereEqualTo("userId", uid)
                                .whereEqualTo("serviceType", title)
                                .whereEqualTo("status", message)
                                .get()
                                .await()

                            if (existing.isEmpty) {
                                notificationHelper.sendNotification(
                                    serviceType    = title,
                                    status         = message,
                                    notificationId = notifId
                                )

                                val rec = NotificationRecord(
                                    userId      = uid,
                                    serviceType = title,
                                    status      = message,
                                    timestamp   = System.currentTimeMillis()
                                )
                                db.collection("Notifications").add(rec)
                            }
                        }
                    }
            }
        }
    }

    private fun parseDateToMillis(date: String): Long =
        try {
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                .parse(date)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
}
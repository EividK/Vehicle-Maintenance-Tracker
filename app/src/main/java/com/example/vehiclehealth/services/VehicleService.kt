package com.example.vehiclehealth.services

import android.util.Log
import com.example.vehiclehealth.models.Vehicle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class VehicleService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getUserVehicles(): List<Vehicle> {
        val userId = auth.currentUser?.uid ?: return emptyList() // Get the logged-in user's ID
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("vehicles")
                .get()
                .await()

            Log.d("VehicleService", "Documents found: ${snapshot.documents.size}") // Test and debugging

            snapshot.documents.map { doc ->
                Vehicle(
                    id = doc.id.toIntOrNull() ?: 0,
                    brand = doc.getString("brand") ?: "",
                    model = doc.getString("model") ?: "",
                    year = doc.getLong("year")?.toInt() ?: 0,
                    vin = doc.getString("vin") ?: "",
                    registrationNumber = doc.getString("registrationNumber") ?: "",
                    mileage = doc.getLong("mileage")?.toInt() ?: 0,
                    engineType = "Unknown",
                    bodyStyle = "Unknown",
                    trimLevel = "Unknown",
                    transmissionType = "Unknown"
                )
            }
        } catch (e: Exception) {
            Log.e("VehicleService", "Error fetching vehicles", e)
            emptyList()
        }
    }
}

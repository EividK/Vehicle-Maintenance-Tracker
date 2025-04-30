package com.example.vehiclehealth.services

/**
 * Handles Vehicle Actions using Firebase
 */

import android.util.Log
import com.example.vehiclehealth.models.Vehicle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale

class VehicleService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getUserVehicles(): List<Vehicle> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("vehicles")
                .get()
                .await()

            Log.d("VehicleService", "Documents found: ${snapshot.documents.size}")

            snapshot.documents.map { doc ->
                Vehicle(
                    id = doc.id,
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

    suspend fun getVehicleById(vehicleId: String): Vehicle? {
        val userId = auth.currentUser?.uid ?: return null
        return try {
            val doc  = db.collection("users")
                .document(userId)
                .collection("vehicles")
                .document(vehicleId)
                .get()
                .await()

            if (doc.exists()) {
                Vehicle(
                    id                 = doc.id,
                    brand              = doc.getString("brand") ?: "",
                    model              = doc.getString("model") ?: "",
                    year               = doc.getLong("year")?.toInt() ?: 0,
                    vin                = doc.getString("vin") ?: "",
                    registrationNumber = doc.getString("registrationNumber") ?: "",
                    mileage            = doc.getLong("mileage")?.toInt() ?: 0,
                    engineType         = doc.getString("engineType") ?: "Unknown",
                    bodyStyle          = doc.getString("bodyStyle") ?: "Unknown",
                    trimLevel          = doc.getString("trimLevel") ?: "Unknown",
                    transmissionType   = doc.getString("transmissionType") ?: "Unknown"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("VehicleService", "Error fetching vehicle", e)
            null
        }
    }

    suspend fun addVehicle(vehicle: Vehicle): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to add vehicles.")

        val vehiclesRef = db
            .collection("users")
            .document(uid)
            .collection("vehicles")

        val newDoc = vehiclesRef.document()
        val newId  = newDoc.id
        val payload = mapOf(
            "brand"              to vehicle.brand,
            "model"              to vehicle.model,
            "year"               to vehicle.year,
            "vin"                to vehicle.vin,
            "registrationNumber" to vehicle.registrationNumber,
            "mileage"            to vehicle.mileage,
            "engineType"         to vehicle.engineType,
            "bodyStyle"          to vehicle.bodyStyle,
            "trimLevel"          to vehicle.trimLevel,
            "transmissionType"   to vehicle.transmissionType
        )
        newDoc.set(payload).await()

        return newId
    }

    suspend fun deleteServiceHistoryForVehicle(vehicleId: String) {
        try {
            val snapshot = db.collection("ServiceHistory")
                .whereEqualTo("vehicleId", vehicleId)
                .get()
                .await()

            val batch = db.batch()

            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("VehicleService", "Error deleting service history", e)
            throw e
        }
    }

    suspend fun deleteVehicle(vehicleId: String) {
        try {
            val uid = auth.currentUser?.uid ?: return

            db.collection("users")
                .document(uid)
                .collection("vehicles")
                .document(vehicleId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("VehicleService", "Error deleting vehicle", e)
            throw e
        }
    }

    suspend fun updateVehicleFromVin(vehicle: Vehicle) {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to update vehicles.")
        require(vehicle.id.isNotBlank()) { "Vehicle ID is required for update." }

        val vehiclesRef = db.collection("users")
            .document(uid)
            .collection("vehicles")

        if (vehicle.vin != "not specified") {
            val dup = vehiclesRef
                .whereEqualTo("vin", vehicle.vin)
                .get()
                .await()
                .documents
                .any { it.id != vehicle.id }
            if (dup) {
                throw IllegalStateException("Another vehicle with this VIN already exists.")
            }
        }

        vehiclesRef
            .document(vehicle.id)
            .update(
                mapOf(
                    "vin"              to vehicle.vin,
                    "brand"            to vehicle.brand,
                    "model"            to vehicle.model,
                    "year"             to vehicle.year,
                    "engineType"       to vehicle.engineType,
                    "bodyStyle"        to vehicle.bodyStyle,
                    "trimLevel"        to vehicle.trimLevel,
                    "transmissionType" to vehicle.transmissionType
                )
            ).await()
    }

    suspend fun updateMileage(vehicleId: String, newMileage: Int) {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Must be signed in.")
        db.collection("users")
            .document(uid)
            .collection("vehicles")
            .document(vehicleId)
            .update("mileage", newMileage)
            .await()
    }
}


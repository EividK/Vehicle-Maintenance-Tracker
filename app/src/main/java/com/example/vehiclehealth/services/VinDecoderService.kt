package com.example.vehiclehealth.services

/**
 *
 * VIN (Vehicle Identification Number) Decoder
 *
 */

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.vehiclehealth.models.Vehicle
import com.example.vehiclehealth.utils.isValidVin
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

// Intermediate data class for local decoding
private data class DecodedVinData(
    val vin: String,
    val brand: String,
    val year: Int,
    val modelCode: String,
    val model: String = "Unknown" // default if mapping lookup fails
)

// New data class to hold extended model mapping information.
private data class ModelMappingData(
    val modelName: String,
    val engineType: String?,
    val bodyStyle: String?,
    val trimLevel: String?,
    val transmissionType: String?
)

class VinDecoderService : Service() {

    private val firestore = FirebaseFirestore.getInstance()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val vin = intent?.getStringExtra("vin")
        if (vin.isNullOrEmpty() || vin.length != 17) {
            Log.e("VinDecoderService", "Invalid VIN provided!")
            stopSelf()
        } else {
            decodeVinUsingDatabase(vin)
        }
        return START_NOT_STICKY
    }

    /**
     * Decodes the VIN by:
     * 1. Extracting WMI, year code, and model code.
     * 2. Querying Firestore to get the brand (from wmiMapping) and year (from vinMapping).
     * 3. Looking up the extended model information in the modelMapping collection.
     * 4. Broadcasting the complete Vehicle object.
     */
    private fun decodeVinUsingDatabase(vin: String) {
        // Validate the VIN first.
        if (!isValidVin(vin)) {
            Log.e("VinDecoderService", "Invalid VIN: check digit does not match")
            val errorVehicle = Vehicle(
                vin = vin,
                brand = "Invalid VIN",
                model = "Invalid VIN",
                year = 0,
                mileage = 0,
                registrationNumber = "",
                engineType = "Unknown",
                bodyStyle = "Unknown",
                trimLevel = "Unknown",
                transmissionType = "Unknown"
            )
            broadcastDecodedData(errorVehicle)
            stopSelf()
            return
        }

        val wmi = vin.substring(0, 3)
        val yearCode = vin[9]
        val modelCode = vin.substring(3, 8)

        // Retrieve brand from wmiMapping.
        getBrandForWMI(wmi, onSuccess = { brand ->
            // Retrieve year from yearMapping.
            getYearForCode(yearCode, onSuccess = { year ->
                // Lookup the model mapping using brand and modelCode.
                lookupModelMapping(brand, modelCode, onSuccess = { mappingData ->
                    val finalModel = mappingData?.modelName ?: "Unknown"
                    val vehicleResult = Vehicle(
                        //id = "",
                        vin = vin,
                        brand = brand,
                        model = finalModel,
                        year = year,
                        mileage = 0,
                        registrationNumber = "",
                        engineType = mappingData?.engineType ?: "Unknown",
                        bodyStyle = mappingData?.bodyStyle ?: "Unknown",
                        trimLevel = mappingData?.trimLevel ?: "Unknown",
                        transmissionType = mappingData?.transmissionType ?: "Unknown"
                    )
                    broadcastDecodedData(vehicleResult)
                    stopSelf()
                }, onFailure = {
                    // Fallback if no mapping found.
                    val vehicleResult = Vehicle(
                        //id = "",
                        vin = vin,
                        brand = brand,
                        model = "Unknown",
                        year = year,
                        mileage = 0,
                        registrationNumber = "",
                        engineType = "Unknown",
                        bodyStyle = "Unknown",
                        trimLevel = "Unknown",
                        transmissionType = "Unknown"
                    )
                    broadcastDecodedData(vehicleResult)
                    stopSelf()
                })
            }, onFailure = {
                Log.e("VinDecoderService", "Year mapping not found for code: $yearCode")
                stopSelf()
            })
        }, onFailure = {
            Log.e("VinDecoderService", "Brand mapping not found for WMI: $wmi")
            stopSelf()
        })
    }

    /**
     * Retrieves the brand for the given WMI from the "wmiMapping" collection.
     */
    private fun getBrandForWMI(wmi: String, onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        firestore.collection("wmiMapping").document(wmi)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val brand = document.getString("brand")
                    if (brand != null) {
                        onSuccess(brand.trim())
                    } else {
                        onFailure()
                    }
                } else {
                    onFailure()
                }
            }
            .addOnFailureListener { onFailure() }
    }

    /**
     * Retrieves the model year for the given year code from the "vinMapping/yearMapping" document.
     */
    private fun getYearForCode(yearCode: Char, onSuccess: (Int) -> Unit, onFailure: () -> Unit) {
        firestore.collection("vinMapping").document("yearMapping")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val mapping = document.get("mapping") as? Map<String, Long>
                    val codeStr = yearCode.toString()
                    val yearLong = mapping?.get(codeStr)
                    if (yearLong != null) {
                        onSuccess(yearLong.toInt())
                    } else {
                        onFailure()
                    }
                } else {
                    onFailure()
                }
            }
            .addOnFailureListener { onFailure() }
    }

    /**
     * Looks up the modelMapping collection for a document matching the given brand and modelCode.
     * Returns the extended mapping information (modelName, engineType, bodyStyle, trimLevel, transmissionType).
     */
    private fun lookupModelMapping(
        brand: String,
        modelCode: String,
        onSuccess: (ModelMappingData?) -> Unit,
        onFailure: () -> Unit
    ) {
        val normalizedBrand = brand.trim()
        val normalizedModelCode = modelCode.trim()
        firestore.collection("modelMapping")
            .whereEqualTo("brand", normalizedBrand)
            .whereEqualTo("modelCode", normalizedModelCode)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d("VinDecoderService", "lookupModelMapping: snapshot.size() = ${snapshot.size()}")
                if (snapshot.documents.isNotEmpty()) {
                    val doc = snapshot.documents[0]
                    Log.d("VinDecoderService", "lookupModelMapping: Document data: ${doc.data}")
                    val modelName = doc.getString("modelName")
                    val engineType = doc.getString("engineType")
                    val bodyStyle = doc.getString("bodyStyle")
                    val trimLevel = doc.getString("trimLevel")
                    val transmissionType = doc.getString("transmissionType")
                    onSuccess(
                        ModelMappingData(
                            modelName = modelName ?: "Unknown",
                            engineType = engineType,
                            bodyStyle = bodyStyle,
                            trimLevel = trimLevel,
                            transmissionType = transmissionType
                        )
                    )
                } else {
                    Log.d("VinDecoderService", "lookupModelMapping: No document found for brand=$normalizedBrand, modelCode=$normalizedModelCode")
                    onSuccess(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("VinDecoderService", "lookupModelMapping error: ${e.message}")
                onFailure()
            }
    }

    /**
     * Broadcasts the decoded vehicle details so that the UI can update.
     */
    private fun broadcastDecodedData(vehicle: Vehicle) {
        val broadcastIntent = Intent("com.example.vehiclehealth.ACTION_VIN_DECODED").apply {
            putExtra("vin", vehicle.vin)
            putExtra("brand", vehicle.brand)
            putExtra("model", vehicle.model)
            putExtra("year", vehicle.year)
            // Optionally broadcast extra fields if needed.
            putExtra("engineType", vehicle.engineType)
            putExtra("bodyStyle", vehicle.bodyStyle)
            putExtra("trimLevel", vehicle.trimLevel)
            putExtra("transmissionType", vehicle.transmissionType)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
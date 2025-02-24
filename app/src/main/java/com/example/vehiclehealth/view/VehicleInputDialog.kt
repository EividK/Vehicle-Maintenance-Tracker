package com.example.vehiclehealth.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.vehiclehealth.models.Vehicle
import com.example.vehiclehealth.services.VinDecoderService

@Composable
fun VehicleInputDialog(
    onDismiss: () -> Unit,
    onAddVehicle: (Vehicle) -> Unit
) {
    val context = LocalContext.current

    var vinValue by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var registrationNumber by remember { mutableStateOf("") }
    var decodedBrand by remember { mutableStateOf<String?>(null) }
    var decodedModel by remember { mutableStateOf<String?>(null) }
    var decodedYear by remember { mutableStateOf<Int?>(null) }
    var decodingInProgress by remember { mutableStateOf(false) }

    // BroadcastReceiver to receive decoded details from the service.
    val decodedReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.vehiclehealth.ACTION_VIN_DECODED") {
                    decodedBrand = intent.getStringExtra("brand")
                    decodedModel = intent.getStringExtra("model")
                    decodedYear = intent.getIntExtra("year", 0)
                    decodingInProgress = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter("com.example.vehiclehealth.ACTION_VIN_DECODED")
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(decodedReceiver, filter)
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(decodedReceiver)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Vehicle") },
        text = {
            Column(modifier = Modifier.padding(8.dp)) {
                // VIN input row with a "Decode" button.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextField(
                        value = vinValue,
                        onValueChange = { vinValue = it },
                        label = { Text("VIN") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (vinValue.length != 17) {
                            Toast.makeText(context, "VIN must be 17 characters", Toast.LENGTH_SHORT).show()
                        } else {
                            decodingInProgress = true
                            // Start the service to decode the VIN using the database.
                            val serviceIntent = Intent(context, VinDecoderService::class.java).apply {
                                putExtra("vin", vinValue)
                            }
                            context.startService(serviceIntent)
                        }
                    }) {
                        Text("Decode")
                    }
                }
                if (decodingInProgress) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Decoding VIN...")
                }
                // When decoded data is available, display it and show additional input fields.
                if (decodedBrand != null && decodedModel != null && decodedYear != null && decodedYear != 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Brand: $decodedBrand")
                    Text("Model: $decodedModel")
                    Text("Year: $decodedYear")
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = mileage,
                        onValueChange = { mileage = it },
                        label = { Text("Mileage") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = registrationNumber,
                        onValueChange = { registrationNumber = it },
                        label = { Text("Registration Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (decodedBrand != null && decodedModel != null && decodedYear != null && decodedYear != 0) {
                Button(onClick = {
                    val vehicle = Vehicle(
                        //id = "",
                        vin = vinValue,
                        brand = decodedBrand!!,
                        model = decodedModel!!,
                        year = decodedYear!!,
                        mileage = mileage.toIntOrNull() ?: 0,
                        registrationNumber = registrationNumber
                    )
                    onAddVehicle(vehicle)
                    onDismiss()
                }) {
                    Text("Add Vehicle")
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

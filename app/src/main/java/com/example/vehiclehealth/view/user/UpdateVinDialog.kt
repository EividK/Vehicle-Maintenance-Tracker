package com.example.vehiclehealth.view.user


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.vehiclehealth.models.Vehicle
import com.example.vehiclehealth.services.VehicleService
import com.example.vehiclehealth.services.VinDecoderService
import com.example.vehiclehealth.utils.isValidVin
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch

@Composable
fun UpdateVinDialog(
    initialVehicle: Vehicle,
    onDismiss: () -> Unit,
    onVinApplied: (Vehicle) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vehicleService = remember { VehicleService() }

    // VIN + decoded fields
    var vinValue by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var yearText by remember { mutableStateOf("") }
    var engineType by remember { mutableStateOf("") }
    var bodyStyle by remember { mutableStateOf("") }
    var trimLevel by remember { mutableStateOf("") }
    var transmissionType by remember { mutableStateOf("") }
    var decodingInProgress by remember { mutableStateOf(false) }
    var vinConfirmed by remember { mutableStateOf(false) }
    var decodeFailed by remember { mutableStateOf(false) }

    // Camera launcher for VIN scanning
    val captureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap == null) {
            Log.e("UpdateVinDialog", "Camera capture failed")
            Toast.makeText(context, "Camera capture failed", Toast.LENGTH_SHORT).show()
        } else {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val raw = visionText.text
                        val cleaned = raw
                            .uppercase()
                            .replace("O", "0")
                            .replace("\n", " ")
                            .replace("[^A-HJ-NPR-Z0-9 ]".toRegex(), " ")
                            .replace("\\s+".toRegex(), " ")
                            .trim()
                        var candidate = Regex("[A-HJ-NPR-Z0-9]{17}")
                            .find(cleaned)
                            ?.value
                            .takeIf { it != null && isValidVin(it) }
                        if (candidate == null) {
                            candidate = cleaned
                                .split(" ")
                                .find { it.length == 17 && isValidVin(it) }
                        }
                        if (candidate != null) {
                            vinValue = candidate
                            decodingInProgress = true
                            decodeFailed = false
                            Intent(context, VinDecoderService::class.java).also {
                                it.putExtra("vin", candidate)
                                context.startService(it)
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "No valid VIN found, please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("UpdateVinDialog", "Text recognition error", e)
                        Toast.makeText(context, "Error processing image", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Log.e("UpdateVinDialog", "Exception during image processing", e)
                Toast.makeText(context, "Error processing image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            captureLauncher.launch()
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to scan VIN.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //BroadcastReceiver for decoded VIN details
    val decodedReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == "com.example.vehiclehealth.ACTION_VIN_DECODED") {
                    brand = intent.getStringExtra("brand") ?: ""
                    model = intent.getStringExtra("model") ?: ""
                    yearText = intent.getIntExtra("year", 0).toString()
                    engineType = intent.getStringExtra("engineType") ?: ""
                    bodyStyle = intent.getStringExtra("bodyStyle") ?: ""
                    trimLevel = intent.getStringExtra("trimLevel") ?: ""
                    transmissionType = intent.getStringExtra("transmissionType") ?: ""
                    decodingInProgress = false
                    decodeFailed = false
                }
            }
        }
    }
    DisposableEffect(Unit) {
        val filter = IntentFilter("com.example.vehiclehealth.ACTION_VIN_DECODED")
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(decodedReceiver, filter)
        onDispose {
            LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(decodedReceiver)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2A2A2A),
        shape = MaterialTheme.shapes.medium,
        title = {
            Text(
                "Update VIN",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = vinValue,
                    onValueChange = { vinValue = it },
                    label = { Text("VIN", color = Color.White) },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White),
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (vinValue.length == 17) {
                            decodingInProgress = true
                            decodeFailed = false
                            Intent(context, VinDecoderService::class.java).also {
                                it.putExtra("vin", vinValue)
                                context.startService(it)
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "VIN must be 17 characters",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Text("Search")
                    }
                    Button(onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            captureLauncher.launch()
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }) {
                        Text("Scan")
                    }
                }

                if (decodingInProgress) {
                    Text("Decoding VIN...", color = Color.Yellow)
                }
                if (decodeFailed) {
                    Text("Decoding failed, please retry.", color = Color.Red)
                }

                if (!decodingInProgress && brand.isNotBlank()) {
                    Text("Decoded Details:", color = Color.White, fontSize = 14.sp)
                    Text("Brand: $brand", color = Color.White)
                    Text("Model: $model", color = Color.White)
                    Text("Year: $yearText", color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!vinConfirmed) {
                            Button(onClick = { vinConfirmed = true }) {
                                Text("Yes")
                            }
                            Button(onClick = {
                                vinValue = ""
                                brand = ""
                                model = ""
                                yearText = ""
                                engineType = ""
                                bodyStyle = ""
                                trimLevel = ""
                                transmissionType = ""
                            }) {
                                Text("No")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updated = initialVehicle.copy(
                    vin = vinValue,
                    brand = brand,
                    model = model,
                    year = yearText.toIntOrNull() ?: initialVehicle.year,
                    engineType = engineType.ifBlank { initialVehicle.engineType },
                    bodyStyle = bodyStyle.ifBlank { initialVehicle.bodyStyle },
                    trimLevel = trimLevel.ifBlank { initialVehicle.trimLevel },
                    transmissionType = transmissionType.ifBlank { initialVehicle.transmissionType },
                )
                scope.launch {
                    try {
                        vehicleService.updateVehicleFromVin(updated)
                        onVinApplied(updated)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Failed to update VIN: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
                enabled = vinConfirmed
            ) {
                Text("Apply", color = if (vinConfirmed) Color.White else Color.Gray)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Red)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun textFieldColors(): TextFieldColors =
    TextFieldDefaults.outlinedTextFieldColors(
        containerColor       = Color(0xFF3A3A3A),
        focusedBorderColor   = Color.White,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor    = Color.White,
        unfocusedLabelColor  = Color.Gray,
        cursorColor          = Color.White
    )

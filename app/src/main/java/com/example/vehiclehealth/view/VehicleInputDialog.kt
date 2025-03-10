package com.example.vehiclehealth.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.vehiclehealth.components.MileageTextField
import com.example.vehiclehealth.components.RegistrationNumberTextField
import com.example.vehiclehealth.models.Vehicle
import com.example.vehiclehealth.services.VinDecoderService
import com.example.vehiclehealth.utils.isValidVin
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.NumberFormat
import java.util.Locale

@Composable
fun VehicleInputDialog(
    onDismiss: () -> Unit,
    onAddVehicle: (Vehicle) -> Unit
) {
    val context = LocalContext.current

    // Vehicle Details by VIN
    var vinValue by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var registrationNumber by remember { mutableStateOf("") }
    var decodedBrand by remember { mutableStateOf<String?>(null) }
    var decodedModel by remember { mutableStateOf<String?>(null) }
    var decodedYear by remember { mutableStateOf<Int?>(null) }
    var decodedEngineType by remember { mutableStateOf<String?>(null) }
    var decodedBodyStyle by remember { mutableStateOf<String?>(null) }
    var decodedTrimLevel by remember { mutableStateOf<String?>(null) }
    var decodedTransmissionType by remember { mutableStateOf<String?>(null) }

    var decodingInProgress by remember { mutableStateOf(false) }
    var decodeFailed by remember { mutableStateOf(false) }

    var mileageState by remember { mutableStateOf(TextFieldValue("")) }
    var regNumberState by remember { mutableStateOf(TextFieldValue("")) }

    // Start a timeout effect when decoding begins.
    LaunchedEffect(decodingInProgress) {
        if (decodingInProgress) {
            // Wait for 10 seconds.
            kotlinx.coroutines.delay(10000L)
            // If still decoding after 10 seconds, consider it a failure.
            if (decodingInProgress) {
                decodeFailed = true
                decodingInProgress = false
            }
        }
    }

    fun adjustBrightnessAndContrast(
        bitmap: Bitmap,
        brightnessFactor: Float,
        contrast: Float
    ): Bitmap {
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightnessFactor * 50, // Increase brightness offset
                0f, contrast, 0f, 0f, brightnessFactor * 50,
                0f, 0f, contrast, 0f, brightnessFactor * 50,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val ret = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(ret)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return ret
    }


    // Launcher to capture a picture and returns a Bitmap preview.
    val captureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap == null) {
            Log.e("VehicleInputDialog", "Camera capture cancelled or failed")
            Toast.makeText(context, "Camera capture failed", Toast.LENGTH_SHORT).show()
        } else {
            Log.d(
                "VehicleInputDialog",
                "Bitmap captured: width=${bitmap.width}, height=${bitmap.height}"
            )
            // Process the captured image using ML Kit Text Recognition.
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        // Preprocess recognized text: remove newlines and extra spaces.
                        // Log the raw recognized text
                        val rawText = visionText.text
                        Log.d("VehicleInputDialog", "Raw recognized text: $rawText")

                        // Preprocess the recognized text: remove newlines, non-VIN characters, and extra spaces.
                        val cleanedText = rawText
                            .uppercase()
                            .replace("O", "0")
                            .replace("\n", " ")
                            .replace("[^A-HJ-NPR-Z0-9 ]".toRegex(), " ")
                            .replace("\\s+".toRegex(), " ")
                            .trim()
                        Log.d("VehicleInputDialog", "Cleaned recognized text: $cleanedText")

                        // Attempt 1: Use regex on the entire cleaned text.
                        var candidate = Regex("[A-HJ-NPR-Z0-9]{17}").find(cleanedText)?.value
                        if (candidate != null) {
                            Log.d("VehicleInputDialog", "Regex candidate: $candidate")
                            if (!isValidVin(candidate)) {
                                Log.d("VehicleInputDialog", "Regex candidate invalid: $candidate")
                                candidate = null
                            }
                        }

                        // Attempt 2: If no candidate from regex, try splitting by space.
                        if (candidate == null) {
                            candidate = cleanedText.split(" ")
                                .find { it.length == 17 && isValidVin(it) }
                        }

                        if (candidate != null) {
                            Log.d("VehicleInputDialog", "Captured VIN: $candidate")
                            vinValue = candidate
                            // Auto-trigger decoding service.
                            decodingInProgress = true
                            decodeFailed = false
                            val serviceIntent =
                                Intent(context, VinDecoderService::class.java).apply {
                                    putExtra("vin", candidate)
                                }
                            context.startService(serviceIntent)
                        } else {
                            Log.e("VehicleInputDialog", "No valid VIN found in captured image")
                            Toast.makeText(
                                context,
                                "No valid VIN found. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("VehicleInputDialog", "Text recognition error: ${e.message}", e)
                        Toast.makeText(context, "Error processing image", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Log.e("VehicleInputDialog", "Exception during image processing: ${e.message}", e)
                Toast.makeText(context, "Error processing image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // BroadcastReceiver to receive decoded details from the service.
    val decodedReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.vehiclehealth.ACTION_VIN_DECODED") {
                    decodedBrand = intent.getStringExtra("brand")
                    decodedModel = intent.getStringExtra("model")
                    decodedYear = intent.getIntExtra("year", 0)
                    decodedEngineType = intent.getStringExtra("engineType")
                    decodedBodyStyle = intent.getStringExtra("bodyStyle")
                    decodedTrimLevel = intent.getStringExtra("trimLevel")
                    decodedTransmissionType = intent.getStringExtra("transmissionType")
                    decodingInProgress = false
                    // Reset failure state if we received a response.
                    decodeFailed = false
                    Log.d(
                        "VehicleInputDialog",
                        "Received decoded: brand=$decodedBrand, model=$decodedModel, year=$decodedYear, engine=$decodedEngineType, bodyStyle=$decodedBodyStyle, trim=$decodedTrimLevel, transmission=$decodedTransmissionType"
                    )
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
                            Toast.makeText(context, "VIN must be 17 characters", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            decodingInProgress = true
                            decodeFailed = false
                            // Start the service to decode the VIN using the database.
                            val serviceIntent =
                                Intent(context, VinDecoderService::class.java).apply {
                                    putExtra("vin", vinValue)
                                }
                            context.startService(serviceIntent)
                        }
                    }) {
                        Text("Decode")
                    }
                }
                // Capture VIN Button
                Spacer(modifier = Modifier.height(8.dp))
                // Capture VIN button.
                Button(
                    onClick = {
                        // Launch the camera to capture an image.
                        captureLauncher.launch()
                    },
                    enabled = !decodingInProgress && !decodeFailed
                ) {
                    Text("Capture VIN")
                }

                if (decodingInProgress) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Decoding VIN...")
                }
                if (decodeFailed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Vehicle not found. Please try again.",
                        color = androidx.compose.ui.graphics.Color.Red
                    )
                }
                // When decoded data is available, display it and show additional input fields.
                if (decodedBrand != null && decodedModel != null && decodedYear != null && decodedYear != 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Brand: $decodedBrand")
                    Text("Model: $decodedModel")
                    Text("Year: $decodedYear")
                    Text("Engine Type: $decodedEngineType")
                    Text("Body Style: $decodedBodyStyle")
                    Text("Trim Level: $decodedTrimLevel")
                    Text("Transmission: $decodedTransmissionType")
                    Spacer(modifier = Modifier.height(16.dp))

                    MileageTextField(
                        value = mileageState,
                        onValueChange = { mileageState = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    RegistrationNumberTextField(
                        value = regNumberState,
                        onValueChange = { regNumberState = it }
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
                        mileage = mileageState.text.replace(",", "").toIntOrNull() ?: 0,
                        registrationNumber = regNumberState.text,
                        engineType = "Unknown",
                        bodyStyle = "Unknown",
                        trimLevel = "Unknown",
                        transmissionType = "Unknown"
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

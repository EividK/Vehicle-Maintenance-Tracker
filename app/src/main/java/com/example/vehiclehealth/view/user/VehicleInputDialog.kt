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
import com.example.vehiclehealth.components.MileageTextField
import com.example.vehiclehealth.models.Vehicle
import com.example.vehiclehealth.services.VinDecoderService
import com.example.vehiclehealth.utils.isValidVin
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.compose.runtime.rememberCoroutineScope
import com.example.vehiclehealth.components.RegistrationNumberTextField
import com.example.vehiclehealth.services.VehicleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleInputDialog(
    onDismiss: () -> Unit,
    onAddVehicle: (Vehicle) -> Unit
) {
    val context = LocalContext.current

    var currentPage by remember { mutableStateOf(0) }
    var usingVin by remember { mutableStateOf(false) }
    var vinConfirmed by remember { mutableStateOf(false) }

    var vinValue by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var yearText by remember { mutableStateOf("") }
    var mileageState by remember { mutableStateOf(TextFieldValue("")) }
    var regNumberState by remember { mutableStateOf(TextFieldValue("")) }
    var engineType by remember { mutableStateOf("") }
    var bodyStyle by remember { mutableStateOf("") }
    var trimLevel by remember { mutableStateOf("") }
    var transmissionType by remember { mutableStateOf("") }

    // dropdowns
    var brandExpanded by remember { mutableStateOf(false) }
    val brandOptions = listOf(
        "Toyota", "Honda", "Ford", "Chevrolet", "Nissan", "BMW", "Mercedes-Benz",
        "Audi", "Volkswagen", "Hyundai", "Kia", "Subaru", "Mazda", "Lexus",
        "Jeep", "GMC", "Dodge", "Ram", "Volvo", "Porsche", "Land Rover", "Tesla",
        "Fiat"
    )
    var bodyStyleExpanded by remember { mutableStateOf(false) }
    val bodyStyleOptions = listOf(
        "Saloon", "Hatchback", "Coupe", "Convertible", "SUV", "Crossover",
        "Wagon", "Van", "Pickup", "Minivan", "Roadster"
    )
    var transmissionExpanded by remember { mutableStateOf(false) }
    val transmissionOptions = listOf(
        "Automatic", "Manual", "CVT", "Semi-Automatic", "Dual-Clutch", "Direct-Shift"
    )

    val scope: CoroutineScope = rememberCoroutineScope()
    val vehicleService = remember { VehicleService() }

    var decodingInProgress by remember { mutableStateOf(false) }
    var decodeFailed by remember { mutableStateOf(false) }

    // Camera launcher for VIN scanning
    val captureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap == null) {
            Log.e("VehicleInputDialog", "Camera capture cancelled or failed")
            Toast.makeText(context, "Camera capture failed", Toast.LENGTH_SHORT).show()
        } else {
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val rawText = visionText.text
                        val cleanedText = rawText
                            .uppercase()
                            .replace("O", "0")
                            .replace("\n", " ")
                            .replace("[^A-HJ-NPR-Z0-9 ]".toRegex(), " ")
                            .replace("\\s+".toRegex(), " ")
                            .trim()
                        var candidate = Regex("[A-HJ-NPR-Z0-9]{17}").find(cleanedText)?.value
                        if (candidate != null && !isValidVin(candidate)) {
                            candidate = null
                        }
                        if (candidate == null) {
                            candidate = cleanedText.split(" ").find { it.length == 17 && isValidVin(it) }
                        }
                        if (candidate != null) {
                            vinValue = candidate
                            decodingInProgress = true
                            decodeFailed = false
                            usingVin = true
                            val serviceIntent = Intent(context, VinDecoderService::class.java).apply {
                                putExtra("vin", candidate)
                            }
                            context.startService(serviceIntent)
                        } else {
                            Toast.makeText(context, "No valid VIN found. Please try again.", Toast.LENGTH_SHORT).show()
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            captureLauncher.launch()
        } else {
            Toast.makeText(context, "Camera permission is required for VIN scanning", Toast.LENGTH_SHORT).show()
        }
    }

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
        LocalBroadcastManager.getInstance(context).registerReceiver(decodedReceiver, filter)
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(decodedReceiver)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2A2A2A),
        shape = MaterialTheme.shapes.medium,
        title = {
            Text(
                "Add Vehicle",
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
                when (currentPage) {
                    0 -> {
                        OutlinedTextField(
                            value = vinValue,
                            onValueChange = { vinValue = it },
                            label = { Text("VIN", fontSize = 14.sp, color = Color.White) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (vinValue.length != 17) {
                                        Toast.makeText(context, "VIN must be 17 characters", Toast.LENGTH_SHORT).show()
                                    } else {
                                        decodingInProgress = true
                                        decodeFailed = false
                                        usingVin = true
                                        val serviceIntent = Intent(context, VinDecoderService::class.java).apply {
                                            putExtra("vin", vinValue)
                                        }
                                        context.startService(serviceIntent)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Search Vehicle", fontSize = 12.sp) }
                            Button(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        captureLauncher.launch()
                                    } else {
                                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Scan VIN", fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = {
                                usingVin = false
                                vinValue = "not specified"
                                currentPage = 1
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Skip VIN", fontSize = 12.sp) }

                        if (decodingInProgress) {
                            Text("Decoding VIN...", color = Color.Yellow, fontSize = 12.sp)
                        }
                        if (decodeFailed) {
                            Text("Vehicle not found. Please try again.", color = Color.Red, fontSize = 12.sp)
                        }
                        if (vinValue.length == 17 && brand.isNotBlank() && model.isNotBlank() && !decodingInProgress) {
                            Text("Vehicle Details:", color = Color.White, fontSize = 14.sp)
                            Text("Brand: $brand", color = Color.White, fontSize = 14.sp)
                            Text("Model: $model", color = Color.White, fontSize = 14.sp)
                            Text("Year: $yearText", color = Color.White, fontSize = 14.sp)
                            if (!vinConfirmed) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            vinConfirmed = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Yes", fontSize = 12.sp) }
                                    Button(
                                        onClick = {
                                            vinValue = ""
                                            vinConfirmed = false
                                            brand = ""
                                            model = ""
                                            yearText = ""
                                            engineType = ""
                                            bodyStyle = ""
                                            trimLevel = ""
                                            transmissionType = ""
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("No", fontSize = 12.sp) }
                                }
                            }
                            if (vinConfirmed) {
                                RegistrationNumberTextField(
                                    value = regNumberState,
                                    onValueChange = { regNumberState = it }
                                )
                                MileageTextField(
                                    value = mileageState,
                                    onValueChange = { mileageState = it }
                                )
                                Button(
                                    onClick = {
                                        if (regNumberState.text.isBlank()) {
                                            Toast.makeText(context, "Registration Number is required", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val vehicle = Vehicle(
                                                id  = "",
                                                vin = vinValue,
                                                brand = brand,
                                                model = model,
                                                year = yearText.toIntOrNull() ?: 0,
                                                mileage = mileageState.text.replace(",", "").toIntOrNull() ?: 0,
                                                registrationNumber = regNumberState.text,
                                                engineType = if (engineType.isNotBlank()) engineType else "not specified",
                                                bodyStyle = if (bodyStyle.isNotBlank()) bodyStyle else "not specified",
                                                trimLevel = if (trimLevel.isNotBlank()) trimLevel else "not specified",
                                                transmissionType = if (transmissionType.isNotBlank()) transmissionType else "not specified"
                                            )
                                            scope.launch {
                                                try {
                                                    val newId = vehicleService.addVehicle(vehicle)
                                                    onAddVehicle(vehicle.copy(id = newId))
                                                    onDismiss()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Failed to add vehicle: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                                }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Add Vehicle", fontSize = 14.sp) }
                            }
                        }
                    }
                    1 -> {
                        ExposedDropdownMenuBox(
                            expanded = brandExpanded,
                            onExpandedChange = { brandExpanded = !brandExpanded }
                        ) {
                            OutlinedTextField(
                                value = brand,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Brand", fontSize = 14.sp, color = Color.White) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(brandExpanded) },
                                textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                                colors = textFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = brandExpanded,
                                onDismissRequest = { brandExpanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                brandOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            brand = option
                                            brandExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Model", fontSize = 14.sp, color = Color.White) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = yearText,
                            onValueChange = { yearText = it },
                            label = { Text("Year", fontSize = 14.sp, color = Color.White) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        MileageTextField(
                            value = mileageState,
                            onValueChange = { mileageState = it }
                        )
                        RegistrationNumberTextField(
                            value = regNumberState,
                            onValueChange = { regNumberState = it }
                        )
                        Button(
                            onClick = {
                                if (brand.isBlank() || model.isBlank() || yearText.isBlank() || mileageState.text.isBlank() || regNumberState.text.isBlank()) {
                                    Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                                } else {
                                    currentPage = 2
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Next", fontSize = 14.sp) }
                    }
                    2 -> {
                        OutlinedTextField(
                            value = engineType,
                            onValueChange = { engineType = it },
                            label = { Text("Engine Type", fontSize = 14.sp, color = Color.White) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExposedDropdownMenuBox(
                            expanded = bodyStyleExpanded,
                            onExpandedChange = { bodyStyleExpanded = !bodyStyleExpanded }
                        ) {
                            OutlinedTextField(
                                value = bodyStyle,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Body Style", fontSize = 14.sp, color = Color.White) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(bodyStyleExpanded) },
                                textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                                colors = textFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = bodyStyleExpanded,
                                onDismissRequest = { bodyStyleExpanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                bodyStyleOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            bodyStyle = option
                                            bodyStyleExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = trimLevel,
                            onValueChange = { trimLevel = it },
                            label = { Text("Trim Level", fontSize = 14.sp, color = Color.White) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        ExposedDropdownMenuBox(
                            expanded = transmissionExpanded,
                            onExpandedChange = { transmissionExpanded = !transmissionExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = transmissionType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Transmission", fontSize = 14.sp, color = Color.White) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(transmissionExpanded) },
                                textStyle = TextStyle(fontSize = 14.sp, color = Color.White),
                                colors = textFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = transmissionExpanded,
                                onDismissRequest = { transmissionExpanded = false }
                            ) {
                                transmissionOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            transmissionType = option
                                            transmissionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = {
                                if (engineType.isBlank()) engineType = "not specified"
                                if (bodyStyle.isBlank()) bodyStyle = "not specified"
                                if (trimLevel.isBlank()) trimLevel = "not specified"
                                if (transmissionType.isBlank()) transmissionType = "not specified"

                                if (!usingVin) vinValue = "not specified"

                                val vehicle = Vehicle(
                                    vin = vinValue,
                                    brand = brand,
                                    model = model,
                                    year = yearText.toIntOrNull() ?: 0,
                                    mileage = mileageState.text.replace(",", "").toIntOrNull() ?: 0,
                                    registrationNumber = regNumberState.text,
                                    engineType = engineType,
                                    bodyStyle = bodyStyle,
                                    trimLevel = trimLevel,
                                    transmissionType = transmissionType
                                )
                                scope.launch {
                                    try {
                                        val newId = vehicleService.addVehicle(vehicle)
                                        onAddVehicle(vehicle.copy(id = newId))
                                        onDismiss()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Failed to add vehicle: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Add Vehicle", fontSize = 14.sp) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun textFieldColors(): TextFieldColors {
    return TextFieldDefaults.outlinedTextFieldColors(
        containerColor = Color(0xFF3A3A3A),
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.Gray,
        cursorColor = Color.White
    )
}

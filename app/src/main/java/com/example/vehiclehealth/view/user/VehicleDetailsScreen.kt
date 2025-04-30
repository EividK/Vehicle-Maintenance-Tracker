package com.example.vehiclehealth.view.user

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vehiclehealth.models.NotificationRecord
import com.example.vehiclehealth.models.ServiceHistory
import com.example.vehiclehealth.models.ServiceReminder
import com.example.vehiclehealth.models.Vehicle
import com.example.vehiclehealth.services.NotificationHelper
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailsScreen(
    navController: NavController,
    vehicle: Vehicle,
    onBack: () -> Unit,
    onAddService: (ServiceHistory) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val allServices = remember { mutableStateListOf<ServiceHistory>() }
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showUpdateVinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(vehicle.id) {
        db.collection("ServiceHistory")
            .whereEqualTo("vehicleId", vehicle.id.toString())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("VehicleDetailsScreen", "Error fetching service history", error)
                    return@addSnapshotListener
                }
                snapshot?.let { snap ->
                    allServices.clear()
                    for (doc in snap.documents) {
                        val service = doc.toObject(ServiceHistory::class.java)
                        service?.let {
                            it.id = doc.id
                            allServices.add(it)
                        }
                    }
                }
            }
    }

    // SAF picker launcher for JSON
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                try {
                    context.contentResolver.openInputStream(it)?.use { input ->
                        val json = input.bufferedReader().readText()
                        val listType = object : TypeToken<List<ServiceHistory>>() {}.type
                        val importedList: List<ServiceHistory> = Gson().fromJson(json, listType)

                        val db = FirebaseFirestore.getInstance()
                        var importedCount = 0
                        importedList
                            .filter { it.vehicleId == vehicle.id.toString() }
                            .forEach { service ->
                                val data = hashMapOf(
                                    "vehicleId"    to service.vehicleId,
                                    "serviceType"  to service.serviceType,
                                    "date"         to service.date,
                                    "description"  to service.description,
                                    "cost"         to service.cost,
                                    "mileage"      to service.mileage
                                )
                                db.collection("ServiceHistory")
                                    .add(data)
                                    .addOnSuccessListener {
                                        importedCount++
                                        if (importedCount == importedList.count { it.vehicleId == vehicle.id.toString() }) {
                                            Toast.makeText(
                                                context,
                                                "Imported $importedCount record(s)",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(
                                            context,
                                            "Failed to import one record: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                    } ?: throw IllegalStateException("Could not open stream")
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("VehicleDetailsScreen", "Import error", e)
                }
            }
        }
    )

    val recommendedIntervals = mapOf(
        "oil change" to Pair(31557600000L, 10000), // 1 year in ms and 10,000 km
        "fuel filter" to Pair(63115200000L, 30000),         // 2 years, 30,000 km
        "car inspection" to Pair(31557600000L, Int.MAX_VALUE), // 1 year, mileage ignored
        "brake inspection" to Pair(31557600000L, 15000)      // 1 year, 15,000 km
    )

    val currentTime = System.currentTimeMillis()
    val serviceReminders by remember(allServices, vehicle.mileage) {
        derivedStateOf {
            val grouped = allServices.groupBy { it.serviceType.trim().lowercase() }
            grouped.mapNotNull { (type, recs) ->
                val interval = recommendedIntervals[type] ?: return@mapNotNull null
                val last = recs.maxByOrNull { parseDateToMillis(it.date) } ?: return@mapNotNull null
                val timeDiff = currentTime - parseDateToMillis(last.date)
                val mileageDiff = vehicle.mileage - last.mileage
                val timeStatus = when {
                    timeDiff >= interval.first -> "overdue"
                    timeDiff >= interval.first/2 -> "upcoming"
                    else -> "none"
                }
                val mileageStatus = when {
                    mileageDiff >= interval.second -> "overdue"
                    mileageDiff >= interval.second/2 -> "upcoming"
                    else -> "none"
                }
                val overall = when {
                    timeStatus=="overdue" || mileageStatus=="overdue" -> "overdue"
                    timeStatus=="upcoming" || mileageStatus=="upcoming" -> "upcoming"
                    else -> null
                }
                overall?.let { ServiceReminder(type, last.date, last.mileage, it) }
            }
        }
    }

    val remindersUpcoming = serviceReminders.filter { it.status == "upcoming" }
    val remindersOverdue = serviceReminders.filter { it.status == "overdue" }

    Scaffold(
        containerColor = Color(0xFF1E1D2B),
        topBar = {
            SmallTopAppBar(
                title = { Text("Vehicle Details", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF2A293D))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF4C4B60),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Service")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1D2B))
                .padding(paddingValues)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    VehicleInfoCard(
                        vehicle = vehicle,
                        onRequestUpdateVin = { showUpdateVinDialog = true },
                        onRequestUpdateMileage = { navController.navigate("updateMileage/${vehicle.id}")}
                    )
                }

                if (remindersUpcoming.isNotEmpty()) {
                    item { SectionHeader("Upcoming Services") }
                    items(remindersUpcoming) { reminder ->
                        ServiceReminderCard(reminder)
                    }
                }
                if (remindersOverdue.isNotEmpty()) {
                    item { SectionHeader("Overdue Services") }
                    items(remindersOverdue) { reminder ->
                        ServiceReminderCard(reminder)
                    }
                }
                item {
                    SectionHeader("Completed Services")
                    if (allServices.isEmpty()) {
                        Text("No service records found.", color = Color.Gray)
                    }
                }
                items(allServices) { service ->
                    ServiceHistoryItem(service)
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showExportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C4B60)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Export")
                            Spacer(Modifier.width(4.dp))
                            Text("Export History")
                        }
                        Button(
                            onClick = { showImportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C4B60)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Import")
                            Spacer(Modifier.width(4.dp))
                            Text("Import History")
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Service History") },
            text = {
                Text(
                    "Export all service history for\n" +
                            "${vehicle.brand} ${vehicle.model}\n" +
                            "(${vehicle.registrationNumber})?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val json = Gson().toJson(allServices)
                    val safeName = "service_history_" +
                            "${vehicle.brand}_${vehicle.model}_${vehicle.registrationNumber}"
                                .replace("\\s+".toRegex(), "_") + ".json"
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/json")
                        put(
                            MediaStore.Downloads.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS
                        )
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                    )
                    if (uri != null) {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { os ->
                                os.write(json.toByteArray())
                                os.flush()
                            }
                            Toast.makeText(
                                context,
                                "Exported to Downloads/$safeName",
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (e: IOException) {
                            Toast.makeText(
                                context,
                                "Export failed: ${e.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(context, "Could not create file", Toast.LENGTH_LONG).show()
                    }
                    showExportDialog = false
                }) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Service History") },
            text = {
                Text(
                    "To import a export file from a vehicle, first add the exact same vehicle to your garage\n" +
                            "and then pick the JSON file here."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    importLauncher.launch(arrayOf("application/json"))
                    showImportDialog = false
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddDialog) {
        AddServiceDialog(
            vehicleId = vehicle.id.toString(),
            onDismiss = { showAddDialog = false },
            onServiceAdded = { newRecord ->
                onAddService(newRecord)
                showAddDialog = false
            }
        )
    }

    if (showUpdateVinDialog) {
        UpdateVinDialog(
            initialVehicle = vehicle,
            onDismiss = { showUpdateVinDialog = false },
            onVinApplied = { updated ->
                showUpdateVinDialog = false
            }
        )
    }
}

private fun parseDateToMillis(dateString: String): Long {
    return try {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        sdf.parse(dateString)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}

@Composable
private fun VehicleInfoCard(
    vehicle: Vehicle,
    onRequestUpdateVin: () -> Unit,
    onRequestUpdateMileage: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A293D)),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${vehicle.brand} ${vehicle.model}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(onClick = onRequestUpdateMileage) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Update Mileage",
                            tint = Color.White
                        )
                    }

                    if (vehicle.vin == "not specified") {
                        TextButton(onClick = onRequestUpdateVin) {
                            Text("Update VIN", color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Engine: ${vehicle.engineType}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = "Body Style: ${vehicle.bodyStyle}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = "Trim Level: ${vehicle.trimLevel}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = "Transmission: ${vehicle.transmissionType}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = "Year: ${vehicle.year}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Text(
                text = "VIN: ${vehicle.vin}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            if (vehicle.registrationNumber.isNotBlank()) {
                Text(
                    text = "Reg: ${vehicle.registrationNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Text(
                text = "Mileage: ${vehicle.mileage} km",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun ServiceHistoryItem(service: ServiceHistory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3A3A3A)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = service.serviceType.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Date: ${service.date}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            if (service.description.isNotBlank()) {
                Text(
                    text = "Description: ${service.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            Text(
                text = "Cost: €${service.cost}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Text(
                text = "Mileage: ${service.mileage} km",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
fun ServiceReminderCard(reminder: ServiceReminder) {
    val backgroundColor = when (reminder.status) {
        "overdue" -> Color(0xFF3D2A2A)
        "upcoming" -> Color(0xFF2A293D)
        else -> Color(0xFF3A3A3A)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${reminder.serviceType.replaceFirstChar { it.uppercase() }} service is ${reminder.status}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Last done on: ${reminder.lastDate} at ${reminder.lastMileage} km",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

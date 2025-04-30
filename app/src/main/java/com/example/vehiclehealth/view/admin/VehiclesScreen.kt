package com.example.vehiclehealth.view.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vehiclehealth.models.Vehicle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesScreen(
    vehicles: SnapshotStateList<Vehicle>,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    coroutineScope: CoroutineScope,
    onBack: () -> Unit
) {
    val actorId = auth.currentUser?.uid ?: "unknown"

    var searchQuery by remember { mutableStateOf("") }
    var filterBrand by remember { mutableStateOf("") }
    var filterModel by remember { mutableStateOf("") }
    var filterYear by remember { mutableStateOf("") }

    var showHistoryDialog   by remember { mutableStateOf(false) }
    var showVehicleDialog   by remember { mutableStateOf(false) }
    var selectedVehicle     by remember { mutableStateOf<Vehicle?>(null) }


    DisposableEffect(Unit) {
        val listener = db
            .collectionGroup("vehicles")
            .addSnapshotListener { snap, _ ->
                vehicles.clear()
                snap?.documents?.forEach { doc ->
                    doc.toObject(Vehicle::class.java)?.apply {
                        id = doc.id
                        vehicles.add(this)
                    }
                }
            }
        onDispose { listener.remove() }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search vehicles") },
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = filterBrand,
                onValueChange = { filterBrand = it },
                label = { Text("Brand") },
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = filterModel,
                onValueChange = { filterModel = it },
                label = { Text("Model") },
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = filterYear,
                onValueChange = { filterYear = it },
                label = { Text("Year") },
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(vehicles.filter { v ->
                v.brand.contains(filterBrand, ignoreCase = true) &&
                        v.model.contains(filterModel, ignoreCase = true) &&
                        (filterYear.isBlank() || v.year.toString().contains(filterYear)) &&
                        (searchQuery.isBlank() ||
                                v.vin.contains(searchQuery, ignoreCase = true) ||
                                v.registrationNumber.contains(searchQuery, ignoreCase = true))
            }) { v ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A293D))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${v.brand} ${v.model} (${v.year})", color = Color.White)
                        Text("VIN: ${v.vin}", color = Color.Gray)
                        Text("Reg: ${v.registrationNumber}", color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                selectedVehicle = v
                                showHistoryDialog = true
                            }) {
                                Text("Delete History", color = Color.Red)
                            }
                            TextButton(onClick = {
                                selectedVehicle = v
                                showVehicleDialog = true
                            }) {
                                Text("Delete Vehicle", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirm Delete Service History
    if (showHistoryDialog && selectedVehicle != null) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Confirm Delete History") },
            text = {
                Text(
                    "Are you sure you want to delete all service history for " +
                            "${selectedVehicle!!.brand} ${selectedVehicle!!.model} (${selectedVehicle!!.year})?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = selectedVehicle!!
                    showHistoryDialog = false
                    selectedVehicle = null
                    coroutineScope.launch {
                        db.collection("ServiceHistory")
                            .whereEqualTo("vehicleId", v.id)
                            .get().addOnSuccessListener { snap ->
                                snap.documents.forEach { it.reference.delete() }
                                db.collection("actionLogs").add(
                                    mapOf(
                                        "timestamp"   to System.currentTimeMillis(),
                                        "action"      to "DeleteServiceHistory",
                                        "vehicleId"   to v.id,
                                        "vehicleInfo" to "${v.brand} ${v.model} (${v.year})",
                                        "actorId"     to actorId
                                    )
                                )
                            }
                    }
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showHistoryDialog = false
                    selectedVehicle = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Delete Vehicle
    if (showVehicleDialog && selectedVehicle != null) {
        AlertDialog(
            onDismissRequest = { showVehicleDialog = false },
            title = { Text("Confirm Delete Vehicle") },
            text = {
                Text(
                    "Are you sure you want to delete vehicle " +
                            "${selectedVehicle!!.brand} ${selectedVehicle!!.model} (${selectedVehicle!!.year})?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = selectedVehicle!!
                    vehicles.remove(v)
                    showVehicleDialog = false
                    selectedVehicle = null
                    coroutineScope.launch {
                        db.collectionGroup("vehicles")
                            .whereEqualTo("id", v.id)
                            .get()
                            .addOnSuccessListener { snap ->
                                snap.documents.forEach { it.reference.delete() }
                                db.collection("actionLogs").add(
                                    mapOf(
                                        "timestamp"   to System.currentTimeMillis(),
                                        "action"      to "DeleteVehicle",
                                        "vehicleId"   to v.id,
                                        "vehicleInfo" to "${v.brand} ${v.model} (${v.year})",
                                        "actorId"     to actorId
                                    )
                                )
                            }
                    }
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showVehicleDialog = false
                    selectedVehicle = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

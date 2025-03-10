package com.example.vehiclehealth.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vehiclehealth.R
import com.example.vehiclehealth.models.Vehicle
import com.example.vehiclehealth.myvehiclestopnavbar.MyVehiclesTopNavBar
import com.example.vehiclehealth.novehicleadded.NoVehicleAdded
import com.example.vehiclehealth.services.VehicleService
import com.example.vehiclehealth.services.VinDecoderService
import com.example.vehiclehealth.ui.theme.VehicleHealthTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun MyVehiclesScreen(navController: NavController, vehicleService: VehicleService) {
    val firebaseAuth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val userId = firebaseAuth.currentUser?.uid ?: "defaultUserId"

    val vehicles = remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showVehicleDialog by remember { mutableStateOf(false) }

    // Function to check if a vehicle with the given VIN already exists
    fun vehicleExists(vin: String, onResult: (Boolean) -> Unit) {
        firestore.collection("users")
            .document(userId)
            .collection("vehicles")
            .whereEqualTo("vin", vin)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(!snapshot.isEmpty)
            }
            .addOnFailureListener {
                // In case of error, assume it does not exist (or handle accordingly)
                onResult(false)
            }
    }
    // Function to add the vehicle if it doesn't already exist
    fun addVehicleToDatabase(vehicle: Vehicle) {
        vehicleExists(vehicle.vin) { exists ->
            if (exists) {
                Toast.makeText(context, "Vehicle with VIN ${vehicle.vin} already exists", Toast.LENGTH_SHORT).show()
            } else {
                firestore.collection("users")
                    .document(userId)
                    .collection("vehicles")
                    .add(vehicle)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Vehicle added successfully", Toast.LENGTH_SHORT).show()
                        // Refresh list of vehicles by updating vehicles.value.
                        coroutineScope.launch {
                            vehicles.value = vehicleService.getUserVehicles()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error adding vehicle: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("MyVehiclesScreen", "Error adding vehicle", e)
                    }
            }
        }
    }

    //--------- Screen UI Display

    // Fetch vehicles when screen loads
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            vehicles.value = vehicleService.getUserVehicles()
        }
    }

    Column ( modifier = Modifier
        .fillMaxSize()) {
        MyVehiclesTopNavBar(
            topBarImg = painterResource(id = R.drawable.my_vehicles_top_nav_bar_settings),
            topBarText = "My Vehicles",
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1D2B))
                .padding(top = 50.dp, bottom = 30.dp, start = 25.dp)
                .height(29.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize() // This Box now occupies the remaining space
                .background(Color(0xFF1E1D2B))
        ) {
            // Main dashboard displaying user's vehicles
            if (vehicles.value.isEmpty()) {
                NoVehicleAdded(
                    noVehicleText = "You have no vehicles added",
                    noVehicleDescText = "Get started by adding a vehicle to begin \ntracking its performance and maintenance.",
                    noVehicleImg = painterResource(id = R.drawable.no_vehicle_added_car_picture),
                    modifier = Modifier.wrapContentSize()
                )
            } else {
                VehicleList(vehicles.value)
            }

            // Bottom Nav bar
            MainNavigationBar(navController = navController, onVinClick = {
                showVehicleDialog = true
            })
        }
    }

    if (showVehicleDialog) {
        VehicleInputDialog(
            onDismiss = { showVehicleDialog = false },
            onAddVehicle = { vehicle ->
                addVehicleToDatabase(vehicle)
                showVehicleDialog = false
            }
        )
    }
}


@Composable
fun VehicleList(vehicles: List<Vehicle>) {
    LazyColumn {
        items(vehicles) { vehicle ->
            VehicleCard(vehicle)
        }
    }
}

@Composable
fun VehicleCard(vehicle: Vehicle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A293D))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "${vehicle.brand} ${vehicle.model}", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(text = "Year: ${vehicle.year}", color = Color.Gray)
            Text(text = "VIN: ${vehicle.vin}", color = Color.Gray)
            Text(text = "Reg: ${vehicle.registrationNumber}", color = Color.Gray)
            Text(text = "Mileage: ${vehicle.mileage} km", color = Color.Gray)
        }
    }
}


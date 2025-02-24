package com.example.vehiclehealth.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.vehiclehealth.myvehiclestopbar.MyVehiclesTopBar
import com.example.vehiclehealth.novehicleadded.NoVehicleAdded
import com.example.vehiclehealth.services.VehicleService
import com.example.vehiclehealth.services.VinDecoderService
import com.example.vehiclehealth.ui.theme.VehicleHealthTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MyVehiclesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VehicleHealthTheme {
            }
        }
    }
}

@Composable
fun MyVehiclesScreen(navController: NavController, vehicleService: VehicleService) {
    val vehicles = remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    var showVehicleDialog by remember { mutableStateOf(false) }

    // Fetch vehicles when screen loads
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            vehicles.value = vehicleService.getUserVehicles()
        }
    }

    MyVehiclesTopBar(
        topBarImg = painterResource(id = R.drawable.my_vehicles_top_bar_settings),
        topBarText = "My Vehicles"
    )

    Box(
        modifier = Modifier
            .fillMaxSize() // Ensures it takes up the full screen
            .background(Color(0xFF1E1D2B)), // Screen background color
    ) {
        MainNavigationBar(navController = navController, onVinClick = {
            showVehicleDialog = true
        })

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
    }

    // Show the dialog when the state is true.
    if (showVehicleDialog) {
        VehicleInputDialog(
            onDismiss = { showVehicleDialog = false },
            onAddVehicle = { vehicle ->
                // Handle the added vehicle, e.g. store it in Firestore or update your UI.
                // Then hide the dialog.
                showVehicleDialog = false
            }
        )
    } //1HGCM82633A004352

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


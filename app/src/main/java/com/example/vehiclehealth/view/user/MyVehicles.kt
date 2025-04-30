package com.example.vehiclehealth.view.user

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vehiclehealth.R
import com.example.vehiclehealth.models.Vehicle
import com.example.vehiclehealth.myvehiclestopnavbar.MyVehiclesTopNavBar
import com.example.vehiclehealth.novehicleadded.NoVehicleAdded
import com.example.vehiclehealth.services.VehicleService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun MyVehiclesScreen(navController: NavController, vehicleService: VehicleService) {

    val vehicles = remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showVehicleDialog by remember { mutableStateOf(false) }

    //--------- Screen UI Display

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
            topBarSettingsBtn = { navController.navigate("settings") },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1D2B))
                .padding(top = 50.dp, bottom = 30.dp, start = 25.dp)
                .height(29.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1D2B))
        ) {
            // Main dashboard displaying users vehicles
            if (vehicles.value.isEmpty()) {
                NoVehicleAdded(
                    noVehicleText = "You have no vehicles added",
                    noVehicleDescText = "Get started by adding a vehicle to begin \ntracking its performance and maintenance.",
                    noVehicleImg = painterResource(id = R.drawable.no_vehicle_added_car_picture),
                    addVehiclebtn = { showVehicleDialog = true },
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(bottom = 150.dp)
                )
//            } else {
//                VehicleList(vehicles.value)
//            }
            } else {
                VehicleList(vehicles = vehicles.value, onVehicleClick = { selectedVehicle ->
                    navController.navigate("vehicleDetails/${selectedVehicle.id}")
                })
            }

            // Bottom Nav bar
            MainNavigationBar(navController = navController, onVinClick = {
                showVehicleDialog = true
            })
        }
    }

    if (showVehicleDialog) {
        VehicleInputDialog(
            onDismiss = {
                showVehicleDialog = false
            },
            onAddVehicle = { vehicle ->
                coroutineScope.launch {
                    try {
                        vehicles.value = vehicleService.getUserVehicles()

                        Toast.makeText(
                            context,
                            "Vehicle added successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Failed to add vehicle: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}

@Composable
fun VehicleList(vehicles: List<Vehicle>, onVehicleClick: (Vehicle) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 130.dp)
    ) {
        items(vehicles) { vehicle ->
            VehicleCard(vehicle = vehicle, onClick = { onVehicleClick(vehicle) })
        }
    }
}

@Composable
fun VehicleCard(
    vehicle: Vehicle,
    onClick: () -> Unit = {}
) {
    val brandIconResId = when (vehicle.brand.trim().lowercase()) {
        "bmw" -> R.drawable.icon_bmw
        "mercedes-benz" -> R.drawable.icon_mercedes
        "audi" -> R.drawable.icon_audi
        "toyota" -> R.drawable.icon_toyota
        "volkswagen" -> R.drawable.icon_volkswagen
        "volvo" -> R.drawable.icon_volvo

        else -> R.drawable.ic_car
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A293D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = brandIconResId),
                    contentDescription = "${vehicle.brand} logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${vehicle.brand} ${vehicle.model}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    if (vehicle.registrationNumber.isNotBlank()) {
                        Text(
                            text = vehicle.registrationNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = "${vehicle.year}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "${vehicle.mileage} km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Go to vehicle details",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}



package com.example.vehiclehealth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vehiclehealth.ui.theme.VehicleHealthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VehicleHealthTheme {
                MyVehiclesPage()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVehiclesPage() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "My Vehicles", color = Color.White) },
                modifier = Modifier.background(Color.DarkGray)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Action to add a vehicle */ },
                containerColor = Color.Blue,
                contentColor = Color.White
            ) {
                Text("+")
            }
        },
        bottomBar = {
            BottomNavigationBar()
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_car), // Use your car image here
                        contentDescription = "Car",
                        modifier = Modifier.size(200.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You have no vehicles added",
                        color = Color.LightGray,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Get started by adding a vehicle to begin tracking its performance and maintenance.",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { /* Action to add a vehicle */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                    ) {
                        Text(text = "Add Vehicle", color = Color.White)
                    }
                }
            }
        }
    )
}

@Composable
fun BottomNavigationBar() {
    NavigationBar(
        containerColor = Color.DarkGray,
        contentColor = Color.White
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { /* Action for Home */ },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home), // Replace with your icon
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* Action for Maps */ },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_maps), // Replace with your icon
                    contentDescription = "Maps"
                )
            },
            label = { Text("Maps") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* Action for Calendar */ },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendar), // Replace with your icon
                    contentDescription = "Calendar"
                )
            },
            label = { Text("Calendar") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* Action for Notifications */ },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_notifications), // Replace with your icon
                    contentDescription = "Notifications"
                )
            },
            label = { Text("Notifications") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MyVehiclesPagePreview() {
    VehicleHealthTheme {
        MyVehiclesPage()
    }
}
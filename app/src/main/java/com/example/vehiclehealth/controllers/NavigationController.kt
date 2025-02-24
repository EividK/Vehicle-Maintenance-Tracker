package com.example.vehiclehealth.controllers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vehiclehealth.services.AuthService
import com.example.vehiclehealth.services.VehicleService
import com.example.vehiclehealth.ui.theme.VehicleHealthTheme
import com.example.vehiclehealth.view.LoginScreen
import com.example.vehiclehealth.view.MyVehiclesScreen
import com.example.vehiclehealth.view.RegisterScreen

class NavigationController : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VehicleHealthTheme {
                // Creates and remembers navController
                val navController = rememberNavController()
                val vehicleService = VehicleService()

                NavHost(
                    navController = navController,
                    startDestination = if (AuthService().isUserLoggedIn()) "home" else "login"
                    //startDestination = "home"
                ) { // Navigation Menu
                    composable("home") { MyVehiclesScreen(navController, vehicleService) }
                    composable("login") { LoginScreen(navController, AuthService()) }
                    composable("register") { RegisterScreen(navController, AuthService()) }
                }
            }
        }
    }
}
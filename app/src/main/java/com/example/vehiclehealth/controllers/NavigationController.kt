package com.example.vehiclehealth.controllers

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vehiclehealth.models.Vehicle
import com.example.vehiclehealth.services.AuthService
import com.example.vehiclehealth.services.VehicleService
import com.example.vehiclehealth.ui.theme.VehicleHealthTheme
import com.example.vehiclehealth.view.LoginScreen
import com.example.vehiclehealth.view.user.MyVehiclesScreen
import com.example.vehiclehealth.view.RegisterScreen
import com.example.vehiclehealth.view.user.VehicleDetailsScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.vehiclehealth.security.AuthServiceWithRoles
import com.example.vehiclehealth.security.UserRole
import com.example.vehiclehealth.view.admin.AdminPanelScreen
import com.example.vehiclehealth.view.user.GarageLocatorScreen
import com.example.vehiclehealth.view.user.NotificationScreen
import com.example.vehiclehealth.view.user.SettingsScreen
import kotlinx.coroutines.launch
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.vehiclehealth.view.user.CalendarScreen
import com.example.vehiclehealth.view.user.UpdateMileageScreen

class NavigationController : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VehicleHealthTheme {
                val navController = rememberNavController()
                val vehicleService = VehicleService()
                val authRoles = remember { AuthServiceWithRoles() }
                val context = LocalContext.current

                val role by produceState(initialValue = UserRole.USER) {
                    value = authRoles.getCurrentUserRole()
                }

                val startDest = when {
                    !AuthService().isUserLoggedIn() -> "login"
                    else                              -> "home"
                }

                NavHost(navController = navController, startDestination = startDest)
                { // Navigation Menu

                    // ---- USER routes ----
                    composable("home") {
                        MyVehiclesScreen(navController, vehicleService)
                    }
                    composable("login") {
                        LoginScreen(navController, AuthService())
                    }
                    composable("register") {
                        RegisterScreen(navController, AuthService())
                    }
                    composable("garages") {
                        GarageLocatorScreen(onBack = { navController.popBackStack() })
                    }
                    composable("notifications") {
                        NotificationScreen (onBack = { navController.popBackStack() })
                    }

                    composable(
                        "updateMileage/{vehicleId}",
                        arguments = listOf(navArgument("vehicleId") {
                            type = NavType.StringType
                        })
                    ) { backStack ->
                        val vehicleId = backStack.arguments!!.getString("vehicleId")!!
                        UpdateMileageScreen(
                            nav = navController,
                            vehicleService = remember { VehicleService() }
                        )
                    }

                    composable("vehicleDetails/{vehicleId}") { backStackEntry ->
                        val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""
                        var vehicle by remember { mutableStateOf<Vehicle?>(null) }

                        LaunchedEffect(vehicleId) {
                            vehicle = vehicleService.getVehicleById(vehicleId)
                        }

                        val context = LocalContext.current

                        if (vehicle != null) {
                            VehicleDetailsScreen(
                                navController = navController,
                                vehicle = vehicle!!,
                                onBack = { navController.popBackStack() },
                                onAddService = { service ->
                                    Log.d("NavigationController", "Service record added for vehicle: ${service.vehicleId} with type: ${service.serviceType}")
                                    Toast.makeText(context, "Service record added successfully!", Toast.LENGTH_SHORT).show()
                                },

                            )
                        } else {
                            Text("Loading vehicle details...")
                        }
                    }

                    composable("calendar") { CalendarScreen(onBack = { navController.popBackStack() }) }

                    composable("settings") {
                        val scope = rememberCoroutineScope()
                        var notificationsEnabled by remember { mutableStateOf(true) }
                        val vehicles = remember { mutableStateListOf<Vehicle>() }
                        val authService = remember { AuthService() }
                        val vehicleService = remember { VehicleService() }
                        val ctx = LocalContext.current

                        LaunchedEffect(Unit) {
                            val list = vehicleService.getUserVehicles()
                            vehicles.clear()
                            vehicles.addAll(list)
                        }

                        SettingsScreen(
                            notificationsEnabled = notificationsEnabled,
                            onNotificationToggle = { notificationsEnabled = it },
                            onOpenCalendar       = { navController.navigate("calendar") },
                            onChangePassword     = { navController.navigate("changePassword") },
                            onAdminDashboard          = { navController.navigate("admin") },
                            vehicles             = vehicles,

                            onDeleteHistoryConfirmed = { vehicle ->
                                scope.launch {
                                    try {
                                        vehicleService.deleteServiceHistoryForVehicle(vehicle.id.toString())
                                        Toast.makeText(context, "History deleted for ${vehicle.brand} ${vehicle.model}", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to delete history: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },

                            onRemoveVehicleConfirmed = { vehicle ->
                                scope.launch {
                                    try {
                                        vehicleService.deleteVehicle(vehicle.id.toString())
                                        Toast.makeText(
                                            context,
                                            "Removed ${vehicle.brand} ${vehicle.model} from your garage.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Failed to remove vehicle: ${e.localizedMessage}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },

                            onDeleteAccountConfirmed = { enteredPassword: String ->
                                authService.deleteCurrentUser(enteredPassword) { success, err ->
                                    if (success) {
                                        navController.navigate("login") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(ctx, err ?: "Unknown error", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },

                            onLogout = {
                                authService.logoutUser()
                                navController.navigate("login") { popUpTo("home") { inclusive = true } }
                            },

                            onBack = { navController.popBackStack() }
                        )
                    }

                    // ---- ADMIN routes ----
                    composable("admin") {
                        if (role != UserRole.ADMIN) {
                            // unauthorized
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Access denied", color = Color.Red)
                            }
                        } else {
                            AdminPanelScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
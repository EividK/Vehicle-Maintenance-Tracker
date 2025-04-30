package com.example.vehiclehealth.view.user

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.vehiclehealth.models.Vehicle
import com.example.vehiclehealth.security.AuthServiceWithRoles
import com.example.vehiclehealth.security.UserRole
import com.example.vehiclehealth.services.AuthService
import com.example.vehiclehealth.services.VehicleService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    notificationsEnabled: Boolean,
    onNotificationToggle: (Boolean) -> Unit,
    onOpenCalendar: () -> Unit,
    onChangePassword: () -> Unit,
    vehicles: List<Vehicle>,
    onDeleteAccountConfirmed: (String) -> Unit,
    onDeleteHistoryConfirmed: (Vehicle) -> Unit,
    onRemoveVehicleConfirmed: (Vehicle) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onAdminDashboard: () -> Unit
) {
    val context = LocalContext.current
    val authService = remember { AuthService() }
    val vehicleService = remember { VehicleService() }
    val scope = rememberCoroutineScope()

    val authWithRoles = remember { AuthServiceWithRoles() }
    val isAdmin by produceState(initialValue = false) {
        value = (authWithRoles.getCurrentUserRole() == UserRole.ADMIN)
    }

    // Dialog state
    var showUpdateDetailsDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }

    var showVehiclePicker by remember { mutableStateOf(false) }
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var showDeleteHistoryConfirm by remember { mutableStateOf(false) }
    var showHistorySuccessDialog by remember { mutableStateOf(false) }
    var showRemoveVehicleDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF1E1D2B),
        topBar = {
            SmallTopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF2A293D))
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1D2B))
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Profile", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                }
                Divider(color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showUpdateDetailsDialog = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Update",
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Update Details",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                if (isAdmin) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onAdminDashboard() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "Admin",
                            tint = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Admin Dashboard",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Column {
                Text(
                    "Notification Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Enable Service Reminders",
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onNotificationToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Cyan)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text("Schedule", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenCalendar, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Calendar", color = Color.White)
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Vehicle Management",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showVehiclePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.Red),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete History")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showRemoveVehicleDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.Red),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("Remove Vehicle from Garage")
                }
                Spacer(Modifier.height(150.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onLogout, modifier = Modifier.height(48.dp)) {
                        Text("Logout", color = Color.White)
                    }
                    Text(
                        "Privacy Policy",
                        color = Color.Gray,
                        modifier = Modifier
                            .clickable { showPrivacyPolicyDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (showPrivacyPolicyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyPolicyDialog = false },
                containerColor = Color(0xFF2A293D),
                title = { Text("Privacy Policy", color = Color.White) },
                text = {
                    Box(
                        Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                "What we collect",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            BulletItem("Vehicle details, make, model, registration and VIN number.")
                            BulletItem("Service history logs, dates, actions taken.")
                            BulletItem("User email, for account ID and login.")

                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Why we use it",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            BulletItem("Provide personalised service reminders.")
                            BulletItem("Let you overview and manage your vehicle history.")
                            BulletItem("Authenticate and secure your account.")

                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Where it’s stored",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "All data is encrypted in transit (HTTPS) and at rest in our secure cloud database. " +
                                        "We store service history for as long as your account is active.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )

                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Third-Party Services",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            BulletItem("Firebase Authentication (email sign-in).")
                            BulletItem("Cloud Firestore (data storage).")
                            BulletItem("Calendar API (if you sync reminders).")

                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Your rights",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            BulletItem("Delete your account at any time (Settings → Delete Account).")
                            BulletItem("Export your service history at anytime.")
                            BulletItem("Contact vehiclehealth@support.com for any enquiries.")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                        Text("Close", color = Color.Cyan)
                    }
                }
            )
        }

        if (showUpdateDetailsDialog) {
            var newPassword by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showUpdateDetailsDialog = false },
                containerColor = Color(0xFF2A293D),
                title = { Text("Update Details", color = Color.White) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = authService.getCurrentUser()?.email ?: "",
                            onValueChange = {},
                            label = { Text("Email", color = Color.Gray) },
                            readOnly = true,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = Color(0xFF3A3A3A),
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.Gray,
                                cursorColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password", color = Color.Gray) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions.Default,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = Color(0xFF3A3A3A),
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.Gray,
                                cursorColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            authService.updatePassword(newPassword) { success, msg ->
                                Toast.makeText(
                                    context,
                                    if (success) "Password updated" else msg ?: "Error",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Change Password", color = Color.White)
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                showUpdateDetailsDialog = false
                                showDeleteAccountDialog = true
                            },
                            border = BorderStroke(1.dp, Color.Red),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete Account")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showUpdateDetailsDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }

        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAccountDialog = false },
                containerColor  = Color(0xFF2A293D),
                title           = { Text("Confirm Deletion", color = Color.White) },
                text            = {
                    Column {
                        Text(
                            "This will permanently delete your account and all data.\n" +
                                    "Please enter your password to confirm:",
                            color = Color.White
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value                  = deletePassword,
                            onValueChange          = { deletePassword = it },
                            label                  = { Text("Password", color = Color.Gray) },
                            singleLine             = true,
                            visualTransformation   = PasswordVisualTransformation(),
                            keyboardOptions        = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier               = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor   = Color.White,
                                unfocusedBorderColor = Color.Gray,
                                cursorColor          = Color.White,
                                focusedLabelColor    = Color.White,
                                unfocusedLabelColor  = Color.Gray
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (deletePassword.isBlank()) {
                            Toast.makeText(
                                context,
                                "Please enter your password",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            showDeleteAccountDialog = false
                            onDeleteAccountConfirmed(deletePassword)
                        }
                    }) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }

        if (showVehiclePicker) {
            var expandedVehiclePicker by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = { showVehiclePicker = false },
                containerColor = Color(0xFF2A293D),
                title = { Text("Select Vehicle", color = Color.White) },
                text = {
                    ExposedDropdownMenuBox(
                        expanded = expandedVehiclePicker,
                        onExpandedChange = { expandedVehiclePicker = !expandedVehiclePicker }
                    ) {
                        OutlinedTextField(
                            value = selectedVehicle?.let { "${it.brand} ${it.model} - ${it.registrationNumber}" }
                                ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Vehicle", color = Color.White) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expandedVehiclePicker
                                )
                            },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = Color(0xFF3A3A3A),
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color.White,
                                unfocusedLabelColor = Color.Gray,
                                cursorColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedVehiclePicker,
                            onDismissRequest = { expandedVehiclePicker = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2A293D))
                        ) {
                            vehicles.forEach { vehicle ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${vehicle.brand} ${vehicle.model} - ${vehicle.registrationNumber}",
                                            color = Color.White
                                        )
                                    },
                                    onClick = {
                                        selectedVehicle = vehicle
                                        expandedVehiclePicker = false
                                        showVehiclePicker = false
                                        showDeleteHistoryConfirm = true
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showVehiclePicker = false }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }

        if (showDeleteHistoryConfirm && selectedVehicle != null) {
            AlertDialog(
                onDismissRequest = { showDeleteHistoryConfirm = false },
                title = { Text("Confirm Delete History") },
                text = {
                    Text("Delete service history for ${selectedVehicle!!.brand} ${selectedVehicle!!.model}? This cannot be undone.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteHistoryConfirm = false
                        onDeleteHistoryConfirmed(selectedVehicle!!)
                        showHistorySuccessDialog = true
                    }) {
                        Text("Delete", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteHistoryConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showHistorySuccessDialog && selectedVehicle != null) {
            AlertDialog(
                onDismissRequest = { showHistorySuccessDialog = false },
                title = { Text("History Deleted") },
                text = {
                    Text("Service history for ${selectedVehicle!!.brand} ${selectedVehicle!!.model} was successfully deleted.\n\nRemove this vehicle from your garage?")
                },
                confirmButton = {
                    TextButton(onClick = {
                        showHistorySuccessDialog = false
                        onRemoveVehicleConfirmed(selectedVehicle!!)
                    }) {
                        Text("Remove Vehicle", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showHistorySuccessDialog = false }) {
                        Text("Keep Vehicle")
                    }
                }
            )
        }

        if (showRemoveVehicleDialog) {
            var expandedRemovePicker by remember { mutableStateOf(false) }
            var vehicleToRemove by remember { mutableStateOf<Vehicle?>(null) }
            AlertDialog(
                onDismissRequest = { showRemoveVehicleDialog = false },
                containerColor = Color(0xFF2A293D),
                title = { Text("Remove Vehicle", color = Color.White) },
                text = {
                    Column {
                        ExposedDropdownMenuBox(
                            expanded = expandedRemovePicker,
                            onExpandedChange = { expandedRemovePicker = !expandedRemovePicker }
                        ) {
                            OutlinedTextField(
                                value = vehicleToRemove?.let { "${it.brand} ${it.model} - ${it.registrationNumber}" }
                                    ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Vehicle", color = Color.White) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expandedRemovePicker
                                    )
                                },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    containerColor = Color(0xFF3A3A3A),
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color.Gray,
                                    cursorColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedRemovePicker,
                                onDismissRequest = { expandedRemovePicker = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2A293D))
                            ) {
                                vehicles.forEach { v ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${v.brand} ${v.model} - ${v.registrationNumber}",
                                                color = Color.White
                                            )
                                        },
                                        onClick = {
                                            vehicleToRemove = v
                                            expandedRemovePicker = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showRemoveVehicleDialog = false
                            vehicleToRemove?.let { onRemoveVehicleConfirmed(it) }
                        }
                    ) {
                        Text("Remove", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveVehicleDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
fun BulletItem(text: String) {
    Row(modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) {
        Text("•", style = MaterialTheme.typography.bodySmall, color = Color.White)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}

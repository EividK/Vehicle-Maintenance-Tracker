package com.example.vehiclehealth.view.user

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vehiclehealth.components.MileageTextField
import com.example.vehiclehealth.models.Vehicle
import com.example.vehiclehealth.services.VehicleService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateMileageScreen(
    nav: NavController,
    vehicleService: VehicleService
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val vehicles by produceState<List<Vehicle>>(emptyList()) {
        value = vehicleService.getUserVehicles()
    }

    var selected by remember { mutableStateOf<Vehicle?>(null) }
    var newMileage by remember { mutableStateOf(TextFieldValue("")) }
    var expanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    var showLowerConfirm by remember { mutableStateOf(false) }
    var pendingMileage by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = Color(0xFF1E1D2B),
        topBar = {
            SmallTopAppBar(
                title = { Text("Update Mileage", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color(0xFF2A293D))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1D2B))
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Keep your mileage up to date to get accurate reminders!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selected
                        ?.let { "${it.brand} ${it.model} – ${it.registrationNumber}" }
                        ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Vehicle", color = Color.Gray) },
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = Color(0xFF3A3A3A),
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color.White
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3A3A3A))
                ) {
                    vehicles.forEach { v ->
                        DropdownMenuItem(
                            text = { Text("${v.brand} ${v.model}", color = Color.White) },
                            onClick = {
                                selected = v
                                expanded = false
                            }
                        )
                    }
                }
            }

            LaunchedEffect(expanded) {
                if (expanded) focusRequester.requestFocus()
            }

            MileageTextField(
                value = newMileage,
                onValueChange = { newMileage = it },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    val m = newMileage.text.replace(",", "").toIntOrNull()
                    if (selected == null || m == null) {
                        Toast
                            .makeText(context, "Select a vehicle and enter valid mileage.", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        if (m < selected!!.mileage) {
                            pendingMileage = m
                            showLowerConfirm = true
                        } else {
                            scope.launch {
                                vehicleService.updateMileage(selected!!.id, m)
                                Toast.makeText(context, "Mileage updated!", Toast.LENGTH_SHORT).show()
                                nav.popBackStack()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Save", color = Color.White)
            }
        }
    }

    if (showLowerConfirm && selected != null) {
        AlertDialog(
            onDismissRequest = { showLowerConfirm = false },
            title = { Text("Invalid Mileage.", color = Color.White) },
            text = {
                Text(
                    "You entered ${pendingMileage} km, which is lower than the current " +
                            "mileage (${selected!!.mileage} km).\n\n" +
                            "Mileage was not saved. Please contact support for assistance.",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(onClick = { showLowerConfirm = false }) {
                    Text("OK", color = Color.Cyan)
                }
            },
            containerColor = Color(0xFF2A293D)
        )
    }
}
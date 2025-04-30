package com.example.vehiclehealth.view.user

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.vehiclehealth.models.ServiceHistory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceDialog(
    vehicleId: String,
    onDismiss: () -> Unit,
    onServiceAdded: (ServiceHistory) -> Unit
) {
    val context = LocalContext.current

    var date by remember { mutableStateOf("") }
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            date = formatDate(dayOfMonth, month, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.maxDate = System.currentTimeMillis()
    }

    val serviceTypes = listOf(
        "Oil change",
        "Air Filter",
        "Battery",
        "Spark plugs",
        "Engine oil",
        "Brake fluid",
        "Brake inspection",
        "Fuel Filter",
        "Timing belt",
        "Full service",
        "Coolant and brake fluid top up",
        "Fan belt",
        "Tire pressure",
        "Manufacturer service",
        "Car inspection",
        "Exhaust system",
        "Steering and suspension",
        "Wheel alignment",
        "Pollen filter change"
    )

    var selectedServiceType by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    var description by remember { mutableStateOf("") }
    var mileageString by remember { mutableStateOf("") }
    var costString by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2A2A2A),
        title = { Text("Add Service Record", color = Color.White) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    label = { Text("Date (DD-MM-YYYY)", color = Color.White) },
                    singleLine = true,
                    readOnly = true,
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select date",
                                tint = Color.White
                            )
                        }
                    },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedServiceType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Service Type", color = Color.White) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                        },
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        colors = textFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF3A3A3A))
                    ) {
                        serviceTypes.forEach { service ->
                            DropdownMenuItem(
                                text = { Text(service, color = Color.White) },
                                onClick = {
                                    selectedServiceType = service
                                    expanded = false

                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description", color = Color.White) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    singleLine = false,
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = mileageString,
                    onValueChange = { mileageString = it },
                    label = { Text("Mileage (km)", color = Color.White) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    singleLine = true,
                    colors = textFieldColors(),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costString,
                    onValueChange = { costString = it },
                    label = { Text("Cost", color = Color.White) },
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    singleLine = true,
                    colors = textFieldColors(),
                    leadingIcon = { Text("€", color = Color.White) },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (date.isBlank()) {
                    Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (selectedServiceType.isBlank()) {
                    Toast.makeText(context, "Please select a service type", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (mileageString.isBlank()) {
                    Toast.makeText(context, "Please enter mileage", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val costValue = costString.replace(",", ".").toDoubleOrNull() ?: 0.0
                val mileageValue = mileageString.toIntOrNull() ?: 0

                val serviceCol = FirebaseFirestore.getInstance()
                    .collection("ServiceHistory")
                val newDoc     = serviceCol.document()
                val newId      = newDoc.id

                val newRecord = ServiceHistory(
                    id = newId,
                    vehicleId = vehicleId,
                    date = date,
                    serviceType = selectedServiceType,
                    description = description,
                    cost = costValue,
                    mileage = mileageValue
                )

                newDoc.set(newRecord)
                    .addOnSuccessListener {
                        Log.d("AddServiceDialog", "Service record added with ID: $newId")
                        onServiceAdded(newRecord)
                        onDismiss()
                    }
                    .addOnFailureListener { e ->
                        Log.e("AddServiceDialog", "Error adding service record", e)
                        Toast.makeText(context, "Failed to add service: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

            }) {
                Text("Add Service")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(day: Int, month: Int, year: Int): String {
    val dayString = day.toString().padStart(2, '0')
    val monthString = (month + 1).toString().padStart(2, '0')
    return "$dayString-$monthString-$year"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun textFieldColors(): TextFieldColors {
    return TextFieldDefaults.outlinedTextFieldColors(
        containerColor = Color(0xFF3A3A3A),
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.Gray,
        cursorColor = Color.White
    )
}

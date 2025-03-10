package com.example.vehiclehealth.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.vehiclehealth.ui.components.extractRegistrationNumberParts
import com.example.vehiclehealth.ui.components.processRegistrationInput
import com.example.vehiclehealth.ui.components.validateRegistrationNumber

@Composable
fun RegistrationNumberTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    var isValid by remember { mutableStateOf(true) }

    TextField(
        value = value,
        onValueChange = { newValue ->
            // Process the new input.
            val processedValue = processRegistrationInput(value, newValue)
            onValueChange(processedValue)
            // Remove dashes before extraction.
            val parts = extractRegistrationNumberParts(processedValue.text.replace("-", ""))
            isValid = validateRegistrationNumber(parts)
        },
        label = { Text("Registration Number") },
        modifier = Modifier.fillMaxWidth(),
        isError = !isValid
    )
}

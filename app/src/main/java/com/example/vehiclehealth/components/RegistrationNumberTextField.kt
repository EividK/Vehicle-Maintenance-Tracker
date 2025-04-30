package com.example.vehiclehealth.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.example.vehiclehealth.ui.components.extractRegistrationNumberParts
import com.example.vehiclehealth.ui.components.processRegistrationInput
import com.example.vehiclehealth.ui.components.validateRegistrationNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationNumberTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    var isValid by remember { mutableStateOf(true) }

    TextField(
        value = value,
        onValueChange = { newValue ->
            val processedValue = processRegistrationInput(value, newValue)
            onValueChange(processedValue)

            val parts = extractRegistrationNumberParts(processedValue.text.replace("-", ""))

            isValid = validateRegistrationNumber(parts)
        },
        label = { Text("Registration Number") },
        singleLine = true,
        isError = !isValid,
        textStyle = TextStyle(color = Color.White),
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor        = Color(0xFF3A3A3A),
            focusedBorderColor    = Color.White,
            unfocusedBorderColor  = Color.Gray,
            focusedLabelColor     = Color.White,
            unfocusedLabelColor   = Color.Gray,
            cursorColor           = Color.White,
            errorBorderColor      = Color.Red,
            errorLabelColor       = Color.Red
        )
    )
}

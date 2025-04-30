package com.example.vehiclehealth.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MileageTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = { newValue ->
            val digits = newValue.text.replace(",", "")
            if (digits.isEmpty()) {
                onValueChange(newValue.copy(text = "", selection = TextRange(0)))
            } else {
                val parsedNumber = try {
                    digits.toLong()
                } catch (e: NumberFormatException) {
                    0L
                }
                val maxValue = 99_999_999L
                val clampedNumber = if (parsedNumber > maxValue) maxValue else parsedNumber
                // Format with commas
                val formatted = NumberFormat.getNumberInstance(Locale.US).format(clampedNumber)

                // Adjust cursor position
                val diff = formatted.length - newValue.text.length
                val newCursor = if (newValue.selection.end == newValue.text.length) {
                    formatted.length
                } else {
                    newValue.selection.end + diff
                }

                onValueChange(
                    newValue.copy(
                        text = formatted,
                        selection = TextRange(newCursor.coerceAtLeast(0).coerceAtMost(formatted.length))
                    )
                )
            }
        },
        label = { Text("Mileage") },
        singleLine = true,
        textStyle = TextStyle(color = Color.White),
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor        = Color(0xFF3A3A3A),
            focusedBorderColor    = Color.White,
            unfocusedBorderColor  = Color.Gray,
            focusedLabelColor     = Color.White,
            unfocusedLabelColor   = Color.Gray,
            cursorColor           = Color.White
        )
    )
}

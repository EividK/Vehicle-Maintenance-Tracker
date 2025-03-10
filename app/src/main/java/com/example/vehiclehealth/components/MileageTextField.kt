package com.example.vehiclehealth.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.Modifier
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MileageTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    TextField(
        value = value,
        onValueChange = { newValue ->
            // Remove commas so we work with a pure numeric string.
            val digits = newValue.text.replace(",", "")
            if (digits.isEmpty()) {
                onValueChange(newValue.copy(text = "", selection = TextRange(0)))
            } else {
                // Parse the digits into a number.
                val parsedNumber = try {
                    digits.toLong()
                } catch (e: NumberFormatException) {
                    0L
                }
                // Clamp the value to a maximum of 99,999,999.
                val maxValue = 99_999_999L
                val clampedNumber = if (parsedNumber > maxValue) maxValue else parsedNumber

                // Format the clamped value with commas.
                val formatted = NumberFormat.getNumberInstance(Locale.US).format(clampedNumber)

                // Adjust the cursor position.
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
        modifier = Modifier.fillMaxWidth()
    )
}

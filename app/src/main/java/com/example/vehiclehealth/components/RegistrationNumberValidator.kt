package com.example.vehiclehealth.ui.components

/**
 * Irish Registration Number Plate Validator
 */

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

private val validCountyCodes = setOf(
    "C", "CE", "CN", "CW",
    "D", "DL",
    "G",
    "KE", "KK", "KY",
    "L", "LD", "LH", "LM", "LS",
    "MH", "MN", "MO",
    "OY",
    "RN",
    "SO",
    "T",
    "W", "WH", "WX", "WW"
)

private val allowedOneLetterCountyCodes = setOf("C", "D", "G", "L", "T", "W")

data class RegistrationNumberParts(
    val yearPart: String,
    val countyPart: String,
    val sequencePart: String
)

fun extractRegistrationNumberParts(raw: String): RegistrationNumberParts {
    val firstTwo = raw.take(2)
    val yearPart = if (firstTwo.length < 2) {
        firstTwo
    } else if ((firstTwo.toIntOrNull() ?: 0) < 13) {
        firstTwo
    } else {
        raw.take(3)
    }
    val remainderAfterYear = raw.drop(yearPart.length)
    val countyPart = remainderAfterYear.takeWhile { it.isLetter() }.take(2).uppercase()
    val remainderAfterCounty = remainderAfterYear.drop(countyPart.length)
    val sequencePart = remainderAfterCounty.takeWhile { it.isDigit() }.take(6)
    return RegistrationNumberParts(yearPart, countyPart, sequencePart)
}

fun formatRegistrationNumber(parts: RegistrationNumberParts): String {
    var formatted = parts.yearPart
    if (parts.yearPart.isNotEmpty() && parts.countyPart.isNotEmpty()) {
        formatted += "-"
    }
    formatted += parts.countyPart
    if (parts.countyPart.isNotEmpty() && parts.sequencePart.isNotEmpty()) {
        formatted += "-"
    }
    formatted += parts.sequencePart
    return formatted
}

fun validateRegistrationNumber(parts: RegistrationNumberParts): Boolean {
    val validYear = when (parts.yearPart.length) {
        2 -> true
        3 -> parts.yearPart.last() == '1' || parts.yearPart.last() == '2'
        else -> false
    }
    val validCounty = when (parts.countyPart.length) {
        1 -> parts.countyPart in allowedOneLetterCountyCodes
        2 -> parts.countyPart in validCountyCodes
        else -> false
    }
    val validSequence = parts.sequencePart.isNotEmpty()
    return validYear && validCounty && validSequence
}

fun processRegistrationInput(
    oldValue: TextFieldValue,
    newValue: TextFieldValue
): TextFieldValue {
    // Remove all dashes
    var rawInput = newValue.text.replace("-", "")
    // Reject if the first character is not a digit
    if (rawInput.isNotEmpty() && !rawInput.first().isDigit()) {
        return oldValue
    }
    val leadingDigits = rawInput.takeWhile { it.isDigit() }
    // Determine allowed year length if no letter has been typed yet
    if (rawInput.drop(leadingDigits.length).isEmpty()) {
        if (leadingDigits.length >= 2) {
            val allowedYearLength = if ((leadingDigits.take(2).toIntOrNull() ?: 0) < 13) 2 else 3

            if (leadingDigits.length > allowedYearLength) {
                rawInput = leadingDigits.take(allowedYearLength)
            }
        }
    } else {
        // If there is a letter in the raw input, ensure that the letter comes only after the allowed year digits
        val allowedYearLength = if (leadingDigits.length >= 2) {
            if ((leadingDigits.take(2).toIntOrNull() ?: 0) < 13) 2 else 3
        } else {
            leadingDigits.length
        }
        if (leadingDigits.length < allowedYearLength) {
            return oldValue
        }
    }
    val parts = extractRegistrationNumberParts(rawInput)
    if (parts.countyPart.isNotEmpty()) {
        if (parts.countyPart.length == 1) {
            if (!validCountyCodes.any { it.startsWith(parts.countyPart) }) {
                return oldValue
            }
        } else if (parts.countyPart.length == 2) {
            if (parts.countyPart !in validCountyCodes) {
                return oldValue
            }
        }
    }
    val formatted = formatRegistrationNumber(parts)
    val diff = formatted.length - newValue.text.length
    val newCursor = if (newValue.selection.end == newValue.text.length) {
        formatted.length
    } else {
        newValue.selection.end + diff
    }
    return newValue.copy(
        text = formatted,
        selection = TextRange(newCursor.coerceAtLeast(0).coerceAtMost(formatted.length))
    )
}

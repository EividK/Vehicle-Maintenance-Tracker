package com.example.vehiclehealth.ui.components

/**
 *
 * Irish Registration Number Plate Validator
 *
 */

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

// Allowed county codes based on current index mark codes.
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

// For final validation, allowed one-letter county codes.
private val allowedOneLetterCountyCodes = setOf("C", "D", "G", "L", "T", "W")

/**
 * Data class representing the three parts of an Irish registration plate:
 *
 * - yearPart: For old format, exactly 2 digits; for new format, exactly 3 digits (with the third being 1 or 2).
 * - countyPart: 1–2 letters.
 * - sequencePart: 1 to 6 digits.
 */
data class RegistrationNumberParts(
    val yearPart: String,
    val countyPart: String,
    val sequencePart: String
)

/**
 * Extracts the three parts from the raw input (with dashes removed).
 *
 * 1. The year part is determined from the first characters:
 *    - If there are fewer than 2 digits, use what’s available.
 *    - Otherwise, if the first two digits (as a number) are less than 13, use exactly 2 digits (old format).
 *    - Otherwise, use exactly 3 digits (new format).
 * 2. The county part is the next contiguous letters (up to 2), converted to uppercase.
 * 3. The sequence part is the following contiguous digits (up to 6).
 */
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

/**
 * Formats the registration number parts into a string with dashes.
 *
 * A dash is inserted between the year and county parts (if both are non‑empty)
 * and between the county and sequence parts (if both are non‑empty).
 */
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

/**
 * Validates the registration number parts.
 *
 * - Year part:
 *   • 2 digits (old format): any two digits.
 *   • 3 digits (new format): the third digit must be '1' or '2'.
 * - County part:
 *   • If 1 letter, it must be one of the allowed one-letter codes.
 *   • If 2 letters, it must exactly match one of the allowed codes.
 * - Sequence part must have at least 1 digit.
 */
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

/**
 * Processes the registration input from a TextFieldValue.
 *
 * 1. Removes all dashes from the new input (so that dashes are treated as normal characters).
 * 2. Immediately rejects the new input if the first character isn't a digit.
 * 3. If no letters have been typed yet (i.e. the input is only digits), we "clamp" the year portion:
 *    - Determine the allowed year length:
 *         • If the first two digits represent a number less than 13, allowed length = 2 (old format).
 *         • Otherwise, allowed length = 3 (new format) and the third digit must be '1' or '2'.
 *    - If the raw input contains any letter before reaching the allowed year length, reject the new input.
 * 4. Extracts the registration parts.
 * 5. Checks the county part:
 *    - If 1 letter, it is allowed if it’s a valid prefix for any allowed code.
 *    - If 2 letters, it must exactly match one of the allowed codes.
 *    Otherwise, the new input is rejected (the old value is returned).
 * 6. Rebuilds the formatted string (inserting dashes automatically) and adjusts the cursor position.
 */
fun processRegistrationInput(
    oldValue: TextFieldValue,
    newValue: TextFieldValue
): TextFieldValue {
    // Remove all dashes.
    var rawInput = newValue.text.replace("-", "")
    // Reject if the first character is not a digit.
    if (rawInput.isNotEmpty() && !rawInput.first().isDigit()) {
        return oldValue
    }
    // Determine the leading digits.
    val leadingDigits = rawInput.takeWhile { it.isDigit() }
    // Determine allowed year length if no letter has been typed yet.
    if (rawInput.drop(leadingDigits.length).isEmpty()) {
        if (leadingDigits.length >= 2) {
            val allowedYearLength = if ((leadingDigits.take(2).toIntOrNull() ?: 0) < 13) 2 else 3
            // If extra digits exist beyond the allowed length, trim them.
            if (leadingDigits.length > allowedYearLength) {
                rawInput = leadingDigits.take(allowedYearLength)
            }
        }
    } else {
        // If there is a letter in the raw input, ensure that the letter comes only after the allowed year digits.
        val allowedYearLength = if (leadingDigits.length >= 2) {
            if ((leadingDigits.take(2).toIntOrNull() ?: 0) < 13) 2 else 3
        } else {
            leadingDigits.length
        }
        if (leadingDigits.length < allowedYearLength) {
            // A letter was typed before the year portion is complete: reject the input.
            return oldValue
        }
    }
    val parts = extractRegistrationNumberParts(rawInput)
    // Validate county part.
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

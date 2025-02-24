package com.example.vehiclehealth.utils

// Check Digit Validation: Validate the VIN’s check digit before decoding it
fun isValidVin(vin: String): Boolean {
    if (vin.length != 17) return false

    // Transliteration table: letters to numbers.
    val transliteration = mapOf(
        'A' to 1, 'B' to 2, 'C' to 3, 'D' to 4, 'E' to 5, 'F' to 6, 'G' to 7,
        'H' to 8, 'J' to 1, 'K' to 2, 'L' to 3, 'M' to 4, 'N' to 5, 'P' to 7,
        'R' to 9, 'S' to 2, 'T' to 3, 'U' to 4, 'V' to 5, 'W' to 6, 'X' to 7,
        'Y' to 8, 'Z' to 9
    )

    // Weight factors for each position in the VIN.
    val weights = intArrayOf(8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2)

    // Compute the weighted sum.
    var sum = 0
    for (i in vin.indices) {
        val c = vin[i]
        val value = when {
            c.isDigit() -> c.toString().toInt()
            transliteration.containsKey(c) -> transliteration[c]!!
            else -> return false // invalid character
        }
        sum += value * weights[i]
    }

    // Compute remainder mod 11.
    val remainder = sum % 11
    // If remainder is 10, check digit should be 'X'; otherwise it should be the digit.
    val checkDigitComputed = if (remainder == 10) 'X' else remainder.toString()[0]

    // The actual check digit is at position 9 (index 8)
    return vin[8] == checkDigitComputed
}
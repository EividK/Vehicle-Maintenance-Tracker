package com.example.vehiclehealth.models

data class Vehicle(
    var id: String = "",
    val brand: String,
    val model: String,
    val engineType : String,
    val bodyStyle : String,
    val trimLevel : String,
    val transmissionType : String,
    val year: Int,
    val vin: String,
    val registrationNumber: String = "",
    val mileage: Int = 0
) {
    constructor() : this(
        id = "",
        brand = "",
        model = "",
        year = 0,
        vin = "",
        registrationNumber = "",
        mileage = 0,
        engineType = "",
        bodyStyle = "",
        trimLevel = "",
        transmissionType = ""
    )
}
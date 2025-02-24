package com.example.vehiclehealth.models

data class Vehicle(
    val id: Int = 0,
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
)
package com.example.vehiclehealth.models

data class ServiceHistory(
    var id: String? = "",
    val vehicleId: String = "",
    val date: String = "",
    val serviceType: String = "",
    val description: String = "",
    val cost: Double = 0.0,
    var mileage: Int = 0
)

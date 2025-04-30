package com.example.vehiclehealth.models

data class ServiceReminder(
    val serviceType: String,
    val lastDate: String,
    val lastMileage: Int,
    val status: String
)
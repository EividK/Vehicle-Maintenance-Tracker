package com.example.vehiclehealth.models

data class NotificationRecord(
    val userId: String = "",
    val serviceType: String = "",
    val status: String = "",
    val timestamp: Long = 0L,
    val read: Boolean = false
)

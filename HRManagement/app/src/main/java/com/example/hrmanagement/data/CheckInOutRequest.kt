package com.example.hrmanagement.data

data class CheckInOutRequest(
    val action: String,   // "CHECK_IN" or "CHECK_OUT"
    val qr: String        // QR always required for attendance
)

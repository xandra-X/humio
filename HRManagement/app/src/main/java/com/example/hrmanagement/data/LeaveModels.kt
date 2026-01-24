package com.example.hrmanagement.data

// ---------------- UI MODELS ----------------

data class LeaveBalanceItem(
    val daysText: String,
    val title: String,
    val description: String,
    val iconRes: Int
)

data class LeaveHistoryItem(
    val type: String,
    val daysText: String,
    val note: String,
    val status: String
)

// ---------------- API MODELS ----------------

// One leave type block returned from backend
data class LeaveBalanceBlock(
    val total: Int,
    val used: Int,
    val remaining: Int
)


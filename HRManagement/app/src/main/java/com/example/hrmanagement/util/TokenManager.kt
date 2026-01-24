package com.example.hrmanagement.util

import android.content.Context
import android.content.SharedPreferences

object TokenManager {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs.edit().putString("jwt", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("jwt", null)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

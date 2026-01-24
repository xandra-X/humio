package com.example.hrmanagement.util

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Save JWT token after login.
     */
    fun saveAuthToken(token: String) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
    }

    /**
     * Fetch stored JWT token.
     * Returns null if token does not exist.
     */
    fun fetchAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    /**
     * Save numeric user id (from login response or profile).
     */
    fun saveUserId(userId: Int) {
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .apply()
    }

    /**
     * Fetch stored user id. Returns null if not present.
     */
    fun fetchUserId(): Int? {
        val id = prefs.getInt(KEY_USER_ID, -1)
        return if (id == -1) null else id
    }

    /**
     * Clears all stored session data (logout).
     */
    fun clearSession() {
        prefs.edit()
            .clear()
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "hr_session_prefs"
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_USER_ID = "key_user_id"
    }
}

package com.example.hrmanagement

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hrmanagement.ui.dashboard.DashboardActivity
import com.example.hrmanagement.util.SessionManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)

        // Decide where to go
        if (session.fetchAuthToken() != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
        } else {
            // later you can redirect to LoginActivity
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        finish()
    }
}

package com.example.hrmanagement.ui.notification

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hrmanagement.R
import com.example.hrmanagement.network.RetrofitClient
import com.example.hrmanagement.util.SessionManager
import kotlinx.coroutines.launch

class NotificationActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: NotificationAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        sessionManager = SessionManager(this)

        recycler = findViewById(R.id.recyclerNotifications)
        emptyView = findViewById(R.id.tvEmpty)

        adapter = NotificationAdapter()
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadNotifications()
    }

    private fun loadNotifications() {
        val token = sessionManager.fetchAuthToken() ?: return
        val userId = sessionManager.fetchUserId() ?: return

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.notificationApi.getNotifications(
                    "Bearer $token",
                    userId.toString()
                )

                if (resp.isSuccessful) {
                    val list = resp.body().orEmpty()

                    if (list.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        recycler.visibility = View.GONE
                    } else {
                        adapter.submitList(list)
                        emptyView.visibility = View.GONE
                        recycler.visibility = View.VISIBLE
                    }

                    // ✅ Mark as read
                    RetrofitClient.notificationApi.markAsRead(
                        "Bearer $token",
                        userId.toString()
                    )

                } else {
                    Toast.makeText(
                        this@NotificationActivity,
                        "Failed to load notifications",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@NotificationActivity,
                    e.localizedMessage ?: "Error loading notifications",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

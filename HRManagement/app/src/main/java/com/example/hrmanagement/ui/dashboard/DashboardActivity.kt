package com.example.hrmanagement.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.hrmanagement.R
import com.example.hrmanagement.model.DashboardResponse
import com.example.hrmanagement.network.RetrofitClient
import com.example.hrmanagement.ui.attendance.AttendanceActivity
import com.example.hrmanagement.ui.leave.LeaveRequestActivity
import com.example.hrmanagement.ui.report.ReportActivity
import com.example.hrmanagement.ui.team.TeamActivity
import com.example.hrmanagement.util.SessionManager
import kotlinx.coroutines.launch
import retrofit2.Response


class DashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    // views
    private lateinit var loadingView: View
    private lateinit var contentView: View

    private lateinit var imgAvatar: ImageView
    private lateinit var tvEmployeeName: TextView
    private lateinit var tvEmployeeCodeTitle: TextView
    private lateinit var tvTodayTime: TextView
    private lateinit var btnCheckInOut: androidx.appcompat.widget.AppCompatButton

    private lateinit var tvLeaveBalanceDays: TextView
    private lateinit var tvLeaveThisMonthDays: TextView

    // 🔴 notification badge
    private lateinit var redDot: View

    // quick access include roots
    private lateinit var itemAttendance: View
    private lateinit var itemLeaveRequests: View
    private lateinit var itemProfile: View
    private lateinit var itemMembers: View
    private lateinit var btnLogout: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        loadingView = findViewById(R.id.dashboardLoading)
        contentView = findViewById(R.id.dashboardContent)

        showLoading()
        btnLogout = findViewById(R.id.btnLogout)

        btnLogout.setOnClickListener {
            logout()
        }
        findViewById<ImageView>(R.id.btnReport).setOnClickListener {
            startActivity(
                Intent(this, com.example.hrmanagement.ui.report.ReportActivity::class.java)
            )
        }

        sessionManager = SessionManager(this)

        // find views
        imgAvatar = findViewById(R.id.imgAvatar)
        tvEmployeeName = findViewById(R.id.tvEmployeeName)
        tvEmployeeCodeTitle = findViewById(R.id.tvEmployeeCodeTitle)
        tvTodayTime = findViewById(R.id.tvTodayTime)
        btnCheckInOut = findViewById(R.id.btnCheckInOut)

        tvLeaveBalanceDays = findViewById(R.id.tvLeaveBalanceDays)
        tvLeaveThisMonthDays = findViewById(R.id.tvLeaveThisMonthDays)

        // 🔴 red dot view (must exist in activity_dashboard.xml)
        redDot = findViewById(R.id.redDot)

        itemAttendance = findViewById(R.id.itemAttendance)
        itemLeaveRequests = findViewById(R.id.itemLeaveRequests)
        itemProfile = findViewById(R.id.itemProfile)
        itemMembers = findViewById(R.id.itemMembers)
        setupQuickAccessCards()

        itemProfile.setOnClickListener {
            startActivity(
                Intent(this, com.example.hrmanagement.ui.profile.ProfileActivity::class.java)
            )
        }




        // Navigate to Attendance screen
        btnCheckInOut.setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
        }



    }
    private fun showLoading() {
        loadingView.visibility = View.VISIBLE
        contentView.visibility = View.GONE
    }

    private fun hideLoading() {
        loadingView.visibility = View.GONE
        contentView.visibility = View.VISIBLE
    }


    override fun onResume() {
        super.onResume()
        loadDashboard()
//        loadNotificationBadge() // 🔴 NEW
    }

    // -------------------------------------------------------------------------
    // QUICK ACCESS CARDS
    // -------------------------------------------------------------------------
    private fun setupQuickAccessCards() {
        setQuickAccess(
            root = itemAttendance,
            title = "Attendance",
            subtitle = "Check in/out",
            iconRes = R.drawable.usercheck
        )

        setQuickAccess(
            root = itemLeaveRequests,
            title = "Leave Requests",
            subtitle = "Apply and manage leave",
            iconRes = R.drawable.insomnia
        )

        setQuickAccess(
            root = itemProfile,
            title = "Profile",
            subtitle = "View profile and settings",
            iconRes = R.drawable.user
        )

        setQuickAccess(
            root = itemMembers,
            title = "Members",
            subtitle = "Team members & contacts",
            iconRes = R.drawable.usergroups
        )
    }

    private fun setQuickAccess(
        root: View,
        title: String,
        subtitle: String,
        iconRes: Int
    ) {
        val titleView = root.findViewById<TextView>(R.id.tvQuickAccessTitle)
        val subView = root.findViewById<TextView>(R.id.tvQuickAccessSubtitle)
        val iconView = root.findViewById<ImageView>(R.id.ivQuickIcon)

        titleView.text = title
        subView.text = subtitle
        iconView.setImageResource(iconRes)

        root.setOnClickListener {
            when (root.id) {
                R.id.itemAttendance ->
                    startActivity(Intent(this, AttendanceActivity::class.java))

                R.id.itemLeaveRequests ->
                    startActivity(Intent(this, LeaveRequestActivity::class.java))

                R.id.itemProfile ->
                    startActivity(
                        Intent(this, com.example.hrmanagement.ui.profile.ProfileActivity::class.java))
                R.id.itemMembers ->
                    startActivity(Intent(this, TeamActivity::class.java))

                else ->
                    Toast.makeText(this, title, Toast.LENGTH_SHORT).show()
            }
        }
    }


    // -------------------------------------------------------------------------
    // DASHBOARD API
    // -------------------------------------------------------------------------
    private fun loadDashboard() {
        val token = sessionManager.fetchAuthToken() ?: return
        val userId = sessionManager.fetchUserId()
        if (userId == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val userIdHeader = userId.toString()



        lifecycleScope.launch {
            try {
                val resp: Response<DashboardResponse> =
                    RetrofitClient.dashboardApi.getDashboard(
                        "Bearer $token",
                        userIdHeader
                    )

                if (resp.isSuccessful && resp.body() != null) {
                    bindDashboard(resp.body()!!)
                } else {
                    Toast.makeText(
                        this@DashboardActivity,
                        "Dashboard load failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@DashboardActivity,
                    "Dashboard error: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun bindDashboard(data: DashboardResponse) {
        val avatar = data.employee.avatarUrl
        if (!avatar.isNullOrBlank()) {
            val finalUrl = RetrofitClient.BASE_URL.trimEnd('/') + avatar
            Glide.with(this)
                .load(finalUrl)
                .circleCrop()
                .into(imgAvatar)
            hideLoading()
        }

        tvEmployeeName.text = data.employee.fullName
        tvEmployeeCodeTitle.text =
            "${data.employee.employeeCode} · ${data.employee.jobTitle ?: ""}"

        tvTodayTime.text = data.todayStatus.checkInTime ?: "Not checked in"

        btnCheckInOut.text = when {
            data.todayStatus.canCheckIn -> "Check In"
            data.todayStatus.canCheckOut -> "Check Out"
            else -> "Completed"
        }

        btnCheckInOut.isEnabled =
            data.todayStatus.canCheckIn || data.todayStatus.canCheckOut

        tvLeaveBalanceDays.text =
            "${data.leaveSummary.leaveBalanceDays} days"
        tvLeaveThisMonthDays.text =
            "${data.leaveSummary.usedThisMonthDays} days"

    }

    // -------------------------------------------------------------------------
    // 🔴 NOTIFICATION BADGE
    // -------------------------------------------------------------------------
    private fun loadNotificationBadge() {
        val token = sessionManager.fetchAuthToken() ?: return

        lifecycleScope.launch {
            try {
                val userId = sessionManager.fetchUserId() ?: return@launch

                val resp = RetrofitClient.notificationApi.getUnreadCount(
                    "Bearer $token",
                    userId.toString()
                )

                if (resp.isSuccessful) {
                    val count = resp.body()?.count ?: 0
                    redDot.visibility =
                        if (count > 0) View.VISIBLE else View.GONE
                } else {
                    redDot.visibility = View.GONE
                }
            } catch (e: Exception) {
                redDot.visibility = View.GONE
            }
        }
    }

    private fun openNotifications() {
        val token = sessionManager.fetchAuthToken() ?: return

        lifecycleScope.launch {
            try {
                val userId = sessionManager.fetchUserId() ?: return@launch

                val resp = RetrofitClient.notificationApi.getNotifications(
                    "Bearer $token",
                    userId.toString()
                )

                if (resp.isSuccessful) {
                    val list = resp.body().orEmpty()

                    val message =
                        if (list.isEmpty()) {
                            "No notifications"
                        } else {
                            list.joinToString("\n\n") {
                                "• ${it.message}"
                            }
                        }

                    androidx.appcompat.app.AlertDialog.Builder(this@DashboardActivity)
                        .setTitle("Notifications")
                        .setMessage(message)
                        .setPositiveButton("OK") { _, _ ->
                            markNotificationsRead()
                        }
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@DashboardActivity,
                    "Failed to load notifications",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun markNotificationsRead() {
        val token = sessionManager.fetchAuthToken() ?: return

        lifecycleScope.launch {
            try {
                val userId = sessionManager.fetchUserId() ?: return@launch

                RetrofitClient.notificationApi.markAsRead(
                    "Bearer $token",
                    userId.toString()
                )


                // 🔴 hide red dot immediately
                redDot.visibility = View.GONE

            } catch (_: Exception) {}
        }
    }
    private fun logout() {

        // 1️⃣ Clear session
        sessionManager.clearSession()

        // 2️⃣ Navigate to Login screen
        val intent = Intent(this, com.example.hrmanagement.ui.login.LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)

        // 3️⃣ Finish dashboard
        finish()
    }


}

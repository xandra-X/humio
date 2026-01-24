package com.example.hrmanagement.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.hrmanagement.R
import com.example.hrmanagement.data.profile.ProfileResponse
import com.example.hrmanagement.ui.login.LoginActivity
import com.example.hrmanagement.util.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProfileActivity : AppCompatActivity() {

    private val viewModel: ProfileViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Views
        val cardOvertime = findViewById<CardView>(R.id.cardOvertime)
        val tvOvertimeFee = findViewById<TextView>(R.id.tvOvertimeFee)
        val imgAvatar = findViewById<ImageView>(R.id.imgAvatar)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvCode = findViewById<TextView>(R.id.tvCode)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val tvDepartment = findViewById<TextView>(R.id.tvDepartment)
        val tvRole = findViewById<TextView>(R.id.tvRole)
        val tvShift = findViewById<TextView>(R.id.tvShift)
        val tvMonthly = findViewById<TextView>(R.id.tvMonthly)
        val tvAnnual = findViewById<TextView>(R.id.tvAnnual)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val backBtn = findViewById<ImageView>(R.id.btnBack)
        val rvOvertime = findViewById<RecyclerView>(R.id.rvOvertime)

        rvOvertime.layoutManager = LinearLayoutManager(this)

        backBtn.setOnClickListener { finish() }

        btnLogout.setOnClickListener {
            SessionManager(this).clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Error collector (ONLY once)
        lifecycleScope.launch {
            viewModel.error.collect { isError ->
                if (isError) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Failed to load profile",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }



        // State collector
        lifecycleScope.launch {
            viewModel.state.collect { data ->
                if (data == null) return@collect

                val profile = data.profile
                val pay = data.pay
                val overtimeList = data.overtime

                // Profile info
                tvName.text = profile.fullName
                tvCode.text = profile.employeeCode
                tvEmail.text = profile.email
                tvDepartment.text = profile.department
                tvRole.text = profile.role
                tvShift.text = profile.shift

                tvAnnual.text = "Annual Salary: ${"%.2f".format(pay.annualSalary)}"
                tvMonthly.text = "Monthly Salary: ${"%.2f".format(pay.monthlySalary)}"

                // Avatar (DO NOT rebuild URL)
                if (profile.avatarUrl.isNullOrBlank()) {
                    imgAvatar.setImageResource(R.drawable.user)
                } else {
                    Glide.with(this@ProfileActivity)
                        .load(profile.avatarUrl)
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .into(imgAvatar)
                }

                // Overtime
                if (overtimeList.isNotEmpty()) {
                    cardOvertime.visibility = View.VISIBLE
                    tvOvertimeFee.text =
                        "Overtime Fee: ${"%.2f".format(pay.totalOvertimePay)}"
                    rvOvertime.adapter = OvertimeAdapter(overtimeList)
                } else {
                    cardOvertime.visibility = View.GONE
                }
            }
        }

        // Load data
        viewModel.loadProfile()
    }
    override fun onResume() {
        super.onResume()
        viewModel.loadProfile()
    }

}

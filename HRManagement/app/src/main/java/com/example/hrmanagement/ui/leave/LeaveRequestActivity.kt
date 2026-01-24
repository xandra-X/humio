package com.example.hrmanagement.ui.leave
import com.example.hrmanagement.network.RetrofitClient.LeaveHistoryDto
import android.app.AlertDialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hrmanagement.R
import com.example.hrmanagement.data.LeaveBalanceItem
import com.example.hrmanagement.data.LeaveHistoryItem
import com.example.hrmanagement.network.RetrofitClient
import com.example.hrmanagement.util.SessionManager
import kotlinx.coroutines.*
import android.util.Log

class LeaveRequestActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var gridBalances: GridLayout
    private lateinit var recyclerHistory: RecyclerView
    private lateinit var historyAdapter: LeaveHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leave_request)

        session = SessionManager(this)
        gridBalances = findViewById(R.id.gridBalances)
        recyclerHistory = findViewById(R.id.recyclerHistory)

        // Setup RecyclerView for history
        historyAdapter = LeaveHistoryAdapter(emptyList())
        recyclerHistory.layoutManager = LinearLayoutManager(this)
        recyclerHistory.adapter = historyAdapter

        findViewById<ImageButton>(R.id.btnAdd).setOnClickListener {
            showDialog()
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        loadBalances()
        loadHistory()
        Log.d("TOKEN", session.fetchAuthToken().toString())
        Log.d("USER_ID", session.fetchUserId().toString())
    }

    private fun loadBalances() {
        val token = session.fetchAuthToken()
        if (token == null) {
            toast("Not logged in")
            return
        }



                CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = RetrofitClient.leaveApi.getLeaveBalance("Bearer $token")


                withContext(Dispatchers.Main) {
                    if (!res.isSuccessful || res.body() == null) {
                        toast("Failed to load leave balance")
                        return@withContext
                    }

                    val d = res.body()!!

                    val items = listOf(
                        LeaveBalanceItem(
                            "${d.leaveBalanceDays} days",
                            "Annual Leave",
                            "Paid time off for vacation or personal plans.",
                            R.drawable.cal
                        ),
                        LeaveBalanceItem(
                            "${d.medicalLeaveDays} days",
                            "Medical Leave",
                            "Time off without salary when needed.",
                            R.drawable.cal
                        ),
                        LeaveBalanceItem(
                            "${d.casualLeaveDays} days",
                            "Casual Leave",
                            "Short notice leave for personal.",
                            R.drawable.cal
                        ),
                        LeaveBalanceItem(
                            "${d.unpaidLeaveDays} days",
                            "Unpaid Leave",
                            "Leave for health related reasons.",
                            R.drawable.cal
                        )
                    )

                    renderBalances(items)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toast("Network error: ${e.message}")
                }
            }
        }
    }

    private fun loadHistory() {
        val token = session.fetchAuthToken() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val res = RetrofitClient.leaveApi.getLeaveHistory("Bearer $token")

                withContext(Dispatchers.Main) {
                    if (res.isSuccessful && res.body() != null) {
                        val list = res.body()!!

                        if (list.isEmpty()) {
                            toast("No leave history yet")
                            historyAdapter.submitList(emptyList())
                            return@withContext
                        }

                        val historyList = list.map { dto ->
                            LeaveHistoryItem(
                                type = dto.leaveType,
                                daysText = dto.daysText,
                                note = dto.note ?: "",
                                status = dto.status
                            )
                        }

                        historyAdapter.submitList(historyList)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    toast("Failed to load history: ${e.message}")
                }
            }
        }
    }


    private fun renderBalances(items: List<LeaveBalanceItem>) {
        gridBalances.removeAllViews()
        val inflater = LayoutInflater.from(this)

        items.forEach {
            val v = inflater.inflate(R.layout.item_leave_balance, gridBalances, false)
            v.findViewById<TextView>(R.id.tvDays).text = it.daysText
            v.findViewById<TextView>(R.id.tvLabel).text = it.title
            // Add description if your layout has it
            v.findViewById<TextView>(R.id.tvDesc)?.text = it.description
            gridBalances.addView(v)
        }
    }

    private fun showDialog() {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.dialog_leave_request, null)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(0))
        dialog.show()

        val spnType = view.findViewById<Spinner>(R.id.spnLeaveType)
        val etStart = view.findViewById<EditText>(R.id.etStart)
        val etEnd = view.findViewById<EditText>(R.id.etEnd)
        val etReason = view.findViewById<EditText>(R.id.etReason)

        spnType.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("ANNUAL", "MEDICAL", "CASUAL", "UNPAID")
        )

        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val startDate = etStart.text.toString().trim()
            val endDate = etEnd.text.toString().trim()
            val reason = etReason.text.toString().trim()

            // Validation
            if (startDate.isEmpty()) {
                toast("Please enter start date (yyyy-MM-dd)")
                return@setOnClickListener
            }
            if (endDate.isEmpty()) {
                toast("Please enter end date (yyyy-MM-dd)")
                return@setOnClickListener
            }
            if (reason.isEmpty()) {
                toast("Please enter a reason")
                return@setOnClickListener
            }

            val body = RetrofitClient.LeaveRequestBody(
                leaveType = spnType.selectedItem.toString(),
                startDate = startDate,
                endDate = endDate,
                days = 1.0,
                reason = reason
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val res = RetrofitClient.leaveApi.submitLeave(
                        "Bearer ${session.fetchAuthToken()}",
                        body
                    )

                    withContext(Dispatchers.Main) {
                        if (res.isSuccessful) {
                            toast("Leave request submitted successfully")
                            dialog.dismiss()
                            loadBalances()
                            loadHistory() // Refresh history
                        } else {
                            toast("Submit failed: ${res.message()}")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        toast("Error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
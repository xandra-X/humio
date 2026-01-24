package com.example.hrmanagement.ui.attendance

import android.os.Bundle
import android.widget.*
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hrmanagement.R
import com.example.hrmanagement.data.CheckInOutRequest
import com.example.hrmanagement.data.SimpleResponse
import com.example.hrmanagement.network.RetrofitClient
import com.example.hrmanagement.util.SessionManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.*

class AttendanceActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnCheckIn: Button
    private lateinit var btnCheckOut: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var recyclerRecords: RecyclerView
    private lateinit var adapter: AttendanceRecordAdapter

    private var pendingAction: String? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var barcodeLauncher: ActivityResultLauncher<ScanOptions>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        sessionManager = SessionManager(this)

        btnBack = findViewById(R.id.btnBack)
        btnCheckIn = findViewById(R.id.btnCheck)
        btnCheckOut = findViewById(R.id.btnCheckSecondary)
        tvStatus = findViewById(R.id.tvCurrentStatus)
        tvDate = findViewById(R.id.tvDate)
        tvTime = findViewById(R.id.tvTime)
        recyclerRecords = findViewById(R.id.recyclerRecords)

        adapter = AttendanceRecordAdapter(emptyList())
        recyclerRecords.layoutManager = LinearLayoutManager(this)
        recyclerRecords.adapter = adapter

        btnBack.setOnClickListener { finish() }
        onBackPressedDispatcher.addCallback(this) { finish() }

        barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
            val contents = result?.contents
            if (contents.isNullOrEmpty()) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
                pendingAction = null
                return@registerForActivityResult
            }
            handleScanResult(contents, pendingAction)
        }

        btnCheckIn.setOnClickListener {
            pendingAction = "CHECK_IN"
            startScanner()
        }

        btnCheckOut.setOnClickListener {
            pendingAction = "CHECK_OUT"
            startScanner()
        }

        loadToday()
        loadHistory()
    }

    private fun startScanner() {
        val options = ScanOptions().apply {
            setPrompt("Scan attendance QR")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        }
        barcodeLauncher.launch(options)
    }

    private fun handleScanResult(scannedText: String, action: String?) {
        if (action == null) return

        val token = sessionManager.fetchAuthToken() ?: return
        val authHeader = "Bearer $token"
        val userIdHeader = sessionManager.fetchUserId()?.toString()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val req = CheckInOutRequest(action, scannedText)
                val response: Response<SimpleResponse> =
                    RetrofitClient.dashboardApi.checkInOut(authHeader, userIdHeader, req)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@AttendanceActivity,
                            response.body()?.message ?: "Success",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadToday()
                        loadHistory()
                    } else {
                        Toast.makeText(
                            this@AttendanceActivity,
                            response.errorBody()?.string(),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } finally {
                pendingAction = null
            }
        }
    }

    private fun loadToday() {
        val token = sessionManager.fetchAuthToken() ?: return
        val auth = "Bearer $token"
        val userId = sessionManager.fetchUserId()?.toString()

        CoroutineScope(Dispatchers.IO).launch {
            val resp = RetrofitClient.dashboardApi.getToday(auth, userId)
            withContext(Dispatchers.Main) {
                if (resp.isSuccessful) {
                    val body = resp.body()
                    val now = Date()
                    tvDate.text = java.text.SimpleDateFormat(
                        "EEEE, MMMM d, yyyy",
                        Locale.getDefault()
                    ).format(now)

                    tvTime.text = body?.checkInTime ?: body?.checkOutTime ?: "--:--"
                    tvStatus.text = if (body?.checkedIn == true) "Checked in" else "Not checked"
                    btnCheckIn.isEnabled = body?.canCheckIn ?: true
                    btnCheckOut.isEnabled = body?.canCheckOut ?: false
                }
            }
        }
    }

    private fun loadHistory(limit: Int = 20) {
        val token = sessionManager.fetchAuthToken() ?: return
        val auth = "Bearer $token"
        val userId = sessionManager.fetchUserId()?.toString()

        CoroutineScope(Dispatchers.IO).launch {
            val resp = RetrofitClient.dashboardApi.getHistory(auth, userId, limit)
            withContext(Dispatchers.Main) {
                if (resp.isSuccessful) {
                    adapter.submitList(resp.body() ?: emptyList())
                }
            }
        }
    }
}

package com.example.hrmanagement.ui.report

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.hrmanagement.R
import com.example.hrmanagement.network.ReportRequest
import com.example.hrmanagement.network.RetrofitClient
import com.example.hrmanagement.util.SessionManager
import kotlinx.coroutines.launch

class ReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etDesc = findViewById<EditText>(R.id.etDescription)
        val spinner = findViewById<Spinner>(R.id.spinnerCategory)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnBack.setOnClickListener { finish() }
        btnCancel.setOnClickListener { finish() }

        // Category spinner
        val categories = listOf("Issue", "Request", "Complaint", "Other")
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )

        btnSubmit.setOnClickListener {

            val title = etTitle.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            val category = spinner.selectedItem.toString()

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val token = SessionManager(this).fetchAuthToken()
            if (token.isNullOrBlank()) {
                Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }



            val finalTitle = "[$category] $title"

            lifecycleScope.launch {
                try {
                    val res = RetrofitClient.reportApi.submitReport(
                        "Bearer $token",
                        ReportRequest(finalTitle, desc)
                    )

                    if (res.isSuccessful) {
                        Toast.makeText(
                            this@ReportActivity,
                            "Report submitted",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@ReportActivity,
                            "Failed: ${res.code()} ${res.errorBody()?.string()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(
                        this@ReportActivity,
                        "Error: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

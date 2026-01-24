package com.example.hrmanagement.ui.team

import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.hrmanagement.R
import com.example.hrmanagement.data.team.TeamMemberDto
import com.example.hrmanagement.databinding.ActivityTeamBinding
import com.example.hrmanagement.network.RetrofitClient
import com.example.hrmanagement.repository.TeamRepository
import kotlinx.coroutines.launch

class TeamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeamBinding
    private lateinit var repository: TeamRepository
    private lateinit var adapter: TeamMemberAdapter

    // 🔍 Full list for search
    private var fullMemberList: List<TeamMemberDto> = emptyList()

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.edtSearch.windowToken, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTeamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = TeamRepository(this)

        // Back button
        binding.btnBack.setOnClickListener { finish() }

        // RecyclerView
        adapter = TeamMemberAdapter()
        binding.recyclerMembers.layoutManager = LinearLayoutManager(this)
        binding.recyclerMembers.adapter = adapter

        // ================= LOAD DATA =================
        lifecycleScope.launch {
            try {
                val response = repository.getMyDepartmentTeam()

                // -------- Supervisor --------
                response.head?.let { head ->

                    val supervisorImageUrl = head.avatarUrl?.let {
                        RetrofitClient.BASE_URL.removeSuffix("/") + it
                    }

                    binding.txtSupervisorName.text = head.fullName
                    binding.txtSupervisorCode.text = head.employeeCode
                    binding.txtSupervisorRole.text = head.jobTitle ?: "-"
                    binding.txtSupervisorEmail.text = head.email

                    Glide.with(this@TeamActivity)
                        .load(supervisorImageUrl)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .error(R.drawable.ic_avatar_placeholder)
                        .into(binding.imgSupervisorAvatar)

                } ?: run {
                    binding.txtSupervisorName.text = "-"
                    binding.txtSupervisorCode.text = "-"
                    binding.txtSupervisorRole.text = "-"
                    binding.txtSupervisorEmail.text = "-"
                    binding.imgSupervisorAvatar.setImageResource(R.drawable.ic_avatar_placeholder)
                }

                // -------- Team Members --------
                fullMemberList = response.members
                adapter.submitList(fullMemberList)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@TeamActivity,
                    "Failed to load team data",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

        // ================= SEARCH =================
        binding.edtSearch.addTextChangedListener { editable ->
            val query = editable?.toString()?.trim()?.lowercase().orEmpty()

            val filtered = if (query.isEmpty()) {
                fullMemberList
            } else {
                fullMemberList.filter {
                    it.fullName.lowercase().contains(query) ||
                            it.employeeCode.lowercase().contains(query) ||
                            it.email.lowercase().contains(query)
                }
            }

            adapter.submitList(filtered)
        }
    }
}

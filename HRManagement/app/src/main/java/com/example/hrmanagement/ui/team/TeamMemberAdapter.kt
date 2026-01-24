package com.example.hrmanagement.ui.team

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hrmanagement.R
import com.example.hrmanagement.data.team.TeamMemberDto
import com.example.hrmanagement.databinding.ItemTeamMemberBinding
import com.example.hrmanagement.network.RetrofitClient

class TeamMemberAdapter :
    ListAdapter<TeamMemberDto, TeamMemberAdapter.MemberViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemTeamMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MemberViewHolder(
        private val binding: ItemTeamMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: TeamMemberDto) {
            val memberImageUrl = member.avatarUrl?.let {
                RetrofitClient.BASE_URL.removeSuffix("/") + it
            }

            binding.txtName.text = member.fullName
            binding.txtCode.text = member.employeeCode
            binding.txtRole.text = member.jobTitle ?: "-"
            binding.txtEmail.text = member.email

            // ✅ STATUS
            if (member.active) {
                binding.txtStatus.text = "Active"
                binding.txtStatus.setBackgroundResource(R.drawable.bg_status_active)
            } else {
                binding.txtStatus.text = "Inactive"
                binding.txtStatus.setBackgroundResource(R.drawable.bg_status_inactive)
            }

            // ✅ AVATAR
            Glide.with(binding.root.context)
                .load(memberImageUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(binding.imgAvatar)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TeamMemberDto>() {
        override fun areItemsTheSame(
            oldItem: TeamMemberDto,
            newItem: TeamMemberDto
        ): Boolean = oldItem.employeeId == newItem.employeeId

        override fun areContentsTheSame(
            oldItem: TeamMemberDto,
            newItem: TeamMemberDto
        ): Boolean = oldItem == newItem
    }
}

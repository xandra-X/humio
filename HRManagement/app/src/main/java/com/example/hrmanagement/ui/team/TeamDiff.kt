package com.example.hrmanagement.ui.team

import androidx.recyclerview.widget.DiffUtil
import com.example.hrmanagement.data.team.TeamMemberDto

class TeamDiff : DiffUtil.ItemCallback<TeamMemberDto>() {

    override fun areItemsTheSame(
        oldItem: TeamMemberDto,
        newItem: TeamMemberDto
    ): Boolean {
        return oldItem.employeeId == newItem.employeeId
    }

    override fun areContentsTheSame(
        oldItem: TeamMemberDto,
        newItem: TeamMemberDto
    ): Boolean {
        return oldItem == newItem
    }
}

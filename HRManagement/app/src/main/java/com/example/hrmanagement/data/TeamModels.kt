package com.example.hrmanagement.data.team

data class TeamResponse(
    val department: DepartmentDto,
    val head: TeamMemberDto?,
    val members: List<TeamMemberDto>
)

data class DepartmentDto(
    val id: Int,
    val name: String
)

data class TeamMemberDto(
    val employeeId: Int,
    val employeeCode: String,
    val fullName: String,
    val jobTitle: String?,
    val email: String,
    val avatarUrl: String?,
    val phone: String? = null,
    val active: Boolean
)

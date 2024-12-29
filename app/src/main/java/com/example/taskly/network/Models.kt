package com.example.taskly.network

data class RegisterRequest(val username: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class ResponseMessage(val message: String)

data class LoginResponse(
    val success: Boolean,
    val token: String?,
    val message: String?,
    val user: User?
)

data class RegisterResponse(
    val success: Boolean,
    val message: String?,
    val data: User?
)

data class User(
    val id: Int,
    val username: String,
    val email: String,
    val password: String? = null // Password tidak selalu diperlukan (opsional)
)

// **Personal Tasks**
data class PersonalTask(
    val id: Int,
    val nama_tugas: String,
    val tanggal: String,
    val deadline: String,
    val user_id: Int,
    val is_completed: Boolean
)

// **Personal Subtasks**
data class PersonalSubTask(
    val id: Int,
    val nama_sub_tugas: String,
    val tugas_pribadi_id: Int,
    var is_completed: Boolean
)

// **Group Tasks**
data class GroupTask(
    val id: Int,
    val nama_tugas: String,
    val tanggal: String,
    val deadline: String,
    val is_completed: Boolean
)

// **Group Subtasks**
data class GroupSubTask(
    val id: Int,
    val nama_sub_tugas: String,
    val tugas_kelompok_id: Int,
    var is_completed: Boolean
)

// **Group Members**
data class GroupMember(
    val id: Int,
    val tugas_kelompok_id: Int,
    val user_id: Int
)

data class GroupTasksResponse(
    val inProgress: List<GroupTask>,
    val completed: List<GroupTask>
)

// **Group Members Update Request**
data class GroupMembers(val members: List<Int>)

// **Task Status Update**
data class TaskStatus(val is_completed: Boolean)

// Data class untuk payload `updateToken`
data class TokenUpdate(
    val userId: Int,
    val token: String
)

data class NotificationPreference(val userId: Int, val channelId: String, val isEnabled: Boolean)



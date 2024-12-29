package com.example.taskly.network

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @POST("register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    // Tambahkan di ApiService.kt
    @GET("users")
    fun getUsers(): Call<List<User>>

    @GET("users/{userId}")
    fun getUserById(@Path("userId") userId: Int): Call<User>

    // **Personal Tasks**
    @GET("personal_tasks/{userId}")
    fun getPersonalTasks(@Path("userId") userId: Int): Call<List<PersonalTask>>

    @POST("personal_tasks")
    fun addPersonalTask(@Body task: PersonalTask): Call<ResponseMessage>

    @PUT("personal_tasks/{taskId}")
    fun updatePersonalTaskStatus(
        @Path("taskId") taskId: Int,
        @Body status: TaskStatus
    ): Call<ResponseMessage>

    @GET("personal_tasks/in_progress/{userId}")
    fun getPersonalTasksInProgress(@Path("userId") userId: Int): Call<List<PersonalTask>>

    @GET("personal_tasks/completed/{userId}")
    fun getPersonalTasksCompleted(@Path("userId") userId: Int): Call<List<PersonalTask>>

    // Update status untuk subtugas pribadi
    @PUT("personal_subtasks/{id}")
    fun updatePersonalSubTaskStatus(
        @Path("id") subTaskId: Int,
        @Body status: TaskStatus
    ): Call<ResponseMessage>

    @GET("personal_subtasks/{taskId}")
    fun getPersonalSubTasks(@Path("taskId") taskId: Int): Call<List<PersonalSubTask>>

    @POST("personal_subtasks")
    fun addPersonalSubTask(@Body subTask: PersonalSubTask): Call<ResponseMessage>

    // **Group Tasks**
    @POST("group_tasks")
    fun addGroupTask(@Body task: GroupTask): Call<ResponseMessage>

    @GET("group_tasks/{userId}")
    fun getGroupTasks(@Path("userId") userId: Int): Call<GroupTasksResponse>

    @GET("group_tasks/in_progress/{userId}")
    fun getGroupTasksInProgress(@Path("userId") userId: Int): Call<List<GroupTask>>

    @GET("group_tasks/completed/{userId}")
    fun getGroupTasksCompleted(@Path("userId") userId: Int): Call<List<GroupTask>>

    @PUT("group_tasks/{taskId}")
    fun updateGroupTaskStatus(
        @Path("taskId") taskId: Int,
        @Body status: TaskStatus
    ): Call<ResponseMessage>

    @PUT("group_subtasks/{id}")
    fun updateGroupSubTaskStatus(
        @Path("id") subTaskId: Int,
        @Body status: TaskStatus
    ): Call<ResponseMessage>

    @GET("group_subtasks/{taskId}")
    fun getGroupSubTasks(@Path("taskId") taskId: Int): Call<List<GroupSubTask>>

    @POST("group_subtasks")
    fun addGroupSubTask(
        @Body subTask: GroupSubTask
    ): Call<ResponseMessage>

    // **Group Members**
    @GET("group_task_members/{taskId}")
    fun getGroupMembers(@Path("taskId") taskId: Int): Call<List<GroupMember>>

    @PUT("group_task_members/{taskId}")
    fun updateGroupMembers(
        @Path("taskId") taskId: Int,
        @Body members: GroupMembers
    ): Call<ResponseMessage>

    //firebase
    @POST("updateToken")
    suspend fun updateToken(@Body tokenUpdate: TokenUpdate): Response<Unit>

    @PUT("updateNotificationPreferences")
    suspend fun updateNotificationPreferences(
        @Body preference: NotificationPreference
    ): Response<Unit>

}
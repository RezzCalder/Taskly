package com.example.taskly

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.taskly.network.ApiService
import com.example.taskly.network.GroupMembers
import com.example.taskly.network.GroupSubTask
import com.example.taskly.network.GroupTask
import com.example.taskly.network.ResponseMessage
import com.example.taskly.network.RetrofitInstance
import com.example.taskly.network.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun TambahTugasKelompokScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("TasklyPrefs", Context.MODE_PRIVATE)
    val userId = sharedPreferences.getInt("userId", -1) // Ambil userId dari SharedPreferences

    if (userId == -1) {
        Toast.makeText(context, "User belum login.", Toast.LENGTH_SHORT).show()
        navController.navigate("login")
        return
    }

    var taskTitle by remember { mutableStateOf("") }
    var taskDate by remember { mutableStateOf("") }
    var taskDeadline by remember { mutableStateOf("") }
    val subTaskTitles = remember { mutableStateListOf("") }
    val users = remember { mutableStateListOf<User>() }
    val selectedUsers = remember { mutableStateListOf<Int>() }

    // Fetch users untuk daftar anggota kelompok
    LaunchedEffect(Unit) {
        if (!selectedUsers.contains(userId)) {
            selectedUsers.add(userId)
        }
        val api = RetrofitInstance.getInstance().create(ApiService::class.java)
        api.getUsers().enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (response.isSuccessful) {
                    users.addAll(response.body() ?: emptyList())
                } else {
                    Toast.makeText(context, "Gagal memuat daftar pengguna.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("Tambah Tugas Kelompok", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = taskTitle,
            onValueChange = { taskTitle = it },
            label = { Text("Judul Tugas") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = taskDate,
            onValueChange = { taskDate = it },
            label = { Text("Tanggal (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = taskDeadline,
            onValueChange = { taskDeadline = it },
            label = { Text("Deadline (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Pilih Anggota Kelompok (Max: 4)")
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.height(150.dp)) {
            items(users) { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedUsers.contains(user.id),
                        onCheckedChange = { checked ->
                            if (checked && selectedUsers.size < 4) {
                                selectedUsers.add(user.id)
                            } else if (!checked) {
                                selectedUsers.remove(user.id)
                            } else {
                                Toast.makeText(context, "Maksimal 4 anggota.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    Text("${user.username} (${user.email})", modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Sub-Tugas")
        subTaskTitles.forEachIndexed { index, subTask ->
            OutlinedTextField(
                value = subTask,
                onValueChange = { subTaskTitles[index] = it },
                label = { Text("Sub-Tugas #${index + 1}") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(onClick = { subTaskTitles.add("") }) {
            Text("Tambah Sub-Tugas")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (taskTitle.isNotBlank() && taskDate.isNotBlank() && taskDeadline.isNotBlank() && selectedUsers.isNotEmpty()) {
                    addGroupTask(
                        taskTitle,
                        taskDate,
                        taskDeadline,
                        selectedUsers,
                        subTaskTitles,
                        context,
                        navController
                    )
                } else {
                    Toast.makeText(context, "Lengkapi semua data.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simpan Tugas")
        }
    }
}

fun addGroupTask(
    taskTitle: String,
    taskDate: String,
    taskDeadline: String,
    selectedUsers: List<Int>,
    subTaskTitles: List<String>,
    context: Context,
    navController: NavController
) {
    val api = RetrofitInstance.getInstance().create(ApiService::class.java)
    val groupTask = GroupTask(
        id = 0, // Akan diisi oleh server
        nama_tugas = taskTitle,
        tanggal = taskDate,
        deadline = taskDeadline,
        is_completed = false
    )

    api.addGroupTask(groupTask).enqueue(object : Callback<ResponseMessage> {
        override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
            if (response.isSuccessful) {
                val taskId = response.body()?.message?.toIntOrNull()
                if (taskId != null) {
                    addGroupMembers(taskId, selectedUsers, subTaskTitles, context, navController)
                } else {
                    Toast.makeText(context, "Gagal mendapatkan ID tugas.", Toast.LENGTH_SHORT).show()
                    navController.navigate("group_tasks")
                }
            } else {
                Toast.makeText(context, "Gagal menambahkan tugas kelompok.", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
            Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    })
}

fun addGroupSubTasks(
    taskId: Int,
    subTaskTitles: List<String>,
    context: Context,
    navController: NavController
) {
    val api = RetrofitInstance.getInstance().create(ApiService::class.java)
    subTaskTitles.forEach { subTaskTitle ->
        val groupSubTask = GroupSubTask(
            id = 0, // Akan diisi oleh server
            nama_sub_tugas = subTaskTitle,
            tugas_kelompok_id = taskId,
            is_completed = false
        )

        api.addGroupSubTask(groupSubTask).enqueue(object : Callback<ResponseMessage> {
            override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Sub-tugas berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal menambahkan sub-tugas.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    Toast.makeText(context, "Tugas kelompok berhasil ditambahkan.", Toast.LENGTH_SHORT).show()
    navController.navigate("group_tasks")
}

fun addGroupMembers(
    taskId: Int,
    selectedUsers: List<Int>,
    subTaskTitles: List<String>,
    context: Context,
    navController: NavController
) {
    val api = RetrofitInstance.getInstance().create(ApiService::class.java)
    val groupMembers = GroupMembers(selectedUsers)

    Log.d("AddGroupMembers", "Task ID: $taskId, Members: $selectedUsers")

    api.updateGroupMembers(taskId, groupMembers).enqueue(object : Callback<ResponseMessage> {
        override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
            if (response.isSuccessful) {
                addGroupSubTasks(taskId, subTaskTitles, context, navController)
            } else {
                Toast.makeText(context, "Gagal menambahkan anggota kelompok.", Toast.LENGTH_SHORT).show()
                Log.e("AddGroupMembers", "Response error: ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
            Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    })
}

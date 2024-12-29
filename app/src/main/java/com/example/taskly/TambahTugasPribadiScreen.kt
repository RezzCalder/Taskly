package com.example.taskly

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.taskly.network.PersonalSubTask
import com.example.taskly.network.PersonalTask
import com.example.taskly.network.ResponseMessage
import com.example.taskly.network.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TambahTugasPribadiScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("TasklyPrefs", Context.MODE_PRIVATE)
    val userId = sharedPreferences.getInt("userId", -1)

    var taskTitle by remember { mutableStateOf("") }
    var taskDate by remember { mutableStateOf("") }
    var taskDeadline by remember { mutableStateOf("") }
    val subTaskTitles = remember { mutableStateListOf("") }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US) // Tetap dipakai untuk validasi

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("Tambah Tugas Pribadi", style = MaterialTheme.typography.headlineMedium)
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
                if (taskTitle.isBlank() || taskDate.isBlank() || taskDeadline.isBlank()) {
                    Toast.makeText(context, "Lengkapi semua data.", Toast.LENGTH_SHORT).show()
                } else {
                    try {
                        val parsedTaskDate = dateFormat.parse(taskDate)
                        val parsedTaskDeadline = dateFormat.parse(taskDeadline)

                        if (parsedTaskDate == null || parsedTaskDeadline == null) {
                            Toast.makeText(context, "Format tanggal tidak valid.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (parsedTaskDeadline.before(parsedTaskDate)) {
                            Toast.makeText(context, "Deadline tidak boleh sebelum tanggal tugas.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        addPersonalTask(
                            taskTitle,
                            taskDate,
                            taskDeadline,
                            userId,
                            subTaskTitles,
                            context,
                            navController
                        )
                    } catch (e: ParseException) {
                        Toast.makeText(context, "Format tanggal tidak valid.", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simpan Tugas")
        }
    }
}

fun addPersonalTask(
    taskTitle: String,
    taskDate: String,
    taskDeadline: String,
    userId: Int,
    subTaskTitles: List<String>,
    context: Context,
    navController: NavController
) {
    val api = RetrofitInstance.getInstance().create(ApiService::class.java)
    val task = PersonalTask(
        id = 0,
        nama_tugas = taskTitle,
        tanggal = taskDate,
        deadline = taskDeadline,
        user_id = userId,
        is_completed = false
    )

    Log.d("TambahTugas", "Task Data: $taskTitle, $taskDate, $taskDeadline, $userId")

    api.addPersonalTask(task).enqueue(object : Callback<ResponseMessage> {
        override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
            if (response.isSuccessful) {
                val taskId = response.body()?.message?.toIntOrNull()
                if (taskId != null) {
                    addPersonalSubTasks(taskId, subTaskTitles, context, navController)
                } else {
                    Toast.makeText(context, "Gagal mendapatkan ID tugas.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Gagal menambahkan tugas.", Toast.LENGTH_SHORT).show()
                Log.e("TambahTugas", "Response error: ${response.errorBody()?.string()}")
            }
        }

        override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
            Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    })
}

fun addPersonalSubTasks(taskId: Int, subTaskTitles: List<String>, context: Context, navController: NavController) {
    val api = RetrofitInstance.getInstance().create(ApiService::class.java)
    subTaskTitles.forEach { subTaskTitle ->
        if (subTaskTitle.isBlank()) {
            Toast.makeText(context, "Nama sub-tugas tidak boleh kosong.", Toast.LENGTH_SHORT).show()
            return
        }

        val personalSubTask = PersonalSubTask(
            id = 0,
            nama_sub_tugas = subTaskTitle,
            tugas_pribadi_id = taskId,
            is_completed = false
        )

        Log.d("TambahTugas", "SubTasks: $subTaskTitles")

        api.addPersonalSubTask(personalSubTask).enqueue(object : Callback<ResponseMessage> {
            override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                if (!response.isSuccessful) {
                    Toast.makeText(context, "Gagal menambahkan sub-tugas.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Toast.makeText(context, "Tugas dan subtugas berhasil ditambahkan.", Toast.LENGTH_SHORT).show()
    navController.navigate("personal_tasks")
}

package com.example.taskly

import android.widget.Toast
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
import com.example.taskly.network.PersonalSubTask
import com.example.taskly.network.ResponseMessage
import com.example.taskly.network.RetrofitInstance
import com.example.taskly.network.TaskStatus
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun ProgressTugasPribadiScreen(navController: NavController, taskId: Int) {
    val context = LocalContext.current
    val subTasks = remember { mutableStateListOf<PersonalSubTask>() }
    var isSaving by remember { mutableStateOf(false) }

    // Fetch subtasks from backend
    LaunchedEffect(taskId) {
        val api = RetrofitInstance.getInstance().create(ApiService::class.java)
        api.getPersonalSubTasks(taskId).enqueue(object : Callback<List<PersonalSubTask>> {
            override fun onResponse(
                call: Call<List<PersonalSubTask>>,
                response: Response<List<PersonalSubTask>>
            ) {
                if (response.isSuccessful) {
                    subTasks.clear()
                    response.body()?.let { fetchedSubTasks ->
                        subTasks.addAll(fetchedSubTasks)
                    }
                } else {
                    Toast.makeText(context, "Gagal memuat subtugas.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<PersonalSubTask>>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Progress Sub-Tugas Pribadi",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(subTasks) { subTask ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        subTask.nama_sub_tugas,
                        modifier = Modifier.weight(1f)
                    )
                    Checkbox(
                        checked = subTask.is_completed,
                        onCheckedChange = { isChecked ->
                            // Update the `is_completed` state locally
                            val index = subTasks.indexOf(subTask)
                            if (index != -1) {
                                subTasks[index] = subTask.copy(is_completed = isChecked)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(
            onClick = {
                isSaving = true
                val api = RetrofitInstance.getInstance().create(ApiService::class.java)

                // Update all subtasks
                subTasks.forEach { subTask ->
                    api.updatePersonalSubTaskStatus(subTask.id, TaskStatus(subTask.is_completed))
                        .enqueue(object : Callback<ResponseMessage> {
                            override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                                if (!response.isSuccessful) {
                                    Toast.makeText(context, "Gagal menyimpan subtask.", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                                Toast.makeText(context, "Gagal menyimpan subtask.", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                // Update task status if all subtasks are completed
                if (subTasks.all { it.is_completed }) {
                    api.updatePersonalTaskStatus(taskId, TaskStatus(true))
                        .enqueue(object : Callback<ResponseMessage> {
                            override fun onResponse(
                                call: Call<ResponseMessage>,
                                response: Response<ResponseMessage>
                            ) {
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "Tugas selesai!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Gagal memperbarui tugas.", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                                Toast.makeText(context, "Gagal memperbarui tugas.", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                isSaving = false
                navController.navigate("in_progress_personal") // Navigate to DalamProgressPribadiScreen
            },
            enabled = !isSaving
        ) {
            Text(if (isSaving) "Menyimpan..." else "Simpan Perubahan")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("home") }) { // Navigate back to Home
            Text("Kembali")
        }
    }
}

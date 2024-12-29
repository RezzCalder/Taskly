package com.example.taskly

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taskly.network.ApiService
import com.example.taskly.network.GroupTask
import com.example.taskly.network.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DalamProgressKelompokScreen(navController: NavController) {
    // State untuk daftar tugas dan dialog
    val tasks = remember { mutableStateListOf<GroupTask>() }
    val showDialog = remember { mutableStateOf(false) }
    val selectedTask = remember { mutableStateOf<GroupTask?>(null) }
    val context = LocalContext.current

    // Mendapatkan userId dari SharedPreferences
    val userId = context.getSharedPreferences("TasklyPrefs", Context.MODE_PRIVATE).getInt("userId", -1)
    if (userId == -1) {
        Toast.makeText(context, "User ID tidak ditemukan. Harap login kembali.", Toast.LENGTH_SHORT).show()
        return
    }

    // Mendapatkan tugas dari server
    LaunchedEffect(Unit) {
        val api = RetrofitInstance.getInstance().create(ApiService::class.java)
        api.getGroupTasksInProgress(userId).enqueue(object : Callback<List<GroupTask>> {
            override fun onResponse(call: Call<List<GroupTask>>, response: Response<List<GroupTask>>) {
                if (response.isSuccessful) {
                    tasks.clear()
                    tasks.addAll(response.body() ?: emptyList())
                } else {
                    Toast.makeText(context, "Gagal memuat tugas.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<GroupTask>>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Tugas Kelompok\nDalam Progress",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tasks) { task ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            selectedTask.value = task
                            showDialog.value = true
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = task.nama_tugas,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog untuk memilih aksi pada tugas
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = {
                Text(
                    "Pilih Aksi",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    "Apa yang ingin Anda lakukan untuk tugas ini?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog.value = false
                        val taskId = selectedTask.value?.id
                        if (taskId != null) {
                            navController.navigate("progress_group_task/$taskId")
                        } else {
                            Toast.makeText(context, "Task ID tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(
                        "Ubah Progress",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog.value = false
                        val taskId = selectedTask.value?.id
                        if (taskId != null) {
                            navController.navigate("edit_group_task_members/$taskId")
                        } else {
                            Toast.makeText(context, "Task ID tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(
                        "Ubah Anggota",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        )
    }
}
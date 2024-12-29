package com.example.taskly

import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taskly.network.ApiService
import com.example.taskly.network.PersonalTask
import com.example.taskly.network.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Date

@Composable
fun TugasPribadiScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("TasklyPrefs", Context.MODE_PRIVATE) }
    val userId = sharedPreferences.getInt("userId", -1)

    if (userId == -1) {
        // Handle error jika userId tidak ditemukan
        Text("User ID tidak ditemukan. Harap login kembali.")
        return
    }

    val currentDate = remember { Date() }
    val formattedDate = DateFormat.format("EEEE, dd MMMM", currentDate).toString()

    val completedTasks = remember { mutableStateListOf<PersonalTask>() }
    val inProgressTasks = remember { mutableStateListOf<PersonalTask>() }

    LaunchedEffect(userId) {
        val api = RetrofitInstance.getInstance().create(ApiService::class.java)
        api.getPersonalTasks(userId).enqueue(object : Callback<List<PersonalTask>> {
            override fun onResponse(call: Call<List<PersonalTask>>, response: Response<List<PersonalTask>>) {
                if (response.isSuccessful) {
                    val tasks = response.body()
                    inProgressTasks.clear()
                    inProgressTasks.addAll(tasks?.filter { !it.is_completed } ?: emptyList())

                    completedTasks.clear()
                    completedTasks.addAll(tasks?.filter { it.is_completed } ?: emptyList())
                } else {
                    Toast.makeText(context, "Gagal memuat data: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<PersonalTask>>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = Color.Gray)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tugas Pribadi\nStatus Anda 📋",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black
            ),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(24.dp))

        TaskCategoryCard(
            title = "Selesai",
            count = completedTasks.size,
            onClick = { navController.navigate("completed_personal") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TaskCategoryCard(
            title = "Dalam Progress",
            count = inProgressTasks.size,
            onClick = { navController.navigate("in_progress_personal") }
        )
        Spacer(modifier = Modifier.weight(1f))

        FloatingActionButton(
            onClick = { navController.navigate("add_personal_task") },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Personal Task",
                tint = Color.White
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.navigateUp() }
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Go Back",
                    tint = Color.Gray
                )
            }

            IconButton(
                onClick = { navController.navigate("profile") }
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Go to Profile",
                    tint = Color.Gray
                )
            }
        }
    }
}

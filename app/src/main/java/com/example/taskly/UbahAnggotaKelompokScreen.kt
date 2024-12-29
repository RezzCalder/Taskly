package com.example.taskly

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.taskly.network.ApiService
import com.example.taskly.network.GroupMember
import com.example.taskly.network.GroupMembers
import com.example.taskly.network.ResponseMessage
import com.example.taskly.network.RetrofitInstance
import com.example.taskly.network.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UbahAnggotaKelompokScreen(navController: NavController, taskId: Int) {
    val context = LocalContext.current
    val allUsers = remember { mutableStateListOf<User>() }
    val currentMembers = remember { mutableStateListOf<User>() }
    val dropdownExpanded = remember { mutableStateOf(false) }
    val selectedUser = remember { mutableStateOf<User?>(null) }

    // Load daftar user dan anggota grup
    LaunchedEffect(Unit) {
        val api = RetrofitInstance.getInstance().create(ApiService::class.java)

        // Step 1: Fetch all users
        api.getUsers().enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (response.isSuccessful) {
                    allUsers.clear()
                    allUsers.addAll(response.body() ?: emptyList())

                    // Step 2: Fetch current group members after users are loaded
                    api.getGroupMembers(taskId).enqueue(object : Callback<List<GroupMember>> {
                        override fun onResponse(
                            call: Call<List<GroupMember>>,
                            response: Response<List<GroupMember>>
                        ) {
                            if (response.isSuccessful) {
                                // Hapus anggota lama terlebih dahulu
                                currentMembers.clear()

                                // Log anggota yang diambil
                                println("Members fetched for taskId $taskId: ${response.body()}")

                                // Dapatkan daftar anggota dari response
                                val groupMembers = response.body() ?: emptyList()

                                // Cari user yang sesuai dengan member
                                groupMembers.forEach { member ->
                                    allUsers.find { it.id == member.user_id }?.let { user ->
                                        currentMembers.add(user)
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Gagal memuat anggota grup: ${response.errorBody()?.string()}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onFailure(call: Call<List<GroupMember>>, t: Throwable) {
                            Toast.makeText(
                                context,
                                "Error memuat anggota: ${t.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                } else {
                    Toast.makeText(context, "Gagal memuat daftar pengguna.", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ubah Anggota Kelompok") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Dropdown untuk memilih anggota baru
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text(
                        "Tambah Anggota Baru",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { dropdownExpanded.value = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(selectedUser.value?.username ?: "Pilih Anggota")
                        }
                        Button(
                            onClick = {
                                selectedUser.value?.let {
                                    if (!currentMembers.contains(it)) {
                                        currentMembers.add(it)
                                        selectedUser.value = null
                                    }
                                }
                            },
                            enabled = selectedUser.value != null
                        ) {
                            Text("Tambah")
                        }
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded.value,
                        onDismissRequest = { dropdownExpanded.value = false }
                    ) {
                        allUsers.filter { user -> currentMembers.none { it.id == user.id } }
                            .forEach { user ->
                                DropdownMenuItem(
                                    text = { Text(user.username) },
                                    onClick = {
                                        selectedUser.value = user
                                        dropdownExpanded.value = false
                                    }
                                )
                            }
                    }
                }
            }

            // Daftar anggota saat ini
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    Text(
                        "Anggota Saat Ini (${currentMembers.size} orang)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (currentMembers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Belum ada anggota dalam kelompok ini",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentMembers) { member ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                member.username,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                "ID: ${member.id}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = { currentMembers.remove(member) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus anggota",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tombol simpan
            Button(
                onClick = {
                    val api = RetrofitInstance.getInstance().create(ApiService::class.java)
                    val memberIds = currentMembers.map { it.id }
                    api.updateGroupMembers(taskId, GroupMembers(memberIds))
                        .enqueue(object : Callback<ResponseMessage> {
                            override fun onResponse(
                                call: Call<ResponseMessage>,
                                response: Response<ResponseMessage>
                            ) {
                                if (response.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "Anggota berhasil diperbarui.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.navigate("in_progress_group") {
                                        popUpTo("in_progress_group") { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Gagal memperbarui anggota.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                                Toast.makeText(
                                    context,
                                    "Error: ${t.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text("Simpan Perubahan")
            }
        }
    }
}
package com.example.taskly

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taskly.network.RetrofitInstance
import com.example.taskly.network.TokenUpdate
import com.example.taskly.ui.theme.TasklyTheme
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inisialisasi Firebase dan update token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val sharedPreferences = getSharedPreferences("TasklyPrefs", MODE_PRIVATE)
                val userId = sharedPreferences.getInt("userId", -1)
                println("FCM Token: $token")
                if (userId != -1) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response = RetrofitInstance.ApiServiceFactory.apiService.updateToken(TokenUpdate(userId, token))
                            if (!response.isSuccessful) {
                                Log.e("MainActivity", "Gagal memperbarui token: ${response.errorBody()?.string()}")
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error saat memperbarui token", e)
                        }
                    }
                }
            }
        }
        // Memeriksa izin notifikasi
        checkNotificationPermission()

        // Buat Notification Channels
        createNotificationChannels(this)

        setContent {
            TasklyTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "login") {
                    composable("login") { LoginScreen(navController) }
                    composable("register") { RegisterScreen(navController) }
                    composable("home") { HomeScreen(navController) }
                    composable("personal_tasks") { TugasPribadiScreen(navController) }
                    composable("group_tasks") { TugasKelompokScreen(navController) }
                    composable("add_personal_task") { TambahTugasPribadiScreen(navController) }
                    composable("add_group_task") { TambahTugasKelompokScreen(navController) }
                    composable("profile") { ProfilScreen(navController) }
                    composable("in_progress_personal") { DalamProgressPribadiScreen(navController) }
                    composable("in_progress_group") { DalamProgressKelompokScreen(navController) }
                    composable("completed_personal") { SelesaiPribadiScreen(navController) }
                    composable("completed_group") { SelesaiKelompokScreen(navController) }
                    composable("progress_personal_task/{taskId}") { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull() ?: 0
                        ProgressTugasPribadiScreen(navController, taskId)
                    }
                    composable("progress_group_task/{taskId}") { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull() ?: 0
                        ProgressTugasKelompokScreen(navController, taskId)
                    }
                    composable("edit_group_task_members/{taskId}") { backStackEntry ->
                        val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull() ?: 0
                        UbahAnggotaKelompokScreen(navController, taskId)
                    }
                    composable("notification_settings") { NotificationSettingsScreen(navController) }
                }
            }
        }
    }
    // Fungsi untuk memeriksa izin notifikasi
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100 // Request code
                )
            }
        }
    }

    // Menangani hasil permintaan izin
    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) { // Request code
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("MainActivity", "Izin notifikasi diberikan.")
            } else {
                Log.e("MainActivity", "Izin notifikasi ditolak.")
                // Buka pengaturan untuk mengaktifkan notifikasi secara manual
                openNotificationSettings()
            }
        }
    }

    // Membuka pengaturan notifikasi aplikasi
    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    // Metode untuk membuat Notification Channels
    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel("personaltask_added", "Tugas Pribadi Baru", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel("grouptask_added", "Tugas Kelompok Baru", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel("deadline_personal", "Pengingat Deadline Pribadi", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel("deadline_group", "Pengingat Deadline Kelompok", NotificationManager.IMPORTANCE_HIGH),
            )

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            channels.forEach { notificationManager?.createNotificationChannel(it) }
        }
    }
}

package com.example.taskly.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "http://10.0.2.2:3001/" // Emulator address to localhost

    fun getInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    object ApiServiceFactory {
        val apiService: ApiService by lazy {
            getInstance().create(ApiService::class.java)
        }
    }
}

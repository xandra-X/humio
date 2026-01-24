package com.example.hrmanagement.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logger)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val profileApi: ProfileApi by lazy {
        retrofit.create(ProfileApi::class.java)
    }
    fun getImageUrl(path: String): String {
        return BASE_URL.trimEnd('/') + path
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}

package com.sena.monitoreo.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.20.115:5000/"

    // Retrofit general (reutilizable)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // API de autenticación (ya existente)
    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    // ✅ NUEVO: API de usuarios
    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }
}

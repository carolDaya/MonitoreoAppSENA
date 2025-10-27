package com.sena.monitoreo.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.sena.monitoreo.data.api.ApiService // Para apiAuth
import com.sena.monitoreo.data.api.ApiGraficas // Para apiGraficas
import com.sena.monitoreo.data.api.ApiSensor
import com.sena.monitoreo.data.api.ApiLectura


object RetrofitClient {
    private const val BASE_URL_AUTH = "http://10.0.2.2:5000/auth/"
    private const val BASE_URL_API = "http://10.0.2.2:5000/api/"

    // Cliente para AUTH
    val apiAuth: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_AUTH)
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

    // Cliente para GRAFICAS
    val apiGraficas: ApiGraficas by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_API)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiGraficas::class.java)
    }

    // Cliente para SENSORES
    val apiSensores: ApiSensor by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_API)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiSensor::class.java)
    }
    val apiLecturas: ApiLectura by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_API)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiLectura::class.java)
    }
}

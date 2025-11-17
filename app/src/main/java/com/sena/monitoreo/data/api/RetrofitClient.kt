package com.sena.monitoreo.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL_AUTH = "http://200.234.235.149:5000/auth/"
    private const val BASE_URL_API = "http://200.234.235.149:5000/api/"

    // Instancia compartida de Retrofit para rutas API (no-auth)
    private val retrofitApi: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_API)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Instancia compartida de Retrofit para rutas AUTH
    private val retrofitAuth: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_AUTH)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Cliente para AUTH
    val apiAuth: ApiService by lazy {
        retrofitAuth.create(ApiService::class.java)
    }

    // Cliente para GRAFICAS
    val apiGraficas: ApiGraficas by lazy {
        retrofitApi.create(ApiGraficas::class.java)
    }

    // Cliente para SENSORES
    val apiSensores: ApiSensor by lazy {
        retrofitApi.create(ApiSensor::class.java)
    }

    // Cliente para LECTURAS
    val apiLecturas: ApiLectura by lazy {
        retrofitApi.create(ApiLectura::class.java)
    }

    val apiProceso: ApiProceso by lazy {
        retrofitApi.create(ApiProceso::class.java)
    }

    // Cliente para USUARIOS
    val apiUser: ApiUser by lazy {
        retrofitApi.create(ApiUser::class.java)
    }

    val apiAi: ApiAiService by lazy {
        retrofitApi.create(ApiAiService::class.java)
    }

    val apiVoice: ApiVoice by lazy {
        retrofitApi.create(ApiVoice::class.java)
    }
}
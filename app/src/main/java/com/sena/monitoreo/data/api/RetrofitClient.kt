package com.sena.monitoreo.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Asegúrate de que todas estas interfaces de API estén definidas en tu paquete 'com.sena.monitoreo.data.api'
import com.sena.monitoreo.data.api.ApiService
import com.sena.monitoreo.data.api.ApiGraficas
import com.sena.monitoreo.data.api.ApiSensor
import com.sena.monitoreo.data.api.ApiLectura
import com.sena.monitoreo.data.api.ApiUser // ¡Importante para la gestión de Usuarios!


object RetrofitClient {
    private const val BASE_URL_AUTH = "http://10.0.2.2:5000/auth/"
    // Asumimos que las rutas de /users/ están bajo /api/ para usar una única URL base para los clientes no-Auth.
    private const val BASE_URL_API = "http://10.0.2.2:5000/api/"

    // Cliente para AUTH
    val apiAuth: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_AUTH)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
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

    // Cliente para LECTURAS
    val apiLecturas: ApiLectura by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_API)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiLectura::class.java)
    }

    // Cliente para USUARIOS (Nuevo, basado en tu implementación anterior)
    val apiUsers: ApiUser by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_API) // Revisa si tu Flask está registrando users_bp sin prefijo. Si es así, esta URL Base debe ser "http://10.0.2.2:5000/"
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiUser::class.java)
    }
}
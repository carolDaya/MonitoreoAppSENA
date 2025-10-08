package com.sena.monitoreo.data.repository


import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.LoginRequest
import com.sena.monitoreo.data.model.LoginResponse
import com.sena.monitoreo.data.model.RegisterRequest
import com.sena.monitoreo.data.model.RegisterResponse
import retrofit2.Response

class AuthRepository {

    // ✅ CORRECCIÓN: Llamamos a Retrofit y ejecutamos la llamada de forma síncrona con .execute()
    fun register(request: RegisterRequest): Response<RegisterResponse> {
        // Ejecuta la llamada y espera la respuesta Response<T>
        return RetrofitClient.api.register(request).execute()
    }

    // ✅ CORRECCIÓN: Llamamos a Retrofit y ejecutamos la llamada de forma síncrona con .execute()
    fun login(request: LoginRequest): Response<LoginResponse> {
        // Ejecuta la llamada y espera la respuesta Response<T>
        return RetrofitClient.api.login(request).execute()
    }
}
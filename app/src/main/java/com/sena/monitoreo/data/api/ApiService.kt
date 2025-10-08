package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.LoginRequest
import com.sena.monitoreo.data.model.LoginResponse
import com.sena.monitoreo.data.model.RegisterRequest
import com.sena.monitoreo.data.model.RegisterResponse
import retrofit2.Call // ⭐ ESTO ES LO CORRECTO PARA LA INTERFAZ ⭐
import retrofit2.http.Body
import retrofit2.http.POST

// Asumo que estas clases deben estar correctamente definidas en su respectivo paquete o aquí
class ResetPasswordRequest()
class ForgotPasswordRequest()

// Define la interfaz de Retrofit
interface ApiService {

    @POST("auth/forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<Void>

    @POST("auth/reset-password")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<Void>

    // ✅ CORRECCIÓN FINAL: Se usa Call<T>
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    // ✅ CORRECCIÓN FINAL: Se usa Call<T>
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}
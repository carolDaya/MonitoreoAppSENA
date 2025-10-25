package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.LoginRequest
import com.sena.monitoreo.data.model.LoginResponse
import com.sena.monitoreo.data.model.RegisterRequest
import com.sena.monitoreo.data.model.RegisterResponse
import com.sena.monitoreo.data.model.UserResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// 📦 Clases placeholder para los endpoints de recuperación de contraseña
class ResetPasswordRequest()
class ForgotPasswordRequest()

// ✅ Interfaz principal de Retrofit
interface ApiService {

    // 🔐 Autenticación
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("auth/forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<Void>

    @POST("auth/reset-password")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<Void>

    // 👥 Usuarios
    @GET("users")
    fun getAllUsers(): Call<List<UserResponse>>

    @GET("users/activos")
    fun getActiveUsers(): Call<List<UserResponse>>

    @GET("users/bloqueados")
    fun getBlockedUsers(): Call<List<UserResponse>>
}

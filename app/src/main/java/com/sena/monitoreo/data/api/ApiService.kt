package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.auth.LoginRequest
import com.sena.monitoreo.data.model.auth.LoginResponse
import com.sena.monitoreo.data.model.auth.RegisterRequest
import com.sena.monitoreo.data.model.auth.RegisterResponse
import com.sena.monitoreo.data.model.auth.MessageResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Registro de un nuevo usuario
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    // Inicio de sesión y obtención de token
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Solicitar correo para restablecer contraseña
    @POST("password/reset-request")
    suspend fun resetPasswordRequest(@Body data: Map<String, String>): Response<MessageResponse>

    // Actualizar la contraseña del usuario
    @PATCH("password")
    suspend fun updatePassword(@Body data: Map<String, String>): Response<MessageResponse>
}

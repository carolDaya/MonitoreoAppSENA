package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.auth.LoginRequest
import com.sena.monitoreo.data.model.auth.LoginResponse
import com.sena.monitoreo.data.model.auth.RegisterRequest
import com.sena.monitoreo.data.model.auth.RegisterResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    @POST("password/reset-request")
    suspend fun resetPasswordRequest(@Body data: Map<String, String>): Response<Map<String, Any>>

    @PATCH("password")
    suspend fun updatePassword(@Body data: Map<String, String>): Response<Map<String, Any>>

}

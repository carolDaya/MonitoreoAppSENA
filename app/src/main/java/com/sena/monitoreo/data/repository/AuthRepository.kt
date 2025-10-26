package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.auth.LoginRequest
import com.sena.monitoreo.data.model.auth.LoginResponse
import com.sena.monitoreo.data.model.auth.RegisterRequest
import com.sena.monitoreo.data.model.auth.RegisterResponse
import retrofit2.Response

class AuthRepository {
    suspend fun register(request: RegisterRequest): Response<RegisterResponse> {
        return RetrofitClient.apiAuth.register(request)
    }

    suspend fun login(request: LoginRequest): Response<LoginResponse> {
        return RetrofitClient.apiAuth.login(request)
    }
}

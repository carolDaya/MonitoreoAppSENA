package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.model.auth.*
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.utils.ApiResult
import com.sena.monitoreo.utils.parseErrorMessage

class AuthRepository {

    suspend fun register(request: RegisterRequest): ApiResult<RegisterResponse> {
        return try {
            val response = RetrofitClient.apiAuth.register(request)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.parseErrorMessage())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Error desconocido")
        }
    }

    suspend fun login(request: LoginRequest): ApiResult<LoginResponse> {
        return try {
            val response = RetrofitClient.apiAuth.login(request)
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(response.parseErrorMessage())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Error desconocido")
        }
    }
}


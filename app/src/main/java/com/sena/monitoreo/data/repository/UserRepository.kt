package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.user.UpdateEstadoRequest
import com.sena.monitoreo.data.model.user.UserResponse
import retrofit2.Response

class UserRepository {

    suspend fun getAllUsers(): List<UserResponse>? {
        val response = RetrofitClient.apiUser.getAllUsers()
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }

    suspend fun getActiveUsers(): List<UserResponse>? {
        val response = RetrofitClient.apiUser.getActiveUsers()
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }

    suspend fun getBlockedUsers(): List<UserResponse>? {
        val response = RetrofitClient.apiUser.getBlockedUsers()
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }

    suspend fun updateEstado(userId: Int, nuevoEstado: String): Boolean {
        return try {
            val response = RetrofitClient.apiUser.updateEstado(userId, UpdateEstadoRequest(nuevoEstado))
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}

package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.ApiUser
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.user.UpdateEstadoRequest
import com.sena.monitoreo.data.model.user.UserResponse
import com.sena.monitoreo.utils.ResultWrapper
import com.sena.monitoreo.utils.safeApiCall

/**
 * Repositorio para la gestión de usuarios
 */
class UserRepository(
    private val apiUser: ApiUser = RetrofitClient.apiUser
) {

    suspend fun getAllUsers(): ResultWrapper<List<UserResponse>> {
        return safeApiCall {
            apiUser.getAllUsers()
        }
    }

    suspend fun getActiveUsers(): ResultWrapper<List<UserResponse>> {
        return safeApiCall {
            apiUser.getActiveUsers()
        }
    }

    suspend fun getBlockedUsers(): ResultWrapper<List<UserResponse>> {
        return safeApiCall {
            apiUser.getBlockedUsers()
        }
    }

    /**
     * Actualiza el estado de un usuario.
     */
    suspend fun updateEstado(userId: Int, nuevoEstado: String): ResultWrapper<Unit> {
        val request = UpdateEstadoRequest(nuevoEstado)
        return safeApiCall {
            apiUser.updateEstado(userId, request)
        }
    }
}
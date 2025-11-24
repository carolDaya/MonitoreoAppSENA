package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.ApiService
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.auth.LoginRequest
import com.sena.monitoreo.data.model.auth.LoginResponse
import com.sena.monitoreo.data.model.auth.MessageResponse
import com.sena.monitoreo.data.model.auth.RegisterRequest
import com.sena.monitoreo.data.model.auth.RegisterResponse
import com.sena.monitoreo.utils.ResultWrapper
import com.sena.monitoreo.utils.safeApiCall

/**
 * Repositorio para la gestión de autenticación (Login, Registro y Contraseña).
 */
class AuthRepository(
    private val apiAuth: ApiService = RetrofitClient.apiAuth
) {


    suspend fun register(request: RegisterRequest): ResultWrapper<RegisterResponse> {
        return safeApiCall {
            apiAuth.register(request)
        }
    }

    suspend fun login(request: LoginRequest): ResultWrapper<LoginResponse> {
        return safeApiCall {
            apiAuth.login(request)
        }
    }


    /**
     * Solicita la verificación de existencia de teléfono para restablecimiento de contraseña.
     * Retorna MessageResponse.
     */
    suspend fun requestPasswordReset(telefono: String): ResultWrapper<MessageResponse> {
        val data = mapOf("telefono" to telefono)
        return safeApiCall {
            apiAuth.resetPasswordRequest(data)
        }
    }

    /**
     * Actualiza la contraseña del usuario.
     */
    suspend fun updatePassword(
        telefono: String,
        nuevaContrasena: String,
        confirmarContrasena: String
    ): ResultWrapper<MessageResponse> {
        val data = mapOf(
            "telefono" to telefono,
            "nueva_contrasena" to nuevaContrasena,
            "confirmar_contrasena" to confirmarContrasena
        )
        return safeApiCall {
            apiAuth.updatePassword(data)
        }
    }
}
package com.sena.monitoreo.utils

import com.google.gson.Gson
import com.sena.monitoreo.data.model.auth.ErrorResponse
import retrofit2.Response

fun Response<*>.parseErrorMessage(): String {
    // Es mejor usar .string() si solo se lee una vez, pero .charStream() también funciona.
    val errorBodyString = this.errorBody()?.string()
        ?: return "Error de servidor (cuerpo vacío)"

    return try {
        // Deserializamos el cuerpo de error en el modelo ErrorResponse
        val errorResponse = Gson().fromJson(errorBodyString, ErrorResponse::class.java)

        // 1. PRIORIZAR el campo 'message' ("Credenciales incorrectas")
        // 2. Usar el campo 'error' ("AuthenticationException") como respaldo
        errorResponse?.message
            ?: errorResponse?.error
            ?: "Error inesperado (código: ${this.code()})"

    } catch (e: Exception) {
        // Fallback si el cuerpo no es un JSON válido
        "Error al procesar la respuesta del servidor."
    }
}
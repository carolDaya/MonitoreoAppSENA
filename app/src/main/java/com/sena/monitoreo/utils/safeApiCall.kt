package com.sena.monitoreo.utils

import com.google.gson.Gson
import com.sena.monitoreo.data.exception.ApiException
import com.sena.monitoreo.data.model.auth.ErrorResponse
import retrofit2.Response
import java.io.IOException

/**
 * Función de extensión genérica que maneja la respuesta cruda de Retrofit (Response<T>)
 */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): ResultWrapper<T> {
    return try {
        val response = call()

        if (response.isSuccessful) {
            val body = response.body()
            @Suppress("UNCHECKED_CAST")
            return ResultWrapper.Success(body as T)

        } else {
            val errorBodyString = response.errorBody()?.string()
            var finalMessage = "Error del servidor (código: ${response.code()})"

            try {
                if (!errorBodyString.isNullOrBlank()) {
                    val gson = Gson()
                    val errorData = gson.fromJson(errorBodyString, ErrorResponse::class.java)

                    finalMessage = errorData.message
                        ?: errorData.detalle
                                ?: errorData.error
                                ?: finalMessage
                }
            } catch (e: Exception) { }

            ResultWrapper.Error(
                exception = ApiException.ApiError(finalMessage),
                message = finalMessage
            )
        }
    } catch (e: IOException) {
        val message = "Error de red: No se pudo conectar al servidor. Verifique su conexión."
        ResultWrapper.Error(
            exception = ApiException.NetworkError(message),
            message = message
        )
    } catch (e: Exception) {
        val message = "Error inesperado: ${e.message ?: "Desconocido"}"
        ResultWrapper.Error(
            exception = ApiException.UnknownError(message),
            message = message
        )
    }
}
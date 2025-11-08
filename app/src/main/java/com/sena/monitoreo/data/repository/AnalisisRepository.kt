package com.sena.monitoreo.data.repository

import com.google.gson.Gson
import com.sena.monitoreo.data.api.ApiAiService
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.ai.AnalisisResponse
import com.sena.monitoreo.data.model.base.ApiErrorResponse
import retrofit2.Response

/**
 * Repositorio para la lógica de análisis de IA.
 * Utiliza un AnalisisResult para devolver el éxito o un mensaje de error controlado.
 */
class AnalisisRepository(private val apiAiService: ApiAiService = RetrofitClient.apiAi) {

    /**
     * Clase de datos sellada para encapsular el resultado de la operación: éxito con datos, o error con mensaje.
     */
    data class AnalisisResult(
        val success: AnalisisResponse? = null,
        val errorMessage: String? = null
    )

    /**
     * Realiza la llamada a la API de análisis de IA y maneja las respuestas HTTP,
     * incluyendo la deserialización de errores 400 Bad Request.
     */
    suspend fun analizarLectura(): AnalisisResult {
        return try {
            val response: Response<AnalisisResponse> = apiAiService.analizarDatos()

            if (response.isSuccessful) {
                // ÉXITO (200 OK)
                AnalisisResult(success = response.body())
            } else {
                // ERROR (4xx, 5xx) - Intentar obtener el cuerpo del error
                val errorBodyString = response.errorBody()?.string()

                // Intenta deserializar el error JSON en nuestro modelo ApiErrorResponse
                val gson = Gson()
                val errorData = try {
                    gson.fromJson(errorBodyString, ApiErrorResponse::class.java)
                } catch (e: Exception) {
                    // Si falla la deserialización, es un error no JSON o inesperado.
                    ApiErrorResponse(error = "Error ${response.code()}: Respuesta de error inesperada.")
                }

                // Usamos el mensaje de error del JSON (que contiene 1001/1002) o un mensaje genérico
                val finalError = errorData.error ?: "Error desconocido del servidor (${response.code()})."
                AnalisisResult(errorMessage = finalError)
            }
        } catch (e: Exception) {
            // ERROR DE CONEXIÓN O EXCEPCIÓN DE KOTLIN/JAVA
            AnalisisResult(errorMessage = "Error de conexión: ${e.message}. Verifique la red y el servidor.")
        }
    }
}
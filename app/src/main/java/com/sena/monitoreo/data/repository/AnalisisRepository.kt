package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.ApiAiService
import com.sena.monitoreo.data.model.ai.AnalisisResponse
import com.sena.monitoreo.utils.ResultWrapper
import com.sena.monitoreo.utils.safeApiCall
import com.sena.monitoreo.data.api.RetrofitClient

/**
 * Repositorio para la lógica de análisis de IA.
 */
class AnalisisRepository(
    private val apiAiService: ApiAiService = RetrofitClient.apiAi
) {

    /**
     * Realiza la llamada a la API de análisis de IA.
     * Retorna un ResultWrapper que gestiona Success y Error de forma tipada.
     */
    suspend fun analizarLectura(): ResultWrapper<AnalisisResponse> {
        // Usa la función safeApiCall para manejar la respuesta de la API de forma segura.
        return safeApiCall {
            apiAiService.analizarDatos()
        }
    }
}
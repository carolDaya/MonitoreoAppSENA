package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.ApiGraficas
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.admin.GraficaResponse
import com.sena.monitoreo.data.model.admin.GraficaUpdateRequest
import com.sena.monitoreo.data.model.admin.GraficaUpdateResponse
import com.sena.monitoreo.utils.ResultWrapper
import com.sena.monitoreo.utils.safeApiCall

/**
 * Repositorio para la configuración de gráficas.
 */
class GraficasRepository(
    // Inyección de dependencia a través del constructor
    private val apiGraficas: ApiGraficas = RetrofitClient.apiGraficas
) {
    /**
     * Actualiza la configuración de una gráfica.
     * Retorna ResultWrapper<GraficaUpdateResponse> para tipar el resultado.
     */
    suspend fun updateGrafica(sensorId: Int, tipo: String): ResultWrapper<GraficaUpdateResponse> {
        val requestBody = GraficaUpdateRequest(sensorId, tipo)

        return safeApiCall {
            apiGraficas.updateGrafica(requestBody)
        }
    }

    /**
     * Obtiene la lista completa de configuraciones de gráficas.
     * Retorna ResultWrapper<List<GraficaResponse>>.
     */
    suspend fun getGraficas(): ResultWrapper<List<GraficaResponse>> {
        return safeApiCall {
            apiGraficas.getGraficas()
        }
    }
}
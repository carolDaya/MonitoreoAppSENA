package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.ai.AnalisisResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET

interface ApiAiService {
    /**
     * Envía los últimos datos de los sensores al endpoint de IA para predecir alertas.
     * Endpoint: http://0.0.0.0:5000/api/analizar
     */
    @GET("analizar")
    suspend fun analizarDatos(): Response<AnalisisResponse>
}
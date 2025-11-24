package com.sena.monitoreo.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import com.sena.monitoreo.data.model.admin.*

interface ApiGraficas {

    @POST("graficas/update")
    suspend fun updateGrafica(
        @Body request: GraficaUpdateRequest
    ): Response<GraficaUpdateResponse>

    @GET("graficas")
    suspend fun getGraficas(): Response<List<GraficaResponse>>
}
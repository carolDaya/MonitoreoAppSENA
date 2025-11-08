package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.admin.ProcesoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
interface ApiProceso {
    @POST("/api/proceso/iniciar")
    suspend fun iniciarProceso(): Response<ProcesoResponse>

    @POST("/api/proceso/finalizar")
    suspend fun finalizarProceso(): Response<ProcesoResponse>

    @GET("/api/proceso/estado")
    suspend fun verificarEstadoProceso(): Response<ProcesoResponse> // <-- Debe existir
}
package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.admin.ProcesoResponse
import com.sena.monitoreo.data.model.admin.ProcesoDetalleResponse
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.GET

interface ApiProceso {

    @POST("proceso/iniciar")
    suspend fun iniciarProceso(): Response<ProcesoDetalleResponse>

    @POST("proceso/finalizar")
    suspend fun finalizarProceso(): Response<ProcesoDetalleResponse>

    @GET("proceso/estado")
    suspend fun verificarEstadoProceso(): Response<ProcesoResponse>
}
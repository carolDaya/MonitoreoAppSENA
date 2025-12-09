package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.admin.ProcesoResponse
import com.sena.monitoreo.data.model.admin.ProcesoDetalleResponse
import retrofit2.Response
import retrofit2.http.POST
import retrofit2.http.GET

/**
 * Define los endpoints para gestionar el ciclo de vida del proceso.
 */
interface ApiProceso {

    // Inicia un nuevo proceso de monitoreo
    @POST("proceso/iniciar")
    suspend fun iniciarProceso(): Response<ProcesoDetalleResponse>

    // Finaliza el proceso que está en ejecución
    @POST("proceso/finalizar")
    suspend fun finalizarProceso(): Response<ProcesoDetalleResponse>

    // Consulta el estado actual del proceso
    @GET("proceso/estado")
    suspend fun verificarEstadoProceso(): Response<ProcesoResponse>
}
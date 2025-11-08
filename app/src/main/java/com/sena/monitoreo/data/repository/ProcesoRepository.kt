package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.ApiProceso
import com.sena.monitoreo.data.api.ApiService
import com.sena.monitoreo.data.model.admin.ProcesoResponse
import retrofit2.Response
class ProcesoRepository(private val apiProceso: ApiProceso) {

    suspend fun iniciarProceso(): Response<ProcesoResponse> {
        return apiProceso.iniciarProceso()
    }

    suspend fun finalizarProceso(): Response<ProcesoResponse> {
        return apiProceso.finalizarProceso()
    }

    suspend fun verificarEstado(): Response<ProcesoResponse> {
        // Llama a la función del servicio de Retrofit
        return apiProceso.verificarEstadoProceso()
    }
}
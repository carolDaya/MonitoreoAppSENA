package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.ApiProceso
import com.sena.monitoreo.data.model.admin.ProcesoResponse
import com.sena.monitoreo.data.model.admin.ProcesoDetalleResponse
import com.sena.monitoreo.utils.ResultWrapper
import com.sena.monitoreo.utils.safeApiCall

/**
 * Repositorio para la gestión del estado del proceso.
 */
class ProcesoRepository(private val apiProceso: ApiProceso) {

    suspend fun iniciarProceso(): ResultWrapper<ProcesoDetalleResponse> {
        return safeApiCall {
            apiProceso.iniciarProceso()
        }
    }

    suspend fun finalizarProceso(): ResultWrapper<ProcesoDetalleResponse> {
        return safeApiCall {
            apiProceso.finalizarProceso()
        }
    }

    suspend fun verificarEstado(): ResultWrapper<ProcesoResponse> {
        return safeApiCall {
            apiProceso.verificarEstadoProceso()
        }
    }
}
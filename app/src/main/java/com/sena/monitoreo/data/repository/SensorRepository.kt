package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.ApiSensor
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.sensor.SensorResponse
import com.sena.monitoreo.utils.ResultWrapper
import com.sena.monitoreo.utils.safeApiCall

/**
 * Repositorio para la gestión de sensores.
 */
class SensorRepository(
    private val apiSensor: ApiSensor = RetrofitClient.apiSensores
) {

    /**
     * Obtiene la lista completa de sensores.
     * Retorna ResultWrapper<List<SensorResponse>>.
     */
    suspend fun getSensores(): ResultWrapper<List<SensorResponse>> {
        return safeApiCall {
            apiSensor.getSensores()
        }
    }

}
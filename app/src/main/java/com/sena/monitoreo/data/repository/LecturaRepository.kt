package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.ApiLectura
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.sensor.LecturaResponse
import com.sena.monitoreo.utils.ResultWrapper
import com.sena.monitoreo.utils.safeApiCall

/**
 * Repositorio para la gestión de lecturas de sensores.
 */
class LecturaRepository(
    private val apiLectura: ApiLectura = RetrofitClient.apiLecturas
) {
    /**
     * Obtiene las lecturas de un sensor.
     * @return ResultWrapper con la lista de lecturas o un objeto Error.
     */
    suspend fun getLecturas(sensorId: Int): ResultWrapper<List<LecturaResponse>> {
        return safeApiCall {
            apiLectura.getLecturasPorSensor(sensorId)
        }
    }
}
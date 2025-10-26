package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.sensor.LecturaResponse
import com.sena.monitoreo.data.model.sensor.SensorResponse

class SensorRepository {
    private val api = RetrofitClient.apiSensores

    suspend fun getSensores(): List<SensorResponse> {
        val response = api.getSensores()
        return if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
    }

    suspend fun getLecturas(sensorId: Int): List<LecturaResponse> {
        val response = api.getLecturas(sensorId)
        return if (response.isSuccessful) response.body() ?: emptyList() else emptyList()
    }
}

package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.sensor.LecturaResponse
import com.sena.monitoreo.data.model.sensor.SensorResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiSensor {
    @GET("sensors")
    suspend fun getSensores(): Response<List<SensorResponse>>

    @GET("lecturas/{sensor_id}")
    suspend fun getLecturas(@Path("sensor_id") sensorId: Int): Response<List<LecturaResponse>>
}

// Archivo: com.sena.monitoreo.data.api.LecturaApi.kt

package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.sensor.LecturaResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiLectura {
    // Mapea a tu endpoint de Flask: /api/lecturas/<sensor_id>
    @GET("lecturas/{sensor_id}")
    suspend fun getLecturasPorSensor(@Path("sensor_id") sensorId: Int): Response<List<LecturaResponse>>
}
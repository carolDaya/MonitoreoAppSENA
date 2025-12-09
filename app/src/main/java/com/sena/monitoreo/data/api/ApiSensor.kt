package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.sensor.LecturaResponse
import com.sena.monitoreo.data.model.sensor.SensorResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

// Interfaz que define los endpoints relacionados con sensores.
// Retrofit genera automáticamente la implementación de esta interfaz.
interface ApiSensor {

    // Endpoint para obtener la lista completa de sensores disponibles.
    // Response<List<SensorResponse>> → envuelve la respuesta HTTP y la lista de sensores.
    @GET("sensors")
    suspend fun getSensores(): Response<List<SensorResponse>>

}

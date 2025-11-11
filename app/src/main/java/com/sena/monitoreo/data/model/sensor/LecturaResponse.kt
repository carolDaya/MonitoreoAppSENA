package com.sena.monitoreo.data.model.sensor

import com.google.gson.annotations.SerializedName

data class LecturaResponse(
    val id: Int,
    @SerializedName("sensor_id") // 💡 Asegura que mapea el snake_case de Flask
    val sensorId: Int,
    @SerializedName("fecha_hora") // 💡 Asegura que mapea el snake_case de Flask
    val fechaHora: String,
    val valor: Double,
    val observaciones: String?
)
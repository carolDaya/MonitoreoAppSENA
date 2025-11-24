package com.sena.monitoreo.data.model.sensor

import com.google.gson.annotations.SerializedName

data class LecturaResponse(
    val id: Int,
    @SerializedName("sensor_id")
    val sensorId: Int,
    @SerializedName("fecha_hora")
    val fechaHora: String,
    val valor: Double,
    val observaciones: String?
)
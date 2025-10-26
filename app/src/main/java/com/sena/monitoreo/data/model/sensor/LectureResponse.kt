package com.sena.monitoreo.data.model.sensor

data class LecturaResponse(
    val id: Int,
    val sensorId: Int,
    val fechaHora: String,
    val valor: Double,
    val observaciones: String?
)

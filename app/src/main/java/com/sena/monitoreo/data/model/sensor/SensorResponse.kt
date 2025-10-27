package com.sena.monitoreo.data.model.sensor
data class SensorResponse(
    val id: Int,
    val nombre: String,
    val tipo: String,
    val unidad: String,
    val activo: Boolean
)
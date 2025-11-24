package com.sena.monitoreo.data.model.admin

import com.google.gson.annotations.SerializedName

/**
 * DTO para la respuesta detallada del proceso (usado en Iniciar/Finalizar)
 * que devuelve el backend con todos los campos de la base de datos.
 */
data class ProcesoDetalleResponse(
    val id: Int,
    val estado: String,
    // Aseguramos el mapeo de snake_case a camelCase para las fechas
    @SerializedName("fecha_inicio")
    val fechaInicio: String?,
    @SerializedName("fecha_fin")
    val fechaFin: String?,
    val mensaje: String?,
)
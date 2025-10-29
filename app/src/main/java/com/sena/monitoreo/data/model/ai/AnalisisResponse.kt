package com.sena.monitoreo.data.model.ai

data class AnalisisResponse(
    val alerta_ia: Int,
    val dia_proceso: Int,
    val mensaje_lectura: String,
    val recomendacion: String,
    val tipo_alerta_modelo: String,
    val tipo_estado: String
)
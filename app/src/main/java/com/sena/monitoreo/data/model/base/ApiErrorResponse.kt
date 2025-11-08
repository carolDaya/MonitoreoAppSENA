package com.sena.monitoreo.data.model.base

data class ApiErrorResponse(
    // Contiene el mensaje principal de error, como "El nuevo proceso no tiene datos..."
    val error: String? = null,
    // Un código numérico para identificar el tipo de error (ej. 1001, 1002)
    val codigo_error: Int? = null,
    // Detalle adicional para errores de servidor (código 500)
    val detalle: String? = null
)
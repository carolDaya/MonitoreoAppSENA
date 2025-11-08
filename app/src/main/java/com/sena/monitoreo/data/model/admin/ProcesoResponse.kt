package com.sena.monitoreo.data.model.admin

data class ProcesoResponse(
    val proceso_activo: Boolean, // <--- CAMPO CLAVE
    val mensaje: String?
)
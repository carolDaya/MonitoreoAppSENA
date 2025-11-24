package com.sena.monitoreo.data.model.auth

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("error")
    val error: String?, // Captura el error general (ej: "Usuario bloqueado")

    @SerializedName("message") // Captura el error de validación del servicio (ej: "Las contraseñas no coinciden")
    val message: String?,

    @SerializedName("detalle") // Captura detalles adicionales (ej: "Comuníquese con el administrador.")
    val detalle: String?
)
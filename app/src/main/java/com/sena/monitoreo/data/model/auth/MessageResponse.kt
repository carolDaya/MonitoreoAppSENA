package com.sena.monitoreo.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * DTO para las respuestas de restablecimiento de contraseña que devuelven solo un mensaje.
 */
data class MessageResponse(
    @SerializedName("mensaje") // Captura el mensaje de éxito o info (ej: "Contraseña actualizada...")
    val mensaje: String,
    val telefono: String? = null // Incluido en el /reset-request
)
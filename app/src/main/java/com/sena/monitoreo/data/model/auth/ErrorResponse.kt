package com.sena.monitoreo.data.model.auth

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("error")
    val error: String, // Puedes mantenerlo para logging

    @SerializedName("message") // <--- ¡Asegúrate de agregar este campo!
    val message: String?
)
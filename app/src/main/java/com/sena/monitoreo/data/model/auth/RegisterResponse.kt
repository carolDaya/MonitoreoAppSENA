package com.sena.monitoreo.data.model.auth

import com.google.gson.annotations.SerializedName

data class RegisterResponse(
    val id: Int,
    val nombre: String,
    val telefono: String,
    val rol: String,
    val estado: String,
    val conectado: Boolean,

    @SerializedName("ultima_conexion")
    val ultimaConexion: String?,

    val message: String? // Mensaje de éxito del registro
)
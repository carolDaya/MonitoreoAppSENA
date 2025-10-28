package com.sena.monitoreo.data.model.user

data class UserResponse(
    val id: Int,
    val nombre: String,
    val telefono: String,
    val rol: String,
    val estado: String,
    val conectado: Boolean,
    val ultima_conexion: String?
)


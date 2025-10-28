package com.sena.monitoreo.data.model.user

// com.sena.monitoreo.data.model.UserResponse
data class UserResponse(
    val id: Int,
    val nombre: String,
    val telefono: String,
    val rol: String,
    val estado: String, // "activo" o "bloqueado"
    val conectado: Boolean,
    val ultima_conexion: String?
)
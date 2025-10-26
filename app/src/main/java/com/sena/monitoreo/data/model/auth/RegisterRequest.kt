package com.sena.monitoreo.data.model.auth

data class RegisterRequest(
    val nombre: String,
    val telefono: String,
    val password: String,
    val confirm_password: String
)
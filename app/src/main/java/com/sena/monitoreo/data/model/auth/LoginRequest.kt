package com.sena.monitoreo.data.model.auth

data class LoginRequest(
    val telefono: String,
    val password: String
)
package com.sena.monitoreo.data.model.auth

data class LoginResponse(
    val usuario: String,
    val rol: String,
    val token: String?
)
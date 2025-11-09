package com.sena.monitoreo.data.model.auth

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("usuario")
    val usuario: String,

    @SerializedName("rol")
    val rol: String,

    @SerializedName("token")
    val token: String?
)
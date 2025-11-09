package com.sena.monitoreo.data.model.auth

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("telefono")
    val telefono: String,

    @SerializedName("password")
    val password: String
)
package com.sena.monitoreo.data.model.auth

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("telefono")
    val telefono: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("confirm_password")
    val confirmPassword: String
)

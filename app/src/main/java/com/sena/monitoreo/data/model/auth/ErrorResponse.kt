package com.sena.monitoreo.data.model.auth

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("error")
    val error: String
)

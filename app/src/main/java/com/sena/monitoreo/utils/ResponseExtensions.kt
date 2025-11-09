package com.sena.monitoreo.utils

import com.google.gson.Gson
import com.sena.monitoreo.data.model.auth.ErrorResponse
import retrofit2.Response

fun Response<*>.parseErrorMessage(): String {
    return try {
        val errorBody = errorBody()?.charStream()
        val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
        errorResponse?.error ?: "Error desconocido"
    } catch (e: Exception) {
        "Error desconocido"
    }
}

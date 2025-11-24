package com.sena.monitoreo.utils

import com.sena.monitoreo.data.exception.ApiException

/**
 * Clase sellada universal para envolver resultados de la API.
 * T es el tipo de dato de éxito (e.g., AnalisisResponse).
 */
sealed class ResultWrapper<out T> {
    data class Success<T>(val data: T) : ResultWrapper<T>()

    /** * Representa un fallo. Contiene el tipo de excepción y un mensaje legible.
     */
    data class Error(
        val exception: ApiException,
        val message: String
    ) : ResultWrapper<Nothing>()
}
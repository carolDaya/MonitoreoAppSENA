package com.sena.monitoreo.data.exception

/**
 * Custom exceptions used to handle common API and network errors in a unified way.
 */
sealed class ApiException(message: String) : Exception(message) {

    /** Thrown when a network or server connection fails. */
    class NetworkError(message: String = "Network connection error") : ApiException(message)

    /** Thrown when the API responds with an error code (4xx or 5xx). */
    class ApiError(message: String = "API response error") : ApiException(message)

    /** Thrown when an unexpected error occurs during processing. */
    class UnknownError(message: String = "Unknown error occurred") : ApiException(message)
}

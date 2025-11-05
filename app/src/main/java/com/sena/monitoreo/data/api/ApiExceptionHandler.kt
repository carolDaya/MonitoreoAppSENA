package com.sena.monitoreo.data.api

/**
 * ApiExceptionHandler centralizes exception types related to API and Retrofit operations.
 *
 * Use these classes to standardize error handling in repositories or view models.
 */
sealed class ApiExceptionHandler(message: String, cause: Throwable? = null) : Exception(message, cause) {

    // Thrown when Retrofit initialization fails
    class RetrofitInitException(message: String, cause: Throwable? = null) : ApiExceptionHandler(message, cause)

    // Thrown when a request fails due to connection or timeout
    class NetworkException(message: String, cause: Throwable? = null) : ApiExceptionHandler(message, cause)

    // Thrown when server returns unexpected response or error code
    class ServerException(message: String, cause: Throwable? = null) : ApiExceptionHandler(message, cause)

    // Thrown when data parsing or serialization fails
    class ParsingException(message: String, cause: Throwable? = null) : ApiExceptionHandler(message, cause)
}

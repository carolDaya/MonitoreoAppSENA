package com.sena.monitoreo.data.exception

/**
 * Custom exceptions used to handle common API and network errors in a unified way.
 */
sealed class ApiException(message: String) : Exception(message) {

    /** * Thrown when a network or server connection fails.
     * Usada cuando hay una IOException (ej. sin internet, timeout).
     */
    class NetworkError(message: String = "Error de conexión de red. Verifique su internet.") : ApiException(message)

    /** * Thrown when the API responds with an error code (4xx or 5xx).
     * Usada cuando Retrofit recibe una respuesta no exitosa (response.isSuccessful == false).
     */
    class ApiError(message: String = "Error de respuesta de la API") : ApiException(message)

    /** * Thrown when an unexpected error occurs during processing.
     * Usada para cualquier otra excepción no cubierta (ej. error de parsing JSON).
     */
    class UnknownError(message: String = "Ocurrió un error inesperado del sistema") : ApiException(message)
    class TimeoutError(message: String) : ApiException(message)
}
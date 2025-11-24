package com.sena.monitoreo.utils

/**
 * Interfaz para Activities que necesitan implementar una lógica de recarga
 * cuando se detecta un error de conexión de red.
 */
interface NetworkRetryListener {
    /**
     * Se llama cuando el usuario presiona el botón "Reintentar Conexión"
     * en la pantalla de error de red.
     */
    fun onNetworkRetry()
}
package com.sena.monitoreo.ui.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sena.monitoreo.utils.NetworkRetryListener

/**
 * Clase base que maneja utilidades comunes para todas las Activities.
 * Ahora usa NetworkErrorActivity para manejar errores de red.
 */
abstract class BaseActivity : AppCompatActivity(), NetworkRetryListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * Método por defecto para reintentar conexión.
     * Las Activities hijas pueden sobreescribirlo si necesitan lógica específica.
     */
    override fun onNetworkRetry() {
        // Lógica genérica de reintento - las Activities hijas pueden sobreescribir
        recreate() // O simplemente recargar la actividad
    }

    /**
     * Muestra la pantalla de error de red usando la Activity dedicada
     */
    protected fun showNetworkError(message: String = "Problema de conexión. Verifica tu internet e intenta nuevamente.") {
        NetworkErrorActivity.start(this, message)
    }
}
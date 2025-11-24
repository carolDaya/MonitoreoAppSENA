package com.sena.monitoreo.ui.base

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.sena.monitoreo.R
import com.sena.monitoreo.utils.NetworkRetryListener
import com.sena.monitoreo.utils.UiUtils

/**
 * Clase base que maneja utilidades comunes para todas las Activities,
 * incluyendo la pantalla de error de red.
 */
abstract class BaseActivity : AppCompatActivity() {

    // Vistas para la pantalla de error
    private var networkErrorLayout: View? = null
    private var btnRetry: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * Inicializa las vistas de error de red. DEBE llamarse en el onCreate
     * de las Activities hijas, después de setContentView.
     * @param rootLayout El ViewGroup principal que contiene el include_network_error.xml.
     * @param listener El objeto que manejará el evento de reintentar (la Activity hija).
     */
    protected fun setupNetworkErrorHandling(rootLayout: ViewGroup, listener: NetworkRetryListener) {
        // 💡 CORRECCIÓN: Buscar el layout por el ID correcto
        networkErrorLayout = rootLayout.findViewById(R.id.network_error_layout)
        btnRetry = networkErrorLayout?.findViewById(R.id.btnRetry)

        btnRetry?.setOnClickListener {
            hideNetworkError()
            UiUtils.showSnackbar(rootLayout, "Intentando reconexión...")
            listener.onNetworkRetry()
        }
    }

    protected fun showNetworkError(message: String = "Error de conexión, por favor reintente.") {
        networkErrorLayout?.let {
            it.visibility = View.VISIBLE
            // 💡 CORRECCIÓN: Usar el ID correcto tvErrorMessage
            it.findViewById<TextView>(R.id.tvErrorMessage)?.text = message
        }
    }
    /**
     * Oculta la pantalla de error de red.
     */
    protected fun hideNetworkError() {
        networkErrorLayout?.visibility = View.GONE
    }

    /**
     * 💡 FUNCIÓN AÑADIDA: Verifica si la pantalla de error de red está visible.
     */
    protected fun isNetworkErrorVisible(): Boolean {
        // Retorna true si networkErrorLayout no es null y su visibilidad es VISIBLE
        return networkErrorLayout?.visibility == View.VISIBLE
    }

    /**
     * Método abstracto que obliga a la clase final (Activity) a implementar la
     * lógica de recarga de datos específica de esa pantalla.
     */
    abstract fun onNetworkRetry()
}
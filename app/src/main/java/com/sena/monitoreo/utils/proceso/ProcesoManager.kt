package com.sena.monitoreo.utils.proceso

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.sena.monitoreo.R
import com.sena.monitoreo.ui.admin.viewmodel.ProcesoViewModel

class ProcesoManager(
    private val context: Context,
    private val procesoViewModel: ProcesoViewModel,
    private val btnIniciar: Button,
    private val btnFinalizar: Button,
    private val tvEstado: TextView,
    private val progressBar: ProgressBar,
    private val lifecycleOwner: LifecycleOwner,
    private val onStatusUpdate: (String) -> Unit = {}
) {
    companion object {
        private const val TAG = "ProcesoManager"
    }

    fun setupProcesoControl() {
        setupClickListeners()
        setupObservers()
        // Cargar estado inicial del proceso
        procesoViewModel.loadProcesoStatus()
    }

    private fun setupClickListeners() {
        btnIniciar.setOnClickListener {
            Log.d(TAG, "Botón Iniciar presionado.")
            procesoViewModel.iniciarProceso()
        }

        btnFinalizar.setOnClickListener {
            mostrarDialogoConfirmacionFinalizar()
        }
    }

    private fun setupObservers() {
        // Observar estado de loading
        procesoViewModel.isLoading.observe(lifecycleOwner) { isLoading ->
            updateLoadingState(isLoading ?: false)
        }

        // 💡 CORRECCIÓN: Observar estado del proceso con nombre único
        procesoViewModel.isProcesoActivo.observe(lifecycleOwner) { isActive ->
            handleProcesoStateChange(isActive)
        }

        // Observar mensajes de estado
        procesoViewModel.procesoStatus.observe(lifecycleOwner) { message ->
            handleProcesoStatus(message ?: "")
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        val isActive = procesoViewModel.isProcesoActivo.value ?: false
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnIniciar.isEnabled = !isLoading && !isActive
        btnFinalizar.isEnabled = !isLoading && isActive
    }

    /**
     * 💡 CORRECCIÓN: Función renombrada para evitar conflicto
     */
    private fun handleProcesoStateChange(isActive: Boolean?) {
        val activo = isActive ?: false
        val isLoading = procesoViewModel.isLoading.value ?: false

        Log.d(TAG, "🔄 Actualizando UI del proceso. Activo: $activo, Loading: $isLoading")

        // Actualizar visibilidad de botones
        if (activo) {
            btnIniciar.visibility = View.GONE
            btnFinalizar.visibility = View.VISIBLE
            tvEstado.text = "Proceso activo"
            tvEstado.setTextColor(ContextCompat.getColor(context, R.color.teal_dark))
        } else {
            btnIniciar.visibility = View.VISIBLE
            btnFinalizar.visibility = View.GONE
            tvEstado.text = "Proceso inactivo"
            tvEstado.setTextColor(ContextCompat.getColor(context, R.color.temp_color))
        }

        // Actualizar estados de botones
        btnIniciar.isEnabled = !activo && !isLoading
        btnFinalizar.isEnabled = activo && !isLoading
    }

    private fun handleProcesoStatus(message: String) {
        val mensajeLimpio = message.trim()

        if (mensajeLimpio.isNotEmpty() && mensajeLimpio != "(mensaje vacío)") {
            Log.d(TAG, "✅ Mensaje de estado: $mensajeLimpio")

            // Actualizar estado del TextView si el mensaje es informativo
            if (mensajeLimpio.contains("Error", ignoreCase = true) ||
                mensajeLimpio.contains("éxito", ignoreCase = true) ||
                mensajeLimpio.contains("correctamente", ignoreCase = true)) {
                tvEstado.text = mensajeLimpio
            }

            onStatusUpdate(mensajeLimpio)
        } else {
            Log.d(TAG, "⏭️ Mensaje vacío omitido: '$message'")
        }
    }

    /**
     * Muestra diálogo de confirmación para finalizar el proceso
     */
    private fun mostrarDialogoConfirmacionFinalizar() {
        AlertDialog.Builder(context)
            .setTitle("⚠️ Confirmar Finalización")
            .setMessage("¿Estás seguro de que deseas finalizar el proceso de monitoreo?")
            .setPositiveButton("Sí, Finalizar") { dialog, _ ->
                Log.d(TAG, "Usuario confirmó finalización de proceso")
                procesoViewModel.finalizarProceso()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                Log.d(TAG, "Usuario canceló finalización de proceso")
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
            .show()
    }

    /**
     * Limpieza, aunque los observers de LiveData se limpian automáticamente
     * cuando el LifecycleOwner se destruye.
     */
    fun cleanup() {
        Log.d(TAG, "Cleanup ejecutado. Observers liberados por Lifecycle.")
    }
}
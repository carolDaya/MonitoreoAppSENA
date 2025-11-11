package com.sena.monitoreo.utils.proceso

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
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

    fun setupProcesoControl() {
        // 1. Listeners de los botones
        btnIniciar.setOnClickListener {
            procesoViewModel.iniciarProceso()
        }

        btnFinalizar.setOnClickListener {
            // Mostrar diálogo de confirmación antes de finalizar
            mostrarDialogoConfirmacionFinalizar()
        }

        // 2. Observar el estado de Carga (Loading)
        procesoViewModel.isLoading.observe(lifecycleOwner) { isLoading ->
            // Deshabilitar ambos botones durante la operación para evitar doble click
            btnIniciar.isEnabled = !isLoading && (procesoViewModel.isProcesoActivo.value == false)
            btnFinalizar.isEnabled = !isLoading && (procesoViewModel.isProcesoActivo.value == true)

            // Mostrar u ocultar la barra de progreso
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 3. Observar si hay Proceso Activo para actualizar la UI
        procesoViewModel.isProcesoActivo.observe(lifecycleOwner) { isActive ->
            tvEstado.text = if (isActive) "Estado: 🟢 Activo (Monitoreando)" else "Estado: 🔴 Inactivo (Se requiere iniciar proceso)"

            // Habilitar/Deshabilitar botones basado en el estado
            val isLoading = procesoViewModel.isLoading.value ?: false
            btnIniciar.isEnabled = !isActive && !isLoading
            btnFinalizar.isEnabled = isActive && !isLoading
        }

        // 4. Observar el mensaje de Estado (Éxito/Error) - FILTRAR VACÍOS
        procesoViewModel.procesoStatus.observe(lifecycleOwner) { message ->
            // Filtrar mensajes vacíos o que contengan "(mensaje vacío)"
            val mensajeLimpio = message.trim()

            if (mensajeLimpio.isNotEmpty() && mensajeLimpio != "(mensaje vacío)") {
                Log.d("ProcesoManager", "✅ Mensaje válido: $mensajeLimpio")
                onStatusUpdate(mensajeLimpio)
            } else {
                Log.d("ProcesoManager", "⏭️ Mensaje vacío omitido: '$message'")
            }
        }

        // 5. Verificar el estado inicial del proceso al cargar la actividad
        procesoViewModel.verificarEstadoProceso()
    }

    /**
     * Muestra diálogo de confirmación para finalizar el proceso
     */
    private fun mostrarDialogoConfirmacionFinalizar() {
        AlertDialog.Builder(context)
            .setTitle("⚠️ Confirmar Finalización")
            .setMessage("¿Estás seguro de que deseas finalizar el proceso de monitoreo?")
            .setPositiveButton("Sí, Finalizar") { dialog, _ ->
                // Usuario confirmó, proceder a finalizar
                Log.d("ProcesoManager", "Usuario confirmó finalización de proceso")
                procesoViewModel.finalizarProceso()
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                // Usuario canceló, no hacer nada
                Log.d("ProcesoManager", "Usuario canceló finalización de proceso")
                dialog.dismiss()
            }
            .setCancelable(true) // Permite cerrar tocando fuera del diálogo
            .create()
            .show()
    }

    fun cleanup() {
        // Los observers se limpian automáticamente cuando el lifecycleOwner se destruye
    }
}
package com.sena.monitoreo.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import android.view.View
import androidx.core.content.ContextCompat
import com.github.ybq.android.spinkit.SpinKitView
import com.google.android.material.snackbar.Snackbar
import com.sena.monitoreo.R
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import kotlinx.coroutines.*

object UiUtils {

    private var progressDialog: Dialog? = null
    private var loadingJob: Job? = null // Job para manejar el retraso
    private const val LOADING_DELAY_MS = 200L // Retraso de 200 ms

    // 💡 Estado público para que las Activities/ViewModels sepan si el loading está activo
    val isLoadingVisible = MutableStateFlow(false)

    // Snackbar bonito con color y optional icon
    fun showSnackbar(view: View, message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        // Usar ContextCompat.getColor() para mayor compatibilidad
        val colorSuccess = ContextCompat.getColor(view.context, R.color.green_primary)
        val colorError = ContextCompat.getColor(view.context, R.color.red)

        snackbar.setBackgroundTint(if (isError) colorError else colorSuccess)
        snackbar.setTextColor(ContextCompat.getColor(view.context, R.color.white))
        snackbar.show()
    }

    // Loading reutilizable con SpinKit
    fun showLoading(activity: Activity, message: String = "Cargando...") {
        // 1. Cancelar cualquier trabajo de retraso anterior que aún no haya mostrado el diálogo
        loadingJob?.cancel()

        // 2. Crear un nuevo Job con un retraso
        loadingJob = CoroutineScope(Dispatchers.Main).launch {
            delay(LOADING_DELAY_MS) // Esperar 200ms

            // Si llegamos aquí, la operación es lenta, mostramos el diálogo
            if (progressDialog == null || !progressDialog!!.isShowing) {
                // Si el diálogo es nulo o no se está mostrando, lo creamos/recreamos
                if (progressDialog == null) {
                    progressDialog = createDialog(activity)
                }

                // Actualizar el mensaje del diálogo si es diferente (opcional)
                val tvMessage = progressDialog?.findViewById<TextView>(R.id.loading_message)
                tvMessage?.text = message

                progressDialog?.show()
                isLoadingVisible.value = true
            }
        }
    }

    fun hideLoading() {
        loadingJob?.cancel() // 1. Cancelar el retraso (si aún no se mostró el diálogo)

        // 2. Si el diálogo se llegó a mostrar, lo ocultamos
        if (progressDialog?.isShowing == true) {
            progressDialog?.dismiss()
            progressDialog = null
            isLoadingVisible.value = false
        }
        // Si el diálogo nunca se mostró (porque la operación fue rápida), no hacemos nada
    }

    private fun createDialog(activity: Activity): Dialog {
        val dialog = Dialog(activity)
        dialog.setContentView(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            // Asegúrate de que R.drawable.bg_rounded_loading exista o usa un color/drawable simple
            setBackgroundResource(R.drawable.bg_rounded_loading)

            // Spinner
            addView(SpinKitView(context).apply {
                setColor(ContextCompat.getColor(context, R.color.white))
                setIndeterminate(true)
                layoutParams = LinearLayout.LayoutParams(150, 150)
            })

            // Mensaje debajo del spinner
            addView(TextView(context).apply {
                id = R.id.loading_message // Asignar ID para poder actualizar el texto
                text = "Cargando..."
                setTextColor(ContextCompat.getColor(context, R.color.white))
                textSize = 16f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
            })
        })
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    /**
     * Navega a la Activity de destino. (Lógica sin cambios)
     * ...
     */
    fun navigateTo(context: Context, destination: Class<*>, finishCurrent: Boolean = false) {
        // Amauroses que cliquier loading pendent se occult antes de nave gar
        hideLoading()

        val intent = Intent(context, destination)

        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        if (finishCurrent && context is Activity) {
            context.finish()
        }
    }
}
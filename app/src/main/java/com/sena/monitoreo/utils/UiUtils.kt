package com.sena.monitoreo.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.ybq.android.spinkit.SpinKitView
import com.github.ybq.android.spinkit.style.Circle
import com.google.android.material.snackbar.Snackbar
import com.sena.monitoreo.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

object UiUtils {

    private var progressDialog: Dialog? = null
    private var loadingJob: Job? = null
    private const val LOADING_DELAY_MS = 200L

    val isLoadingVisible = MutableStateFlow(false)

    // Snackbar simplificado - sin dependencia de recursos
    fun showSnackbar(view: View, message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)

        // Colores directos sin recursos
        val backgroundColor = if (isError) Color.parseColor("#F44336") else Color.parseColor("#4CAF50")
        val textColor = Color.WHITE

        snackbar.setBackgroundTint(backgroundColor)
        snackbar.setTextColor(textColor)
        snackbar.show()
    }

    // Loading mejorado y simplificado
    fun showLoading(activity: Activity, message: String = "Cargando...") {
        loadingJob?.cancel()

        loadingJob = CoroutineScope(Dispatchers.Main).launch {
            delay(LOADING_DELAY_MS)

            if (progressDialog == null || !progressDialog!!.isShowing) {
                if (progressDialog == null) {
                    progressDialog = createSimpleDialog(activity)
                }

                // Actualizar mensaje
                val tvMessage = progressDialog?.findViewById<TextView>(R.id.loading_message)
                tvMessage?.text = message

                try {
                    progressDialog?.show()
                    isLoadingVisible.value = true
                } catch (e: Exception) {
                    // Ignorar errores de ventana si la activity está destruyéndose
                }
            }
        }
    }

    fun hideLoading() {
        loadingJob?.cancel()

        try {
            if (progressDialog?.isShowing == true) {
                progressDialog?.dismiss()
            }
            progressDialog = null
            isLoadingVisible.value = false
        } catch (e: Exception) {
            // Ignorar errores al ocultar
            progressDialog = null
            isLoadingVisible.value = false
        }
    }

    // Diálogo simplificado sin recursos externos

    private fun createSimpleDialog(activity: Activity): Dialog {
        return Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)

            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setDimAmount(0.2f)
            }

            val layout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(40, 40, 40, 40)
                background = ColorDrawable(Color.parseColor("#CC000000"))

                val radius = 16f.dpToPx(activity)
                clipToOutline = true
            }

            val spinner = SpinKitView(activity).apply {
                setIndeterminateDrawable(Circle().apply {
                    color = Color.WHITE
                })
                layoutParams = LinearLayout.LayoutParams(
                    80f.dpToPx(activity),
                    80f.dpToPx(activity)
                )
            }

            val textView = TextView(activity).apply {
                id = R.id.loading_message
                text = "Cargando..."
                setTextColor(Color.WHITE)
                textSize = 16f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16f.dpToPx(activity)
                }
            }

            layout.addView(spinner)
            layout.addView(textView)
            setContentView(layout)
        }
    }


    // Extensión para convertir dp a px
    private fun Float.dpToPx(context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (this * density).toInt()
    }

    // Navegación simplificada
    fun navigateTo(context: Context, destination: Class<*>, finishCurrent: Boolean = false) {
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
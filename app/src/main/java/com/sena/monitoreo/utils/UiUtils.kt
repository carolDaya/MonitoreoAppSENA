package com.sena.monitoreo.utils

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.github.ybq.android.spinkit.SpinKitView
import com.google.android.material.snackbar.Snackbar
import com.sena.monitoreo.R
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
object UiUtils {

    private var progressDialog: Dialog? = null

    // Snackbar bonito con color y optional icon
    fun showSnackbar(view: View, message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(if (isError) 0xFFD32F2F.toInt() else 0xFF4CAF50.toInt())
        snackbar.setTextColor(0xFFFFFFFF.toInt())
        snackbar.show()
    }

    // Loading reutilizable con SpinKit
    fun showLoading(activity: Activity, message: String = "Cargando...") {
        if (progressDialog == null) {
            progressDialog = Dialog(activity).apply {
                setContentView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundResource(R.drawable.bg_rounded_loading)

                    // Spinner
                    addView(SpinKitView(context).apply {
                        setColor(ContextCompat.getColor(context, R.color.white))
                        setIndeterminate(true)
                        layoutParams = LinearLayout.LayoutParams(150, 150)
                    })

                    // Mensaje debajo del spinner
                    addView(TextView(context).apply {
                        text = message
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
                setCancelable(false)
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
        }
        progressDialog?.show()
    }

    fun hideLoading() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    fun navigateTo(activity: Activity, destination: Class<*>, finishCurrent: Boolean = false) {
        val intent = Intent(activity, destination)
        activity.startActivity(intent)
        if (finishCurrent) activity.finish()
    }
}

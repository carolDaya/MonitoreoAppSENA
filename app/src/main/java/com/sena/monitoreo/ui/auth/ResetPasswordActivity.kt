package com.sena.monitoreo.ui.auth

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout // Importar para usar el error debajo del campo (RECOMENDADO)
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResetPasswordActivity : AppCompatActivity() {

    // Componentes de la UI
    private lateinit var inputNewPassword: TextInputEditText
    private lateinit var inputConfirmPassword: TextInputEditText
    private lateinit var layoutNewPassword: TextInputLayout // Para mostrar el error debajo del input
    private lateinit var layoutConfirmPassword: TextInputLayout // Para mostrar el error debajo del input
    private lateinit var buttonSetPassword: MaterialButton
    private lateinit var containerView: android.view.View // Referencia al contenedor principal para Snackbar

    private val TAG = "ResetPasswordActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reset_password)

        // Obtener la vista contenedora principal (asumiendo que tiene el ID container_reset)
        containerView = findViewById(R.id.container_reset)

        ViewCompat.setOnApplyWindowInsetsListener(containerView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar vistas
        layoutNewPassword = findViewById(R.id.input_new_password_layout) // Asumiendo que el TextInputLayout tiene este ID
        layoutConfirmPassword = findViewById(R.id.input_confirm_new_password_layout) // Asumiendo que el TextInputLayout tiene este ID
        inputNewPassword = findViewById(R.id.input_new_password)
        inputConfirmPassword = findViewById(R.id.input_confirm_new_password)
        buttonSetPassword = findViewById(R.id.button_set_new_password)
        val backArrow = findViewById<ImageView>(R.id.back_arrow)

        val phone = intent.getStringExtra("PHONE_NUMBER")

        // Lógica de habilitación del botón
        val watcher = {
            val newPass = inputNewPassword.text.toString().trim()
            val confirmPass = inputConfirmPassword.text.toString().trim()

            // Limpiar errores mientras el usuario escribe
            layoutNewPassword.error = null
            layoutConfirmPassword.error = null

            buttonSetPassword.isEnabled = newPass.isNotEmpty() && confirmPass.isNotEmpty()
        }

        inputNewPassword.addTextChangedListener { watcher() }
        inputConfirmPassword.addTextChangedListener { watcher() }

        // Acción del botón
        buttonSetPassword.setOnClickListener {
            val newPass = inputNewPassword.text.toString().trim()
            val confirmPass = inputConfirmPassword.text.toString().trim()

            if (isInputValid(newPass, confirmPass)) {
                if (phone.isNullOrEmpty()) {
                    // Este error es del sistema, no del input, se usa Snackbar
                    UiUtils.showSnackbar(containerView, "Error interno: número de teléfono no recibido", isError = true)
                } else {
                    cambiarContrasena(phone, newPass)
                }
            }
        }

        // Flecha atrás
        backArrow.setOnClickListener { finish() }
    }

    /**
     * Valida la longitud y coincidencia de las contraseñas.
     * Utiliza el error de TextInputLayout para mensajes debajo del input.
     */
    private fun isInputValid(newPass: String, confirmPass: String): Boolean {
        // Reiniciar errores
        layoutNewPassword.error = null
        layoutConfirmPassword.error = null

        if (newPass.length < 6) {
            layoutNewPassword.error = "Debe tener al menos 6 caracteres"
            return false
        }

        if (newPass != confirmPass) {
            layoutConfirmPassword.error = "Las contraseñas no coinciden"
            return false
        }

        return true
    }

    private fun cambiarContrasena(phone: String, newPassword: String) {
        lifecycleScope.launch {
            // Mostrar loading antes de la llamada a la API
            UiUtils.showLoading(this@ResetPasswordActivity, "Actualizando contraseña...")

            try {
                Log.d(TAG, "Cambiando contraseña para $phone")

                val response = RetrofitClient.apiAuth.updatePassword(
                    mapOf(
                        "telefono" to phone,
                        "nueva_contrasena" to newPassword,
                        "confirmar_contrasena" to newPassword
                    )
                )

                // Asegurar la actualización de la UI en el hilo principal
                withContext(Dispatchers.Main) {
                    UiUtils.hideLoading() // Ocultar loading

                    if (response.isSuccessful) {
                        // Éxito: Usar Snackbar y luego redirigir
                        UiUtils.showSnackbar(containerView, "Contraseña actualizada correctamente")

                        // Espera un momento para que el usuario vea el mensaje antes de redirigir
                        delay(1200)
                        UiUtils.navigateTo(this@ResetPasswordActivity, LoginActivity::class.java, true)
                    } else {
                        // Error del servidor: Usar Snackbar
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "Error al actualizar: $errorBody")
                        UiUtils.showSnackbar(containerView, "Error al actualizar la contraseña. Inténtalo de nuevo.", isError = true)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error de conexión", e)
                // Error de conexión: Usar Snackbar
                withContext(Dispatchers.Main) {
                    UiUtils.hideLoading()
                    UiUtils.showSnackbar(containerView, "Error de conexión con el servidor", isError = true)
                }
            }
        }
    }
}
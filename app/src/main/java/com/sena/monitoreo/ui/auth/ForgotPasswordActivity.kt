package com.sena.monitoreo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.databinding.ActivityForgotPasswordBinding
import com.sena.monitoreo.ui.auth.ResetPasswordActivity
import com.sena.monitoreo.utils.UiUtils
import kotlinx.coroutines.launch
import retrofit2.HttpException

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val TAG = "ForgotPasswordActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajustar padding automático por barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.containerForgot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Botón de cierre
        binding.closeButton.setOnClickListener { finish() }

        // Ocultar botón inicialmente
        binding.buttonResetPassword.visibility = View.GONE

        // Detectar texto en input
        binding.editTextPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Limpiar error tan pronto como el usuario empiece a escribir
                binding.textInputLayoutPhone.error = null
                toggleResetButton()
            }
        })

        // Acción al presionar "Restablecer contraseña"
        binding.buttonResetPassword.setOnClickListener {
            val phone = binding.editTextPhone.text?.toString()?.trim().orEmpty()

            // Usar la nueva función de validación antes de llamar a la API
            if (isInputValid(phone)) {
                verificarTelefono(phone)
            }
        }
    }

    /**
     * Valida el formato y presencia del número de teléfono.
     * Muestra el error debajo del input (TextInputLayout.error).
     */
    private fun isInputValid(phone: String): Boolean {
        binding.textInputLayoutPhone.error = null // Reiniciar error

        if (phone.isBlank()) {
            binding.textInputLayoutPhone.error = "Ingrese su número de teléfono"
            return false
        }

        // Agregar validación de 10 dígitos (asumiendo el requisito de 10)
        if (!phone.matches(Regex("^\\d{10}$"))) {
            binding.textInputLayoutPhone.error = "El número debe tener exactamente 10 dígitos"
            return false
        }

        return true
    }

    /**
     * Animación para mostrar u ocultar el botón de restablecer
     */
    private fun toggleResetButton() {
        val phone = binding.editTextPhone.text?.toString()?.trim().orEmpty()

        if (phone.isNotEmpty()) {
            if (binding.buttonResetPassword.visibility == View.GONE) {
                binding.buttonResetPassword.translationY = 300f
                binding.buttonResetPassword.visibility = View.VISIBLE
                binding.buttonResetPassword.animate()
                    .translationY(0f)
                    .setDuration(200)
                    .start()
            }
        } else {
            if (binding.buttonResetPassword.visibility == View.VISIBLE) {
                binding.buttonResetPassword.animate()
                    .translationY(300f)
                    .setDuration(200)
                    .withEndAction { binding.buttonResetPassword.visibility = View.GONE }
                    .start()
            }
        }
    }

    /**
     * Verifica si el teléfono existe en el servidor.
     * Los errores de servidor o conexión se muestran con Snackbar (práctica correcta).
     */
    private fun verificarTelefono(phone: String) {
        UiUtils.showLoading(this, "Verificando teléfono...")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiAuth.resetPasswordRequest(mapOf("telefono" to phone))
                UiUtils.hideLoading()

                if (response.isSuccessful) {
                    UiUtils.showSnackbar(binding.containerForgot, "Teléfono verificado correctamente")
                    val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java)
                    intent.putExtra("PHONE_NUMBER", phone)
                    startActivity(intent)
                } else {
                    // El error de "No existe usuario" es una respuesta del servidor,
                    // por lo tanto, se usa Snackbar para avisar globalmente.
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error del servidor: $errorBody")
                    UiUtils.showSnackbar(binding.containerForgot, "No existe un usuario con ese teléfono", isError = true)
                }
            } catch (e: HttpException) {
                UiUtils.hideLoading()
                Log.e(TAG, "Error HTTP: ${e.message()}")
                UiUtils.showSnackbar(binding.containerForgot, "Error de conexión con el servidor", isError = true)
            } catch (e: Exception) {
                UiUtils.hideLoading()
                Log.e(TAG, "Error inesperado", e)
                UiUtils.showSnackbar(binding.containerForgot, "Error inesperado al verificar", isError = true)
            }
        }
    }
}
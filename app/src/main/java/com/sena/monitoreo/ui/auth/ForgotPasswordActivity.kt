package com.sena.monitoreo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.databinding.ActivityForgotPasswordBinding
import com.sena.monitoreo.ui.auth.factory.AuthViewModelFactory
import com.sena.monitoreo.ui.auth.viewmodel.password_reset.ForgotPasswordViewModel
import com.sena.monitoreo.ui.auth.viewmodel.password_reset.ForgotPasswordUiState
import com.sena.monitoreo.ui.base.BaseActivity // 💡 Importado
import com.sena.monitoreo.utils.NetworkRetryListener // 💡 Importado
import com.sena.monitoreo.utils.UiUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException

// 💡 1. Heredar de BaseActivity e implementar NetworkRetryListener
class ForgotPasswordActivity : BaseActivity(), NetworkRetryListener {

    private lateinit var binding: ActivityForgotPasswordBinding

    // Inicialización del ViewModel usando el Factory
    private val viewModel: ForgotPasswordViewModel by lazy {
        val factory = AuthViewModelFactory(AuthRepository())
        ViewModelProvider(this, factory)[ForgotPasswordViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()

        // 💡 2. Inicializar el manejo de errores de red (Método heredado)
        setupNetworkErrorHandling(binding.containerForgot as ViewGroup, this)

        setupListeners()
        observeUiState()

        // Ocultar botón inicialmente
        binding.buttonResetPassword.visibility = View.GONE
    }

    // 💡 3. Implementación obligatoria del método de reintento
    override fun onNetworkRetry() {
        // En esta pantalla, el reintento significa intentar de nuevo la verificación del teléfono
        val phone = binding.editTextPhone.text?.toString()?.trim().orEmpty()
        if (phone.isNotEmpty()) {
            viewModel.verifyPhoneNumber(phone)
        } else {
            hideNetworkError()
            // Podrías forzar el reintento a fallar si el campo está vacío, pero es mejor notificar.
            UiUtils.showSnackbar(binding.containerForgot, "Por favor, ingrese un número de teléfono para reintentar.", isError = true)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.containerForgot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupListeners() {
        // Botón de cerrar (volver al Login)
        binding.closeButton.setOnClickListener { finish() }

        binding.editTextPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Limpiar errores (incluyendo error de TextInputLayout) y ocultar la pantalla de red
                binding.textInputLayoutPhone.error = null
                hideNetworkError()
                toggleResetButton()
            }
        })

        binding.buttonResetPassword.setOnClickListener {
            // Aseguramos que la vista de error se oculte antes del intento
            hideNetworkError()
            val phone = binding.editTextPhone.text?.toString()?.trim().orEmpty()
            viewModel.verifyPhoneNumber(phone)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    ForgotPasswordUiState.Idle -> {
                        UiUtils.hideLoading()
                        hideNetworkError()
                    }
                    ForgotPasswordUiState.Loading -> {
                        // Limpiar errores mientras carga para mejor UX
                        binding.textInputLayoutPhone.error = null
                        UiUtils.showLoading(this@ForgotPasswordActivity, "Verificando teléfono...")
                    }
                    is ForgotPasswordUiState.Success -> {
                        UiUtils.hideLoading()
                        hideNetworkError() // Ocultar si la conexión fue exitosa
                        UiUtils.showSnackbar(binding.containerForgot, "Teléfono verificado correctamente", isError = false)

                        // Navegación al siguiente paso
                        val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java)
                        intent.putExtra("PHONE_NUMBER", state.phone)
                        startActivity(intent)
                    }
                    is ForgotPasswordUiState.Error -> {
                        UiUtils.hideLoading()

                        val errorMessage = state.message

                        // 💡 CLAVE 4: Manejo de error de red y errores de negocio

                        if (errorMessage.contains("Error de red", ignoreCase = true) || errorMessage.contains("IOException", ignoreCase = true)) {
                            // 1. Error de red: Muestra la pantalla de error de red (Método heredado)
                            showNetworkError(errorMessage)
                        } else {
                            // Identificar errores de validación local (formato/vacío)
                            val isLocalValidationError = errorMessage.contains("formato", ignoreCase = true) ||
                                    errorMessage.contains("vacío", ignoreCase = true)

                            if (isLocalValidationError) {
                                // 2. Error de validación local: Mostrar en el TextInputLayout
                                binding.textInputLayoutPhone.error = errorMessage
                            } else {
                                // 3. Errores de negocio/servidor (ej. "Usuario no existe"): Mostrar en Snackbar
                                binding.textInputLayoutPhone.error = null // Asegurar que el input esté limpio
                                UiUtils.showSnackbar(binding.containerForgot, errorMessage, isError = true)
                            }
                        }
                    }
                    else -> Unit // Asegurarse de manejar cualquier estado futuro si se añade
                }
            }
        }
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
}
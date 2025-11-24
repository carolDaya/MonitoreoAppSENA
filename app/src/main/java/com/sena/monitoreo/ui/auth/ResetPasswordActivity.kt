package com.sena.monitoreo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.sena.monitoreo.R
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.ui.auth.factory.AuthViewModelFactory
import com.sena.monitoreo.ui.auth.viewmodel.password_reset.ResetPasswordViewModel
import com.sena.monitoreo.ui.auth.viewmodel.password_reset.ResetPasswordUiState
import com.sena.monitoreo.ui.base.BaseActivity // Importar BaseActivity
import com.sena.monitoreo.utils.NetworkRetryListener // Importar la interfaz
import com.sena.monitoreo.utils.UiUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// 💡 1. Heredar de BaseActivity e implementar NetworkRetryListener
class ResetPasswordActivity : BaseActivity(), NetworkRetryListener {

    // Componentes de la UI
    private lateinit var inputNewPassword: TextInputEditText
    private lateinit var inputConfirmPassword: TextInputEditText
    private lateinit var layoutNewPassword: TextInputLayout
    private lateinit var layoutConfirmPassword: TextInputLayout
    private lateinit var buttonSetPassword: MaterialButton
    private lateinit var containerView: View

    // ViewModel Inicialización con Factory
    private val viewModel: ResetPasswordViewModel by lazy {
        val factory = AuthViewModelFactory(AuthRepository())
        ViewModelProvider(this, factory)[ResetPasswordViewModel::class.java]
    }

    private lateinit var phoneNumber: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reset_password)

        containerView = findViewById(R.id.container_reset)

        // 💡 2. Inicializar el manejo de errores de red (Método heredado)
        setupNetworkErrorHandling(containerView as ViewGroup, this)

        // 3. Obtener datos y ajustar insets
        phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: ""
        if (phoneNumber.isEmpty()) {
            UiUtils.showSnackbar(containerView, "Error interno: número de teléfono no recibido", isError = true)
            // finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(containerView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupListeners()
        observeUiState()
    }

    // 💡 4. Implementación obligatoria del método de reintento (Heredado de BaseActivity)
    override fun onNetworkRetry() {
        // La lógica de recarga en esta pantalla es reintentar la acción de actualizar la contraseña,
        // solo si los campos están llenos y el botón estaba habilitado.
        val newPass = inputNewPassword.text.toString().trim()
        val confirmPass = inputConfirmPassword.text.toString().trim()

        // Llamamos al ViewModel para que vuelva a validar y si es válido, reintente la llamada a la API
        if (newPass.isNotEmpty() && confirmPass.isNotEmpty()) {
            viewModel.updatePassword(phoneNumber, newPass, confirmPass)
        } else {
            // Si falta validación (aunque el botón debería estar deshabilitado)
            UiUtils.showSnackbar(containerView, "Por favor, ingresa y confirma la nueva contraseña.", isError = true)
            hideNetworkError()
        }
    }

    private fun initializeViews() {
        layoutNewPassword = findViewById(R.id.input_new_password_layout)
        layoutConfirmPassword = findViewById(R.id.input_confirm_new_password_layout)
        inputNewPassword = findViewById(R.id.input_new_password)
        inputConfirmPassword = findViewById(R.id.input_confirm_new_password)
        buttonSetPassword = findViewById(R.id.button_set_new_password)
        val backArrow = findViewById<ImageView>(R.id.back_arrow)

        backArrow.setOnClickListener { finish() }
    }

    private fun setupListeners() {
        // Lógica de habilitación del botón
        val watcher = {
            clearInputErrors() // Limpiar errores al escribir
            hideNetworkError() // Ocultar error de red al escribir

            val newPass = inputNewPassword.text.toString().trim()
            val confirmPass = inputConfirmPassword.text.toString().trim()

            buttonSetPassword.isEnabled = newPass.isNotEmpty() && confirmPass.isNotEmpty()
        }

        inputNewPassword.addTextChangedListener { watcher() }
        inputConfirmPassword.addTextChangedListener { watcher() }

        // Acción del botón
        buttonSetPassword.setOnClickListener {
            hideNetworkError() // Aseguramos que la vista de error se oculte antes del intento
            val newPass = inputNewPassword.text.toString().trim()
            val confirmPass = inputConfirmPassword.text.toString().trim()

            viewModel.updatePassword(phoneNumber, newPass, confirmPass)
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    ResetPasswordUiState.Idle -> {
                        UiUtils.hideLoading()
                        clearInputErrors()
                        hideNetworkError() // Ocultar si está en Idle
                    }
                    ResetPasswordUiState.Loading -> {
                        UiUtils.showLoading(this@ResetPasswordActivity, "Actualizando contraseña...")
                        clearInputErrors()
                    }
                    is ResetPasswordUiState.Success -> {
                        UiUtils.hideLoading()
                        hideNetworkError() // Ocultar si la conexión fue exitosa
                        UiUtils.showSnackbar(containerView, "Contraseña actualizada correctamente")

                        delay(1200)
                        UiUtils.navigateTo(this@ResetPasswordActivity, LoginActivity::class.java, true)
                    }
                    is ResetPasswordUiState.Error -> {
                        UiUtils.hideLoading()
                        val errorMessage = state.message

                        // 💡 CLAVE 5: Manejo de error UNIFICADO
                        if (errorMessage.contains("Error de red", ignoreCase = true) || errorMessage.contains("IOException", ignoreCase = true)) {
                            // 1. Error de red: Muestra la pantalla de error de red (Método heredado)
                            showNetworkError(errorMessage)
                        } else {
                            // 2. Errores de validación/servidor
                            clearInputErrors() // Limpiar errores previos

                            if (errorMessage.contains("Debe tener al menos 6 caracteres", ignoreCase = true)) {
                                // Error de validación: Longitud de contraseña
                                layoutNewPassword.error = errorMessage
                            } else if (errorMessage.contains("Las contraseñas no coinciden", ignoreCase = true)) {
                                // Error de validación: No coinciden
                                layoutConfirmPassword.error = errorMessage
                            } else {
                                // Error de servidor/negocio: Mostrar en Snackbar
                                UiUtils.showSnackbar(containerView, errorMessage, isError = true)
                            }
                        }
                    }
                    // ❌ Eliminado: Ya no se maneja el estado ValidationError
                    // is ResetPasswordUiState.ValidationError -> {}
                    is ResetPasswordUiState.ValidationError -> TODO()
                }
            }
        }
    }

    private fun clearInputErrors() {
        layoutNewPassword.error = null
        layoutConfirmPassword.error = null
    }
}
package com.sena.monitoreo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.sena.monitoreo.ui.base.BaseActivity
import com.sena.monitoreo.utils.UiUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ResetPasswordActivity : BaseActivity() {

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

        // Obtener datos y ajustar insets
        phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: ""
        if (phoneNumber.isEmpty()) {
            UiUtils.showSnackbar(containerView, "Error interno: número de teléfono no recibido", isError = true)
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

    override fun onNetworkRetry() {
        val newPass = inputNewPassword.text.toString().trim()
        val confirmPass = inputConfirmPassword.text.toString().trim()

        if (newPass.isNotEmpty() && confirmPass.isNotEmpty()) {
            viewModel.updatePassword(phoneNumber, newPass, confirmPass)
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
            clearInputErrors()
            val newPass = inputNewPassword.text.toString().trim()
            val confirmPass = inputConfirmPassword.text.toString().trim()
            buttonSetPassword.isEnabled = newPass.isNotEmpty() && confirmPass.isNotEmpty()
        }

        inputNewPassword.addTextChangedListener { watcher() }
        inputConfirmPassword.addTextChangedListener { watcher() }

        // Acción del botón - VALIDACIÓN LOCAL PRIMERO
        buttonSetPassword.setOnClickListener {
            val newPass = inputNewPassword.text.toString().trim()
            val confirmPass = inputConfirmPassword.text.toString().trim()

            when {
                newPass.isEmpty() || confirmPass.isEmpty() -> {
                    UiUtils.showSnackbar(containerView, "Por favor, completa ambos campos", isError = true)
                }
                newPass.length < 6 -> {
                    layoutNewPassword.error = "La contraseña debe tener al menos 6 caracteres"
                }
                newPass != confirmPass -> {
                    layoutConfirmPassword.error = "Las contraseñas no coinciden"
                }
                else -> {
                    viewModel.updatePassword(phoneNumber, newPass, confirmPass)
                }
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    ResetPasswordUiState.Idle -> {
                        UiUtils.hideLoading()
                        clearInputErrors()
                    }
                    ResetPasswordUiState.Loading -> {
                        UiUtils.showLoading(this@ResetPasswordActivity, "Actualizando contraseña...")
                        clearInputErrors()
                    }
                    is ResetPasswordUiState.Success -> {
                        UiUtils.hideLoading()
                        UiUtils.showSnackbar(containerView, "Contraseña actualizada correctamente")
                        delay(1200)
                        UiUtils.navigateTo(this@ResetPasswordActivity, LoginActivity::class.java, true)
                    }
                    is ResetPasswordUiState.Error -> {
                        UiUtils.hideLoading()
                        val errorMessage = state.message

                        // Solo manejar errores de red aquí
                        if (errorMessage.contains("Error de red", ignoreCase = true) ||
                            errorMessage.contains("IOException", ignoreCase = true)) {
                            showNetworkError(errorMessage)
                        } else {
                            // Otros errores del servidor
                            UiUtils.showSnackbar(containerView, errorMessage, isError = true)
                        }
                    }

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
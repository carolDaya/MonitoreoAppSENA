package com.sena.monitoreo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.databinding.ActivityForgotPasswordBinding
import com.sena.monitoreo.ui.auth.factory.AuthViewModelFactory
import com.sena.monitoreo.ui.auth.viewmodel.password_reset.ForgotPasswordViewModel
import com.sena.monitoreo.ui.auth.viewmodel.password_reset.ForgotPasswordUiState
import com.sena.monitoreo.ui.base.BaseActivity
import com.sena.monitoreo.utils.UiUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ForgotPasswordActivity : BaseActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
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
        setupListeners()
        observeUiState()

        binding.buttonResetPassword.visibility = View.GONE
    }

    override fun onNetworkRetry() {
        val phone = binding.editTextPhone.text?.toString()?.trim().orEmpty()
        viewModel.verifyPhoneNumber(phone)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.containerForgot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupListeners() {
        binding.closeButton.setOnClickListener { finish() }

        binding.editTextPhone.addTextChangedListener {
            val phone = it?.toString()?.trim().orEmpty()
            // Validación en tiempo real del formato
            validatePhoneFormat(phone)
            toggleResetButton()
        }

        binding.buttonResetPassword.setOnClickListener {
            val phone = binding.editTextPhone.text?.toString()?.trim().orEmpty()
            viewModel.verifyPhoneNumber(phone)
        }
    }

    private fun validatePhoneFormat(phone: String) {
        when {
            phone.isEmpty() -> {
                binding.textInputLayoutPhone.error = null
            }
            phone.length != 10 -> {
                binding.textInputLayoutPhone.error = "El teléfono debe tener 10 dígitos"
            }
            !phone.all { it.isDigit() } -> {
                binding.textInputLayoutPhone.error = "Solo se permiten números"
            }
            else -> {
                binding.textInputLayoutPhone.error = null
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    ForgotPasswordUiState.Idle -> {
                        UiUtils.hideLoading()
                        binding.textInputLayoutPhone.error = null
                    }
                    ForgotPasswordUiState.Loading -> {
                        binding.textInputLayoutPhone.error = null
                        UiUtils.showLoading(this@ForgotPasswordActivity, "Verificando teléfono...")
                    }
                    is ForgotPasswordUiState.Success -> {
                        UiUtils.hideLoading()
                        UiUtils.showSnackbar(binding.containerForgot, "Teléfono verificado correctamente", isError = false)

                        val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java)
                        intent.putExtra("PHONE_NUMBER", state.phone)
                        startActivity(intent)
                    }
                    is ForgotPasswordUiState.ValidationError -> {
                        UiUtils.hideLoading()
                        binding.textInputLayoutPhone.error = state.message
                    }
                    is ForgotPasswordUiState.Error -> {
                        UiUtils.hideLoading()
                        val errorMessage = state.message

                        when {
                            errorMessage.contains("Error de red", ignoreCase = true) -> {
                                showNetworkError(errorMessage)
                            }
                            else -> {
                                UiUtils.showSnackbar(binding.containerForgot, errorMessage, isError = true)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun toggleResetButton() {
        val phone = binding.editTextPhone.text?.toString()?.trim().orEmpty()
        // El botón solo se habilita si tiene exactamente 10 dígitos numéricos
        val isValidFormat = phone.length == 10 && phone.all { it.isDigit() }
        binding.buttonResetPassword.isEnabled = isValidFormat

        if (isValidFormat) {
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
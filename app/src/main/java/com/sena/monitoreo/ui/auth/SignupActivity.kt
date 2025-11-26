package com.sena.monitoreo.ui.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.R
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.databinding.ActivitySignupBinding
import com.sena.monitoreo.ui.auth.factory.AuthViewModelFactory
import com.sena.monitoreo.ui.auth.viewmodel.signup.SignupUiState
import com.sena.monitoreo.ui.auth.viewmodel.signup.SignupViewModel
import com.sena.monitoreo.ui.base.BaseActivity
import com.sena.monitoreo.ui.user.HomeUserActivity
import com.sena.monitoreo.utils.UiUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SignupActivity : BaseActivity() {

    private lateinit var binding: ActivitySignupBinding

    // Inicialización del ViewModel usando el Factory
    private val viewModel: SignupViewModel by lazy {
        val factory = AuthViewModelFactory(AuthRepository())
        ViewModelProvider(this, factory)[SignupViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ ELIMINADO: setupNetworkErrorHandling - ya no se necesita

        ViewCompat.setOnApplyWindowInsetsListener(binding.containerSignup) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBarsInsets.left, systemBarsInsets.top, systemBarsInsets.right, systemBarsInsets.bottom)
            insets
        }

        setupClickListeners()
        observeUiState()
    }

    override fun onNetworkRetry() {
        // Reintentar el registro automáticamente con los datos actuales
        registerUser()
    }

    private fun setupClickListeners() {
        binding.backArrow.setOnClickListener { finish() }

        binding.textLoginLink.setOnClickListener {
            UiUtils.navigateTo(this, LoginActivity::class.java, finishCurrent = true)
        }

        binding.buttonRegister.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val getStringFromContext: (Int) -> String = { resId -> getString(resId) }

        viewModel.register(
            name = binding.editTextName.text.toString().trim(),
            phone = binding.editTextPhone.text.toString().trim(),
            password = binding.inputPassword.text.toString().trim(),
            confirmPassword = binding.inputConfirmPassword.text.toString().trim(),
            getString = getStringFromContext
        )
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    SignupUiState.Idle -> {
                        clearInputErrors()
                        UiUtils.hideLoading()
                    }
                    SignupUiState.Loading -> {
                        UiUtils.showLoading(this@SignupActivity, "Registrando...")
                    }
                    SignupUiState.Success -> {
                        UiUtils.hideLoading()
                        UiUtils.showSnackbar(binding.root, getString(R.string.msg_registration_success))
                        UiUtils.navigateTo(this@SignupActivity, HomeUserActivity::class.java, finishCurrent = true)
                    }
                    is SignupUiState.Error -> {
                        UiUtils.hideLoading()

                        // 💡 NUEVO ENFOQUE: Usar NetworkErrorActivity para errores de red
                        if (state.message.contains("Error de red", ignoreCase = true) ||
                            state.message.contains("IOException", ignoreCase = true)) {
                            showNetworkError(state.message)
                        } else {
                            // Para otros errores (validación, servidor, etc.), usamos el Snackbar
                            UiUtils.showSnackbar(binding.root, state.message, isError = true)
                        }
                    }
                }
            }
        }
    }

    private fun clearInputErrors() {
        binding.textInputLayoutName.error = null
        binding.textInputLayoutPhone.error = null
        binding.inputPasswordLayout.error = null
        binding.textInputLayoutConfirmPassword.error = null
    }
}
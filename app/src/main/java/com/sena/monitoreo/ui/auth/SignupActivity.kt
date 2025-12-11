package com.sena.monitoreo.ui.auth

import android.graphics.Color
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.containerSignup) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBarsInsets.left, systemBarsInsets.top, systemBarsInsets.right, systemBarsInsets.bottom)
            insets
        }

        // SOLUCIÓN: Forzar colores después de que la UI esté lista
        binding.root.post {
            forceDarkHintColors()
        }

        setupClickListeners()
        observeUiState()
    }

    /**
     * SOLUCIÓN DEFINITIVA - Simple y sin errores
     */
    private fun forceDarkHintColors() {
        try {
            // Color NEGRO PURO - Esto SÍ se verá sobre fondo claro
            val blackColor = Color.BLACK

            // 1. APLICAR DIRECTAMENTE A LOS EDITTEXTS
            binding.editTextName.setHintTextColor(blackColor)
            binding.editTextPhone.setHintTextColor(blackColor)
            binding.inputPassword.setHintTextColor(blackColor)
            binding.inputConfirmPassword.setHintTextColor(blackColor)

            // 2. También usar ColorStateList para los TextInputLayouts
            val colorStateList = android.content.res.ColorStateList.valueOf(blackColor)

            binding.textInputLayoutName.setHintTextColor(colorStateList)
            binding.textInputLayoutPhone.setHintTextColor(colorStateList)
            binding.inputPasswordLayout.setHintTextColor(colorStateList)
            binding.textInputLayoutConfirmPassword.setHintTextColor(colorStateList)

            // 3. Segundo intento después de un pequeño delay (por si acaso)
            binding.root.postDelayed({
                binding.editTextName.setHintTextColor(blackColor)
                binding.editTextPhone.setHintTextColor(blackColor)
                binding.inputPassword.setHintTextColor(blackColor)
                binding.inputConfirmPassword.setHintTextColor(blackColor)
            }, 50)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNetworkRetry() {
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

                        // Usar NetworkErrorActivity para errores de red
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

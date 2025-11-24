package com.sena.monitoreo.ui.auth

import android.os.Bundle
import android.view.ViewGroup
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
import com.sena.monitoreo.ui.base.BaseActivity // Importar BaseActivity
import com.sena.monitoreo.ui.user.HomeUserActivity
import com.sena.monitoreo.utils.NetworkRetryListener // Importar la interfaz
import com.sena.monitoreo.utils.UiUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// 💡 1. Heredar de BaseActivity e implementar NetworkRetryListener
class SignupActivity : BaseActivity(), NetworkRetryListener {

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

        // 💡 2. Inicializar el manejo de errores de red (Método heredado)
        // 'binding.containerSignup' es el ViewGroup raíz (CoordinatorLayout)
        setupNetworkErrorHandling(binding.containerSignup as ViewGroup, this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.containerSignup) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBarsInsets.left, systemBarsInsets.top, systemBarsInsets.right, systemBarsInsets.bottom)
            insets
        }

        setupClickListeners()
        observeUiState()
    }

    // 💡 3. Implementación obligatoria del método de reintento
    override fun onNetworkRetry() {
        // En el caso del registro, solo reintentamos si el usuario había introducido
        // datos previamente (es decir, re-ejecutamos la función de registro).
        // Sin embargo, es más seguro que el usuario presione el botón Registrar de nuevo
        // para asegurarse de que los datos son actuales.
        // Aquí solo limpiamos errores y permitimos que el usuario intente de nuevo.
        clearInputErrors()
        // Si quieres forzar el reintento del registro:
        registerUser()
    }


    private fun setupClickListeners() {
        binding.backArrow.setOnClickListener { finish() }

        binding.textLoginLink.setOnClickListener {
            UiUtils.navigateTo(this, LoginActivity::class.java, finishCurrent = true)
        }

        binding.buttonRegister.setOnClickListener {
            hideNetworkError() // Ocultar el error si estaba visible antes del intento
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
                        hideNetworkError() // Asegurarse de que el error esté oculto en estado IDLE
                    }
                    SignupUiState.Loading -> {
                        UiUtils.showLoading(this@SignupActivity, "Registrando...")
                    }
                    SignupUiState.Success -> {
                        UiUtils.hideLoading()
                        hideNetworkError() // Asegurar de ocultar si se reconectó exitosamente
                        UiUtils.showSnackbar(binding.root, getString(R.string.msg_registration_success))
                        UiUtils.navigateTo(this@SignupActivity, HomeUserActivity::class.java, finishCurrent = true)
                    }
                    is SignupUiState.Error -> {
                        UiUtils.hideLoading()

                        // 💡 CLAVE 4: Manejo de error de red
                        if (state.message.contains("Error de red", ignoreCase = true) || state.message.contains("IOException", ignoreCase = true)) {
                            showNetworkError(state.message) // Muestra la pantalla de error de red (Método heredado)
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
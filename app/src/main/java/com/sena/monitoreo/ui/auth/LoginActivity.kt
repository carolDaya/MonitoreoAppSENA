package com.sena.monitoreo.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.R
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.databinding.ActivityLoginBinding
import com.sena.monitoreo.ui.admin.HomeAdminActivity
import com.sena.monitoreo.ui.user.HomeUserActivity
import com.sena.monitoreo.utils.UiUtils
import com.sena.monitoreo.ui.auth.viewmodel.login.LoginViewModel
import com.sena.monitoreo.ui.auth.viewmodel.login.LoginUiState
import com.sena.monitoreo.ui.auth.factory.AuthViewModelFactory
import com.sena.monitoreo.ui.base.BaseActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: LoginViewModel by lazy {
        val factory = AuthViewModelFactory(AuthRepository())
        ViewModelProvider(this, factory)[LoginViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ ELIMINADO: setupNetworkErrorHandling - ya no se necesita

        setupWindowInsets()
        setupInputWatcher()
        setupListeners()
        observeUiState()
    }

    override fun onNetworkRetry() {
        Log.d("LoginActivity", "Reintentando conexión desde NetworkErrorActivity...")
        val phone = binding.inputPhone.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()

        if (phone.isNotEmpty() && password.isNotEmpty()) {
            // Reintentar el login automáticamente
            viewModel.login(phone, password)
        }
        // Si no hay credenciales, el usuario tendrá que ingresarlas manualmente
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.containerLogin) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupInputWatcher() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                toggleLoginButton()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.inputPhone.addTextChangedListener(watcher)
        binding.inputPassword.addTextChangedListener(watcher)
        toggleLoginButton()
    }

    private fun toggleLoginButton() {
        val phone = binding.inputPhone.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()

        if (phone.isNotEmpty() && password.isNotEmpty()) {
            if (binding.loginButton.visibility == View.GONE) {
                binding.loginButton.translationY = 300f
                binding.loginButton.visibility = View.VISIBLE
                binding.loginButton.animate()
                    .translationY(0f)
                    .setDuration(200)
                    .start()
            }
        } else {
            if (binding.loginButton.visibility == View.VISIBLE) {
                binding.loginButton.animate()
                    .translationY(300f)
                    .setDuration(200)
                    .withEndAction { binding.loginButton.visibility = View.GONE }
                    .start()
            }
        }
    }

    private fun setupListeners() {
        binding.loginButton.setOnClickListener {
            val phone = binding.inputPhone.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()
            viewModel.login(phone, password)
        }

        binding.createAccountText.setOnClickListener {
            UiUtils.navigateTo(
                this@LoginActivity,
                SignupActivity::class.java,
                finishCurrent = false
            )
        }

        binding.forgotPasswordText.setOnClickListener {
            UiUtils.navigateTo(
                this@LoginActivity,
                ForgotPasswordActivity::class.java,
                finishCurrent = false
            )
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is LoginUiState.Loading -> {
                        UiUtils.showLoading(this@LoginActivity, "Iniciando sesión...")
                    }

                    is LoginUiState.Success -> {
                        UiUtils.hideLoading()
                        UiUtils.showSnackbar(binding.root, "Bienvenido ${state.role}!")

                        when (state.role.lowercase()) {
                            "admin" -> UiUtils.navigateTo(
                                this@LoginActivity,
                                HomeAdminActivity::class.java,
                                finishCurrent = true
                            )
                            else -> UiUtils.navigateTo(
                                this@LoginActivity,
                                HomeUserActivity::class.java,
                                finishCurrent = true
                            )
                        }
                    }

                    is LoginUiState.Error -> {
                        UiUtils.hideLoading()

                        // 💡 NUEVO ENFOQUE: Usar NetworkErrorActivity para errores de red
                        if (state.message.contains("Error de red", ignoreCase = true)) {
                            showNetworkError("Problema de conexión. Verifica tu internet e intenta nuevamente.")
                        } else {
                            // Para otros errores (credenciales, servidor, etc.)
                            UiUtils.showSnackbar(binding.root, state.message, isError = true)
                        }
                    }

                    LoginUiState.Idle -> {
                        UiUtils.hideLoading()
                    }
                }
            }
        }
    }
}
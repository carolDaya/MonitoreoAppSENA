package com.sena.monitoreo.ui.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.data.model.auth.RegisterRequest
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.databinding.ActivitySignupBinding
import com.sena.monitoreo.ui.user.HomeUserActivity
import com.sena.monitoreo.utils.ApiResult
import com.sena.monitoreo.utils.UiUtils
import kotlinx.coroutines.launch
import com.sena.monitoreo.R

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val authRepository = AuthRepository()

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

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.backArrow.setOnClickListener { finish() }

        binding.textLoginLink.setOnClickListener {
            UiUtils.navigateTo(this, LoginActivity::class.java, finishCurrent = true)
        }

        binding.buttonRegister.setOnClickListener {
            if (isFormValid()) registerUser()
        }
    }

    private fun registerUser() {
        val request = RegisterRequest(
            nombre = binding.editTextName.text.toString().trim(),
            telefono = binding.editTextPhone.text.toString().trim(),
            password = binding.inputPassword.text.toString().trim(),
            confirmPassword = binding.inputConfirmPassword.text.toString().trim()
        )

        UiUtils.showLoading(this, "Registrando...")

        lifecycleScope.launch {
            when (val result = authRepository.register(request)) {
                is ApiResult.Success -> {
                    UiUtils.hideLoading()
                    UiUtils.showSnackbar(binding.root, result.data.message)
                    UiUtils.navigateTo(this@SignupActivity, HomeUserActivity::class.java, finishCurrent = true)
                }

                is ApiResult.Error -> {
                    UiUtils.hideLoading()
                    UiUtils.showSnackbar(binding.root, result.message, isError = true)
                }
            }
        }
    }

    private fun isFormValid(): Boolean {
        val name = binding.editTextName.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()
        val confirmPassword = binding.inputConfirmPassword.text.toString().trim()

        binding.textInputLayoutName.error = null
        binding.textInputLayoutPhone.error = null
        binding.inputPasswordLayout.error = null
        binding.textInputLayoutConfirmPassword.error = null

        if (name.isBlank()) {
            binding.textInputLayoutName.error = getString(R.string.error_field_required)
            return false
        }
        if (phone.isBlank()) {
            binding.textInputLayoutPhone.error = getString(R.string.error_field_required)
            return false
        }
        if (!phone.matches(Regex("^\\d{10}$"))) {
            binding.textInputLayoutPhone.error = getString(R.string.forbided_field_required)
            return false
        }
        if (password.isBlank()) {
            binding.inputPasswordLayout.error = getString(R.string.error_field_required)
            return false
        }
        if (confirmPassword.isBlank()) {
            binding.textInputLayoutConfirmPassword.error = getString(R.string.error_field_required)
            return false
        }
        if (password != confirmPassword) {
            binding.textInputLayoutConfirmPassword.error = getString(R.string.error_passwords_not_match)
            return false
        }

        return true
    }
}

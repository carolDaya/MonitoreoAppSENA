package com.sena.monitoreo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.R
import com.sena.monitoreo.data.model.auth.RegisterRequest
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.databinding.ActivitySignupBinding
import com.sena.monitoreo.ui.user.HomeUserActivity
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.containerSignup) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.backArrow.setOnClickListener { finish() }

        binding.textLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.textViewTermsAndConditions.setOnClickListener {
            Toast.makeText(this, "Redireccionando a los términos...", Toast.LENGTH_SHORT).show()
        }

        // 🟢 Aquí cambiamos la lógica del botón:
        binding.buttonRegister.setOnClickListener {
            if (validateForm()) {
                registerUser()
            }
        }
    }

    private fun registerUser() {
        val request = RegisterRequest(
            nombre = binding.editTextName.text.toString().trim(),
            telefono = binding.editTextPhone.text.toString().trim(),
            password = binding.inputPassword.text.toString().trim(),
            confirm_password = binding.inputConfirmPassword.text.toString().trim()
        )

        lifecycleScope.launch {
            try {
                val response = authRepository.register(request)

                if (response.isSuccessful) {
                    val message = response.body()?.message ?: "Registro exitoso"
                    Toast.makeText(this@SignupActivity, message, Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@SignupActivity, HomeUserActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this@SignupActivity,
                        "Error al registrar: ${response.errorBody()?.string()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SignupActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun validateForm(): Boolean {
        val name = binding.editTextName.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()
        val confirmPassword = binding.inputConfirmPassword.text.toString().trim()
        val termsAccepted = binding.checkboxTerms.isChecked

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
        if (!termsAccepted) {
            Toast.makeText(this, getString(R.string.error_accept_terms), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}

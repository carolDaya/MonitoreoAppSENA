package com.sena.monitoreo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.databinding.ActivitySignupBinding
import com.sena.monitoreo.data.model.auth.RegisterRequest
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.ui.user.HomeUserActivity
import kotlinx.coroutines.launch
import org.json.JSONObject

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val repository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.containerSignup) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Flecha atrás
        binding.backArrow.setOnClickListener { finish() }

        // Ir al login
        binding.textLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Botón registrar
        binding.buttonRegister.setOnClickListener {
            if (validateForm()) {
                val request = RegisterRequest(
                    nombre = binding.editTextName.text.toString().trim(),
                    telefono = binding.editTextPhone.text.toString().trim(),
                    password = binding.inputPassword.text.toString().trim(),
                    confirm_password = binding.inputConfirmPassword.text.toString().trim()
                )


                binding.buttonRegister.isEnabled = false // bloquear mientras hace la petición

                lifecycleScope.launch {
                    try {
                        val response = repository.register(request)
                        if (response.isSuccessful && response.body() != null) {
                            Toast.makeText(this@SignupActivity, "Registro exitoso", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@SignupActivity, HomeUserActivity::class.java))
                            finish()
                        } else {
                            val errorJson = JSONObject(response.errorBody()?.string() ?: "{}")
                            val msg = errorJson.optString("error", "Error en el registro")
                            // Mostrar error en el campo correspondiente
                            if (msg.contains("teléfono")) {
                                binding.textInputLayoutPhone.error = msg
                            } else {
                                Toast.makeText(this@SignupActivity, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@SignupActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        binding.buttonRegister.isEnabled = true // desbloquear botón
                    }
                }
            }
        }
    }

    private fun validateForm(): Boolean {
        val name = binding.editTextName.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()
        val confirmPassword = binding.inputConfirmPassword.text.toString().trim()
        val termsAccepted = binding.checkboxTerms.isChecked

        // Limpiar errores previos
        binding.textInputLayoutName.error = null
        binding.textInputLayoutPhone.error = null
        binding.inputPasswordLayout.error = null
        binding.textInputLayoutConfirmPassword.error = null

        if (name.isBlank()) { binding.textInputLayoutName.error = "Campo requerido"; return false }
        if (phone.isBlank()) { binding.textInputLayoutPhone.error = "Campo requerido"; return false }
        if (password.isBlank()) { binding.inputPasswordLayout.error = "Campo requerido"; return false }
        if (confirmPassword.isBlank()) { binding.textInputLayoutConfirmPassword.error = "Campo requerido"; return false }
        if (password != confirmPassword) { binding.textInputLayoutConfirmPassword.error = "No coinciden las contraseñas"; return false }
        if (!termsAccepted) { Toast.makeText(this, "Debe aceptar los términos", Toast.LENGTH_SHORT).show(); return false }

        return true
    }
}

package com.sena.monitoreo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.data.model.auth.LoginRequest
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.databinding.ActivityLoginBinding
import com.sena.monitoreo.ui.admin.HomeAdminActivity
import com.sena.monitoreo.ui.user.HomeUserActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajustar insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.containerLogin) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Mostrar / ocultar botón según inputs
        fun checkInputs() {
            val phone = binding.inputPhone.text?.toString()?.trim()
            val password = binding.inputPassword.text?.toString()?.trim()
            binding.loginButton.visibility =
                if (!phone.isNullOrEmpty() && !password.isNullOrEmpty()) View.VISIBLE
                else View.GONE
        }

        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkInputs()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }

        binding.inputPhone.addTextChangedListener(watcher)
        binding.inputPassword.addTextChangedListener(watcher)

        // Login
        binding.loginButton.setOnClickListener {
            val phone = binding.inputPhone.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()

            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("LoginActivity", "Intentando iniciar sesión con teléfono: $phone")

            lifecycleScope.launch {
                try {
                    Log.d("LoginActivity", "Enviando petición al servidor...")
                    val response = authRepository.login(LoginRequest(phone, password))
                    Log.d("LoginActivity", "Respuesta recibida del servidor. Código: ${response.code()}")

                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        Log.d("LoginActivity", "Cuerpo de respuesta: $loginResponse")

                        if (loginResponse != null) {
                            Toast.makeText(this@LoginActivity, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                            Log.d("LoginActivity", "Usuario: ${loginResponse.usuario}, Rol: ${loginResponse.rol}")

                            when (loginResponse.rol.lowercase()) {
                                "admin" -> {
                                    Log.d("LoginActivity", "Redirigiendo a HomeAdminActivity")
                                    startActivity(Intent(this@LoginActivity, HomeAdminActivity::class.java))
                                }
                                else -> {
                                    Log.d("LoginActivity", "Redirigiendo a HomeUserActivity")
                                    startActivity(Intent(this@LoginActivity, HomeUserActivity::class.java))
                                }
                            }

                            finish()
                        } else {
                            Log.e("LoginActivity", "Error: respuesta vacía del servidor")
                            Toast.makeText(this@LoginActivity, "Error: respuesta vacía del servidor", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("LoginActivity", "Error de autenticación. Código: ${response.code()}, cuerpo: $errorBody")
                        Toast.makeText(this@LoginActivity, "Credenciales inválidas o error ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Error de conexión o excepción inesperada", e)
                    Toast.makeText(this@LoginActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Crear cuenta
        binding.createAccountText.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        // Recuperar contraseña
        binding.forgotPasswordText.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}

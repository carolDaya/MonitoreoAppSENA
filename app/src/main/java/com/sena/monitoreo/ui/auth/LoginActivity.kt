package com.sena.monitoreo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.sena.monitoreo.data.model.auth.LoginRequest
// Asegúrate de que este modelo tenga la propiedad 'rol'
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.databinding.ActivityLoginBinding
// Importa la actividad de usuario
import com.sena.monitoreo.ui.user.HomeUserActivity
// Importa la actividad de administrador (DEBES CREAR ESTA CLASE)
import com.sena.monitoreo.ui.admin.HomeAdminActivity // ASUMIDO
// Importa la actividad de registro (SignupActivity)
import com.sena.monitoreo.ui.auth.SignupActivity // ASUMIDO
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val repository = AuthRepository() // sin pasar Retrofit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.containerLogin) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Función para mostrar u ocultar botón según inputs
        fun checkInputs() {
            val phone = binding.inputPhone.text?.toString()?.trim()
            val password = binding.inputPassword.text?.toString()?.trim()
            binding.loginButton.visibility =
                if (!phone.isNullOrEmpty() && !password.isNullOrEmpty()) View.VISIBLE
                else View.GONE

            // Ocultar error cuando el usuario escribe
            binding.tvError.visibility = View.GONE
        }

        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { checkInputs() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }

        binding.inputPhone.addTextChangedListener(watcher)
        binding.inputPassword.addTextChangedListener(watcher)

        // Login
        binding.loginButton.setOnClickListener {
            val phone = binding.inputPhone.text.toString()
            val password = binding.inputPassword.text.toString()

            val request = LoginRequest(phone, password)
            lifecycleScope.launch {
                try {
                    Log.d("LoginActivity", "Iniciando login con: phone=$phone, password=$password")
                    val response = repository.login(request)
                    Log.d("LoginActivity", "Response recibido: $response")

                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()!!
                        Log.d("LoginActivity", "Login exitoso: usuario=${user.usuario}, rol=${user.rol}")

                        // Lógica de validación de rol para redirigir
                        val intent = if (user.rol == "admin") {
                            Intent(this@LoginActivity, HomeAdminActivity::class.java)
                        } else {
                            Intent(this@LoginActivity, HomeUserActivity::class.java)
                        }

                        Snackbar.make(binding.containerLogin, "Bienvenido ${user.usuario}", Snackbar.LENGTH_SHORT).show()
                        startActivity(intent) // Usa el Intent basado en el rol
                        finish()

                    } else {
                        val msg = response.errorBody()?.string()
                        val errorMessage = try {
                            JSONObject(msg!!).getString("error")
                        } catch (e: Exception) {
                            "Credenciales inválidas"
                        }
                        // Mostrar error visual
                        binding.tvError.text = errorMessage
                        binding.tvError.visibility = View.VISIBLE
                        Log.e("LoginActivity", "Login fallido: $errorMessage")
                    }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Error de conexión", e)
                    binding.tvError.text = "Error de conexión: ${e.message}"
                    binding.tvError.visibility = View.VISIBLE
                }
            }
        }

        // Crear cuenta
        binding.createAccountText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
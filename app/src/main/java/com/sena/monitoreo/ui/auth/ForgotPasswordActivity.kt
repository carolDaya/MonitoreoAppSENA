package com.sena.monitoreo.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val TAG = "ForgotPasswordActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.containerForgot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.closeButton.setOnClickListener { finish() }
        binding.buttonBackToLogin.setOnClickListener { finish() }

        binding.buttonResetPassword.setOnClickListener {
            val phone = binding.inputPhoneForgot.text?.toString()?.trim()
            if (phone.isNullOrEmpty()) {
                binding.inputLayoutPhone.error = "Ingrese su número de teléfono"
                return@setOnClickListener
            }

            binding.inputLayoutPhone.error = null
            verificarTelefono(phone)
        }
    }

    private fun verificarTelefono(phone: String) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Verificando teléfono: $phone")
                val response = RetrofitClient.apiAuth.resetPasswordRequest(mapOf("telefono" to phone))
                Log.d(TAG, "Respuesta del servidor: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d(TAG, "Teléfono válido: $body")

                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Teléfono verificado. Continúa con el cambio de contraseña.",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java)
                    intent.putExtra("PHONE_NUMBER", phone)
                    startActivity(intent)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error: $errorBody")
                    Toast.makeText(this@ForgotPasswordActivity, "No existe un usuario con ese teléfono", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de conexión", e)
                Toast.makeText(this@ForgotPasswordActivity, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

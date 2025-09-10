package com.sena.monitoreo.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sena.monitoreo.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajuste de insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.containerForgot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Botón cerrar
        binding.closeButton.setOnClickListener { finish() }

        // Botón "Back to Login"
        binding.buttonBackToLogin.setOnClickListener {
            finish()
        }

        // Botón "Reset Password" → ResetPasswordActivity
        binding.buttonResetPassword.setOnClickListener {
            val phone = binding.inputPhoneForgot.text?.toString()?.trim()
            if (phone.isNullOrEmpty()) {
                binding.inputLayoutPhone.error = "Ingrese su número de teléfono"
            } else {
                binding.inputLayoutPhone.error = null
                val intent = Intent(this, ResetPasswordActivity::class.java)
                intent.putExtra("PHONE_NUMBER", phone) // opcional, si quieres pasar el número
                startActivity(intent)
            }
        }
    }
}

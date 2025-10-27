package com.sena.monitoreo.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sena.monitoreo.databinding.ActivityLoginBinding
import com.sena.monitoreo.ui.user.HomeUserActivity
import com.sena.monitoreo.ui.admin.HomeAdminActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajuste insets
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
                if (!phone.isNullOrEmpty() && !password.isNullOrEmpty()) android.view.View.VISIBLE
                else android.view.View.GONE
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

            // 📌 Verificar credenciales
            if (phone == "1" && password == "a") {
                val intent = Intent(this, HomeAdminActivity::class.java)
                startActivity(intent)
            } else {
                // 👉 Usuario normal
                val intent = Intent(this, HomeUserActivity::class.java)
                startActivity(intent)
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


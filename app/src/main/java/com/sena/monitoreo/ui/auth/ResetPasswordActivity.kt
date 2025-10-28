package com.sena.monitoreo.ui.auth

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var inputNewPassword: TextInputEditText
    private lateinit var inputConfirmPassword: TextInputEditText
    private lateinit var buttonSetPassword: MaterialButton
    private val TAG = "ResetPasswordActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reset_password)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.container_reset)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inputNewPassword = findViewById(R.id.input_new_password)
        inputConfirmPassword = findViewById(R.id.input_confirm_new_password)
        buttonSetPassword = findViewById(R.id.button_set_new_password)
        val backArrow = findViewById<ImageView>(R.id.back_arrow)

        val phone = intent.getStringExtra("PHONE_NUMBER")

        buttonSetPassword.isEnabled = false

        val watcher = {
            val newPass = inputNewPassword.text.toString().trim()
            val confirmPass = inputConfirmPassword.text.toString().trim()
            buttonSetPassword.isEnabled = newPass.isNotEmpty() && confirmPass.isNotEmpty()
        }

        inputNewPassword.addTextChangedListener { watcher() }
        inputConfirmPassword.addTextChangedListener { watcher() }

        buttonSetPassword.setOnClickListener {
            val newPass = inputNewPassword.text.toString()
            val confirmPass = inputConfirmPassword.text.toString()

            if (newPass != confirmPass) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 6) {
                Toast.makeText(this, "Debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (phone == null) {
                Toast.makeText(this, "Error: teléfono no recibido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            cambiarContrasena(phone, newPass)
        }

        backArrow.setOnClickListener { finish() }
    }

    private fun cambiarContrasena(phone: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Cambiando contraseña para $phone")
                val response = RetrofitClient.apiAuth.updatePassword(
                    mapOf(
                        "telefono" to phone,
                        "nueva_contrasena" to newPassword,
                        "confirmar_contrasena" to newPassword
                    )
                )
                Log.d(TAG, "Respuesta del servidor: ${response.code()}")

                if (response.isSuccessful) {
                    Toast.makeText(this@ResetPasswordActivity, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error al actualizar: $errorBody")
                    Toast.makeText(this@ResetPasswordActivity, "Error al actualizar la contraseña", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error de conexión", e)
                Toast.makeText(this@ResetPasswordActivity, "Error de conexión con el servidor", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

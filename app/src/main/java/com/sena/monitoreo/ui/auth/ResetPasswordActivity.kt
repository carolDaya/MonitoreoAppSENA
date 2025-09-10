package com.sena.monitoreo.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.sena.monitoreo.R
import com.google.android.material.button.MaterialButton

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var inputNewPassword: TextInputEditText
    private lateinit var inputConfirmPassword: TextInputEditText
    private lateinit var buttonSetPassword: MaterialButton

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

        // Inicialmente desactivado hasta que se escriban contraseñas
        buttonSetPassword.isEnabled = false

        val textWatcher = {
            val newPass = inputNewPassword.text.toString().trim()
            val confirmPass = inputConfirmPassword.text.toString().trim()
            buttonSetPassword.isEnabled = newPass.isNotEmpty() && confirmPass.isNotEmpty()
        }

        inputNewPassword.addTextChangedListener { textWatcher() }
        inputConfirmPassword.addTextChangedListener { textWatcher() }

        // Acción del botón
        buttonSetPassword.setOnClickListener {
            val newPass = inputNewPassword.text.toString()
            val confirmPass = inputConfirmPassword.text.toString()

            if (newPass != confirmPass) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            } else if (newPass.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Contraseña restablecida correctamente", Toast.LENGTH_SHORT).show()
                finish() // Cierra la actividad o navega a login
            }
        }

        // Botón de regresar
        findViewById<android.widget.ImageView>(R.id.back_arrow).setOnClickListener {
            finish()
        }
    }
}

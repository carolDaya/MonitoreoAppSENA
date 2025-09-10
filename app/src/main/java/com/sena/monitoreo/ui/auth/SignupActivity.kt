package com.sena.monitoreo.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sena.monitoreo.R
import com.sena.monitoreo.databinding.ActivitySignupBinding
import com.sena.monitoreo.ui.user.HomeUserActivity
import androidx.core.net.toUri

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajuste de los márgenes de la ventana para una visualización de borde a borde
        ViewCompat.setOnApplyWindowInsetsListener(binding.containerSignup) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Lógica del botón de retroceso para cerrar la actividad
        binding.backArrow.setOnClickListener {
            finish()
        }

        // Navegación a la pantalla de inicio de sesión cuando se hace clic en el enlace
        binding.textLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Lógica para el clic en "Términos y condiciones"
        binding.textViewTermsAndConditions.setOnClickListener {
            Toast.makeText(this, "Redireccionando a los términos y condiciones...", Toast.LENGTH_SHORT).show()
            val url = "www.google.com" // Ejemplo de URL
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }

        // Lógica del botón de registro: valida el formulario y navega si es correcto
        binding.buttonRegister.setOnClickListener {
            if (validateForm()) {
                // Si la validación es exitosa, muestra un mensaje y navega a la siguiente pantalla
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomeUserActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    /**
     * Valida todos los campos del formulario de registro.
     * @return true si todos los campos son válidos, false en caso contrario.
     */
    private fun validateForm(): Boolean {
        val name = binding.editTextName.text.toString().trim()
        val phone = binding.editTextPhone.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()
        val confirmPassword = binding.inputConfirmPassword.text.toString().trim()
        val termsAccepted = binding.checkboxTerms.isChecked

        // Limpia los errores de los campos para la validación actual
        binding.textInputLayoutName.error = null
        binding.textInputLayoutPhone.error = null
        binding.inputPasswordLayout.error = null
        binding.textInputLayoutConfirmPassword.error = null

        // Comprueba si el nombre está vacío
        if (name.isBlank()) {
            binding.textInputLayoutName.error = getString(R.string.error_field_required)
            return false
        }

        // Comprueba si el teléfono está vacío
        if (phone.isBlank()) {
            binding.textInputLayoutPhone.error = getString(R.string.error_field_required)
            return false
        }

        // Comprueba si la contraseña está vacía
        if (password.isBlank()) {
            binding.inputPasswordLayout.error = getString(R.string.error_field_required)
            return false
        }

        // Comprueba si la confirmación de la contraseña está vacía
        if (confirmPassword.isBlank()) {
            binding.textInputLayoutConfirmPassword.error = getString(R.string.error_field_required)
            return false
        }

        // Verifica si las contraseñas coinciden
        if (password != confirmPassword) {
            binding.textInputLayoutConfirmPassword.error = getString(R.string.error_passwords_not_match)
            return false
        }

        // Verifica si se aceptaron los términos y condiciones
        if (!termsAccepted) {
            Toast.makeText(this, getString(R.string.error_accept_terms), Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}
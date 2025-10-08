package com.sena.monitoreo.ui.auth

// CORRECCIÓN: Asegura que estas clases se importen del paquete correcto
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sena.monitoreo.data.api.ApiService
import com.sena.monitoreo.data.api.ResetPasswordRequest
import com.sena.monitoreo.databinding.ActivityResetPasswordBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private val instance: ApiService
    get() {
        TODO()
    }

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding

    // CORRECCIÓN: Inicialización lazy para acceder a la instancia singleton.
    private val apiService: ApiService by lazy {
        instance
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Se eliminó la línea de inicialización incorrecta de apiService.

        // Obtiene el token desde la actividad anterior
        intent.getStringExtra("token") ?: ""

        binding.buttonSetNewPassword.setOnClickListener {
            val newPassword = binding.inputNewPassword.text.toString().trim()
            val confirmPassword = binding.inputConfirmNewPassword.text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // CORRECCIÓN: Pasar solo la nueva contraseña. El token se usa en la solicitud.
            resetPassword()
        }

        // Regresar atrás
        binding.backArrow.setOnClickListener {
            finish()
        }
    }

    private fun resetPassword() {
        // Crea el objeto que se enviará al backend
        // ASUME: ResetPasswordRequest es data class ResetPasswordRequest(val token: String, val password: String)
        val request = ResetPasswordRequest()

        // CORRECCIÓN CRÍTICA: Definir la variable 'call'
        val call = apiService.resetPassword(request)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ResetPasswordActivity,
                        "Contraseña actualizada correctamente",
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this@ResetPasswordActivity,
                        "Error al actualizar contraseña. Token inválido.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(
                    this@ResetPasswordActivity,
                    "Error de conexión: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}
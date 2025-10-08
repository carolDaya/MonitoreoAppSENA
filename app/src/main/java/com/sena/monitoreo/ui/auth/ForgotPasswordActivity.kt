package com.sena.monitoreo.ui.auth

// Importaciones correctas asumiendo que consolidaste las clases en 'api'
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.sena.monitoreo.data.api.ApiService
import com.sena.monitoreo.data.api.ForgotPasswordRequest
import com.sena.monitoreo.databinding.ActivityForgotPasswordBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private val instance: ApiService
    get() {
        TODO()
    }

class ForgotPasswordActivity : AppCompatActivity() { // <- Propiedad dentro de la Activity

    private lateinit var binding: ActivityForgotPasswordBinding

    // CORRECCIÓN: La inicialización de apiService DEBE ir aquí, en la Activity.
    private val apiService: ApiService by lazy {
        instance // <- Esto ahora debería ser reconocido si RetrofitClient.kt está bien
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajuste visual de bordes
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
            showLoading(true)

            // CORRECCIÓN: Crea el objeto de solicitud con el teléfono.
            val request = ForgotPasswordRequest()
            sendForgotPasswordRequest(request)
        }
    }

    private fun sendForgotPasswordRequest(request: ForgotPasswordRequest) {
        // CORRECCIÓN: Llama al método de la API y obtiene el Call<Void>
        val call = apiService.forgotPassword(request)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                showLoading(false)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Se ha enviado un código a su número registrado. Verifique su SMS.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@ForgotPasswordActivity,
                        "Número no registrado o error del servidor.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showLoading(false)
                Toast.makeText(
                    this@ForgotPasswordActivity,
                    "Error de conexión: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    // Función para mostrar/ocultar el estado de carga
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonResetPassword.isEnabled = !isLoading
    }
}
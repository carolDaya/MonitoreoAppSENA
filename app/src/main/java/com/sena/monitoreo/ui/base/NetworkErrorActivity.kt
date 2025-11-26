package com.sena.monitoreo.ui.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.sena.monitoreo.databinding.ActivityNetworkErrorBinding

class NetworkErrorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNetworkErrorBinding

    companion object {
        private const val EXTRA_ERROR_MESSAGE = "error_message"

        fun start(context: Context, message: String? = null) {
            val intent = Intent(context, NetworkErrorActivity::class.java).apply {
                putExtra(EXTRA_ERROR_MESSAGE, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            // Animación simple de entrada
            if (context is AppCompatActivity) {
                context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityNetworkErrorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animación simple al crear
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        setupUI()
    }

    private fun setupUI() {
        val errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE)
            ?: "Problema de conexión. Verifica tu internet e intenta nuevamente."

        binding.tvErrorMessage.text = errorMessage

        binding.btnRetry.setOnClickListener {
            finishWithAnimation()
        }

        binding.btnClose.setOnClickListener {
            finishAffinity() // Cierra todas las actividades
        }
    }

    private fun finishWithAnimation() {
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onBackPressed() {
        // ✅ CORREGIDO: Llamar al super primero
        super.onBackPressed()
        // Luego nuestra animación personalizada
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
package com.sena.monitoreo.ui.user

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.sena.monitoreo.R

class AlertsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alerts)

        // Ajustar insets del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.alerta_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Recuperar el texto que llega del Intent
        val message = intent.getStringExtra("alert_message")
            ?: getString(R.string.sensor_waiting_data)

        // 2. Ponerlo en el TextView
        val textView = findViewById<TextView>(R.id.ia_response_text)
        textView.text = message

        // 3. Botón OK que cierra la alerta
        val button = findViewById<MaterialButton>(R.id.button_message)
        button.setOnClickListener {
            finish()
        }
    }
}

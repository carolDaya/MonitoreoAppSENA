package com.sena.monitoreo.ui.user

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.masoudss.lib.WaveformSeekBar
import com.sena.monitoreo.R
import com.sena.monitoreo.databinding.ActivityHomeUserBinding
import com.sena.monitoreo.ui.auth.LoginActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

class HomeUserActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityHomeUserBinding
    private var tts: TextToSpeech? = null
    private var isSpeaking = false
    private lateinit var waveformSeekBar: WaveformSeekBar
    private lateinit var btnPlay: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar TextToSpeech
        tts = TextToSpeech(this, this)

        // Inicializar vistas del waveform
        initWaveformViews()

        // --- Menú lateral ---
        // Conecta el icono de ajustes para abrir el menú
        binding.mainHeader.settingsIcon.setOnClickListener {
            binding.homeAdmin.openDrawer(GravityCompat.START)
        }

        // Configurar NavigationView
        setupNavigationView()
    }

    private fun setupNavigationView() {
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Ya estás en home, simplemente cierra el drawer
                    Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_datos_gas -> {
                    // Navegar a SensorDataActivity con tipo GAS
                    val intent = Intent(this, SensorDataActivity::class.java)
                    intent.putExtra("SENSOR_TYPE", "GAS")
                    startActivity(intent)
                }
                R.id.nav_datos_tem -> {
                    // Navegar a SensorDataActivity con tipo TEMPERATURA
                    val intent = Intent(this, SensorDataActivity::class.java)
                    intent.putExtra("SENSOR_TYPE", "TEMPERATURA")
                    startActivity(intent)
                }
                R.id.nav_datos_presion -> {
                    // Navegar a SensorDataActivity con tipo PRESION
                    val intent = Intent(this, SensorDataActivity::class.java)
                    intent.putExtra("SENSOR_TYPE", "PRESION")
                    startActivity(intent)
                }
                R.id.nav_settings -> {
                    // Acción para la configuración
                    Toast.makeText(this, "Configuración Usuario", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_logout -> {
                    // Detener TTS si está hablando
                    stopSpeaking()

                    // Lógica para cerrar sesión
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().clear().apply()

                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                    Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                }
            }
            binding.homeAdmin.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun initWaveformViews() {
        // Acceso al waveform dentro del header
        val waveformBinding = binding.mainHeader.waveformSection

        waveformSeekBar = waveformBinding.waveformSeekBar
        btnPlay = waveformBinding.btnPlayMessage

        setupWaveformSamples()

        btnPlay.setOnClickListener {
            if (!isSpeaking) startSpeaking() else stopSpeaking()
        }
    }


    private fun setupWaveformSamples() {
        // Crear datos de muestra para el waveform (simulando audio)
        val samples = IntArray(100) {
            Random.nextInt(10, 100) // Valores aleatorios entre 10 y 100
        }
        waveformSeekBar.setSampleFrom(samples)
        waveformSeekBar.progress = 0f
    }

    private fun startSpeaking() {
        val message = getString(R.string.greeting_text)
        speakText(message)
        isSpeaking = true
        btnPlay.setIconResource(R.drawable.ic_stop)

        // Iniciar animación del waveform
        startWaveformAnimation()
    }

    private fun stopSpeaking() {
        tts?.stop()
        isSpeaking = false
        btnPlay.setIconResource(R.drawable.ic_play)
        waveformSeekBar.progress = 0f
    }

    private fun startWaveformAnimation() {
        lifecycleScope.launch {
            var progress = 0f
            val maxProgress = waveformSeekBar.maxProgress
            val duration = 5000L // 5 segundos para el mensaje

            while (isSpeaking && progress < maxProgress && tts?.isSpeaking == true) {
                progress += (maxProgress / (duration / 50)).toFloat()
                waveformSeekBar.progress = progress.coerceAtMost(maxProgress)

                // Simular movimiento de ondas cambiando los samples dinámicamente
                val dynamicSamples = IntArray(100) {
                    Random.nextInt(5, 95)
                }
                waveformSeekBar.setSampleFrom(dynamicSamples)

                delay(50L)
            }

            // Cuando termina de hablar
            if (isSpeaking) {
                isSpeaking = false
                btnPlay.setIconResource(R.drawable.ic_play)
                waveformSeekBar.progress = 0f
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Idioma no compatible", Toast.LENGTH_SHORT).show()
                btnPlay.isEnabled = false
            } else {
                btnPlay.isEnabled = true
            }
        } else {
            Toast.makeText(this, "Error con TextToSpeech", Toast.LENGTH_SHORT).show()
            btnPlay.isEnabled = false
        }
    }

    // Reproducir texto
    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
    }

    // Manejar el botón de retroceso para cerrar el drawer si está abierto
    override fun onBackPressed() {
        if (binding.homeAdmin.isDrawerOpen(GravityCompat.START)) {
            binding.homeAdmin.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // Liberar recursos
    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }
}
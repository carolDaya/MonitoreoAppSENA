package com.sena.monitoreo.ui.user

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.masoudss.lib.WaveformSeekBar
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.databinding.ActivityHomeUserBinding
import com.sena.monitoreo.ui.auth.LoginActivity
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModel
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

class HomeUserActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityHomeUserBinding
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var isSpeaking = false
    private lateinit var waveformSeekBar: WaveformSeekBar
    private lateinit var btnPlay: MaterialButton

    // Configuración actual de voz
    private var currentPitch: Float = 1.0f
    private var currentGender: String = "FEMALE"

    // Voces preferidas
    private val preferredMaleVoices = listOf("male", "hombre", "masculino", "man", "mfb")
    private val preferredFemaleVoices = listOf("female", "mujer", "femenino", "woman", "efb")

    // ViewModel
    private val viewModel: AdminConfigViewModel by lazy {
        val repository = VoiceRepository(RetrofitClient.apiVoice)
        ViewModelProvider(this, AdminConfigViewModelFactory(repository))
            .get(AdminConfigViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar TextToSpeech
        tts = TextToSpeech(this, this)

        // Inicializar waveform
        initWaveformViews()

        // Cargar configuración de voz
        loadVoiceConfiguration()

        // --- Menú lateral ---
        binding.mainHeader.settingsIcon.setOnClickListener {
            binding.homeAdmin.openDrawer(GravityCompat.START)
        }

        setupNavigationView()
    }

    private fun loadVoiceConfiguration() {
        viewModel.loadCurrentConfig()

        viewModel.currentConfig.observe(this) { config ->
            currentPitch = config.pitch
            currentGender = config.gender

            if (ttsReady) {
                applyTtsSettings()
                lifecycleScope.launch {
                    delay(500)
                    speakText("Configuración de voz actualizada")
                }
            }
        }
    }

    private fun applyTtsSettings() {
        if (tts == null || !ttsReady) return

        val locale = Locale("es", "ES")
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "Instale voces en español", Toast.LENGTH_LONG).show()
            btnPlay.isEnabled = false
            return
        }

        when (currentGender.uppercase()) {
            "MALE" -> setupMaleVoice(locale)
            "FEMALE" -> setupFemaleVoice(locale)
            "ROBOTIC" -> setupRoboticVoice()
            else -> setupFemaleVoice(locale)
        }

        tts?.setPitch(mapPitchValue(currentPitch))
        btnPlay.isEnabled = true
    }

    private fun setupMaleVoice(locale: Locale) {
        tts?.voices?.find {
            it.locale.language == "es" &&
                    preferredMaleVoices.any { pref -> it.name.contains(pref, true) }
        }?.let {
            tts?.voice = it
            Toast.makeText(this, "Voz masculina activada", Toast.LENGTH_SHORT).show()
            return
        }
        tts?.setPitch(0.8f)
        tts?.setSpeechRate(0.9f)
    }

    private fun setupFemaleVoice(locale: Locale) {
        tts?.voices?.find {
            it.locale.language == "es" &&
                    preferredFemaleVoices.any { pref -> it.name.contains(pref, true) }
        }?.let {
            tts?.voice = it
            Toast.makeText(this, "Voz femenina activada", Toast.LENGTH_SHORT).show()
            return
        }
        tts?.setPitch(1.1f)
        tts?.setSpeechRate(1.0f)
    }

    private fun setupRoboticVoice() {
        val locale = Locale("es", "ES")
        tts?.voices?.find { it.locale.language == "es" }?.let {
            tts?.voice = it
        }
        tts?.setPitch(0.3f)
        tts?.setSpeechRate(0.75f)
        Toast.makeText(this, "Modo robótico activado", Toast.LENGTH_SHORT).show()
    }

    private fun mapPitchValue(value: Float): Float {
        return when (value) {
            0.8f -> 0.6f
            1.0f -> 1.0f
            1.3f -> 1.6f
            else -> value.coerceIn(0.5f, 2.0f)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            applyTtsSettings()
            Toast.makeText(this, "TTS inicializado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error al iniciar TTS", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initWaveformViews() {
        val waveformBinding = binding.mainHeader.waveformSection
        waveformSeekBar = waveformBinding.waveformSeekBar
        btnPlay = waveformBinding.btnPlayMessage
        setupWaveformSamples()
        btnPlay.setOnClickListener {
            if (!isSpeaking) startSpeaking() else stopSpeaking()
        }
    }

    private fun setupWaveformSamples() {
        val samples = IntArray(100) { Random.nextInt(10, 100) }
        waveformSeekBar.setSampleFrom(samples)
        waveformSeekBar.progress = 0f
    }

    private fun startSpeaking() {
        val message = getString(R.string.greeting_text)
        speakText(message)
        isSpeaking = true
        btnPlay.setIconResource(R.drawable.ic_stop)
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
            val max = waveformSeekBar.maxProgress
            while (isSpeaking && tts?.isSpeaking == true) {
                progress += 1
                waveformSeekBar.progress = progress.coerceAtMost(max)
                delay(50)
            }
            stopSpeaking()
        }
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
    }

    private fun setupNavigationView() {
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show()
                R.id.nav_datos_gas -> startActivity(Intent(this, SensorDataActivity::class.java).putExtra("SENSOR_TYPE", "GAS"))
                R.id.nav_datos_tem -> startActivity(Intent(this, SensorDataActivity::class.java).putExtra("SENSOR_TYPE", "TEMPERATURA"))
                R.id.nav_datos_presion -> startActivity(Intent(this, SensorDataActivity::class.java).putExtra("SENSOR_TYPE", "PRESION"))
                R.id.nav_logout -> {
                    stopSpeaking()
                    getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                }
            }
            binding.homeAdmin.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }
}

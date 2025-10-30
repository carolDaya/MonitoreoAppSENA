package com.sena.monitoreo.ui.user

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.masoudss.lib.WaveformSeekBar
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.repository.AnalisisRepository
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.databinding.ActivityAlertsBinding
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModel
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class AlertsActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityAlertsBinding
    private val TAG = "AlertsActivity"
    private val analisisRepo = AnalisisRepository()

    // PROPIEDADES PARA VOZ Y ONDAS
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var isSpeaking = false
    private lateinit var waveformSeekBar: WaveformSeekBar
    private lateinit var btnPlay: MaterialButton

    // 🆕 Variables para almacenar TODOS los datos de la alerta
    private var fullAlertMessage: String = "" // Mensaje completo que se reproducirá

    // PROPIEDADES DE CONFIGURACIÓN DE VOZ
    private var currentPitch: Float = 1.0f
    private var currentGender: String = "FEMALE"
    private val preferredMaleVoices = listOf("male", "hombre", "masculino", "man", "mfb")
    private val preferredFemaleVoices = listOf("female", "mujer", "femenino", "woman", "efb")

    // ViewModel para configuración de voz
    private val viewModel: AdminConfigViewModel by lazy {
        val repository = VoiceRepository(RetrofitClient.apiVoice)
        ViewModelProvider(this, AdminConfigViewModelFactory(repository))
            .get(AdminConfigViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajustar insets del sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.alertaLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // INICIALIZAR VOZ/ONDAS y CONFIGURACIÓN
        tts = TextToSpeech(this, this)
        initWaveformViews()
        loadVoiceConfiguration()

        // 🆕 CARGAR DATOS COMPLETOS DE LA ALERTA
        loadAlertData()

        // Botón para cerrar la alerta
        binding.buttonMessage.setOnClickListener {
            stopSpeaking()
            finish()
        }
    }

    // 🆕 Cargar los datos completos de la alerta desde el backend
    private fun loadAlertData() {
        lifecycleScope.launch {
            try {
                val analisis = analisisRepo.analizarLectura()

                if (analisis != null && analisis.alerta_ia == 1) {
                    // 🎯 CONSTRUIR EL MENSAJE COMPLETO CON TODA LA INFORMACIÓN IMPORTANTE
                    val mensaje = analisis.mensaje_lectura
                    val tipoEstado = analisis.tipo_estado ?: "Estado desconocido"
                    val tipoAlerta = analisis.tipo_alerta_modelo ?: "Alerta general"
                    val recomendacion = analisis.recomendacion ?: "Sin recomendaciones"

                    // 📝 Mostrar en la interfaz (TextView)
                    val displayText = buildString {
                        appendLine("-$tipoEstado")
                        appendLine()
                        appendLine("-Tipo de Alerta: $tipoAlerta")
                        appendLine()
                        appendLine("-Recomendación:")
                        appendLine(recomendacion)
                    }

                    binding.iaResponseText.text = displayText

                    // 🔊 Mensaje para reproducir por voz (SIN mensaje_lectura)
                    fullAlertMessage = buildString {
                        append("Atención. ")
                        append("$tipoEstado. ")
                        append("Tipo de alerta: $tipoAlerta. ")
                        append("Recomendación: ")
                        append(recomendacion)
                    }

                    Log.d(TAG, "✅ Alerta cargada: $tipoEstado")
                    Log.d(TAG, "🔊 Mensaje a reproducir: $fullAlertMessage")

                } else {
                    // Si no hay alerta, mostrar mensaje genérico
                    val fallbackMessage = intent.getStringExtra("alert_message")
                        ?: "No hay alertas activas en este momento"

                    binding.iaResponseText.text = fallbackMessage
                    fullAlertMessage = fallbackMessage

                    Log.w(TAG, "⚠️ No se detectó alerta activa")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al cargar datos de alerta", e)

                // Fallback: usar el mensaje del Intent si existe
                val fallbackMessage = intent.getStringExtra("alert_message")
                    ?: "Error al cargar información de la alerta"

                binding.iaResponseText.text = fallbackMessage
                fullAlertMessage = fallbackMessage
            }
        }
    }

    // ---------------------------------------------------------------------
    //                      LÓGICA DE VOZ Y ONDAS
    // ---------------------------------------------------------------------

    private fun loadVoiceConfiguration() {
        viewModel.loadCurrentConfig()

        viewModel.currentConfig.observe(this) { config ->
            currentPitch = config.pitch
            currentGender = config.gender

            if (ttsReady) {
                applyTtsSettings()
            }
        }
    }

    private fun initWaveformViews() {
        val waveformBinding = binding.waveformSection

        waveformSeekBar = waveformBinding.waveformSeekBar
        btnPlay = waveformBinding.btnPlayMessage
        setupWaveformSamples()

        btnPlay.setOnClickListener {
            if (!isSpeaking) {
                // Esperar a que el mensaje esté cargado
                if (fullAlertMessage.isNotEmpty()) {
                    startSpeaking()
                } else {
                    Log.w(TAG, "⚠️ Esperando a que se cargue el mensaje...")
                }
            } else {
                stopSpeaking()
            }
        }
    }

    private fun setupWaveformSamples() {
        val samples = IntArray(100) { Random.nextInt(10, 100) }
        waveformSeekBar.setSampleFrom(samples)
        waveformSeekBar.progress = 0f
    }

    private fun startSpeaking() {
        if (tts?.isSpeaking == true || fullAlertMessage.isEmpty()) {
            stopSpeaking()
            return
        }

        isSpeaking = true
        btnPlay.setIconResource(R.drawable.ic_stop)

        // 🔊 REPRODUCE EL MENSAJE COMPLETO (con recomendaciones)
        speakText(fullAlertMessage)
        startWaveformAnimation(fullAlertMessage.length)

        Log.d(TAG, "🔊 Reproduciendo alerta completa")
    }

    private fun stopSpeaking() {
        tts?.stop()
        isSpeaking = false
        btnPlay.setIconResource(R.drawable.ic_play)
        waveformSeekBar.progress = 0f
    }

    private fun startWaveformAnimation(textLength: Int) {
        lifecycleScope.launch {
            val estimatedDurationMs = (textLength * 80).toLong().coerceAtLeast(3000L)
            val steps = estimatedDurationMs / 50L
            val progressStep = waveformSeekBar.maxProgress / steps.toFloat()

            for (i in 0 until steps.toInt()) {
                if (!isSpeaking) break

                waveformSeekBar.progress += progressStep

                val dynamicSamples = IntArray(100) { Random.nextInt(5, 95) }
                waveformSeekBar.setSampleFrom(dynamicSamples)

                delay(50L)
            }
            if (isSpeaking) stopSpeaking()
        }
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
    }

    // ---------------------------------------------------------------------
    //                      CONFIGURACIÓN TTS
    // ---------------------------------------------------------------------

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            applyTtsSettings()

            // 🔊 Reproducir automáticamente cuando esté listo
            lifecycleScope.launch {
                // Esperar un poco a que se carguen los datos
                delay(1000)
                if (fullAlertMessage.isNotEmpty()) {
                    startSpeaking()
                }
            }
        } else {
            Log.e(TAG, "Error con TextToSpeech: $status")
            btnPlay.isEnabled = false
        }
    }

    private fun applyTtsSettings() {
        if (tts == null || !ttsReady) return

        val locale = Locale("es", "ES")
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Idioma de TTS no compatible. Instale voces en español")
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
    }

    private fun mapPitchValue(value: Float): Float {
        return when (value) {
            0.8f -> 0.6f
            1.0f -> 1.0f
            1.3f -> 1.6f
            else -> value.coerceIn(0.5f, 2.0f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }
}
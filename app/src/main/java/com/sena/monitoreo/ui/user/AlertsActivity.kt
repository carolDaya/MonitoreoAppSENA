package com.sena.monitoreo.ui.user

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.ai.AnalisisResponse
import com.sena.monitoreo.data.repository.AnalisisRepository
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.databinding.ActivityAlertsBinding
import com.sena.monitoreo.ui.base.BaseVoiceActivity
import com.sena.monitoreo.ui.base.factory.VoiceConfigViewModelFactory
import com.sena.monitoreo.ui.base.viewmodel.VoiceConfigViewModel
import com.sena.monitoreo.utils.ResultWrapper
import com.sena.monitoreo.utils.UiUtils
import com.sena.monitoreo.utils.alerts.AlertManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AlertsActivity : BaseVoiceActivity() {

    private lateinit var binding: ActivityAlertsBinding
    private lateinit var alertManager: AlertManager
    private val TAG = "AlertsActivity"

    // Repositories
    private val analisisRepo = AnalisisRepository(RetrofitClient.apiAi)
    private val voiceRepo = VoiceRepository(RetrofitClient.apiVoice)

    private val voiceConfigViewModel: VoiceConfigViewModel by lazy {
        val factory = VoiceConfigViewModelFactory(voiceRepo)
        ViewModelProvider(this, factory)[VoiceConfigViewModel::class.java]
    }

    // Variables para almacenar TODOS los datos de la alerta
    private var fullAlertMessage: String = ""
    private var alertData: AnalisisResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.alertaLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // VERIFICAR SI VIENE CON DATOS COMPLETOS DEL INTENT
        val alertDataFromIntent = intent.getSerializableExtra("alert_data") as? AnalisisResponse
        if (alertDataFromIntent != null) {
            Log.d(TAG, "✅ Datos COMPLETOS recibidos del intent")
            Log.d(TAG, "- mensaje_lectura: ${alertDataFromIntent.mensaje_lectura}")
            Log.d(TAG, "- recomendacion: ${alertDataFromIntent.recomendacion}")
            Log.d(TAG, "- tipo_estado: ${alertDataFromIntent.tipo_estado}")
            Log.d(TAG, "- tipo_alerta_modelo: ${alertDataFromIntent.tipo_alerta_modelo}")

            handleAlertData(alertDataFromIntent) // Usar datos directamente
            initializeManagers()
            setupVoiceConfiguration()
            setupUI()
            return // NO cargar desde red si ya tenemos datos
        }

        // Si no hay datos en el intent, verificar mensaje simple
        val alertMessageFromIntent = intent.getStringExtra("alert_message")
        if (!alertMessageFromIntent.isNullOrEmpty()) {
            Log.d(TAG, "⚠️ Solo mensaje recibido del intent")
            binding.iaResponseText.text = alertMessageFromIntent
            fullAlertMessage = alertMessageFromIntent
        }

        initializeManagers()
        setupVoiceConfiguration()
        setupUI()

        // Solo cargar datos si no vinieron del intent
        if (alertDataFromIntent == null && alertMessageFromIntent == null) {
            loadAlertData()
        }
    }

    override fun onNetworkRetry() {
        Log.d(TAG, "onNetworkRetry: Reintentando carga de alerta y configuración de voz...")
        // Reintentar cargar la configuración de voz
        voiceConfigViewModel.loadCurrentConfig()
        // Reintentar cargar los datos de la alerta
        loadAlertData()
    }

    private fun initializeManagers() {
        alertManager = AlertManager(
            context = this,
            analisisRepo = analisisRepo,
            onAlertDetected = { _ -> /* No necesario aquí ya que estamos en la activity de alerta */ },
            onError = { errorMessage ->
                runOnUiThread {
                    // Solo mostrar snackbar si no es un error de red
                    if (!errorMessage.contains("Error de red", ignoreCase = true) &&
                        !errorMessage.contains("IOException", ignoreCase = true)
                    ) {
                        UiUtils.showSnackbar(binding.root, "Error: $errorMessage", true)
                    }
                }
            }
        )
    }

    private fun setupVoiceConfiguration() {
        // Configurar waveform components usando el método de la clase base
        setupWaveformComponents(
            binding.waveformSection.waveformSeekBar,
            binding.waveformSection.btnPlayMessage
        )

        // 1. Cargar configuración de voz UNA SOLA VEZ al inicio
        voiceConfigViewModel.loadCurrentConfig()

        // 2. Observar cambios de configuración de manera CENTRALIZADA
        lifecycleScope.launch {
            voiceConfigViewModel.currentConfig.collectLatest { config ->
                Log.d(
                    TAG,
                    "Nueva configuración de voz recibida: ${config.voiceGender}, pitch: ${config.voicePitch}"
                )

                // Aplicar configuración inmediatamente
                voiceManager.currentPitch = config.voicePitch.toFloat()
                voiceManager.currentGender = config.voiceGender

                // Forzar re-aplicación de settings si TTS ya está inicializado
                if (isVoiceInitialized) {
                    voiceManager.applyTtsSettings()
                    Log.d(TAG, "Configuración de voz aplicada: ${config.voiceGender}")
                }
            }
        }

        // 3. Observar solo errores de red (separado del flujo de datos)
        lifecycleScope.launch {
            voiceConfigViewModel.uiState.collectLatest { state ->
                when (state) {
                    is VoiceConfigViewModel.VoiceConfigUiState.Error -> {
                        Log.e(TAG, "Error de Config. Voz: ${state.message}")
                        if (state.message.contains("Error de red", ignoreCase = true) ||
                            state.message.contains("IOException", ignoreCase = true)
                        ) {
                            showNetworkError(state.message)
                        } else {
                            // Mostrar otros errores como snackbar
                            UiUtils.showSnackbar(binding.root, state.message, true)
                        }
                    }

                    VoiceConfigViewModel.VoiceConfigUiState.Success -> {
                        // Éxito, no hacer nada especial
                    }

                    else -> {}
                }
            }
        }
    }

    private fun setupUI() {
        // Botón para cerrar la alerta
        binding.buttonMessage.setOnClickListener {
            stopSpeaking()
            finish()
        }

        // Configurar el botón de play/pause del waveform
        binding.waveformSection.btnPlayMessage.setOnClickListener {
            if (!voiceManager.isSpeaking) {
                startSpeaking()
            } else {
                stopSpeaking()
            }
        }
    }

    /**
     * Carga los datos completos de la alerta desde el backend o maneja el error.
     */
    private fun loadAlertData() {
        if (fullAlertMessage.isNotEmpty()) {
            return
        }

        lifecycleScope.launch {
            UiUtils.showLoading(this@AlertsActivity, "Cargando alerta...")

            try {
                Log.d(TAG, "🔄 Consultando backend para análisis...")
                val analisisResult = analisisRepo.analizarLectura()

                when (analisisResult) {
                    is ResultWrapper.Success -> {
                        val analisis = analisisResult.data
                        Log.d(TAG, "📦 Respuesta recibida del backend")
                        Log.d(TAG, "  alerta_ia: ${analisis.alerta_ia}")
                        Log.d(TAG, "  tipo_estado: ${analisis.tipo_estado}")
                        Log.d(TAG, "  mensaje: ${analisis.mensaje_lectura}")

                        // Verificar alerta de múltiples formas
                        val isAlert = when {
                            analisis.alerta_ia == 1 -> true
                            analisis.tipo_estado?.contains("Alerta", ignoreCase = true) == true -> true
                            analisis.tipo_estado?.contains("Crítico", ignoreCase = true) == true -> true
                            analisis.tipo_estado?.contains("Anormal", ignoreCase = true) == true -> true
                            else -> false
                        }

                        if (isAlert) {
                            Log.d(TAG, "✅ CONDICIÓN DE ALERTA CUMPLIDA")
                            handleAlertData(analisis)
                        } else {
                            Log.d(TAG, "❌ NO es alerta. alerta_ia=${analisis.alerta_ia}")
                            handleNoActiveAlerts()
                        }
                    }
                    is ResultWrapper.Error -> {
                        val errorMsg = analisisResult.message ?: "Error desconocido"
                        Log.e(TAG, "❌ Error en la respuesta: $errorMsg")
                        handleError(errorMsg)
                        if (errorMsg.contains("Error de red", ignoreCase = true) ||
                            errorMsg.contains("IOException", ignoreCase = true)) {
                            showNetworkError(errorMsg)
                        } else {

                        }
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Error inesperado: ${e.message}"
                Log.e(TAG, "💥 Excepción: $errorMsg", e)
                handleError(errorMsg)
                if (errorMsg.contains("Error de red", ignoreCase = true) ||
                    errorMsg.contains("IOException", ignoreCase = true)) {
                    showNetworkError(errorMsg)
                } else {

                }
            } finally {
                UiUtils.hideLoading()
            }
        }
    }
    private fun handleAlertData(analisis: com.sena.monitoreo.data.model.ai.AnalisisResponse) {
        alertData = analisis

        // DEBUG: Verificar TODOS los campos
        Log.d(TAG, "DEBUG - AnalisisResponse recibido:")
        Log.d(TAG, "  alerta_ia: ${analisis.alerta_ia} (tipo: ${analisis.alerta_ia?.javaClass?.simpleName})")
        Log.d(TAG, "  mensaje_lectura: ${analisis.mensaje_lectura}")
        Log.d(TAG, "  recomendacion: ${analisis.recomendacion}")
        Log.d(TAG, "  tipo_alerta_modelo: ${analisis.tipo_alerta_modelo}")
        Log.d(TAG, "  tipo_estado: ${analisis.tipo_estado}")

        // Verificar de diferentes formas
        val isAlert = when {
            analisis.alerta_ia == 1 -> true
            analisis.tipo_estado?.contains("Alerta", ignoreCase = true) == true -> true
            analisis.tipo_alerta_modelo?.contains("Anormal", ignoreCase = true) == true -> true
            else -> false
        }

        if (isAlert) {
            // Mostrar alerta completa
            val displayText = buildString {
                analisis.tipo_estado?.let {
                    appendLine(" $it ")
                    appendLine()
                }

                analisis.tipo_alerta_modelo?.let {
                    appendLine("Tipo de Alerta: $it")
                    appendLine()
                }

                analisis.mensaje_lectura?.let {
                    appendLine("Lectura de Sensores:")
                    appendLine(it)
                    appendLine()
                }

                analisis.recomendacion?.let {
                    appendLine("Recomendación:")
                    appendLine(it)
                }
            }

            binding.iaResponseText.text = displayText
            fullAlertMessage = formatAnalysisMessageForTTS(analisis)

            Log.d(TAG, "ALERTA DETECTADA: $displayText")

            if (isVoiceReady()) {
                startAutoPlay()
            }
        } else {
            Log.w(TAG, "Backend dice alerta_ia=1 pero no se detectó como alerta")
            handleNoActiveAlerts()
        }
    }
    private fun handleNoActiveAlerts() {
        val fallbackMessage = "No hay alertas activas en este momento"
        binding.iaResponseText.text = fallbackMessage
        fullAlertMessage = fallbackMessage
        Log.w(TAG, "Backend OK, pero no se detectó alerta activa")
    }

    private fun handleError(errorMessage: String) {
        Log.e(TAG, "Error al cargar alerta: $errorMessage")
        runOnUiThread {
            binding.iaResponseText.text = errorMessage
            fullAlertMessage = errorMessage
            // Solo mostrar snackbar si no es un error de red
            if (!errorMessage.contains("Error de red", ignoreCase = true) &&
                !errorMessage.contains("IOException", ignoreCase = true)
            ) {
                UiUtils.showSnackbar(binding.root, errorMessage, true)
            }
        }
    }

    override fun onVoiceInitialized() {
        // Cuando la voz esté lista, SINCRONIZAR configuración actual
        lifecycleScope.launch {
            // Pequeño delay para asegurar que TTS esté completamente listo
            delay(100)

            // Forzar aplicación de settings actuales
            voiceConfigViewModel.currentConfig.value?.let { config ->
                voiceManager.currentPitch = config.voicePitch.toFloat()
                voiceManager.currentGender = config.voiceGender
                voiceManager.applyTtsSettings()
                Log.d(
                    TAG,
                    "Configuración sincronizada en onVoiceInitialized: ${config.voiceGender}"
                )
            }

            // Solo entonces reproducir si hay mensaje
            if (fullAlertMessage.isNotEmpty()) {
                startAutoPlay()
            }
        }
    }

    private fun startAutoPlay() {
        // Pequeño delay para mejor UX
        lifecycleScope.launch {
            delay(500)
            startSpeaking()
        }
    }

    override fun startSpeaking() {
        // VERIFICAR configuración actual antes de hablar
        voiceConfigViewModel.currentConfig.value?.let { config ->
            if (voiceManager.currentGender != config.voiceGender ||
                voiceManager.currentPitch != config.voicePitch.toFloat()
            ) {

                Log.w(TAG, "Configuración desincronizada, re-aplicando...")
                voiceManager.currentPitch = config.voicePitch.toFloat()
                voiceManager.currentGender = config.voiceGender
                voiceManager.applyTtsSettings()
            }
        }

        super.startSpeaking()

        // Preparar mensaje para TTS
        val ttsMessage = if (alertData != null) {
            formatAnalysisMessageForTTS(alertData!!)
        } else if (fullAlertMessage.isNotEmpty()) {
            fullAlertMessage
        } else {
            "No hay mensaje de alerta disponible"
        }

        Log.d(TAG, "Iniciando reproducción de TTS: ${ttsMessage.length} caracteres")

        // Usar el método de la clase base para hablar con waveform
        speakWithPausesAndWaveform(ttsMessage)
    }

    /**
     * Formatea el mensaje para TTS (más simple y fluido)
     */
    private fun formatAnalysisMessageForTTS(analisis: AnalisisResponse): String {
        return buildString {
            // Versión más corta y fluida para TTS
            analisis.tipo_estado?.let {
                append("$it. ")
            }

            analisis.mensaje_lectura?.let {
                // Simplificar mensaje de lectura para TTS
                val simplified = it
                    .replace("°C", " grados Celsius")
                    .replace("kPa", " kilo pascales")
                    .replace("ppm", " partes por millón")
                    .replace("|", " y ")
                append("$simplified. ")
            }

            analisis.recomendacion?.let {
                append("Recomendación: $it. ")
            }

            analisis.tipo_alerta_modelo?.let {
                append("Tipo de alerta: $it. ")
            }

            analisis.dia_proceso?.let {
                append("Día del proceso: $it. ")
            }

            // Añadir indicador de que es una alerta importante
            if (analisis.alerta_ia == 1) {
                append("Esta es una alerta crítica que requiere atención inmediata.")
            }
        }.trim().ifEmpty { "Información de alerta no disponible." }
    }

    override fun onBackPressed() {
        stopSpeaking()
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        // AlertManager no necesita stop aquí ya que no iniciamos verificación periódica
    }
}
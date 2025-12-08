package com.sena.monitoreo.ui.user

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.repository.AnalisisRepository
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.databinding.ActivityAlertsBinding
import com.sena.monitoreo.ui.base.BaseVoiceActivity
import com.sena.monitoreo.ui.base.factory.VoiceConfigViewModelFactory
import com.sena.monitoreo.ui.base.viewmodel.VoiceConfigViewModel
import com.sena.monitoreo.utils.ResultWrapper
import com.sena.monitoreo.utils.UiUtils
import com.sena.monitoreo.utils.alerts.AlertManager
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
    private var alertData: com.sena.monitoreo.data.model.ai.AnalisisResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ ELIMINADO: setupNetworkErrorHandling - ya no se necesita

        // Ajustar insets del sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.alertaLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeManagers()
        setupVoiceConfiguration()
        setupUI()

        // CARGAR DATOS COMPLETOS DE LA ALERTA
        loadAlertData()
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
            onAlertDetected = { _ -> /* No necesario aquí */ },
            onError = { errorMessage ->
                runOnUiThread {
                    // Solo mostrar snackbar si no es un error de red
                    if (!errorMessage.contains("Error de red", ignoreCase = true) &&
                        !errorMessage.contains("IOException", ignoreCase = true)) {
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
                Log.d(TAG, "🔄 Nueva configuración de voz recibida: ${config.voiceGender}, pitch: ${config.voicePitch}")

                // Aplicar configuración inmediatamente
                voiceManager.currentPitch = config.voicePitch.toFloat()
                voiceManager.currentGender = config.voiceGender

                // Forzar re-aplicación de settings si TTS ya está inicializado
                if (isVoiceInitialized) {
                    voiceManager.applyTtsSettings()
                    Log.d(TAG, "✅ Configuración de voz aplicada: ${config.voiceGender}")
                }
            }
        }

        // 3. Observar solo errores de red (separado del flujo de datos)
        lifecycleScope.launch {
            voiceConfigViewModel.uiState.collectLatest { state ->
                when (state) {
                    is VoiceConfigViewModel.VoiceConfigUiState.Error -> {
                        Log.e(TAG, "Error de Config. Voz: ${state.message}")
                        // 💡 NUEVO ENFOQUE: Usar showNetworkError para errores de red
                        if (state.message.contains("Error de red", ignoreCase = true) ||
                            state.message.contains("IOException", ignoreCase = true)) {
                            showNetworkError(state.message)
                        } else {
                            // Mostrar otros errores como snackbar
                            UiUtils.showSnackbar(binding.root, state.message, true)
                        }
                    }
                    VoiceConfigViewModel.VoiceConfigUiState.Success -> {
                        // Éxito en la carga - no necesita acción específica
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

        // Verificar si llegamos con un mensaje de alerta desde SensorDataActivity
        val alertMessageFromIntent = intent.getStringExtra("alert_message")
        if (!alertMessageFromIntent.isNullOrEmpty()) {
            binding.iaResponseText.text = alertMessageFromIntent
            fullAlertMessage = alertMessageFromIntent
        }
    }

    /**
     * Carga los datos completos de la alerta desde el backend o maneja el error.
     */
    private fun loadAlertData() {
        // Bloquear si ya tenemos mensaje
        if (fullAlertMessage.isNotEmpty()) {
            return
        }

        lifecycleScope.launch {
            UiUtils.showLoading(this@AlertsActivity, "Cargando alerta...")

            try {
                val analisisResult = analisisRepo.analizarLectura()

                when (analisisResult) {
                    is ResultWrapper.Success -> {
                        val analisis = analisisResult.data

                        if (analisis.alerta_ia == 1) {
                            handleAlertData(analisis)
                        } else {
                            handleNoActiveAlerts()
                        }
                    }
                    is ResultWrapper.Error -> {
                        val errorMsg = analisisResult.message ?: "Error desconocido"
                        handleError(errorMsg)
                        // 💡 NUEVO ENFOQUE: Usar showNetworkError para errores de red
                        if (errorMsg.contains("Error de red", ignoreCase = true) ||
                            errorMsg.contains("IOException", ignoreCase = true)) {
                            showNetworkError(errorMsg)
                        }
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Error inesperado: ${e.message}"
                handleError(errorMsg)
                // 💡 NUEVO ENFOQUE: Usar showNetworkError para errores de red
                if (errorMsg.contains("Error de red", ignoreCase = true) ||
                    errorMsg.contains("IOException", ignoreCase = true)) {
                    showNetworkError(errorMsg)
                }
            } finally {
                UiUtils.hideLoading()
            }
        }
    }

    private fun handleAlertData(analisis: com.sena.monitoreo.data.model.ai.AnalisisResponse) {
        alertData = analisis

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

        // 🔊 Mensaje para reproducir por voz - USANDO MÉTODO DE LA CLASE BASE
        fullAlertMessage = formatAnalysisMessage(analisis)

        Log.d(TAG, "✅ Alerta cargada: $tipoEstado")

        // Reproducir automáticamente cuando la voz esté lista
        if (isVoiceReady()) {
            startAutoPlay()
        }
    }

    private fun handleNoActiveAlerts() {
        val fallbackMessage = "No hay alertas activas en este momento"
        binding.iaResponseText.text = fallbackMessage
        fullAlertMessage = fallbackMessage
        Log.w(TAG, "⚠️ Backend OK, pero no se detectó alerta activa")
    }

    private fun handleError(errorMessage: String) {
        Log.e(TAG, "❌ Error al cargar alerta: $errorMessage")
        runOnUiThread {
            binding.iaResponseText.text = errorMessage
            fullAlertMessage = errorMessage
            // Solo mostrar snackbar si no es un error de red
            if (!errorMessage.contains("Error de red", ignoreCase = true) &&
                !errorMessage.contains("IOException", ignoreCase = true)) {
                UiUtils.showSnackbar(binding.root, errorMessage, true)
            }
        }
    }

    override fun onVoiceInitialized() {
        // Cuando la voz esté lista, SINCRONIZAR configuración actual
        lifecycleScope.launch {
            // Pequeño delay para asegurar que TTS esté completamente listo
            kotlinx.coroutines.delay(100)

            // Forzar aplicación de settings actuales
            voiceConfigViewModel.currentConfig.value?.let { config ->
                voiceManager.currentPitch = config.voicePitch.toFloat()
                voiceManager.currentGender = config.voiceGender
                voiceManager.applyTtsSettings()
                Log.d(TAG, "🎯 Configuración sincronizada en onVoiceInitialized: ${config.voiceGender}")
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
            kotlinx.coroutines.delay(500)
            startSpeaking()
        }
    }

    override fun startSpeaking() {
        // VERIFICAR configuración actual antes de hablar
        voiceConfigViewModel.currentConfig.value?.let { config ->
            if (voiceManager.currentGender != config.voiceGender ||
                voiceManager.currentPitch != config.voicePitch.toFloat()) {

                Log.w(TAG, "⚠️ Configuración desincronizada, re-aplicando...")
                voiceManager.currentPitch = config.voicePitch.toFloat()
                voiceManager.currentGender = config.voiceGender
                voiceManager.applyTtsSettings()
            }
        }

        super.startSpeaking()

        if (alertData != null) {
            val fullMessage = formatAnalysisMessage(alertData!!)
            // ✅ CORREGIDO: Usar el método con pausas para texto largo
            speakWithPausesAndWaveform(fullMessage)
            Log.d(TAG, "🔊 Reproduciendo mensaje largo con waveform continuo: ${fullMessage.length} caracteres")
        } else if (fullAlertMessage.isNotEmpty()) {
            // ✅ CORREGIDO: Usar el método con pausas para texto largo
            speakWithPausesAndWaveform(fullAlertMessage)
            Log.d(TAG, "🔊 Reproduciendo mensaje del intent con waveform continuo: ${fullAlertMessage.length} caracteres")
        }
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
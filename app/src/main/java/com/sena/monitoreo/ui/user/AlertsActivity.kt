package com.sena.monitoreo.ui.user

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.repository.AnalisisRepository
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.databinding.ActivityAlertsBinding
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModel
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModelFactory
import com.sena.monitoreo.ui.base.BaseVoiceActivity
import com.sena.monitoreo.utils.UiUtils
import com.sena.monitoreo.utils.alerts.AlertManager
import kotlinx.coroutines.launch

class AlertsActivity : BaseVoiceActivity() {

    private lateinit var binding: ActivityAlertsBinding
    private lateinit var alertManager: AlertManager

    // Repositories
    private val analisisRepo = AnalisisRepository(RetrofitClient.apiAi)

    // ViewModel para configuración de voz
    private val viewModel: AdminConfigViewModel by lazy {
        val repository = VoiceRepository(RetrofitClient.apiVoice)
        ViewModelProvider(this, AdminConfigViewModelFactory(repository))
            .get(AdminConfigViewModel::class.java)
    }

    // Variables para almacenar TODOS los datos de la alerta
    private var fullAlertMessage: String = "" // Mensaje completo que se reproducirá
    private var alertData: com.sena.monitoreo.data.model.ai.AnalisisResponse? = null

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

        initializeManagers()
        setupVoiceConfiguration()
        setupUI()

        // CARGAR DATOS COMPLETOS DE LA ALERTA
        loadAlertData()
    }

    private fun initializeManagers() {
        alertManager = AlertManager(
            context = this,
            analisisRepo = analisisRepo,
            onAlertDetected = { alertMessage ->
                // Esta función se usa para alertas periódicas, no necesaria aquí
            },
            onError = { errorMessage ->
                runOnUiThread {
                    UiUtils.showSnackbar(binding.root, "Error: $errorMessage", true)
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

        viewModel.loadCurrentConfig()
        viewModel.currentConfig.observe(this) { config ->
            voiceManager.currentPitch = config.pitch
            voiceManager.currentGender = config.gender
            if (isVoiceInitialized) {
                voiceManager.applyTtsSettings()
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
        // Si ya tenemos un mensaje del intent, no necesitamos cargar más datos
        if (fullAlertMessage.isNotEmpty()) {
            return
        }

        lifecycleScope.launch {
            UiUtils.showLoading(this@AlertsActivity, "Cargando alerta...")

            try {
                val analisisResult = analisisRepo.analizarLectura()

                if (analisisResult.success != null) {
                    // CASO 1: ÉXITO (Respuesta 200 OK)
                    val analisis = analisisResult.success

                    // Aseguramos que solo se muestre si el backend indica alerta activa (alerta_ia == 1)
                    if (analisis.alerta_ia == 1) {
                        handleAlertData(analisis)
                    } else {
                        // Si el 200 OK no contiene alerta activa
                        handleNoActiveAlerts()
                    }
                } else if (analisisResult.errorMessage != null) {
                    // CASO 2: ERROR CONTROLADO
                    handleError(analisisResult.errorMessage)
                } else {
                    // CASO 3: Error de red o desconocido
                    handleError("Error de conexión al cargar la alerta.")
                }
            } catch (e: Exception) {
                handleError("Error inesperado: ${e.message}")
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
            UiUtils.showSnackbar(binding.root, errorMessage, true)
        }
    }

    override fun onVoiceInitialized() {
        // Cuando la voz esté lista, reproducir automáticamente si hay mensaje
        if (fullAlertMessage.isNotEmpty()) {
            startAutoPlay()
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
        super.startSpeaking()

        if (alertData != null) {
            // 🔊 USAR MÉTODO MEJORADO CON WAVEFORM CONTINUO
            val fullMessage = formatAnalysisMessage(alertData!!)
            speakLongTextWithContinuousWaveform(fullMessage)
            Log.d(TAG, "🔊 Reproduciendo mensaje largo con waveform continuo: ${fullMessage.length} caracteres")
        } else if (fullAlertMessage.isNotEmpty()) {
            // Mensaje simple del intent - también usar método continuo
            speakLongTextWithContinuousWaveform(fullAlertMessage)
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

    companion object {
        private const val TAG = "AlertsActivity"
    }
}
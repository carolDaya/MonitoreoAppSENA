package com.sena.monitoreo.ui.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import com.sena.monitoreo.data.model.admin.*
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.data.Entry
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.repository.*
import com.sena.monitoreo.databinding.ActivitySensorDataBinding
import com.sena.monitoreo.ui.admin.viewmodel.ProcesoViewModel
import com.sena.monitoreo.ui.admin.factory.ProcesoViewModelFactory
import com.sena.monitoreo.ui.base.BaseVoiceActivity
import com.sena.monitoreo.ui.base.factory.VoiceConfigViewModelFactory
import com.sena.monitoreo.ui.base.viewmodel.VoiceConfigViewModel
import com.sena.monitoreo.utils.ResultWrapper
import com.sena.monitoreo.utils.UiUtils
import com.sena.monitoreo.utils.alerts.AlertManager
import com.sena.monitoreo.utils.charts.ChartManager
import com.sena.monitoreo.utils.navigation.NavigationManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class SensorDataActivity : BaseVoiceActivity() {

    private lateinit var binding: ActivitySensorDataBinding
    private lateinit var navigationManager: NavigationManager
    private lateinit var alertManager: AlertManager
    private lateinit var chartManager: ChartManager
    private val TAG = "SensorDataActivity"

    // Repositories
    private val graficasRepo = GraficasRepository()
    private val lecturaRepo = LecturaRepository()
    private val analisisRepo = AnalisisRepository(RetrofitClient.apiAi)
    private val voiceRepo = VoiceRepository(RetrofitClient.apiVoice)

    // ViewModels
    private val voiceConfigViewModel: VoiceConfigViewModel by lazy {
        val factory = VoiceConfigViewModelFactory(voiceRepo)
        ViewModelProvider(this, factory)[VoiceConfigViewModel::class.java]
    }

    private val procesoViewModel: ProcesoViewModel by lazy {
        val repository = ProcesoRepository(RetrofitClient.apiProceso)
        ViewModelProvider(this, ProcesoViewModelFactory(repository))
            .get(ProcesoViewModel::class.java)
    }

    private val refreshTime = 30 * 1000L // 30 segundos
    private var cachedConfiguraciones: List<GraficaResponse>? = null
    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySensorDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ ELIMINADO: setupNetworkErrorHandling - ya no se necesita

        initializeManagers()
        setupNavigation()
        setupVoiceConfiguration()
        setupAlertTestButton()

        // Manejar navegación desde el menú
        handleIntentNavigation()

        // CARGAR CONFIGURACIONES UNA SOLA VEZ AL INICIO
        lifecycleScope.launch {
            loadConfiguraciones()
        }

        // Observar estado del proceso
        procesoViewModel.isProcesoActivo.observe(this) { activo ->
            if (activo != null) {
                Log.d(TAG, "📊 Estado del proceso OBSERVADO: $activo")
                lifecycleScope.launch {
                    loadSensorsAndCharts(activo)
                }
            }
        }

        // Cargar estado inicial del proceso
        lifecycleScope.launch {
            Log.d(TAG, "🔄 Cargando estado inicial del proceso...")
            procesoViewModel.loadProcesoStatus()
        }

        startChartRefreshLoop()
    }

    /**
     * Manejar navegación desde intent (scroll a cards específicas)
     */
    private fun handleIntentNavigation() {
        intent.getStringExtra("SENSOR_TYPE")?.let { sensorType ->
            lifecycleScope.launch {
                delay(1000) // Esperar a que la UI esté completamente cargada
                navigateToCard(sensorType)
            }
        }
    }

    /**
     * Navegación por scroll a cards específicas - MÉTODO PÚBLICO
     */
    fun navigateToCard(cardType: String) {
        Log.d(TAG, "🎯 Navegando a card: $cardType")

        when (cardType.uppercase()) {
            "GAS" -> {
                binding.mainScroll.post {
                    // 💡 CORRECCIÓN: Scroll directo al card específico
                    val cardView = binding.cardMq4.root
                    val top = cardView.top
                    binding.mainScroll.smoothScrollTo(0, top - 150) // Offset para mejor visualización
                    UiUtils.showSnackbar(binding.root, "Desplazando a Gas Metano", false)
                }
            }
            "TEMP" -> {
                binding.mainScroll.post {
                    val cardView = binding.cardTemperatura.root
                    val top = cardView.top
                    binding.mainScroll.smoothScrollTo(0, top - 150)
                    UiUtils.showSnackbar(binding.root, "Desplazando a Temperatura", false)
                }
            }
            "PRESSURE" -> {
                binding.mainScroll.post {
                    val cardView = binding.cardPresion.root
                    val top = cardView.top
                    binding.mainScroll.smoothScrollTo(0, top - 150)
                    UiUtils.showSnackbar(binding.root, "Desplazando a Presión", false)
                }
            }
        }
    }

    override fun onNetworkRetry() {
        Log.d(TAG, "🔄 Reintentando carga completa...")
        lifecycleScope.launch {
            // Mostrar mensaje de intento
            UiUtils.showSnackbar(binding.root, "Reconectando...", false)

            // Recargar configuraciones y estado del proceso
            loadConfiguraciones()
            procesoViewModel.loadProcesoStatus()
        }
    }

    private fun initializeManagers() {
        chartManager = ChartManager(this)
        alertManager = AlertManager(
            context = this,
            analisisRepo = analisisRepo,
            onAlertDetected = { alertMessage ->
                runOnUiThread {
                    showAlert(alertMessage)
                    speakWithWaveform("Alerta crítica detectada")
                }
            },
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

    private fun setupNavigation() {
        navigationManager = NavigationManager(
            context = this,
            drawerLayout = binding.containerSensor,
            navigationView = binding.navView,
            currentActivity = "sensor_data",
            view = binding.root
        )

        binding.headerUser.settingsIcon.setOnClickListener {
            if (binding.containerSensor.isDrawerOpen(GravityCompat.START)) {
                binding.containerSensor.closeDrawer(GravityCompat.START)
            } else {
                binding.containerSensor.openDrawer(GravityCompat.START)
            }
        }

        navigationManager.setupNavigation("sensor_data")
    }

    private fun setupVoiceConfiguration() {
        setupWaveformComponents(
            binding.headerUser.waveformSection.waveformSeekBar,
            binding.headerUser.waveformSection.btnPlayMessage
        )

        voiceConfigViewModel.loadCurrentConfig()

        lifecycleScope.launch {
            voiceConfigViewModel.currentConfig.collect { config ->
                voiceManager.currentPitch = config.voicePitch.toFloat()
                voiceManager.currentGender = config.voiceGender
                if (isVoiceInitialized) {
                    voiceManager.applyTtsSettings()
                }
            }
        }

        lifecycleScope.launch {
            voiceConfigViewModel.uiState.collect { state ->
                when (state) {
                    is VoiceConfigViewModel.VoiceConfigUiState.Error -> {
                        Log.e(TAG, "❌ Error configuración voz: ${state.message}")
                        // 💡 NUEVO ENFOQUE: Usar showNetworkError para errores de red
                        if (state.message.contains("Error de red", ignoreCase = true) ||
                            state.message.contains("IOException", ignoreCase = true)) {
                            showNetworkError("Error de voz: ${state.message}")
                        } else {
                            UiUtils.showSnackbar(binding.root, "Error de voz: ${state.message}", true)
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

    private fun setupAlertTestButton() {
        binding.headerUser.waveformSection.btnPlayMessage.setOnLongClickListener {
            showTestAlertDialog()
            true
        }
    }

    private fun showTestAlertDialog() {
        AlertDialog.Builder(this)
            .setTitle("🧪 Modo Desarrollo")
            .setMessage("¿Forzar verificación de alerta?")
            .setPositiveButton("Sí") { _, _ ->
                alertManager.forceAlertCheck(lifecycleScope)
                UiUtils.showSnackbar(binding.root, "Verificando alertas...")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun startChartRefreshLoop() {
        lifecycleScope.launch {
            delay(5000)

            while (true) {
                Log.d(TAG, "🕒 Refresh automático - Solicitando estado del proceso")
                procesoViewModel.loadProcesoStatus()
                delay(refreshTime)
            }
        }
    }

    /**
     * Carga las configuraciones UNA SOLA VEZ al inicio
     */
    private suspend fun loadConfiguraciones() {
        Log.d(TAG, "⚙️ Cargando configuraciones de gráficas...")

        when (val result = graficasRepo.getGraficas()) {
            is ResultWrapper.Success -> {
                cachedConfiguraciones = result.data
                Log.d(TAG, "✅ Configuraciones cargadas: ${result.data.size}")

                if (isFirstLoad) {
                    isFirstLoad = false
                    procesoViewModel.loadProcesoStatus()
                }
            }
            is ResultWrapper.Error -> {
                cachedConfiguraciones = null
                val errorMsg = "Error configuraciones: ${result.message}"
                Log.e(TAG, "❌ Error configuraciones: ${result.message}")
                // 💡 NUEVO ENFOQUE: Usar showNetworkError para errores de red
                if (result.message?.contains("Error de red", ignoreCase = true) == true ||
                    result.message?.contains("IOException", ignoreCase = true) == true) {
                    showNetworkError(errorMsg)
                } else {
                    UiUtils.showSnackbar(binding.root, errorMsg, true)
                }
            }
        }
    }

    private suspend fun loadSensorsAndCharts(hayProcesoActivo: Boolean) {
        val configuraciones = cachedConfiguraciones ?: run {
            Log.w(TAG, "📭 No hay configuraciones en caché - mostrando gráficas por defecto")
            runOnUiThread {
                showDefaultCharts(hayProcesoActivo)
            }
            return
        }

        Log.d(TAG, "🚀 Cargando ${configuraciones.size} sensores. Proceso activo: $hayProcesoActivo")

        if (!hayProcesoActivo || isFirstLoad) {
            UiUtils.showLoading(this, "Actualizando datos...")
        }

        try {
            for (config in configuraciones) {
                loadSensorData(config, hayProcesoActivo)
                delay(300)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error general en carga sensores: ${e.message}", e)
            // 💡 NUEVO ENFOQUE: Usar showNetworkError para errores de red
            if (e.message?.contains("Error de red", ignoreCase = true) == true ||
                e.message?.contains("IOException", ignoreCase = true) == true) {
                showNetworkError("Error: ${e.message}")
            } else {
                UiUtils.showSnackbar(binding.root, "Error: ${e.message}", true)
            }
        } finally {
            UiUtils.hideLoading()
        }
    }

    private suspend fun loadSensorData(config: GraficaResponse, hayProcesoActivo: Boolean) {
        val sensorId = config.sensor_id
        val tipoGrafica = config.tipo_grafica
        val color = chartManager.getSensorColor(sensorId)

        val (nombreSensor, cardView) = when (sensorId) {
            1 -> "Gas Metano" to binding.cardMq4.root
            2 -> "Presión" to binding.cardPresion.root
            3 -> "Temperatura" to binding.cardTemperatura.root
            else -> {
                Log.w(TAG, "⚠️ Sensor ID desconocido: $sensorId")
                return
            }
        }

        Log.d(TAG, "📡 Cargando: $nombreSensor (Proceso activo: $hayProcesoActivo)")

        val lecturaResult = if (hayProcesoActivo) {
            lecturaRepo.getLecturas(sensorId)
        } else {
            ResultWrapper.Success(emptyList())
        }

        when (lecturaResult) {
            is ResultWrapper.Success -> {
                val lecturas = lecturaResult.data
                runOnUiThread {
                    if (lecturas.isNotEmpty()) {
                        Log.d(TAG, "✅ $nombreSensor: ${lecturas.size} lecturas")
                        val entries = lecturas.mapIndexed { index, lectura ->
                            Entry(index.toFloat(), lectura.valor.toFloat())
                        }
                        chartManager.displayChart(tipoGrafica, cardView, nombreSensor, color, entries, sensorId)
                    } else {
                        Log.w(TAG, "📭 $nombreSensor: Sin lecturas (Proceso activo: $hayProcesoActivo)")
                        showNoDataChart(cardView, nombreSensor, hayProcesoActivo)
                    }
                }
            }
            is ResultWrapper.Error -> {
                Log.e(TAG, "❌ Error $nombreSensor: ${lecturaResult.message}")
                runOnUiThread {
                    showNoDataChart(cardView, nombreSensor, hayProcesoActivo,
                        "❌ Error: ${lecturaResult.message}")

                    // 💡 NUEVO ENFOQUE: Usar showNetworkError para errores de red
                    if (lecturaResult.message?.contains("Error de red", ignoreCase = true) == true ||
                        lecturaResult.message?.contains("IOException", ignoreCase = true) == true) {
                        showNetworkError("Error de conexión: ${lecturaResult.message}")
                    }
                }
            }
        }
    }

    override fun onVoiceInitialized() {
        super.onVoiceInitialized()
        Log.d(TAG, "🎙️ Voz inicializada - Iniciando verificaciones...")

        lifecycleScope.launch {
            delay(1000)

            alertManager.checkAndHandleAlert()
            alertManager.startPeriodicAlertCheck(lifecycleScope)
        }
    }

    private fun showNoDataChart(cardView: android.view.View, sensorName: String,
                                hayProcesoActivo: Boolean, customMessage: String? = null) {
        try {
            val titleTextView = cardView.findViewById<android.widget.TextView>(R.id.card_title)
            titleTextView?.text = sensorName

            cardView.findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.chart_line)?.visibility = android.view.View.GONE
            cardView.findViewById<com.github.mikephil.charting.charts.BarChart>(R.id.chart_bar)?.visibility = android.view.View.GONE
            cardView.findViewById<com.github.mikephil.charting.charts.PieChart>(R.id.chart_pie)?.visibility = android.view.View.GONE

            val mensaje = customMessage ?: if (hayProcesoActivo) {
                "⏳ Esperando datos del sensor..."
            } else {
                "⏸️ Proceso inactivo"
            }

            val chartContainer = cardView.findViewById<android.view.ViewGroup>(R.id.chart_container)
            chartContainer?.removeAllViews()

            android.widget.TextView(this).apply {
                text = mensaje
                textSize = 14f
                setTextColor(android.graphics.Color.GRAY)
                gravity = android.view.Gravity.CENTER
                setPadding(0, 50, 0, 0)
            }.also { chartContainer?.addView(it) }

        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando chart vacío: ${e.message}")
        }
    }

    private fun showDefaultCharts(hayProcesoActivo: Boolean) {
        Log.d(TAG, "📊 Mostrando gráficas por defecto. Proceso activo: $hayProcesoActivo")
        showNoDataChart(binding.cardTemperatura.root, "Temperatura", hayProcesoActivo)
        showNoDataChart(binding.cardPresion.root, "Presión", hayProcesoActivo)
        showNoDataChart(binding.cardMq4.root, "Gas Metano", hayProcesoActivo)
    }

    override fun startSpeaking() {
        super.startSpeaking()
        lifecycleScope.launch {
            when (val analisisResult = analisisRepo.analizarLectura()) {
                is ResultWrapper.Success -> {
                    val fullMessage = formatAnalysisMessage(analisisResult.data)
                    speakWithPausesAndWaveform(fullMessage, 1000L)
                }
                is ResultWrapper.Error -> {
                    UiUtils.showSnackbar(binding.root, "Error en análisis: ${analisisResult.message}", true)
                    stopSpeaking()

                    // 💡 NUEVO ENFOQUE: Usar showNetworkError para errores de red
                    if (analisisResult.message?.contains("Error de red", ignoreCase = true) == true ||
                        analisisResult.message?.contains("IOException", ignoreCase = true) == true) {
                        showNetworkError("Error de conexión en análisis: ${analisisResult.message}")
                    }
                }
            }
        }
    }

    private fun showAlert(message: String) {
        startActivity(Intent(this, AlertsActivity::class.java).apply {
            putExtra("alert_message", message)
        })
    }

    override fun onBackPressed() {
        if (binding.containerSensor.isDrawerOpen(GravityCompat.START)) {
            binding.containerSensor.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        alertManager.stopPeriodicAlertCheck()
    }
}
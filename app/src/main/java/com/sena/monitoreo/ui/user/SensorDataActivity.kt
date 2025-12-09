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
import com.sena.monitoreo.data.model.ai.AnalisisResponse
import com.sena.monitoreo.data.repository.*
import com.sena.monitoreo.databinding.ActivitySensorDataBinding
import com.sena.monitoreo.ui.admin.viewmodel.ProcesoViewModel
import com.sena.monitoreo.ui.admin.factory.ProcesoViewModelFactory
import com.sena.monitoreo.ui.base.BaseVoiceActivity
import com.sena.monitoreo.ui.base.factory.VoiceConfigViewModelFactory
import com.sena.monitoreo.ui.base.viewmodel.VoiceConfigViewModel
import com.sena.monitoreo.utils.ResultWrapper
import com.sena.monitoreo.utils.UiUtils
import com.sena.monitoreo.utils.cache.*
import com.sena.monitoreo.utils.alerts.AlertManager
import com.sena.monitoreo.utils.charts.ChartManager
import com.sena.monitoreo.utils.navigation.NavigationManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

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

    companion object {
        private var cachedConfiguraciones: List<GraficaResponse>? = null
        private var cachedAnalisis: AnalisisResponse? = null
        private var lastAnalisisTime: Long = 0
        private var lastConfigLoadTime: Long = 0
        private const val CONFIG_CACHE_DURATION = 5 * 60 * 1000L // 5 minutos
        private const val ANALISIS_CACHE_DURATION = 2 * 60 * 1000L // 2 minutos para análisis

        fun clearCache() {
            cachedConfiguraciones = null
            cachedAnalisis = null // ✅ Limpiar también el análisis
            lastConfigLoadTime = 0
            lastAnalisisTime = 0
        }
    }

    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySensorDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeManagers()
        setupNavigation()
        setupVoiceConfiguration()
        setupAlertTestButton()
        setupManualRefreshButton()
        handleIntentNavigation()

        // ✅ MEJORADO: Carga inteligente con caché
        lifecycleScope.launch {
            Log.d(TAG, "🔄 Iniciando carga inicial...")

            // Verificar si las configuraciones en caché son válidas
            val configCacheAge = System.currentTimeMillis() - lastConfigLoadTime
            val needsConfigReload = cachedConfiguraciones == null || configCacheAge > CONFIG_CACHE_DURATION

            if (needsConfigReload) {
                Log.d(TAG, "📥 Cargando configuraciones desde servidor...")
                loadConfiguraciones()
            } else {
                Log.d(TAG, "⚡ Usando configuraciones cacheadas (edad: ${configCacheAge}ms)")
            }

            // Cargar estado del proceso
            Log.d(TAG, "🔄 Cargando estado del proceso...")
            procesoViewModel.loadProcesoStatus()

            // Esperar un poco para la red
            delay(1500)

            // Obtener estado (usar true por defecto si es null)
            val estadoProceso = procesoViewModel.isProcesoActivo.value ?: true
            Log.d(TAG, "📊 Estado del proceso: $estadoProceso")

            // Precargar análisis en background
            preloadAnalisis()

            // Cargar sensores
            if (cachedConfiguraciones != null) {
                Log.d(TAG, "✅ Cargando sensores con configuraciones")
                loadSensorsAndCharts(estadoProceso, isManualRefresh = false)
            } else {
                Log.w(TAG, "⚠️ Sin configuraciones - mostrando defaults")
                showDefaultCharts(estadoProceso)
            }
        }

        // Observer para actualizaciones posteriores
        procesoViewModel.isProcesoActivo.observe(this) { activo ->
            if (activo != null && !isFirstLoad) {
                Log.d(TAG, "🔄 Actualización de estado detectada: $activo")
                lifecycleScope.launch {
                    if (cachedConfiguraciones != null) {
                        loadSensorsAndCharts(activo, isManualRefresh = false)
                    }
                }
            }
        }

        startChartRefreshLoop()
    }

    private fun setupManualRefreshButton() {
        binding.btnManualRefresh.setOnClickListener {
            Log.d(TAG, "Manual Refresh - Botón presionado")
            // Mostrar loading inmediatamente
            UiUtils.showLoading(this, "Recargando datos manualmente...")

            // Iniciar la recarga en una corrutina
            lifecycleScope.launch {
                val estadoProceso = procesoViewModel.isProcesoActivo.value ?: true

                // 1. Recargar configuraciones si es necesario
                val configCacheAge = System.currentTimeMillis() - lastConfigLoadTime
                val needsConfigReload = cachedConfiguraciones == null || configCacheAge > CONFIG_CACHE_DURATION
                if (needsConfigReload) {
                    loadConfiguraciones()
                }

                // 2. Precargar análisis también
                preloadAnalisis()

                // 3. Forzar la recarga de datos de sensores
                if (cachedConfiguraciones != null) {
                    loadSensorsAndCharts(estadoProceso, isManualRefresh = true)
                } else {
                    // Si aún no hay configuraciones, solo mostrar el estado por defecto
                    showDefaultCharts(estadoProceso)
                }
                UiUtils.hideLoading()
                UiUtils.showSnackbar(binding.root, "Datos actualizados", false)
            }
        }
    }

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
        Log.d(TAG, "Navegando a card: $cardType")

        when (cardType.uppercase()) {
            "GAS" -> {
                binding.mainScroll.post {
                    val cardView = binding.cardMq4.root
                    val top = cardView.top
                    binding.mainScroll.smoothScrollTo(0, top - 150)
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
            UiUtils.showSnackbar(binding.root, "Reconectando...", false)
            loadConfiguraciones()
            procesoViewModel.loadProcesoStatus()
            preloadAnalisis()
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
            .setTitle("Modo Desarrollo")
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

            var errorCount = 0
            val maxErrorCount = 3

            while (true) {
                try {
                    Log.d(TAG, "🕒 Refresh automático - Solicitando estado del proceso")
                    procesoViewModel.loadProcesoStatus()
                    errorCount = 0 // Reset error count on success

                    // ✅ Actualizar análisis en background periódicamente
                    if (procesoViewModel.isProcesoActivo.value == true) {
                        preloadAnalisis()
                    }
                } catch (e: Exception) {
                    errorCount++
                    Log.e(TAG, "❌ Error en refresh #$errorCount: ${e.message}")

                    // Backoff exponencial en caso de errores consecutivos
                    if (errorCount >= maxErrorCount) {
                        val backoffTime = refreshTime * errorCount
                        Log.w(TAG, "⚠️ Muchos errores, aplicando backoff: ${backoffTime}ms")
                        UiUtils.showSnackbar(binding.root, "Problemas de conexión - Reintentando...", false)
                        delay(backoffTime)
                    }
                }
                delay(refreshTime)
            }
        }
    }

    private suspend fun loadConfiguraciones() {
        Log.d(TAG, "⚙️ Cargando configuraciones de gráficas...")

        when (val result = graficasRepo.getGraficas()) {
            is ResultWrapper.Success -> {
                cachedConfiguraciones = result.data
                lastConfigLoadTime = System.currentTimeMillis()
                Log.d(TAG, "✅ Configuraciones cargadas y cacheadas: ${result.data.size}")

                result.data.forEach { config ->
                    Log.d(TAG, "📋 Config - SensorID: ${config.sensor_id}, Tipo: ${config.tipo_grafica}")
                }

                isFirstLoad = false
            }
            is ResultWrapper.Error -> {
                val errorMsg = "Error configuraciones: ${result.message}"
                Log.e(TAG, "❌ $errorMsg")

                // ✅ Si hay configuraciones en caché antiguas, usarlas
                if (cachedConfiguraciones != null) {
                    Log.w(TAG, "⚠️ Error de red, pero usando configuraciones en caché")
                } else {
                    if (result.message.contains("Error de red", ignoreCase = true) ||
                        result.message.contains("IOException", ignoreCase = true)) {
                        showNetworkError(errorMsg)
                    } else {
                        UiUtils.showSnackbar(binding.root, errorMsg, true)
                    }
                }
            }
        }
    }

    /**
     * ✅ NUEVO: Precargar análisis en background
     */
    private fun preloadAnalisis() {
        lifecycleScope.launch {
            try {
                // Solo precargar si el análisis está expirado o no existe
                val analisisCacheAge = System.currentTimeMillis() - lastAnalisisTime
                if (cachedAnalisis == null || analisisCacheAge > ANALISIS_CACHE_DURATION) {
                    Log.d(TAG, "🔄 Precargando análisis en background...")

                    // Verificar si hay proceso activo antes de analizar
                    val estadoProceso = procesoViewModel.isProcesoActivo.value ?: true
                    if (!estadoProceso) {
                        Log.d(TAG, "⏸️ Proceso inactivo, no se precarga análisis")
                        return@launch
                    }

                    when (val result = analisisRepo.analizarLectura()) {
                        is ResultWrapper.Success -> {
                            cachedAnalisis = result.data
                            lastAnalisisTime = System.currentTimeMillis()
                            Log.d(TAG, "✅ Análisis precargado exitosamente")
                        }
                        else -> {
                            // Silencioso en error de precarga
                            Log.w(TAG, "⚠️ No se pudo precargar análisis")
                        }
                    }
                } else {
                    Log.d(TAG, "⚡ Análisis en caché está fresco (${analisisCacheAge}ms)")
                }
            } catch (e: Exception) {
                // Silencioso, no afecta al usuario
                Log.d(TAG, "⚠️ Error en precarga de análisis: ${e.message}")
            }
        }
    }

    private suspend fun loadSensorsAndCharts(hayProcesoActivo: Boolean, isManualRefresh: Boolean) {
        val configuraciones = cachedConfiguraciones ?: run {
            Log.w(TAG, "📭 No hay configuraciones en caché - mostrando gráficas por defecto")
            runOnUiThread {
                showDefaultCharts(hayProcesoActivo)
            }
            return
        }

        Log.d(TAG, "🚀 Cargando ${configuraciones.size} sensores. Proceso activo: $hayProcesoActivo. Manual: $isManualRefresh")

        // ✅ MOSTRAR loading SOLAMENTE en la carga INICIAL o MANUAL
        val hayCacheDatos = configuraciones.any { config ->
            SensorCache.isFresh(config.sensor_id)
        }

        if ((!hayCacheDatos && hayProcesoActivo) || isManualRefresh) {
            Log.d(TAG, "📊 Forzando carga/refresco - mostrando loading")
            if (!isManualRefresh) {
                UiUtils.showLoading(this, "Actualizando datos...")
            }
        } else {
            Log.d(TAG, "⚡ Hay caché disponible - carga rápida sin loading")
        }

        try {
            for (config in configuraciones) {
                Log.d(TAG, "🔄 Procesando sensor ${config.sensor_id}...")
                loadSensorData(config, hayProcesoActivo, isManualRefresh)
                delay(100)
            }
            Log.d(TAG, "✅ Todos los sensores procesados")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error general en carga sensores: ${e.message}", e)
            if (e.message?.contains("Error de red", ignoreCase = true) == true ||
                e.message?.contains("IOException", ignoreCase = true) == true) {
                showNetworkError("Error: ${e.message}")
            } else {
                UiUtils.showSnackbar(binding.root, "Error: ${e.message}", true)
            }
        } finally {
            if (!isManualRefresh) {
                UiUtils.hideLoading()
            }
        }
    }

    private suspend fun loadSensorData(config: GraficaResponse, hayProcesoActivo: Boolean, isManualRefresh: Boolean = false) {
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

        // ✅ PRIMERO: Intentar cargar desde caché (SIEMPRE, a menos que sea recarga manual)
        val cachedLecturas = if (!isManualRefresh) SensorCache.get(sensorId) else null

        if (cachedLecturas != null) {
            Log.d(TAG, "⚡ CARGA INSTANTÁNEA desde caché: $nombreSensor (${cachedLecturas.size} lecturas)")
            runOnUiThread {
                try {
                    val entries = cachedLecturas.mapIndexed { index, lectura ->
                        Entry(index.toFloat(), lectura.valor.toFloat())
                    }
                    chartManager.displayChart(tipoGrafica, cardView, nombreSensor, color, entries, sensorId)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ ERROR dibujando desde caché: ${e.message}", e)
                }
            }

            // ✅ Si el proceso está activo, actualizar en background SIN bloquear UI
            if (hayProcesoActivo) {
                lifecycleScope.launch {
                    delay(500)
                    refreshSensorDataInBackground(sensorId, config, hayProcesoActivo)
                }
            }
            return
        }

        // ✅ SEGUNDO: Si no hay caché O es recarga manual, cargar desde red
        if (!hayProcesoActivo && !isManualRefresh) {
            runOnUiThread {
                showNoDataChart(cardView, nombreSensor, false)
            }
            return
        }

        // Si hay proceso activo O es recarga manual, cargamos desde la red
        Log.d(TAG, "📡 Cargando desde red: $nombreSensor (Forzado: $isManualRefresh)")

        try {
            val lecturaResult = withTimeout(15000) {
                lecturaRepo.getLecturas(sensorId)
            }

            when (lecturaResult) {
                is ResultWrapper.Success -> {
                    val lecturas = lecturaResult.data

                    if (lecturas.isNotEmpty()) {
                        SensorCache.put(sensorId, lecturas)

                        runOnUiThread {
                            try {
                                val entries = lecturas.mapIndexed { index, lectura ->
                                    Entry(index.toFloat(), lectura.valor.toFloat())
                                }
                                chartManager.displayChart(tipoGrafica, cardView, nombreSensor, color, entries, sensorId)
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ ERROR renderizando: ${e.message}", e)
                            }
                        }
                    } else {
                        runOnUiThread {
                            showNoDataChart(cardView, nombreSensor, hayProcesoActivo)
                        }
                    }
                }
                is ResultWrapper.Error -> {
                    Log.e(TAG, "❌ Error $nombreSensor: ${lecturaResult.message}")
                    runOnUiThread {
                        showNoDataChart(cardView, nombreSensor, hayProcesoActivo, "🌐 Error de conexión")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "⏰ Timeout en $nombreSensor")
            runOnUiThread {
                showNoDataChart(cardView, nombreSensor, hayProcesoActivo, "⏰ Timeout")
            }
        }
    }

    private suspend fun refreshSensorDataInBackground(
        sensorId: Int,
        config: GraficaResponse,
        hayProcesoActivo: Boolean
    ) {
        if (!hayProcesoActivo) return

        try {
            withTimeout(15000) {
                val result = lecturaRepo.getLecturas(sensorId)
                if (result is ResultWrapper.Success && result.data.isNotEmpty()) {
                    SensorCache.put(sensorId, result.data)
                    Log.d(TAG, "🔄 Cache actualizado en background: sensor $sensorId")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error actualizando cache background: ${e.message}")
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

    /**
     * ✅ MEJORADO: Voz instantánea con caché
     */
    override fun startSpeaking() {
        super.startSpeaking()

        lifecycleScope.launch {
            try {
                // ✅ PRIMERO: Verificar si hay proceso activo
                val estadoProceso = procesoViewModel.isProcesoActivo.value ?: true

                if (!estadoProceso) {
                    speakWithPausesAndWaveform("El proceso está inactivo. No hay datos para analizar.", 1000L)
                    return@launch
                }

                // ✅ SEGUNDO: Verificar caché de análisis
                val analisisCacheAge = System.currentTimeMillis() - lastAnalisisTime
                val shouldUseCache = cachedAnalisis != null &&
                        analisisCacheAge < ANALISIS_CACHE_DURATION

                if (shouldUseCache) {
                    Log.d(TAG, "⚡ Usando análisis en caché ($analisisCacheAge ms)")
                    val fullMessage = formatAnalysisMessage(cachedAnalisis!!)
                    speakWithPausesAndWaveform(fullMessage, 1000L)

                    // ✅ Actualizar caché en background mientras habla
                    updateAnalisisInBackground()
                    return@launch
                }

                Log.d(TAG, "📡 Obteniendo análisis desde servidor...")

                // ✅ TERCERO: Si no hay caché, mostrar mensaje inmediato y luego cargar
                speakWithPausesAndWaveform("Analizando datos de sensores...", 500L)

                // Obtener análisis con timeout
                val analisisResult = withTimeout(8000) {
                    analisisRepo.analizarLectura()
                }

                when (analisisResult) {
                    is ResultWrapper.Success -> {
                        // ✅ Detener mensaje anterior y hablar con nuevo análisis
                        stopSpeaking()
                        cachedAnalisis = analisisResult.data
                        lastAnalisisTime = System.currentTimeMillis()

                        val fullMessage = formatAnalysisMessage(analisisResult.data)
                        speakWithPausesAndWaveform(fullMessage, 1000L)
                    }
                    is ResultWrapper.Error -> {
                        Log.e(TAG, "❌ Error en análisis: ${analisisResult.message}")

                        stopSpeaking()

                        // ✅ Si hay caché antiguo, usarlo aunque haya expirado
                        if (cachedAnalisis != null) {
                            Log.w(TAG, "⚠️ Usando análisis cacheadO (expiró)")
                            val fullMessage = formatAnalysisMessage(cachedAnalisis!!)
                            speakWithPausesAndWaveform(fullMessage, 1000L)
                        } else {
                            // ✅ Mensaje predeterminado si no hay caché
                            speakWithPausesAndWaveform(
                                "No hay datos de análisis disponibles. Revisa la conexión con el servidor.",
                                500L
                            )
                        }

                        if (analisisResult.message.contains("Error de red", ignoreCase = true)) {
                            showNetworkError("Error de conexión en análisis")
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "⏰ Timeout en análisis de voz")

                stopSpeaking()

                // ✅ Usar caché si existe
                if (cachedAnalisis != null) {
                    val fullMessage = formatAnalysisMessage(cachedAnalisis!!)
                    speakWithPausesAndWaveform(fullMessage, 1000L)
                } else {
                    speakWithPausesAndWaveform(
                        "El análisis está tardando demasiado. Revisa el estado de los sensores.",
                        500L
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error general en voz: ${e.message}")
                speakWithPausesAndWaveform("Error al obtener análisis.", 500L)
            }
        }
    }

    /**
     * ✅ NUEVO: Actualizar análisis en background
     */
    private fun updateAnalisisInBackground() {
        lifecycleScope.launch {
            try {
                val analisisCacheAge = System.currentTimeMillis() - lastAnalisisTime
                if (analisisCacheAge > ANALISIS_CACHE_DURATION) {
                    Log.d(TAG, "🔄 Actualizando análisis en background...")
                    when (val result = analisisRepo.analizarLectura()) {
                        is ResultWrapper.Success -> {
                            cachedAnalisis = result.data
                            lastAnalisisTime = System.currentTimeMillis()
                            Log.d(TAG, "✅ Análisis actualizado en background")
                        }
                        else -> {
                            // Error silencioso
                        }
                    }
                }
            } catch (e: Exception) {
                // Error silencioso
                Log.d(TAG, "⚠️ Error actualizando análisis background: ${e.message}")
            }
        }
    }

    private fun showLoadingState(cardView: android.view.View, sensorName: String, message: String) {
        try {
            val titleTextView = cardView.findViewById<android.widget.TextView>(R.id.card_title)
            titleTextView?.text = "$sensorName 🔄"

            val lineChart = cardView.findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.chart_line)
            val barChart = cardView.findViewById<com.github.mikephil.charting.charts.BarChart>(R.id.chart_bar)
            val pieChart = cardView.findViewById<com.github.mikephil.charting.charts.PieChart>(R.id.chart_pie)

            lineChart?.visibility = android.view.View.GONE
            barChart?.visibility = android.view.View.GONE
            pieChart?.visibility = android.view.View.GONE

            val chartContainer = cardView.findViewById<android.view.ViewGroup>(R.id.chart_container)

            val existingLoading = chartContainer?.findViewWithTag<android.view.View>("loading_view")

            if (existingLoading == null) {
                android.widget.LinearLayout(this).apply {
                    tag = "loading_view"
                    orientation = android.widget.LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 50, 0, 0)

                    android.widget.TextView(context).apply {
                        text = message
                        textSize = 14f
                        setTextColor(android.graphics.Color.parseColor("#FFA500"))
                        gravity = android.view.Gravity.CENTER
                    }.also { addView(it) }

                    android.widget.ProgressBar(context).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = 16
                        }
                        isIndeterminate = true
                    }.also { addView(it) }
                }.also { chartContainer?.addView(it) }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando estado de carga: ${e.message}")
        }
    }

    private fun showNoDataChart(cardView: android.view.View, sensorName: String,
                                hayProcesoActivo: Boolean, customMessage: String? = null) {
        try {
            val titleTextView = cardView.findViewById<android.widget.TextView>(R.id.card_title)
            val statusIcon = when {
                customMessage?.contains("Error", ignoreCase = true) == true -> "❌"
                customMessage?.contains("Timeout", ignoreCase = true) == true -> "⏰"
                hayProcesoActivo -> "📡"
                else -> "⏸️"
            }

            titleTextView?.text = "$sensorName $statusIcon"

            val lineChart = cardView.findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.chart_line)
            val barChart = cardView.findViewById<com.github.mikephil.charting.charts.BarChart>(R.id.chart_bar)
            val pieChart = cardView.findViewById<com.github.mikephil.charting.charts.PieChart>(R.id.chart_pie)

            lineChart?.visibility = android.view.View.GONE
            barChart?.visibility = android.view.View.GONE
            pieChart?.visibility = android.view.View.GONE

            val mensaje = customMessage ?: if (hayProcesoActivo) {
                "Esperando datos..."
            } else {
                "⏸️ Proceso inactivo"
            }

            val chartContainer = cardView.findViewById<android.view.ViewGroup>(R.id.chart_container)

            val loadingView = chartContainer?.findViewWithTag<android.view.View>("loading_view")
            loadingView?.let { chartContainer.removeView(it) }

            val existingMessage = chartContainer?.findViewWithTag<android.widget.TextView>("no_data_message")

            if (existingMessage == null) {
                android.widget.TextView(this).apply {
                    tag = "no_data_message"
                    text = mensaje
                    textSize = 14f
                    setTextColor(when {
                        mensaje.contains("✅") -> android.graphics.Color.parseColor("#4CAF50")
                        mensaje.contains("❌") -> android.graphics.Color.parseColor("#F44336")
                        mensaje.contains("⏰") -> android.graphics.Color.parseColor("#FF9800")
                        else -> android.graphics.Color.GRAY
                    })
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 50, 0, 0)
                }.also { chartContainer?.addView(it) }
            } else {
                existingMessage.text = mensaje
            }

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
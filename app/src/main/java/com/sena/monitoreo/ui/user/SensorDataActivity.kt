package com.sena.monitoreo.ui.user

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.data.Entry
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.repository.*
import com.sena.monitoreo.databinding.ActivitySensorDataBinding
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModel
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModelFactory
import com.sena.monitoreo.ui.admin.viewmodel.ProcesoViewModel
import com.sena.monitoreo.ui.admin.viewmodel.ProcesoViewModelFactory
import com.sena.monitoreo.ui.base.BaseVoiceActivity
import com.sena.monitoreo.utils.UiUtils
import com.sena.monitoreo.utils.alerts.AlertManager
import com.sena.monitoreo.utils.charts.ChartManager
import com.sena.monitoreo.utils.navigation.NavigationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SensorDataActivity : BaseVoiceActivity() {

    private lateinit var binding: ActivitySensorDataBinding
    private lateinit var navigationManager: NavigationManager
    private lateinit var alertManager: AlertManager
    private lateinit var chartManager: ChartManager

    // Repositories
    private val graficasRepo = GraficasRepository()
    private val lecturaRepo = LecturaRepository()
    private val analisisRepo = AnalisisRepository(RetrofitClient.apiAi)

    // ViewModels
    private val viewModel: AdminConfigViewModel by lazy {
        val repository = VoiceRepository(RetrofitClient.apiVoice)
        ViewModelProvider(this, AdminConfigViewModelFactory(repository))
            .get(AdminConfigViewModel::class.java)
    }

    private val procesoViewModel: ProcesoViewModel by lazy {
        val repository = ProcesoRepository(RetrofitClient.apiProceso)
        ViewModelProvider(this, ProcesoViewModelFactory(repository))
            .get(ProcesoViewModel::class.java)
    }

    private val refreshTime = 5 * 60 * 1000L // 5 minutos

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySensorDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeManagers()
        setupNavigation()
        setupVoiceConfiguration()
        setupAlertTestButton()

        // Observamos cambios en el proceso activo
        procesoViewModel.isProcesoActivo.observe(this) { activo ->
            lifecycleScope.launch {
                loadSensorsAndCharts()
            }
        }

        // Carga inicial
        lifecycleScope.launch {
            loadSensorsAndCharts()
        }

        // Bucle de actualización
        startChartRefreshLoop()
    }

    private fun initializeManagers() {
        chartManager = ChartManager(this)
        alertManager = AlertManager(
            context = this,
            analisisRepo = analisisRepo,
            onAlertDetected = { alertMessage ->
                runOnUiThread {
                    showAlert(alertMessage)
                    val fullAlertMessage = "⚠️ Alerta crítica detectada. $alertMessage. Por favor, revise la pantalla de alertas para más detalles."
                    speakLongTextWithWaveform(fullAlertMessage)
                }
            },
            onError = { errorMessage ->
                runOnUiThread {
                    UiUtils.showSnackbar(binding.root, "Error: $errorMessage", true)
                    speakWithWaveform("Error al verificar alertas")
                }
            }
        )
    }

    private fun setupNavigation() {
        navigationManager = NavigationManager(
            context = this,
            drawerLayout = binding.containerSensor,
            navigationView = binding.navView,
            currentActivity = "sensor_data"
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

        viewModel.loadCurrentConfig()
        viewModel.currentConfig.observe(this) { config ->
            voiceManager.currentPitch = config.pitch
            voiceManager.currentGender = config.gender
            if (isVoiceInitialized) {
                voiceManager.applyTtsSettings()
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
                UiUtils.showSnackbar(binding.root, "Cooldown reseteado. Verificando...")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startChartRefreshLoop() {
        lifecycleScope.launch {
            while (true) {
                delay(refreshTime)
                loadSensorsAndCharts()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun loadSensorsAndCharts() {
        try {
            UiUtils.showLoading(this, "Actualizando datos...")

            // 1️⃣ Verificar estado real del proceso
            val hayProcesoActivo = verificarProcesoActivo()

            // 2️⃣ Cargar configuraciones
            val configuraciones = graficasRepo.getGraficas()

            if (configuraciones.isEmpty()) {
                showDefaultCharts(hayProcesoActivo)
            } else {
                configuraciones.forEach { config ->
                    val sensorId = config.sensor_id
                    val tipoGrafica = config.tipo_grafica
                    val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        chartManager.getSensorColor(sensorId)
                    } else {
                        getLegacySensorColor(sensorId)
                    }

                    val (nombreSensor, cardView) = when (sensorId) {
                        2 -> "Temperatura" to binding.cardTemperatura.root
                        3 -> "Presión" to binding.cardPresion.root
                        1 -> "Metano" to binding.cardMq4.root
                        else -> return@forEach
                    }

                    val lecturas = if (hayProcesoActivo) {
                        lecturaRepo.getLecturas(sensorId)
                    } else {
                        emptyList() // Sin datos si no hay proceso activo
                    }

                    if (lecturas.isNotEmpty()) {
                        val entries = lecturas.mapIndexed { index, lectura ->
                            Entry(index.toFloat(), lectura.valor.toFloat())
                        }
                        chartManager.displayChart(tipoGrafica, cardView, nombreSensor, color, entries, sensorId)
                    } else {
                        showNoDataChart(cardView, nombreSensor, hayProcesoActivo)
                    }
                }
            }

        } catch (e: Exception) {
            UiUtils.showSnackbar(binding.root, "Error cargando gráficas: ${e.message}", true)
            showDefaultCharts(false)
        } finally {
            UiUtils.hideLoading()
        }
    }

    private suspend fun verificarProcesoActivo(): Boolean {
        return try {
            val response = procesoViewModel.repository.verificarEstado()
            if (response.isSuccessful) {
                response.body()?.proceso_activo ?: false
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showNoDataChart(cardView: android.view.View, sensorName: String, hayProcesoActivo: Boolean) {
        val titleTextView = cardView.findViewById<android.widget.TextView>(R.id.card_title)
        titleTextView?.text = sensorName

        // Ocultar todas las gráficas
        cardView.findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.chart_line)?.visibility = android.view.View.GONE
        cardView.findViewById<com.github.mikephil.charting.charts.BarChart>(R.id.chart_bar)?.visibility = android.view.View.GONE
        cardView.findViewById<com.github.mikephil.charting.charts.PieChart>(R.id.chart_pie)?.visibility = android.view.View.GONE

        val mensaje = if (hayProcesoActivo) {
            "⏳ Esperando datos del sensor..."
        } else {
            "⏸️ Proceso inactivo - Sin datos"
        }

        val textView = android.widget.TextView(this).apply {
            text = mensaje
            textSize = 16f
            setTextColor(android.graphics.Color.GRAY)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 100, 0, 0)
        }

        val chartContainer = cardView.findViewById<android.view.ViewGroup>(R.id.chart_container)
        chartContainer?.removeAllViews()
        chartContainer?.addView(textView)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDefaultCharts(hayProcesoActivo: Boolean) {
        if (hayProcesoActivo) {
            val entriesTemp = listOf(Entry(1f, 25f), Entry(2f, 26f), Entry(3f, 28f))
            val entriesPresion = listOf(Entry(1f, 990f), Entry(2f, 1000f), Entry(3f, 1010f))
            val entriesMetano = listOf(Entry(1f, 0.05f), Entry(2f, 0.06f), Entry(3f, 0.07f))

            chartManager.displayChart("line", binding.cardTemperatura.root, "Temperatura",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) chartManager.getSensorColor(2) else getLegacySensorColor(2),
                entriesTemp, 2)
            chartManager.displayChart("bar", binding.cardPresion.root, "Presión",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) chartManager.getSensorColor(3) else getLegacySensorColor(3),
                entriesPresion, 3)
            chartManager.displayChart("pie", binding.cardMq4.root, "Metano",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) chartManager.getSensorColor(1) else getLegacySensorColor(1),
                entriesMetano, 1)
        } else {
            showNoDataChart(binding.cardTemperatura.root, "Temperatura", false)
            showNoDataChart(binding.cardPresion.root, "Presión", false)
            showNoDataChart(binding.cardMq4.root, "Metano", false)
        }
    }

    private fun getLegacySensorColor(sensorId: Int): Int = when (sensorId) {
        2 -> resources.getColor(R.color.temp_color)
        3 -> resources.getColor(R.color.pressure_color)
        1 -> resources.getColor(R.color.gas_color)
        else -> android.graphics.Color.BLACK
    }

    override fun onVoiceInitialized() {
        lifecycleScope.launch {
            alertManager.checkAndHandleAlert()
        }
        alertManager.startPeriodicAlertCheck(lifecycleScope)
    }

    override fun startSpeaking() {
        super.startSpeaking()
        lifecycleScope.launch {
            val analisisResult = analisisRepo.analizarLectura()
            if (analisisResult.success != null) {
                val analisis = analisisResult.success
                val fullMessage = formatAnalysisMessage(analisis)
                speakWithPausesAndWaveform(fullMessage, 1000L)
            } else {
                val errorMessage = analisisResult.errorMessage ?: "Error desconocido"
                runOnUiThread {
                    UiUtils.showSnackbar(binding.root, errorMessage, true)
                }
                stopSpeaking()
            }
        }
    }

    private fun showAlert(message: String) {
        val intent = Intent(this, AlertsActivity::class.java).apply {
            putExtra("alert_message", message)
        }
        startActivity(intent)
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

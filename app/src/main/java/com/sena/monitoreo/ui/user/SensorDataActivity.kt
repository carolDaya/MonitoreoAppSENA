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
import com.sena.monitoreo.data.repository.AnalisisRepository
import com.sena.monitoreo.data.repository.GraficasRepository
import com.sena.monitoreo.data.repository.LecturaRepository
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.databinding.ActivitySensorDataBinding
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModel
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModelFactory
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

    // ViewModel
    private val viewModel: AdminConfigViewModel by lazy {
        val repository = VoiceRepository(RetrofitClient.apiVoice)
        ViewModelProvider(this, AdminConfigViewModelFactory(repository))
            .get(AdminConfigViewModel::class.java)
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
        btnPlay.setOnLongClickListener {
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
            val configuraciones = graficasRepo.getGraficas()
            if (configuraciones.isEmpty()) {
                showDefaultCharts()
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

                    val lecturas = lecturaRepo.getLecturas(sensorId)
                    if (lecturas.isNotEmpty()) {
                        val entries = lecturas.mapIndexed { index, lectura ->
                            Entry(index.toFloat(), lectura.valor.toFloat())
                        }
                        chartManager.displayChart(tipoGrafica, cardView, nombreSensor, color, entries, sensorId)
                    }
                }
            }
        } catch (e: Exception) {
            UiUtils.showSnackbar(binding.root, "Error cargando gráficas: ${e.message}", true)
            showDefaultCharts()
        } finally {
            UiUtils.hideLoading()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDefaultCharts() {
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
    }

    private fun getLegacySensorColor(sensorId: Int): Int = when (sensorId) {
        2 -> resources.getColor(R.color.temp_color)
        3 -> resources.getColor(R.color.pressure_color)
        1 -> resources.getColor(R.color.gas_color)
        else -> android.graphics.Color.BLACK
    }

    override fun onVoiceInitialized() {
        // Iniciar verificación de alertas cuando la voz esté lista
        lifecycleScope.launch {
            alertManager.checkAndHandleAlert()
        }

        // Iniciar verificación periódica
        alertManager.startPeriodicAlertCheck(lifecycleScope)
    }

    override fun startSpeaking() {
        super.startSpeaking()
        lifecycleScope.launch {
            val analisisResult = analisisRepo.analizarLectura()
            if (analisisResult.success != null) {
                val analisis = analisisResult.success
                // USAR DIRECTAMENTE EL MÉTODO DE LA CLASE BASE
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
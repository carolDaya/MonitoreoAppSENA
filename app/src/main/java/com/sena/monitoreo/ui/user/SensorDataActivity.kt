package com.sena.monitoreo.ui.user

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.button.MaterialButton
import com.masoudss.lib.WaveformSeekBar
import com.sena.monitoreo.R
import com.sena.monitoreo.data.repository.AnalisisRepository
import com.sena.monitoreo.data.repository.GraficasRepository
import com.sena.monitoreo.data.repository.LecturaRepository
import com.sena.monitoreo.databinding.ActivitySensorDataBinding
import com.sena.monitoreo.ui.auth.LoginActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.random.Random

class SensorDataActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivitySensorDataBinding
    private val graficasRepo = GraficasRepository()
    private val lecturaRepo = LecturaRepository()
    private val analisisRepo = AnalisisRepository()
    private val TAG = "SensorDataActivity"
    private val refreshTime = 5 * 60 * 1000L // 5 minutos

    // PROPIEDADES PARA VOZ Y ONDAS
    private var tts: TextToSpeech? = null
    private var isSpeaking = false
    private lateinit var waveformSeekBar: WaveformSeekBar
    private lateinit var btnPlay: MaterialButton

    // LÓGICA DE COOLDOWN DE ALERTA
    private val PREFS_NAME = "AlertaPrefs"
    private val KEY_LAST_ALERT_TIME = "last_alert_time"
    private val ALERT_COOLDOWN_HOURS = 2L

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySensorDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. INICIALIZAR VOZ/ONDAS
        tts = TextToSpeech(this, this)
        initWaveformViews()

        // 2. CONFIGURAR MENÚ LATERAL
        binding.headerUser.settingsIcon.setOnClickListener {
            binding.containerSensor.openDrawer(GravityCompat.START)
        }
        setupNavigationView()

        // 3. LLAMADAS SUSPENDIDAS DENTRO DE CORRUTINAS

        // A. Carga INICIAL de gráficas (inmediata)
        lifecycleScope.launch {
            cargarSensoresYGraficas()
        }

        // B. BUCLE de actualización de gráficas (cada 5 minutos)
        lifecycleScope.launch {
            while (true) {
                delay(refreshTime)
                cargarSensoresYGraficas()
            }
        }

        // C. VERIFICACIÓN INICIAL de alerta (se ejecuta al abrir la app)
        lifecycleScope.launch {
            checkAndHandleAlert()
        }
    }

    // ---------------------------------------------------------------------
    //                       IMPLEMENTACIÓN DE VOZ Y MENÚ
    // ---------------------------------------------------------------------

    private fun setupNavigationView() {
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeUserActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                }
                R.id.nav_datos_gas, R.id.nav_datos_tem, R.id.nav_datos_presion -> {
                    Toast.makeText(this, "Ya estás en la pantalla de datos", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Configuración Usuario", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_logout -> {
                    stopSpeaking()
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                }
            }
            binding.containerSensor.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun initWaveformViews() {
        val waveformBinding = binding.headerUser.waveformSection
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
        if (tts?.isSpeaking == true) {
            stopSpeaking()
            return
        }

        isSpeaking = true
        btnPlay.setIconResource(R.drawable.ic_stop)

        lifecycleScope.launch {
            val analisis = analisisRepo.analizarLectura()

            if (analisis != null) {
                // 🔊 REPRODUCE EL MENSAJE SIN IMPORTAR SI HAY ALERTA O NO
                speakText(analisis.mensaje_lectura)
                startWaveformAnimation(analisis.mensaje_lectura.length)

                Log.d(TAG, "🔊 Reproduciendo mensaje: ${analisis.mensaje_lectura}")
            } else {
                runOnUiThread {
                    Toast.makeText(this@SensorDataActivity, "Error al obtener análisis de la IA.", Toast.LENGTH_SHORT).show()
                }
                stopSpeaking()
            }
        }
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
            stopSpeaking()
        }
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Idioma de TTS no compatible")
                btnPlay.isEnabled = false
            } else {
                btnPlay.isEnabled = true
            }
        } else {
            Log.e(TAG, "Error con TextToSpeech: $status")
            btnPlay.isEnabled = false
        }
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
        tts?.stop()
        tts?.shutdown()
    }

    // ---------------------------------------------------------------------
    //                         LÓGICA DE ALERTA INICIAL
    // ---------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun checkAndHandleAlert() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val lastAlertTimeMillis = prefs.getLong(KEY_LAST_ALERT_TIME, 0L)

        val lastAlertTime = Instant.ofEpochMilli(lastAlertTimeMillis)
        val currentTime = Instant.now()
        val hoursSinceLastAlert = ChronoUnit.HOURS.between(lastAlertTime, currentTime)

        val shouldCheck = lastAlertTimeMillis == 0L || hoursSinceLastAlert >= ALERT_COOLDOWN_HOURS

        if (shouldCheck) {
            Log.d(TAG, "Verificando alerta de IA (cooldown expirado o primer ingreso)...")

            val analisis = analisisRepo.analizarLectura()

            if (analisis != null) {
                if (analisis.alerta_ia == 1) {
                    mostrarAlerta(analisis.mensaje_lectura)
                    prefs.edit().putLong(KEY_LAST_ALERT_TIME, currentTime.toEpochMilli()).apply()
                } else {
                    Log.d(TAG, "Sistema Normal. Mensaje: ${analisis.mensaje_lectura}")
                }
            } else {
                Log.w(TAG, "No se pudo obtener el análisis de la IA.")
            }
        } else {
            Log.d(TAG, "Alerta omitida. Cooldown activo ($hoursSinceLastAlert horas transcurridas).")
        }
    }

    private fun mostrarAlerta(mensaje: String) {
        val intent = Intent(this, AlertsActivity::class.java).apply {
            putExtra("alert_message", mensaje)
        }
        startActivity(intent)
    }

    // ---------------------------------------------------------------------
    //                    CARGA DE GRÁFICAS COMPLETA
    // ---------------------------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun cargarSensoresYGraficas() {
        try {
            Log.d(TAG, "🔄 Cargando configuración y lecturas desde el servidor...")

            val configuraciones = graficasRepo.getGraficas()

            if (configuraciones.isEmpty()) {
                Log.w(TAG, "⚠️ No hay configuraciones guardadas, mostrando gráficas por defecto")
                runOnUiThread {
                    Toast.makeText(this@SensorDataActivity, "No hay configuraciones del administrador", Toast.LENGTH_SHORT).show()
                }
                mostrarGraficasPorDefecto()
                return
            }

            configuraciones.forEach { config ->
                val sensorId = config.sensor_id
                val tipoGrafica = config.tipo_grafica

                val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    getSensorColor(sensorId)
                } else {
                    when (sensorId) {
                        1 -> Color.BLUE
                        2 -> Color.GREEN
                        3 -> Color.RED
                        else -> Color.BLACK
                    }
                }

                val (nombreSensor, cardView) = when (sensorId) {
                    2 -> Pair("Temperatura", binding.cardTemperatura.root)
                    3 -> Pair("Presión", binding.cardPresion.root)
                    1 -> Pair("Metano", binding.cardMq4.root)
                    else -> return@forEach
                }

                val lecturas = lecturaRepo.getLecturas(sensorId)

                if (lecturas.isEmpty()) {
                    Log.w(TAG, "⚠️ Sin lecturas para el sensor $sensorId, usando datos simulados.")
                    return@forEach
                }

                val entries = lecturas.mapIndexed { index, lectura ->
                    Entry(index.toFloat(), lectura.valor.toFloat())
                }

                mostrarGrafica(tipoGrafica, cardView, nombreSensor, color, entries, sensorId)
            }

            Log.d(TAG, "✅ Gráficas actualizadas con datos reales")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al cargar datos del servidor", e)
            runOnUiThread {
                Toast.makeText(this@SensorDataActivity, "Error al cargar gráficas: ${e.message}", Toast.LENGTH_LONG).show()
            }
            mostrarGraficasPorDefecto()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getSensorColor(sensorId: Int): Int {
        return when (sensorId) {
            2 -> resources.getColor(R.color.temp_color, null)
            3 -> resources.getColor(R.color.pressure_color, null)
            1 -> resources.getColor(R.color.gas_color, null)
            else -> Color.BLACK
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getPieChartColors(sensorId: Int): List<Int> {
        return when (sensorId) {
            2 -> listOf(
                resources.getColor(R.color.temp_color_light, null),
                resources.getColor(R.color.temp_color, null),
                resources.getColor(R.color.temp_color_dark, null)
            )
            3 -> listOf(
                resources.getColor(R.color.pressure_color_light, null),
                resources.getColor(R.color.pressure_color, null),
                resources.getColor(R.color.pressure_color_dark, null)
            )
            1 -> listOf(
                resources.getColor(R.color.gas_color_light, null),
                resources.getColor(R.color.gas_color, null),
                resources.getColor(R.color.gas_color_dark, null)
            )
            else -> listOf(Color.GRAY, Color.DKGRAY, Color.BLACK)
        }
    }

    private fun mostrarGraficasPorDefecto() {
        val entriesTemp = listOf(Entry(1f, 25f), Entry(2f, 26f), Entry(3f, 28f))
        val entriesPresion = listOf(Entry(1f, 990f), Entry(2f, 1000f), Entry(3f, 1010f))
        val entriesMetano = listOf(Entry(1f, 0.05f), Entry(2f, 0.06f), Entry(3f, 0.07f))

        mostrarGrafica("line", binding.cardTemperatura.root, "Temperatura", Color.BLUE, entriesTemp, 2)
        mostrarGrafica("bar", binding.cardPresion.root, "Presión", Color.GREEN, entriesPresion, 3)
        mostrarGrafica("pie", binding.cardMq4.root, "Metano", Color.RED, entriesMetano, 1)
    }

    private fun mostrarGrafica(
        tipo: String,
        cardView: View,
        label: String,
        color: Int,
        entries: List<Entry>,
        sensorId: Int
    ) {
        val titleTextView = cardView.findViewById<TextView>(R.id.card_title)
        titleTextView?.text = label

        val lineChart = cardView.findViewById<LineChart>(R.id.chart_line)
        val barChart = cardView.findViewById<BarChart>(R.id.chart_bar)
        val pieChart = cardView.findViewById<PieChart>(R.id.chart_pie)

        lineChart?.visibility = View.GONE
        barChart?.visibility = View.GONE
        pieChart?.visibility = View.GONE

        when (tipo.lowercase()) {
            "line" -> {
                lineChart?.visibility = View.VISIBLE
                lineChart?.let { setupLineChart(it, label, color, entries) }
            }
            "bar" -> {
                barChart?.visibility = View.VISIBLE
                barChart?.let { setupBarChart(it, label, color, entries) }
            }
            "pie" -> {
                pieChart?.visibility = View.VISIBLE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    pieChart?.let { setupPieChart(it, label, entries, sensorId) }
                } else {
                    pieChart?.let { setupPieChartLegacy(it, label, entries) }
                }
            }
            else -> {
                lineChart?.visibility = View.VISIBLE
                lineChart?.let { setupLineChart(it, label, color, entries) }
            }
        }
    }

    private fun setupLineChart(chart: LineChart, label: String, color: Int, entries: List<Entry>) {
        val dataSet = LineDataSet(entries, label).apply {
            this.color = color
            valueTextColor = Color.BLACK
            lineWidth = 2f
            circleRadius = 3f
            setCircleColor(color)
            setDrawValues(true)
        }
        chart.data = LineData(dataSet)
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.invalidate()
    }

    private fun setupBarChart(chart: BarChart, label: String, color: Int, entries: List<Entry>) {
        val barEntries = entries.mapIndexed { index, entry -> BarEntry(index.toFloat(), entry.y) }
        val dataSet = BarDataSet(barEntries, label).apply {
            this.color = color
            valueTextColor = Color.BLACK
            setDrawValues(true)
        }
        chart.data = BarData(dataSet)
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.invalidate()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupPieChart(chart: PieChart, label: String, entries: List<Entry>, sensorId: Int) {
        val pieEntries = entries.mapIndexed { index, entry ->
            PieEntry(entry.y, "P${index + 1}")
        }

        val dataSet = PieDataSet(pieEntries, label).apply {
            colors = getPieChartColors(sensorId)
            valueTextColor = Color.BLACK
            valueTextSize = 12f
            setDrawValues(true)
        }
        chart.data = PieData(dataSet)
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.invalidate()
    }

    private fun setupPieChartLegacy(chart: PieChart, label: String, entries: List<Entry>) {
        val pieEntries = entries.mapIndexed { index, entry ->
            PieEntry(entry.y, "P${index + 1}")
        }
        val dataSet = PieDataSet(pieEntries, label).apply {
            colors = listOf(Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW)
            valueTextColor = Color.BLACK
            valueTextSize = 12f
        }
        chart.data = PieData(dataSet)
        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.invalidate()
    }
}
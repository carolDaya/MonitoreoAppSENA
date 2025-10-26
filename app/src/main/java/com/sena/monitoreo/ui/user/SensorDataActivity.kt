// Archivo: com.sena.monitoreo.ui.user.SensorDataActivity.kt

package com.sena.monitoreo.ui.user

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
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
import com.sena.monitoreo.R
import com.sena.monitoreo.databinding.ActivitySensorDataBinding
import com.sena.monitoreo.data.repository.GraficasRepository
import com.sena.monitoreo.data.repository.LecturaRepository // 👈 IMPORTANTE: Nuevo repositorio
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SensorDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySensorDataBinding
    private val graficasRepo = GraficasRepository()
    private val lecturaRepo = LecturaRepository() // 👈 NUEVA INSTANCIA
    private val TAG = "SensorDataActivity"
    private val refreshTime = 5 * 60 * 1000L // 5 minutos

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySensorDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            while (true) {
                cargarSensoresYGraficas()
                delay(refreshTime)
            }
        }
    }

    // ---------------------------------------------------------------------
    //                       FUNCIONES DE COLORES (EXISTENTES)
    // ---------------------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.M)
    private fun getSensorColor(sensorId: Int): Int {
        return when (sensorId) {
            1 -> resources.getColor(R.color.temp_color, null)
            2 -> resources.getColor(R.color.pressure_color, null)
            3 -> resources.getColor(R.color.gas_color, null)
            else -> Color.BLACK
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getPieChartColors(sensorId: Int): List<Int> {
        return when (sensorId) {
            1 -> listOf(
                resources.getColor(R.color.temp_color_light, null),
                resources.getColor(R.color.temp_color, null),
                resources.getColor(R.color.temp_color_dark, null)
            )
            2 -> listOf(
                resources.getColor(R.color.pressure_color_light, null),
                resources.getColor(R.color.pressure_color, null),
                resources.getColor(R.color.pressure_color_dark, null)
            )
            3 -> listOf(
                resources.getColor(R.color.gas_color_light, null),
                resources.getColor(R.color.gas_color, null),
                resources.getColor(R.color.gas_color_dark, null)
            )
            else -> listOf(Color.GRAY, Color.DKGRAY, Color.BLACK)
        }
    }

    // ---------------------------------------------------------------------
    //                    CARGA DE CONFIGURACIONES Y LECTURAS
    // ---------------------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun cargarSensoresYGraficas() {
        try {
            Log.d(TAG, "🔄 Cargando configuración y lecturas desde el servidor...")

            val configuraciones = graficasRepo.getGraficas() // Obtiene el tipo de gráfica

            if (configuraciones.isEmpty()) {
                Log.w(TAG, "⚠️ No hay configuraciones guardadas, mostrando gráficas por defecto")
                runOnUiThread {
                    Toast.makeText(
                        this@SensorDataActivity,
                        "No hay configuraciones del administrador",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                mostrarGraficasPorDefecto()
                return
            }

            configuraciones.forEach { config ->
                val sensorId = config.sensor_id
                val tipoGrafica = config.tipo_grafica

                // Obtener color según el sensor
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
                    1 -> Pair("Temperatura", binding.cardTemperatura.root)
                    2 -> Pair("Presión", binding.cardPresion.root)
                    3 -> Pair("Metano", binding.cardMq4.root)
                    else -> return@forEach
                }

                // 🔹 Obtener lecturas reales desde el backend
                // LÍNEA CORREGIDA: Usa el nuevo LecturaRepository
                val lecturas = lecturaRepo.getLecturas(sensorId)

                if (lecturas.isEmpty()) {
                    Log.w(TAG, "⚠️ Sin lecturas para el sensor $sensorId, usando datos simulados.")
                    // Se puede comentar mostrarGraficasPorDefecto() si quieres que no muestre nada
                    // mostrarGraficasPorDefecto()
                    return@forEach
                }

                // Convertir lecturas en datos para las gráficas
                val entries = lecturas.mapIndexed { index, lectura ->
                    // Usa el valor del modelo LecturaResponse
                    Entry(index.toFloat(), lectura.valor.toFloat())
                }

                mostrarGrafica(tipoGrafica, cardView, nombreSensor, color, entries, sensorId)
            }

            Log.d(TAG, "✅ Gráficas actualizadas con datos reales")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al cargar datos del servidor", e)
            runOnUiThread {
                Toast.makeText(
                    this@SensorDataActivity,
                    "Error al cargar gráficas: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            mostrarGraficasPorDefecto()
        }
    }

    // ---------------------------------------------------------------------
    //                  GRÁFICAS POR DEFECTO (EXISTENTES)
    // ---------------------------------------------------------------------
    private fun mostrarGraficasPorDefecto() {
        val entriesTemp = listOf(Entry(1f, 25f), Entry(2f, 26f), Entry(3f, 28f))
        val entriesPresion = listOf(Entry(1f, 990f), Entry(2f, 1000f), Entry(3f, 1010f))
        val entriesMetano = listOf(Entry(1f, 0.05f), Entry(2f, 0.06f), Entry(3f, 0.07f))

        mostrarGrafica("line", binding.cardTemperatura.root, "Temperatura", Color.BLUE, entriesTemp, 1)
        mostrarGrafica("bar", binding.cardPresion.root, "Presión", Color.GREEN, entriesPresion, 2)
        mostrarGrafica("pie", binding.cardMq4.root, "Metano", Color.RED, entriesMetano, 3)
    }

    // ---------------------------------------------------------------------
    //                        CONFIGURACIÓN DE GRÁFICAS (EXISTENTES)
    // ---------------------------------------------------------------------
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
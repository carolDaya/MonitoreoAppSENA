// com.sena.monitoreo.utils.charts.AdminChartManager.kt

package com.sena.monitoreo.utils.charts

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleCoroutineScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sena.monitoreo.R
import com.sena.monitoreo.data.repository.GraficasRepository
import kotlinx.coroutines.launch

class AdminChartManager(
    private val context: Context,
    private val graficasRepo: GraficasRepository,
    private val lifecycleScope: LifecycleCoroutineScope
) {

    // --- DATOS FALSOS PARA CONFIGURACIÓN ---
    private fun getFakeEntries(sensorId: Int): List<Entry> {
        return when (sensorId) {
            // Temperatura (2) - Valores de 20 a 30
            2 -> listOf(Entry(1f, 22f), Entry(2f, 25f), Entry(3f, 28f), Entry(4f, 27f))
            // Presión (3) - Valores de 980 a 1020
            3 -> listOf(Entry(1f, 990f), Entry(2f, 1005f), Entry(3f, 1015f), Entry(4f, 1000f))
            // Metano (1) - Valores de 0.01 a 0.10
            1 -> listOf(Entry(1f, 0.05f), Entry(2f, 0.03f), Entry(3f, 0.08f), Entry(4f, 0.06f))
            else -> emptyList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun loadInitialCharts(
        tempContainer: FrameLayout, tempBtn: ImageView,
        pressureContainer: FrameLayout, pressureBtn: ImageView,
        gasContainer: FrameLayout, gasBtn: ImageView
    ) {
        lifecycleScope.launch {
            try {
                val configs = graficasRepo.getGraficas()
                if (configs.isEmpty()) {
                    setupDefaultCharts(tempContainer, tempBtn, pressureContainer, pressureBtn, gasContainer, gasBtn)
                    return@launch
                }
                configs.forEach { config ->
                    when (config.sensor_id) {
                        2 -> updateChart(tempContainer, config.tipo_grafica, "Temperatura", tempBtn, 2)
                        3 -> updateChart(pressureContainer, config.tipo_grafica, "Presión", pressureBtn, 3)
                        1 -> updateChart(gasContainer, config.tipo_grafica, "Metano", gasBtn, 1)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar configuración de gráficas: ${e.message}", Toast.LENGTH_LONG).show()
                setupDefaultCharts(tempContainer, tempBtn, pressureContainer, pressureBtn, gasContainer, gasBtn)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupDefaultCharts(
        tempContainer: FrameLayout, tempBtn: ImageView,
        pressureContainer: FrameLayout, pressureBtn: ImageView,
        gasContainer: FrameLayout, gasBtn: ImageView
    ) {
        updateChart(tempContainer, "line", "Temperatura", tempBtn, 2)
        updateChart(pressureContainer, "line", "Presión", pressureBtn, 3)
        updateChart(gasContainer, "line", "Metano", gasBtn, 1)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun showGraphTypeDialog(label: String, container: FrameLayout, button: ImageView, sensorId: Int) {
        val graphTypes = arrayOf("Gráfica de Barra", "Gráfica de Línea", "Gráfica Circular")
        MaterialAlertDialogBuilder(context)
            .setTitle("Seleccionar Tipo de Gráfica")
            .setItems(graphTypes) { _, which ->
                val type = when (which) {
                    0 -> "bar"
                    1 -> "line"
                    2 -> "pie"
                    else -> "line"
                }
                updateChart(container, type, label, button, sensorId)
                lifecycleScope.launch {
                    try {
                        graficasRepo.updateGrafica(sensorId, type)
                        Toast.makeText(context, "Gráfica de $label actualizada a ${type}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun updateChart(container: FrameLayout, type: String, label: String, button: ImageView, sensorId: Int) {
        container.removeAllViews()
        val chart: Chart<*> = when (type) {
            "bar" -> BarChart(context)
            "line" -> LineChart(context)
            "pie" -> PieChart(context)
            else -> LineChart(context)
        }

        // Dibuja la gráfica con datos falsos
        setupChartData(chart, type, label, sensorId)

        // Configuración visual básica y añade el botón de cambio
        chart.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        container.addView(chart)
        // El botón debe ir sobre la gráfica
        container.addView(button)
        button.bringToFront()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupChartData(chart: Chart<*>, type: String, label: String, sensorId: Int) {
        val entries = getFakeEntries(sensorId)
        val color = getSensorColor(sensorId)

        when (chart) {
            is LineChart -> {
                val dataSet = LineDataSet(entries, label).apply {
                    this.color = color
                    setCircleColor(color)
                    lineWidth = 2f
                    setDrawValues(false)
                }
                chart.data = LineData(dataSet)
            }
            is BarChart -> {
                val barEntries = entries.map { BarEntry(it.x, it.y) }
                val dataSet = BarDataSet(barEntries, label).apply {
                    this.color = color
                    setDrawValues(false)
                }
                chart.data = BarData(dataSet)
            }
            is PieChart -> {
                // Para PieChart, se usa la suma de los valores falsos
                val totalValue = entries.sumOf { it.y.toDouble() }.toFloat()
                val pieEntries = listOf(
                    PieEntry(totalValue * 0.4f, "V1"),
                    PieEntry(totalValue * 0.3f, "V2"),
                    PieEntry(totalValue * 0.3f, "V3")
                )
                val dataSet = PieDataSet(pieEntries, label).apply {
                    colors = getPieChartColors(sensorId)
                    valueTextSize = 12f
                }
                chart.data = PieData(dataSet)
            }
        }
        chart.invalidate()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getSensorColor(sensorId: Int): Int = when (sensorId) {
        2 -> context.resources.getColor(R.color.temp_color, null) // Temperatura
        3 -> context.resources.getColor(R.color.pressure_color, null) // Presión
        1 -> context.resources.getColor(R.color.gas_color, null) // Metano
        else -> context.resources.getColor(R.color.teal_700, null)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getPieChartColors(sensorId: Int): List<Int> = when(sensorId) {
        2 -> listOf(
            context.resources.getColor(R.color.temp_color_light, null),
            context.resources.getColor(R.color.temp_color, null),
            context.resources.getColor(R.color.temp_color_dark, null)
        )
        3 -> listOf(
            context.resources.getColor(R.color.pressure_color_light, null),
            context.resources.getColor(R.color.pressure_color, null),
            context.resources.getColor(R.color.pressure_color_dark, null)
        )
        1 -> listOf(
            context.resources.getColor(R.color.gas_color_light, null),
            context.resources.getColor(R.color.gas_color, null),
            context.resources.getColor(R.color.gas_color_dark, null)
        )
        else -> listOf(context.resources.getColor(R.color.teal_200, null), context.resources.getColor(R.color.teal_700, null))
    }
}
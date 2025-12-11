package com.sena.monitoreo.utils.charts

import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.data.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sena.monitoreo.R
import com.sena.monitoreo.data.repository.GraficasRepository
import com.sena.monitoreo.utils.ResultWrapper
import kotlinx.coroutines.launch

class AdminChartManager(
    private val context: Context,
    private val graficasRepo: GraficasRepository,
    private val lifecycleScope: LifecycleCoroutineScope
) {

    companion object {
        private const val SENSOR_METANO = 1
        private const val SENSOR_TEMPERATURA = 3
        private const val SENSOR_PRESION = 2
    }

    // --- DATOS FALSOS PARA CONFIGURACIÓN ---
    private fun getFakeEntries(sensorId: Int): List<Entry> {
        return when (sensorId) {
            SENSOR_TEMPERATURA -> listOf(Entry(1f, 22f), Entry(2f, 25f), Entry(3f, 28f), Entry(4f, 27f))
            SENSOR_PRESION -> listOf(Entry(1f, 990f), Entry(2f, 1005f), Entry(3f, 1015f), Entry(4f, 1000f))
            SENSOR_METANO -> listOf(Entry(1f, 0.05f), Entry(2f, 0.03f), Entry(3f, 0.08f), Entry(4f, 0.06f))
            else -> emptyList()
        }
    }

    /**
     * Carga la configuración inicial de las gráficas desde el repositorio.
     */
    fun loadInitialCharts(
        tempContainer: FrameLayout, tempBtn: ImageView,
        pressureContainer: FrameLayout, pressureBtn: ImageView,
        gasContainer: FrameLayout, gasBtn: ImageView
    ) {
        lifecycleScope.launch {
            when (val result = graficasRepo.getGraficas()) {
                is ResultWrapper.Success -> {
                    val configs = result.data
                    if (configs.isEmpty()) {
                        setupDefaultCharts(tempContainer, tempBtn, pressureContainer, pressureBtn, gasContainer, gasBtn)
                        return@launch
                    }
                    configs.forEach { config ->
                        when (config.sensor_id) {
                            SENSOR_TEMPERATURA -> updateChart(tempContainer, config.tipo_grafica, "Temperatura", tempBtn, SENSOR_TEMPERATURA)
                            SENSOR_PRESION -> updateChart(pressureContainer, config.tipo_grafica, "Presión", pressureBtn, SENSOR_PRESION)
                            SENSOR_METANO -> updateChart(gasContainer, config.tipo_grafica, "Metano", gasBtn, SENSOR_METANO)
                        }
                    }
                }
                is ResultWrapper.Error -> {
                    Toast.makeText(context, "Error al cargar configuración: ${result.message}", Toast.LENGTH_LONG).show()
                    setupDefaultCharts(tempContainer, tempBtn, pressureContainer, pressureBtn, gasContainer, gasBtn)
                }
            }
        }
    }

    /**
     * Establece gráficas de línea por defecto si no hay configuración guardada.
     */
    private fun setupDefaultCharts(
        tempContainer: FrameLayout, tempBtn: ImageView,
        pressureContainer: FrameLayout, pressureBtn: ImageView,
        gasContainer: FrameLayout, gasBtn: ImageView
    ) {
        updateChart(tempContainer, "line", "Temperatura", tempBtn, SENSOR_TEMPERATURA)
        updateChart(pressureContainer, "line", "Presión", pressureBtn, SENSOR_PRESION)
        updateChart(gasContainer, "line", "Metano", gasBtn, SENSOR_METANO)
    }

    /**
     * Muestra el diálogo para seleccionar el tipo de gráfica y guarda el cambio.
     */
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
                    when (val result = graficasRepo.updateGrafica(sensorId, type)) {
                        is ResultWrapper.Success -> {
                            Toast.makeText(context, "Gráfica de $label actualizada a $type", Toast.LENGTH_SHORT).show()
                        }
                        is ResultWrapper.Error -> {
                            Toast.makeText(context, "Error al guardar configuración: ${result.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }

    /**
     * Crea y dibuja la gráfica seleccionada en el contenedor FrameLayout.
     */
    fun updateChart(container: FrameLayout, type: String, label: String, button: ImageView, sensorId: Int) {
        container.removeAllViews()
        val chart: Chart<*> = when (type) {
            "bar" -> BarChart(context)
            "line" -> LineChart(context)
            "pie" -> PieChart(context)
            else -> LineChart(context)
        }

        setupChartData(chart, label, sensorId)

        chart.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        container.addView(chart)
        container.addView(button)
        button.bringToFront()
    }

    /**
     * Configura los datos y el estilo para el tipo de gráfica dado.
     */
    private fun setupChartData(chart: Chart<*>, label: String, sensorId: Int) {
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
                val totalValue = entries.sumOf { it.y.toDouble() }.toFloat()
                val pieEntries = listOf(
                    PieEntry(totalValue * 0.4f, "Valor 1"),
                    PieEntry(totalValue * 0.3f, "Valor 2"),
                    PieEntry(totalValue * 0.3f, "Valor 3")
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

    /**
     * Obtiene el color principal del sensor usando ContextCompat para mayor seguridad.
     */
    fun getSensorColor(sensorId: Int): Int = ContextCompat.getColor(
        context, when (sensorId) {
            SENSOR_TEMPERATURA -> R.color.temp_color
            SENSOR_PRESION -> R.color.pressure_color
            SENSOR_METANO -> R.color.gas_color
            else -> R.color.teal_700
        }
    )

    /**
     * Obtiene la lista de colores para la gráfica circular (PieChart).
     */
    private fun getPieChartColors(sensorId: Int): List<Int> {
        val lightColor: Int
        val normalColor: Int
        val darkColor: Int

        when (sensorId) {
            SENSOR_TEMPERATURA -> {
                lightColor = R.color.temp_color_light
                normalColor = R.color.temp_color
                darkColor = R.color.temp_color_dark
            }
            SENSOR_PRESION -> {
                lightColor = R.color.pressure_color_light
                normalColor = R.color.pressure_color
                darkColor = R.color.pressure_color_dark
            }
            SENSOR_METANO -> {
                lightColor = R.color.gas_color_light
                normalColor = R.color.gas_color
                darkColor = R.color.gas_color_dark
            }
            else -> return listOf(ContextCompat.getColor(context, R.color.teal_200), ContextCompat.getColor(context, R.color.teal_700))
        }

        return listOf(
            ContextCompat.getColor(context, lightColor),
            ContextCompat.getColor(context, normalColor),
            ContextCompat.getColor(context, darkColor)
        )
    }
}
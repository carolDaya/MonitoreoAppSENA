package com.sena.monitoreo.utils.charts

import android.util.Log
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.sena.monitoreo.R

class ChartManager(private val context: Context) {

    companion object {
        // Constantes para mayor legibilidad
        private const val SENSOR_METANO = 1
        private const val SENSOR_TEMPERATURA = 2
        private const val SENSOR_PRESION = 3
    }
    fun displayChart(
        chartType: String,
        cardView: View,
        label: String,
        color: Int,
        entries: List<Entry>,
        sensorId: Int
    ) {
        Log.d("ChartManager", "🎨 displayChart INICIADO para $label")

        val titleTextView = cardView.findViewById<TextView>(R.id.card_title)
        titleTextView?.text = label

        // ✅ Limpiar mensajes de carga/error previos
        val chartContainer = cardView.findViewById<ViewGroup>(R.id.chart_container)
        chartContainer?.findViewWithTag<View>("loading_view")?.let {
            chartContainer.removeView(it)
            Log.d("ChartManager", "🗑️ Loading view removido")
        }
        chartContainer?.findViewWithTag<View>("no_data_message")?.let {
            chartContainer.removeView(it)
            Log.d("ChartManager", "🗑️ No data message removido")
        }

        val lineChart = cardView.findViewById<LineChart>(R.id.chart_line)
        val barChart = cardView.findViewById<BarChart>(R.id.chart_bar)
        val pieChart = cardView.findViewById<PieChart>(R.id.chart_pie)

        Log.d("ChartManager", "🔍 Charts: Line=${lineChart != null}, Bar=${barChart != null}, Pie=${pieChart != null}")

        if (lineChart == null && barChart == null && pieChart == null) {
            Log.e("ChartManager", "❌ CHARTS NO ENCONTRADOS EN EL LAYOUT")
            return
        }

        // Ocultar todos
        lineChart?.visibility = View.GONE
        barChart?.visibility = View.GONE
        pieChart?.visibility = View.GONE

        val lowerChartType = chartType.lowercase(java.util.Locale.US)

        when (lowerChartType) {
            "line" -> {
                Log.d("ChartManager", "📈 Mostrando LineChart")
                lineChart?.apply {
                    visibility = View.VISIBLE
                    setupLineChart(this, label, color, entries)
                }
            }
            "bar" -> {
                Log.d("ChartManager", "📊 Mostrando BarChart")
                barChart?.apply {
                    visibility = View.VISIBLE
                    setupBarChart(this, label, color, entries)
                }
            }
            "pie" -> {
                Log.d("ChartManager", "🥧 Mostrando PieChart")
                pieChart?.apply {
                    visibility = View.VISIBLE
                    setupPieChart(this, label, entries, sensorId)
                }
            }
            else -> {
                Log.w("ChartManager", "⚠️ Tipo desconocido, usando LINE")
                lineChart?.apply {
                    visibility = View.VISIBLE
                    setupLineChart(this, label, color, entries)
                }
            }
        }

        Log.d("ChartManager", "✅ displayChart completado")
    }

    // ✅ Método helper para debug
    private fun printViewHierarchy(view: View, level: Int) {
        val indent = "  ".repeat(level)
        Log.d("ChartManager", "$indent- ${view::class.simpleName} (id: ${view.id})")

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                printViewHierarchy(view.getChildAt(i), level + 1)
            }
        }
    }
    /**
     * Configura los datos y el estilo de la Gráfica de Línea.
     */
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

    /**
     * Configura los datos y el estilo de la Gráfica de Barra.
     */
    private fun setupBarChart(chart: BarChart, label: String, color: Int, entries: List<Entry>) {
        // El mapeo de BarEntry se realiza directamente en el dataset
        val barEntries = entries.mapIndexed { index, entry -> BarEntry(entry.x, entry.y) }
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

    /**
     * Configura los datos y el estilo de la Gráfica Circular.
     * Usamos el sensorId para obtener la paleta de colores.
     */
    private fun setupPieChart(chart: PieChart, label: String, entries: List<Entry>, sensorId: Int) {
        val pieEntries = entries.mapIndexed { index, entry -> PieEntry(entry.y, "P${index+1}") }

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

    /**
     * Obtiene la paleta de colores específica para el PieChart.
     */
    private fun getPieChartColors(sensorId: Int): List<Int> {
        val lightColorRes: Int
        val normalColorRes: Int
        val darkColorRes: Int

        when (sensorId) {
            SENSOR_TEMPERATURA -> {
                lightColorRes = R.color.temp_color_light
                normalColorRes = R.color.temp_color
                darkColorRes = R.color.temp_color_dark
            }
            SENSOR_PRESION -> {
                lightColorRes = R.color.pressure_color_light
                normalColorRes = R.color.pressure_color
                darkColorRes = R.color.pressure_color_dark
            }
            SENSOR_METANO -> {
                lightColorRes = R.color.gas_color_light
                normalColorRes = R.color.gas_color
                darkColorRes = R.color.gas_color_dark
            }
            else -> return listOf(Color.GRAY, Color.DKGRAY, Color.BLACK)
        }

        return listOf(
            ContextCompat.getColor(context, lightColorRes),
            ContextCompat.getColor(context, normalColorRes),
            ContextCompat.getColor(context, darkColorRes)
        )
    }

    /**
     * Obtiene el color principal del sensor usando ContextCompat para compatibilidad.
     */
    fun getSensorColor(sensorId: Int): Int = ContextCompat.getColor(
        context, when (sensorId) {
            SENSOR_TEMPERATURA -> R.color.temp_color
            SENSOR_PRESION -> R.color.pressure_color
            SENSOR_METANO -> R.color.gas_color
            else -> R.color.teal_700 // Se usa un color por defecto o negro
        }
    )
}
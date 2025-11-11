// ChartManager.kt
package com.sena.monitoreo.utils.charts

import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.sena.monitoreo.R
import android.content.Context

class ChartManager(private val context: Context) {

    @RequiresApi(Build.VERSION_CODES.O)
    fun displayChart(
        chartType: String,
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

        // Ocultar todos primero
        lineChart?.visibility = View.GONE
        barChart?.visibility = View.GONE
        pieChart?.visibility = View.GONE

        when (chartType.lowercase()) {
            "line" -> lineChart?.apply {
                visibility = View.VISIBLE
                setupLineChart(this, label, color, entries)
            }
            "bar" -> barChart?.apply {
                visibility = View.VISIBLE
                setupBarChart(this, label, color, entries)
            }
            "pie" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pieChart?.apply {
                    visibility = View.VISIBLE
                    setupPieChart(this, label, entries, sensorId)
                }
            }
            else -> lineChart?.apply {
                visibility = View.VISIBLE
                setupLineChart(this, label, color, entries)
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
        else -> listOf(Color.GRAY, Color.DKGRAY, Color.BLACK)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getSensorColor(sensorId: Int): Int = when (sensorId) {
        2 -> context.resources.getColor(R.color.temp_color, null)
        3 -> context.resources.getColor(R.color.pressure_color, null)
        1 -> context.resources.getColor(R.color.gas_color, null)
        else -> Color.BLACK
    }
}
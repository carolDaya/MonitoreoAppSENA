package com.sena.monitoreo.ui.admin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.data.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sena.monitoreo.R
import com.sena.monitoreo.databinding.ActivityAdminDashboardBinding
import com.sena.monitoreo.databinding.HeaderLayoutAdminBinding
import com.sena.monitoreo.ui.auth.LoginActivity
import com.sena.monitoreo.data.repository.GraficasRepository
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var headerBinding: HeaderLayoutAdminBinding
    private val graficasRepo = GraficasRepository()
    private val TAG = "AdminDashboard"

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Menú lateral
        setupNavigationDrawer()

        // Cargar gráficas desde el servidor
        cargarGraficasGuardadas()

        // Listeners para cambiar tipo de gráfica
        setupChartClickListeners()

        // Mostrar home por defecto
        showSection(home = true)
    }

    // --- Menú lateral ---
    private fun setupNavigationDrawer() {
        headerBinding = HeaderLayoutAdminBinding.bind(binding.mainHeader.root)

        headerBinding.settingsIcon.setOnClickListener {
            binding.adminDashboard.openDrawer(GravityCompat.START)
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            binding.adminDashboard.closeDrawers()

            when (menuItem.itemId) {
                R.id.nav_home -> showSection(home = true)
                R.id.nav_graphis -> showSection(graphs = true)
                R.id.nav_volumen -> showSection(ai = true)
                R.id.nav_users -> showSection(users = true)
                R.id.nav_logout -> performLogout()
            }
            true
        }
    }

    private fun showSection(
        home: Boolean = false,
        graphs: Boolean = false,
        ai: Boolean = false,
        users: Boolean = false
    ) {
        with(binding) {
            graficasAdminSection.root.visibility = View.GONE
            iaAdminSection.root.visibility = View.GONE
            userAdminSection.root.visibility = View.GONE

            when {
                home -> {
                    graficasAdminSection.root.visibility = View.VISIBLE
                    iaAdminSection.root.visibility = View.VISIBLE
                    userAdminSection.root.visibility = View.VISIBLE
                }
                graphs -> graficasAdminSection.root.visibility = View.VISIBLE
                ai -> iaAdminSection.root.visibility = View.VISIBLE
                users -> userAdminSection.root.visibility = View.VISIBLE
            }
        }
    }

    private fun performLogout() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // --- Cargar configuraciones guardadas ---
    @RequiresApi(Build.VERSION_CODES.M)
    private fun cargarGraficasGuardadas() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔄 Cargando configuraciones guardadas desde el servidor...")

                val configuraciones = graficasRepo.getGraficas()

                if (configuraciones.isEmpty()) {
                    Log.w(TAG, "⚠️ No hay configuraciones guardadas, usando valores por defecto")
                    setupChartsDefault()
                    return@launch
                }

                Log.d(TAG, "✅ Se encontraron ${configuraciones.size} configuraciones guardadas")

                // Mapear las configuraciones a los contenedores correspondientes
                configuraciones.forEach { config ->
                    val (container, button, label) = when (config.sensor_id) {
                        1 -> Triple(
                            binding.graficasAdminSection.graphContainerTemp,
                            binding.graficasAdminSection.btnChangeTemp,
                            "Temperatura"
                        )
                        2 -> Triple(
                            binding.graficasAdminSection.graphContainerPressure,
                            binding.graficasAdminSection.btnChangePressure,
                            "Presión"
                        )
                        3 -> Triple(
                            binding.graficasAdminSection.graphContainerGas,
                            binding.graficasAdminSection.btnChangeGas,
                            "Metano"
                        )
                        else -> {
                            Log.w(TAG, "⚠️ Sensor ID desconocido: ${config.sensor_id}")
                            return@forEach
                        }
                    }

                    // Actualizar la gráfica con el tipo guardado
                    updateChart(container, config.tipo_grafica, label, button, config.sensor_id)
                    Log.d(TAG, "📊 Gráfica de $label cargada como '${config.tipo_grafica}'")
                }

                Toast.makeText(this@AdminDashboardActivity, "Configuraciones cargadas", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al cargar configuraciones", e)
                Toast.makeText(this@AdminDashboardActivity, "Error al cargar configuraciones", Toast.LENGTH_SHORT).show()
                setupChartsDefault()
            }
        }
    }

    // --- Configuración por defecto ---
    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupChartsDefault() {
        Log.d(TAG, "📊 Configurando gráficas por defecto (línea)")
        updateChart(binding.graficasAdminSection.graphContainerTemp, "line", "Temperatura", binding.graficasAdminSection.btnChangeTemp, 1)
        updateChart(binding.graficasAdminSection.graphContainerPressure, "line", "Presión", binding.graficasAdminSection.btnChangePressure, 2)
        updateChart(binding.graficasAdminSection.graphContainerGas, "line", "Metano", binding.graficasAdminSection.btnChangeGas, 3)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupChartClickListeners() {
        binding.graficasAdminSection.btnChangeTemp.setOnClickListener {
            showGraphTypeDialog("Temperatura", binding.graficasAdminSection.graphContainerTemp, binding.graficasAdminSection.btnChangeTemp, 1)
        }
        binding.graficasAdminSection.btnChangePressure.setOnClickListener {
            showGraphTypeDialog("Presión", binding.graficasAdminSection.graphContainerPressure, binding.graficasAdminSection.btnChangePressure, 2)
        }
        binding.graficasAdminSection.btnChangeGas.setOnClickListener {
            showGraphTypeDialog("Metano", binding.graficasAdminSection.graphContainerGas, binding.graficasAdminSection.btnChangeGas, 3)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showGraphTypeDialog(label: String, container: FrameLayout, button: ImageView, sensorId: Int) {
        val graphTypes = arrayOf("Gráfica de Barra", "Gráfica de Línea", "Gráfica Circular")
        MaterialAlertDialogBuilder(this)
            .setTitle("Seleccionar Tipo de Gráfica")
            .setItems(graphTypes) { _, which ->
                val type = when (which) {
                    0 -> "bar"
                    1 -> "line"
                    2 -> "pie"
                    else -> "line"
                }

                // Actualizar UI inmediatamente
                updateChart(container, type, label, button, sensorId)

                // Guardar en backend
                lifecycleScope.launch {
                    try {
                        Log.d(TAG, "💾 Guardando configuración: sensor=$sensorId, tipo=$type")
                        val result = graficasRepo.updateGrafica(sensorId, type)

                        if (result != null) {
                            Log.d(TAG, "✅ Configuración guardada exitosamente")
                            Toast.makeText(
                                this@AdminDashboardActivity,
                                "Gráfica de $label cambiada a ${graphTypes[which]}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Log.e(TAG, "❌ Error al guardar en el servidor")
                            Toast.makeText(
                                this@AdminDashboardActivity,
                                "Error guardando en servidor",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Excepción al guardar", e)
                        Toast.makeText(
                            this@AdminDashboardActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .show()
    }

    // --- Función con colores y datos de ejemplo ajustados ---
    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateChart(container: FrameLayout, type: String, label: String, button: ImageView, sensorId: Int) {
        container.removeAllViews()

        val chart: Chart<*> = when (type) {
            "bar" -> BarChart(this)
            "line" -> LineChart(this)
            "pie" -> PieChart(this)
            else -> LineChart(this)
        }

        // --- ASIGNACIÓN DE COLORES Y DATOS MÁS COHERENTES ---
        val chartColor = when (sensorId) {
            1 -> resources.getColor(R.color.temp_color, null)      // Temperatura -> Sugiere un color como Azul o Rojo
            2 -> resources.getColor(R.color.pressure_color, null)  // Presión -> Sugiere un color como Verde o Naranja
            3 -> resources.getColor(R.color.gas_color, null)       // Metano -> Sugiere un color como Rojo o Amarillo (por riesgo)
            else -> resources.getColor(R.color.teal_700, null)
        }

        when (chart) {
            is BarChart -> {
                val entries = when (sensorId) {
                    1 -> listOf(
                        BarEntry(1f, 25f), // Típico de una lectura normal
                        BarEntry(2f, 28f),
                        BarEntry(3f, 32f) // Un poco alta
                    ) // Temperatura
                    2 -> listOf(
                        BarEntry(1f, 1000f), // Rangos de presión (ej: hPa)
                        BarEntry(2f, 980f),
                        BarEntry(3f, 1015f)
                    ) // Presión
                    3 -> listOf(
                        BarEntry(1f, 0.05f), // Rangos de concentración (ej: % o PPM)
                        BarEntry(2f, 0.12f),
                        BarEntry(3f, 0.08f)
                    ) // Metano
                    else -> listOf(BarEntry(1f, 10f), BarEntry(2f, 15f), BarEntry(3f, 12f))
                }

                val dataSet = BarDataSet(entries, label).apply {
                    color = chartColor
                    valueTextColor = resources.getColor(R.color.black, null)
                }
                chart.data = BarData(dataSet)
                // Ocultar ejes si no son necesarios, o configurarlos
                chart.xAxis.isEnabled = false
                chart.axisLeft.isEnabled = true
                chart.axisRight.isEnabled = false
            }
            is LineChart -> {
                val entries = when (sensorId) {
                    1 -> listOf(
                        Entry(1f, 24.5f), // Típico de una lectura normal
                        Entry(2f, 26.0f),
                        Entry(3f, 25.8f),
                        Entry(4f, 27.1f)
                    ) // Temperatura
                    2 -> listOf(
                        Entry(1f, 1012f), // Rangos de presión (ej: hPa)
                        Entry(2f, 1010f),
                        Entry(3f, 1015f),
                        Entry(4f, 1013f)
                    ) // Presión
                    3 -> listOf(
                        Entry(1f, 0.03f), // Rangos de concentración (ej: % o PPM)
                        Entry(2f, 0.04f),
                        Entry(3f, 0.07f),
                        Entry(4f, 0.05f)
                    ) // Metano
                    else -> listOf(Entry(1f, 20f), Entry(2f, 25f), Entry(3f, 22f))
                }

                val dataSet = LineDataSet(entries, label).apply {
                    color = chartColor
                    valueTextColor = resources.getColor(R.color.black, null)
                    lineWidth = 2f
                    setDrawCircles(true)
                    setDrawValues(true)
                }
                chart.data = LineData(dataSet)
                // Ocultar ejes si no son necesarios, o configurarlos
                chart.xAxis.isEnabled = false
                chart.axisLeft.isEnabled = true
                chart.axisRight.isEnabled = false
            }
            is PieChart -> {
                val entries = when (sensorId) {
                    1 -> listOf(
                        PieEntry(60f, "Normal (20-30°C)"),
                        PieEntry(30f, "Alta (>30°C)"),
                        PieEntry(10f, "Baja (<20°C)")
                    ) // Temperatura: Distribución de las lecturas
                    2 -> listOf(
                        PieEntry(75f, "Normal"),
                        PieEntry(15f, "Baja"),
                        PieEntry(10f, "Alta")
                    ) // Presión: Distribución de las lecturas
                    3 -> listOf(
                        PieEntry(90f, "Baja/Segura"),
                        PieEntry(8f, "Media"),
                        PieEntry(2f, "Alta/Alerta")
                    ) // Metano: Distribución de las lecturas (mayoría segura)
                    else -> listOf(PieEntry(40f, "Alta"), PieEntry(30f, "Media"), PieEntry(30f, "Baja"))
                }

                // Usar un set de colores más variado para PieCharts
                val pieColors = when (sensorId) {
                    1 -> listOf(
                        resources.getColor(R.color.temp_color_light, null),
                        resources.getColor(R.color.temp_color, null),
                        resources.getColor(R.color.temp_color_dark, null)
                    ) // Tonos de temperatura
                    2 -> listOf(
                        resources.getColor(R.color.pressure_color_light, null),
                        resources.getColor(R.color.pressure_color, null),
                        resources.getColor(R.color.pressure_color_dark, null)
                    ) // Tonos de presión
                    3 -> listOf(
                        resources.getColor(R.color.gas_color_light, null),
                        resources.getColor(R.color.gas_color, null),
                        resources.getColor(R.color.gas_color_dark, null)
                    ) // Tonos de gas
                    else -> listOf(
                        resources.getColor(R.color.teal_200, null),
                        resources.getColor(R.color.teal_400, null),
                        resources.getColor(R.color.teal_700, null)
                    )
                }

                val dataSet = PieDataSet(entries, label).apply {
                    colors = pieColors
                    valueTextColor = resources.getColor(R.color.black, null)
                    valueTextSize = 12f
                }
                chart.data = PieData(dataSet).apply {
                    setDrawValues(true)
                }
                chart.setDrawEntryLabels(false) // Opcional: Ocultar etiquetas para limpieza
                chart.legend.isEnabled = true // Mostrar leyenda para PieChart
            }
        }

        chart.description.isEnabled = false
        chart.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        chart.invalidate()

        container.removeAllViews() // Asegura que solo quede uno
        container.addView(chart)
        container.addView(button) // El botón para cambiar el tipo de gráfica
    }
}
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

        // Configuración del menú lateral
        setupNavigationDrawer()

        // Cargar gráficas desde el servidor
        cargarGraficasGuardadas()

        // Listeners para cambiar tipo de gráfica
        setupChartClickListeners()

        // Mostrar home por defecto (que incluye todas las secciones visibles)
        showSection(home = true)
    }

    // --- Menú lateral ---
    private fun setupNavigationDrawer() {
        // Vincula el header incluido usando el .root
        headerBinding = HeaderLayoutAdminBinding.bind(binding.mainHeader.root)

        // Listener para abrir el menú con el ícono de configuración
        headerBinding.settingsIcon.setOnClickListener {
            binding.adminDashboard.openDrawer(GravityCompat.START)
        }

        // Listener para la selección de ítems del menú
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            binding.adminDashboard.closeDrawers()

            when (menuItem.itemId) {
                R.id.nav_home -> showSection(home = true)
                R.id.nav_graphis -> showSection(graphs = true)
                R.id.nav_volumen -> showSection(ai = true)
                R.id.nav_users -> showSection(users = true) // Mostrar solo sección de usuarios
                R.id.nav_logout -> performLogout()
            }
            true
        }
    }

    /**
     * Muestra la sección de contenido solicitada y oculta las demás.
     * Todas las secciones incluidas (<include>) deben ser referenciadas con .root
     */
    private fun showSection(
        home: Boolean = false,
        graphs: Boolean = false,
        ai: Boolean = false,
        users: Boolean = false
    ) {
        with(binding) {
            // Ocultar todas las secciones inicialmente (usando .root para las inclusiones)
            graficasAdminSection.root.visibility = View.GONE
            iaAdminSection.root.visibility = View.GONE
            userAdminSection.root.visibility = View.GONE

            when {
                // El modo Home muestra todas las secciones a la vez
                home -> {
                    graficasAdminSection.root.visibility = View.VISIBLE
                    iaAdminSection.root.visibility = View.VISIBLE
                    userAdminSection.root.visibility = View.VISIBLE
                }
                graphs -> graficasAdminSection.root.visibility = View.VISIBLE
                ai -> iaAdminSection.root.visibility = View.VISIBLE
                // ESTA ES LA ACCIÓN DE USUARIO CORRECTA
                users -> userAdminSection.root.visibility = View.VISIBLE
            }
        }
    }

    private fun performLogout() {
        // Lógica de deslogueo y navegación a la pantalla de Login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // --- Lógica de Gráficas (sin cambios funcionales, solo para completar el archivo) ---

    @RequiresApi(Build.VERSION_CODES.M)
    private fun cargarGraficasGuardadas() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔄 Cargando configuraciones guardadas desde el servidor...")

                // Placeholder: Simular carga de datos
                val configuraciones = emptyList<Any>()

                if (configuraciones.isEmpty()) {
                    Log.w(TAG, "⚠️ No hay configuraciones guardadas, usando valores por defecto")
                    setupChartsDefault()
                    return@launch
                }

                // Lógica de carga real de configuraciones...

                Toast.makeText(this@AdminDashboardActivity, "Configuraciones cargadas", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al cargar configuraciones", e)
                Toast.makeText(this@AdminDashboardActivity, "Error al cargar configuraciones", Toast.LENGTH_SHORT).show()
                setupChartsDefault()
            }
        }
    }

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

                // Guardar en backend (simulación)
                lifecycleScope.launch {
                    try {
                        Log.d(TAG, "💾 Guardando configuración: sensor=$sensorId, tipo=$type")
                        val result = true // Placeholder para simular éxito
                        // val result = graficasRepo.updateGrafica(sensorId, type) // Lógica de API real

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

    // --- Lógica para Dibujar Gráficas ---
    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateChart(container: FrameLayout, type: String, label: String, button: ImageView, sensorId: Int) {
        container.removeAllViews()

        val chart: Chart<*> = when (type) {
            "bar" -> BarChart(this)
            "line" -> LineChart(this)
            "pie" -> PieChart(this)
            else -> LineChart(this)
        }

        // Asignación de colores
        val chartColor = when (sensorId) {
            1 -> resources.getColor(R.color.temp_color, null)
            2 -> resources.getColor(R.color.pressure_color, null)
            3 -> resources.getColor(R.color.gas_color, null)
            else -> resources.getColor(R.color.teal_700, null)
        }

        when (chart) {
            is BarChart -> {
                val entries = when (sensorId) {
                    1 -> listOf(BarEntry(1f, 25f), BarEntry(2f, 28f), BarEntry(3f, 32f))
                    2 -> listOf(BarEntry(1f, 1000f), BarEntry(2f, 980f), BarEntry(3f, 1015f))
                    3 -> listOf(BarEntry(1f, 0.05f), BarEntry(2f, 0.12f), BarEntry(3f, 0.08f))
                    else -> listOf(BarEntry(1f, 10f), BarEntry(2f, 15f), BarEntry(3f, 12f))
                }
                val dataSet = BarDataSet(entries, label).apply { color = chartColor }
                chart.data = BarData(dataSet)
                chart.xAxis.isEnabled = false
                chart.axisLeft.isEnabled = true
                chart.axisRight.isEnabled = false
            }
            is LineChart -> {
                val entries = when (sensorId) {
                    1 -> listOf(Entry(1f, 24.5f), Entry(2f, 26.0f), Entry(3f, 25.8f), Entry(4f, 27.1f))
                    2 -> listOf(Entry(1f, 1012f), Entry(2f, 1010f), Entry(3f, 1015f), Entry(4f, 1013f))
                    3 -> listOf(Entry(1f, 0.03f), Entry(2f, 0.04f), Entry(3f, 0.07f), Entry(4f, 0.05f))
                    else -> listOf(Entry(1f, 20f), Entry(2f, 25f), Entry(3f, 22f))
                }
                val dataSet = LineDataSet(entries, label).apply {
                    color = chartColor
                    lineWidth = 2f
                    setDrawCircles(true)
                    setDrawValues(true)
                }
                chart.data = LineData(dataSet)
                chart.xAxis.isEnabled = false
                chart.axisLeft.isEnabled = true
                chart.axisRight.isEnabled = false
            }
            is PieChart -> {
                val entries = when (sensorId) {
                    1 -> listOf(PieEntry(60f, "Normal"), PieEntry(30f, "Alta"), PieEntry(10f, "Baja"))
                    2 -> listOf(PieEntry(75f, "Normal"), PieEntry(15f, "Baja"), PieEntry(10f, "Alta"))
                    3 -> listOf(PieEntry(90f, "Segura"), PieEntry(8f, "Media"), PieEntry(2f, "Alerta"))
                    else -> listOf(PieEntry(40f, "A"), PieEntry(30f, "M"), PieEntry(30f, "B"))
                }
                val pieColors = when (sensorId) {
                    1 -> listOf(resources.getColor(R.color.temp_color_light, null), resources.getColor(R.color.temp_color, null), resources.getColor(R.color.temp_color_dark, null))
                    2 -> listOf(resources.getColor(R.color.pressure_color_light, null), resources.getColor(R.color.pressure_color, null), resources.getColor(R.color.pressure_color_dark, null))
                    3 -> listOf(resources.getColor(R.color.gas_color_light, null), resources.getColor(R.color.gas_color, null), resources.getColor(R.color.gas_color_dark, null))
                    else -> listOf(resources.getColor(R.color.teal_200, null), resources.getColor(R.color.teal_400, null), resources.getColor(R.color.teal_700, null))
                }
                val dataSet = PieDataSet(entries, label).apply { colors = pieColors }
                chart.data = PieData(dataSet).apply { setDrawValues(true) }
                chart.setDrawEntryLabels(false)
                chart.legend.isEnabled = true
            }
        }

        chart.description.isEnabled = false
        chart.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        chart.invalidate()

        container.removeAllViews()
        container.addView(chart)
        container.addView(button)
    }
}

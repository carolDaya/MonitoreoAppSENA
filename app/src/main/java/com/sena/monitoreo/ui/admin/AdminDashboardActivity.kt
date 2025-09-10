package com.sena.monitoreo.ui.admin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.data.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.sena.monitoreo.R
import com.sena.monitoreo.databinding.ActivityAdminDashboardBinding
import com.sena.monitoreo.databinding.HeaderLayoutAdminBinding
import com.sena.monitoreo.ui.auth.LoginActivity // Reemplaza con tu actividad de login

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var headerBinding: HeaderLayoutAdminBinding

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuración de la navegación del menú
        setupNavigationDrawer()

        // Configuración inicial de los gráficos
        setupCharts()

        // Configuración de los listeners de clic para cambiar los gráficos
        setupChartClickListeners()

        // Mostrar la sección de "home" por defecto
        showSection(home = true)
    }

    // --- Métodos de Navegación del Menú ---

    private fun setupNavigationDrawer() {
        // Inicializa el binding del header
        headerBinding = HeaderLayoutAdminBinding.bind(binding.mainHeader.root)

        // Configura el botón del menú para abrir el DrawerLayout
        headerBinding.settingsIcon.setOnClickListener {
            binding.adminDashboard.openDrawer(GravityCompat.START)
        }

        // Configura el listener para los ítems del menú de navegación
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            binding.adminDashboard.closeDrawers()

            when (menuItem.itemId) {
                R.id.nav_home -> {
                    showSection(home = true)
                }
                R.id.nav_graphis -> {
                    showSection(graphs = true)
                }
                R.id.nav_volumen -> {
                    showSection(ai = true)
                }
                R.id.nav_users -> {
                    showSection(users = true)
                }
                R.id.nav_settings -> {
                    // Lógica para ir a la pantalla de configuración
                    // val intent = Intent(this, SettingsActivity::class.java)
                    // startActivity(intent)
                }
                R.id.nav_logout -> {
                    // Lógica para cerrar sesión
                    performLogout()
                }
            }
            true
        }
    }

    private fun showSection(home: Boolean = false, graphs: Boolean = false, ai: Boolean = false, users: Boolean = false) {
        with(binding) {
            // Oculta todas las secciones
            graficasAdminSection.root.visibility = View.GONE
            iaAdminSection.root.visibility = View.GONE
            userAdminSection.root.visibility = View.GONE

            // Muestra la sección o secciones correctas
            when {
                home -> {
                    graficasAdminSection.root.visibility = View.VISIBLE
                    iaAdminSection.root.visibility = View.VISIBLE
                    userAdminSection.root.visibility = View.VISIBLE
                }
                graphs -> {
                    graficasAdminSection.root.visibility = View.VISIBLE
                }
                ai -> {
                    iaAdminSection.root.visibility = View.VISIBLE
                }
                users -> {
                    userAdminSection.root.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun performLogout() {
        // Lógica para limpiar datos de sesión y redirigir
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // --- Métodos de Configuración y Lógica de Gráficos ---

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupCharts() {
        updateChart(binding.graficasAdminSection.graphContainerGas, "line", "Gas", binding.graficasAdminSection.btnChangeGas)
        updateChart(binding.graficasAdminSection.graphContainerTemp, "line", "Temperatura", binding.graficasAdminSection.btnChangeTemp)
        updateChart(binding.graficasAdminSection.graphContainerPressure, "line", "Presión", binding.graficasAdminSection.btnChangePressure)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupChartClickListeners() {
        binding.graficasAdminSection.btnChangeGas.setOnClickListener {
            showGraphTypeDialog("Gas", binding.graficasAdminSection.graphContainerGas, binding.graficasAdminSection.btnChangeGas)
        }
        binding.graficasAdminSection.btnChangeTemp.setOnClickListener {
            showGraphTypeDialog("Temperatura", binding.graficasAdminSection.graphContainerTemp, binding.graficasAdminSection.btnChangeTemp)
        }
        binding.graficasAdminSection.btnChangePressure.setOnClickListener {
            showGraphTypeDialog("Presión", binding.graficasAdminSection.graphContainerPressure, binding.graficasAdminSection.btnChangePressure)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showGraphTypeDialog(label: String, container: FrameLayout, button: ImageView) {
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
                updateChart(container, type, label, button)
                Toast.makeText(this, "Gráfica de $label cambiada a ${graphTypes[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateChart(container: FrameLayout, type: String, label: String, button: ImageView) {
        container.removeAllViews()

        val chart: Chart<*> = when (type) {
            "bar" -> BarChart(this)
            "line" -> LineChart(this)
            "pie" -> PieChart(this)
            else -> LineChart(this)
        }

        val chartColor = when (label) {
            "Gas" -> resources.getColor(R.color.gas_color, null)
            "Temperatura" -> resources.getColor(R.color.temp_color, null)
            "Presión" -> resources.getColor(R.color.pressure_color, null)
            else -> resources.getColor(R.color.teal_700, null)
        }

        when (chart) {
            is BarChart -> {
                val entries = listOf(BarEntry(1f, 10f), BarEntry(2f, 15f), BarEntry(3f, 12f))
                val dataSet = BarDataSet(entries, label).apply {
                    color = chartColor
                    valueTextColor = resources.getColor(R.color.black, null)
                }
                chart.data = BarData(dataSet)
            }
            is LineChart -> {
                val entries = listOf(Entry(1f, 20f), Entry(2f, 25f), Entry(3f, 22f))
                val dataSet = LineDataSet(entries, label).apply {
                    color = chartColor
                    valueTextColor = resources.getColor(R.color.black, null)
                    lineWidth = 2f
                    setDrawCircles(true)
                    setDrawValues(true)
                }
                chart.data = LineData(dataSet)
            }
            is PieChart -> {
                val entries = listOf(PieEntry(40f, "Alta"), PieEntry(30f, "Media"), PieEntry(30f, "Baja"))
                val dataSet = PieDataSet(entries, label).apply {
                    colors = when(label) {
                        "Gas" -> listOf(
                            resources.getColor(R.color.gas_high, null),
                            resources.getColor(R.color.gas_medium, null),
                            resources.getColor(R.color.gas_low, null)
                        )
                        "Temperatura" -> listOf(
                            resources.getColor(R.color.temp_high, null),
                            resources.getColor(R.color.temp_medium, null),
                            resources.getColor(R.color.temp_low, null)
                        )
                        "Presión" -> listOf(
                            resources.getColor(R.color.pressure_high, null),
                            resources.getColor(R.color.pressure_medium, null),
                            resources.getColor(R.color.pressure_low, null)
                        )
                        else -> listOf(
                            resources.getColor(R.color.teal_200, null),
                            resources.getColor(R.color.teal_400, null),
                            resources.getColor(R.color.teal_700, null)
                        )
                    }
                    valueTextColor = resources.getColor(R.color.black, null)
                }
                chart.data = PieData(dataSet)
            }
        }

        chart.description.isEnabled = false
        chart.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        chart.invalidate()

        container.addView(chart)
        container.addView(button)
    }
}
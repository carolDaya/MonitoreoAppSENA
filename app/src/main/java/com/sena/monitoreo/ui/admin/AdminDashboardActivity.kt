package com.sena.monitoreo.ui.admin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.data.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.user.UserResponse
import com.sena.monitoreo.data.repository.GraficasRepository
import com.sena.monitoreo.data.repository.UserRepository
import com.sena.monitoreo.databinding.ActivityAdminDashboardBinding
import com.sena.monitoreo.databinding.HeaderLayoutAdminBinding
import com.sena.monitoreo.ui.admin.adapter.UserAdapter
import com.sena.monitoreo.ui.auth.LoginActivity
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var headerBinding: HeaderLayoutAdminBinding
    private lateinit var userAdapter: UserAdapter
    private val graficasRepo = GraficasRepository()
    private val TAG = "AdminDashboard"

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigationDrawer()
        setupRecyclerView()
        setupTabs()
        cargarGraficasGuardadas()
        setupChartClickListeners()
        showSection(home = true)
    }

    // ----------------------------------------------------------
    // Configuración del menú lateral
    // ----------------------------------------------------------
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
                R.id.nav_users -> {
                    showSection(users = true)
                    loadUsers("all") // Cargar usuarios al abrir sección
                }
                R.id.nav_logout -> performLogout()
            }
            true
        }
    }

    // ----------------------------------------------------------
    // Mostrar secciones dinámicamente
    // ----------------------------------------------------------
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

    // ----------------------------------------------------------
    // 🔹 Configuración del RecyclerView de Usuarios
    // ----------------------------------------------------------
    private fun setupRecyclerView() {
        val recycler = binding.userAdminSection.recyclerViewUsuarios
        recycler.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(emptyList()) { user ->
            showUserDialog(user)
        }
        recycler.adapter = userAdapter
    }

    // ----------------------------------------------------------
    // 🔹 Configuración de Tabs para filtrar usuarios
    // ----------------------------------------------------------
    private fun setupTabs() {
        val tabLayout = binding.userAdminSection.tabLayoutUsuarios

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadUsers("active")
                    1 -> loadUsers("blocked")
                    2 -> loadUsers("all")
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // ----------------------------------------------------------
    // 🔹 Cargar usuarios desde el backend
    // ----------------------------------------------------------
    private fun loadUsers(type: String) {
        lifecycleScope.launch {
            try {
                val response = when (type) {
                    "active" -> RetrofitClient.apiUser.getActiveUsers()
                    "blocked" -> RetrofitClient.apiUser.getBlockedUsers()
                    else -> RetrofitClient.apiUser.getAllUsers()
                }

                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    userAdapter.updateList(users)
                    Log.d(TAG, "$type -> ${users.size} usuarios cargados")
                } else {
                    showError("Error al obtener usuarios (${response.code()})")
                }

            } catch (e: Exception) {
                showError("Error de conexión: ${e.message}")
            }
        }
    }

    // ----------------------------------------------------------
    // 🔹 Diálogo de detalles del usuario con bloqueo/desbloqueo
    // ----------------------------------------------------------
    private fun showUserDialog(user: UserResponse) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_admin_card_user, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val txtName = dialogView.findViewById<TextView>(R.id.textViewUserName)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.buttonCloseDialog)
        val btnBlock = dialogView.findViewById<MaterialButton>(R.id.buttonBlockUser)

        txtName.text = user.nombre

        // Actualizar texto del botón según el estado del usuario
        btnBlock.text = if (user.estado == "activo") "Bloquear Usuario" else "Desbloquear Usuario"

        btnClose.setOnClickListener { dialog.dismiss() }

        btnBlock.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val nuevoEstado = if (user.estado == "activo") "bloqueado" else "activo"
                    val repo = UserRepository()
                    val success = repo.updateEstado(user.id, nuevoEstado)

                    if (success) {
                        Toast.makeText(
                            this@AdminDashboardActivity,
                            "Usuario ${user.nombre} ahora está $nuevoEstado",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadUsers("all") // Recargar lista
                    } else {
                        Toast.makeText(
                            this@AdminDashboardActivity,
                            "Error al actualizar estado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }


    // ----------------------------------------------------------
    // Cargar configuraciones de gráficas desde backend
    // ----------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.M)
    private fun cargarGraficasGuardadas() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔄 Solicitando configuración de gráficas al servidor...")
                val configuraciones = graficasRepo.getGraficas()

                if (configuraciones.isEmpty()) {
                    Log.w(TAG, "⚠️ No hay configuraciones guardadas, usando valores por defecto")
                    setupChartsDefault()
                    return@launch
                }

                configuraciones.forEach { config ->
                    val tipo = config.tipo_grafica
                    val sensorId = config.sensor_id

                    when (sensorId) {
                        2 -> updateChart(
                            binding.graficasAdminSection.graphContainerTemp,
                            tipo,
                            "Temperatura",
                            binding.graficasAdminSection.btnChangeTemp,
                            sensorId
                        )
                        3 -> updateChart(
                            binding.graficasAdminSection.graphContainerPressure,
                            tipo,
                            "Presión",
                            binding.graficasAdminSection.btnChangePressure,
                            sensorId
                        )
                        1 -> updateChart(
                            binding.graficasAdminSection.graphContainerGas,
                            tipo,
                            "Metano",
                            binding.graficasAdminSection.btnChangeGas,
                            sensorId
                        )
                    }
                }

                Toast.makeText(this@AdminDashboardActivity, "Configuraciones cargadas", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al cargar configuraciones", e)
                Toast.makeText(this@AdminDashboardActivity, "Error al cargar configuraciones", Toast.LENGTH_SHORT).show()
                setupChartsDefault()
            }
        }
    }

    // ----------------------------------------------------------
    // Gráficas por defecto
    // ----------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupChartsDefault() {
        Log.d(TAG, "📊 Configurando gráficas por defecto (línea)")
        updateChart(binding.graficasAdminSection.graphContainerTemp, "line", "Temperatura", binding.graficasAdminSection.btnChangeTemp, 2)
        updateChart(binding.graficasAdminSection.graphContainerPressure, "line", "Presión", binding.graficasAdminSection.btnChangePressure, 3)
        updateChart(binding.graficasAdminSection.graphContainerGas, "line", "Metano", binding.graficasAdminSection.btnChangeGas, 1)
    }


    // ----------------------------------------------------------
    // Botones para cambiar tipo de gráfica
    // ----------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupChartClickListeners() {
        binding.graficasAdminSection.btnChangeTemp.setOnClickListener {
            showGraphTypeDialog("Temperatura", binding.graficasAdminSection.graphContainerTemp, binding.graficasAdminSection.btnChangeTemp, 2)
        }
        binding.graficasAdminSection.btnChangePressure.setOnClickListener {
            showGraphTypeDialog("Presión", binding.graficasAdminSection.graphContainerPressure, binding.graficasAdminSection.btnChangePressure, 3)
        }
        binding.graficasAdminSection.btnChangeGas.setOnClickListener {
            showGraphTypeDialog("Metano", binding.graficasAdminSection.graphContainerGas, binding.graficasAdminSection.btnChangeGas, 1)
        }
    }

    // ----------------------------------------------------------
    // Diálogo para cambiar tipo de gráfica y guardar backend
    // ----------------------------------------------------------
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

                updateChart(container, type, label, button, sensorId)

                lifecycleScope.launch {
                    try {
                        Log.d(TAG, "💾 Guardando configuración: sensor=$sensorId, tipo=$type")
                        val result = graficasRepo.updateGrafica(sensorId, type)
                        if (result != null) {
                            Log.d(TAG, "✅ Configuración guardada exitosamente")
                            Toast.makeText(this@AdminDashboardActivity, "Gráfica de $label cambiada a ${graphTypes[which]}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@AdminDashboardActivity, "Error guardando en servidor", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Excepción al guardar", e)
                        Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    // ----------------------------------------------------------
    // Dibujar gráficas dinámicamente según tipo
    // ----------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateChart(container: FrameLayout, type: String, label: String, button: ImageView, sensorId: Int) {
        container.removeAllViews()

        val chart: Chart<*> = when (type) {
            "bar" -> BarChart(this)
            "line" -> LineChart(this)
            "pie" -> PieChart(this)
            else -> LineChart(this)
        }

        val chartColor = when (sensorId) {
            1 -> resources.getColor(R.color.temp_color, null)
            2 -> resources.getColor(R.color.pressure_color, null)
            3 -> resources.getColor(R.color.gas_color, null)
            else -> resources.getColor(R.color.teal_700, null)
        }

        when (chart) {
            is BarChart -> {
                val entries = listOf(
                    BarEntry(1f, 25f),
                    BarEntry(2f, 28f),
                    BarEntry(3f, 32f)
                )
                val dataSet = BarDataSet(entries, label).apply { color = chartColor }
                chart.data = BarData(dataSet)
                chart.xAxis.isEnabled = false
            }
            is LineChart -> {
                val entries = listOf(
                    Entry(1f, 24.5f),
                    Entry(2f, 26.0f),
                    Entry(3f, 25.8f)
                )
                val dataSet = LineDataSet(entries, label).apply {
                    color = chartColor
                    lineWidth = 2f
                    setDrawCircles(true)
                    setDrawValues(true)
                }
                chart.data = LineData(dataSet)
                chart.xAxis.isEnabled = false
            }
            is PieChart -> {
                val entries = listOf(
                    PieEntry(60f, "Normal"),
                    PieEntry(30f, "Alta"),
                    PieEntry(10f, "Baja")
                )
                val dataSet = PieDataSet(entries, label).apply {
                    colors = listOf(
                        chartColor,
                        resources.getColor(R.color.teal_200, null),
                        resources.getColor(R.color.teal_700, null)
                    )
                }
                chart.data = PieData(dataSet)
                chart.setDrawEntryLabels(false)
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

    // ----------------------------------------------------------
    // Manejo de errores
    // ----------------------------------------------------------
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e(TAG, message)
    }
}
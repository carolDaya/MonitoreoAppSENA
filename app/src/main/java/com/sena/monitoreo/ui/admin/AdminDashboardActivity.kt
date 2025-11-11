package com.sena.monitoreo.ui.admin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.user.UserResponse
import com.sena.monitoreo.data.repository.GraficasRepository
import com.sena.monitoreo.data.repository.UserRepository
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.databinding.ActivityAdminDashboardBinding
import com.sena.monitoreo.databinding.HeaderLayoutAdminBinding
import com.sena.monitoreo.ui.admin.adapter.UserAdapter
import com.sena.monitoreo.ui.auth.LoginActivity
import com.sena.monitoreo.ui.admin.viewmodel.*
import com.sena.monitoreo.utils.charts.AdminChartManager
import com.sena.monitoreo.utils.navigation.NavigationManager
import com.sena.monitoreo.utils.voice.VoiceConfigHelper
import com.sena.monitoreo.utils.voice.VoiceManager
import com.sena.monitoreo.utils.voice.WaveformManager
import com.sena.monitoreo.utils.UiUtils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.button.MaterialButton
import com.masoudss.lib.WaveformSeekBar
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var headerBinding: HeaderLayoutAdminBinding
    private lateinit var userAdapter: UserAdapter

    private val graficasRepo = GraficasRepository()
    private val userRepo = UserRepository()
    private val TAG = "AdminDashboard"

    // Managers / Helpers
    private lateinit var navigationManager: NavigationManager
    private lateinit var voiceManager: VoiceManager
    private lateinit var waveformManager: WaveformManager
    private lateinit var chartManager: AdminChartManager
    private lateinit var voiceConfigHelper: VoiceConfigHelper

    // ViewModels
    private val adminConfigViewModel: AdminConfigViewModel by lazy {
        val repository = VoiceRepository(RetrofitClient.apiVoice)
        ViewModelProvider(this, AdminConfigViewModelFactory(repository))
            .get(AdminConfigViewModel::class.java)
    }

    private val userViewModel: UserViewModel by lazy {
        ViewModelProvider(this, UserViewModelFactory(userRepo))
            .get(UserViewModel::class.java)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeManagers() // Managers, Voz y Waveform consolidados
        setupNavigationDrawer()
        setupVoiceConfigurationSection()
        setupUserSection()
        setupChartSection()

        loadVoiceConfiguration()
        observeLoadingStates()
        handleScrollToSection()
        showSection(home = true)
    }

    // ----------------------------------------------------------
    // 🔄 Observar estados de carga (Loading)
    // ----------------------------------------------------------
    private fun observeLoadingStates() {
        // Observador para AdminConfigViewModel (Guardar configuración de IA)
        adminConfigViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                UiUtils.showLoading(this, "Guardando configuración...")
            } else {
                UiUtils.hideLoading()
            }
        }

        // Observador para UserViewModel (Cargar/Actualizar usuarios)
        lifecycleScope.launch {
            userViewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    UiUtils.showLoading(this@AdminDashboardActivity, "Cargando usuarios...")
                } else {
                    UiUtils.hideLoading()
                }
            }
        }
    }


    // ----------------------------------------------------------
    // 💡 Inicialización de Managers, Voz y Waveform (CONSOLIDADO)
    // ----------------------------------------------------------
    private fun initializeManagers() {
        val headerView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main_header)
        val waveFormSection = headerView.findViewById<View>(R.id.waveform_section)
        val waveformSeekBar = waveFormSection.findViewById<WaveformSeekBar>(R.id.waveformSeekBar)
        val btnPlayMessage = waveFormSection.findViewById<MaterialButton>(R.id.btn_play_message)

        // 1. Inicialización de Managers/Helpers
        waveformManager = WaveformManager(waveformSeekBar)
        voiceManager = VoiceManager(this) { btnPlayMessage.isEnabled = true }

        chartManager = AdminChartManager(this, graficasRepo, lifecycleScope)

        // 💡 CLAVE: Pasar voiceManager al VoiceConfigHelper
        voiceConfigHelper = VoiceConfigHelper(this, adminConfigViewModel, binding.iaAdminSection.btnSaveVoiceConfig, voiceManager)

        // 2. 🔊 Lógica de Voz y Waveform
        waveformManager.setupInitialSamples()
        voiceManager.initialize()

        btnPlayMessage.setOnClickListener {
            val message = "Bienvenido al panel del administrador. Aquí puedes gestionar usuarios, revisar las gráficas y acceder a las funciones de inteligencia artificial."

            // 💡 CLAVE: Aplicar la configuración TTS antes de hablar (redundante pero seguro)
            voiceManager.applyTtsSettings()

            if (!voiceManager.isSpeaking) {
                voiceManager.speak(message)
                waveformManager.startTimedAnimation(totalDurationMs = 8000L, lifecycleScope) {
                    btnPlayMessage.setIconResource(R.drawable.ic_play)
                }
                btnPlayMessage.setIconResource(R.drawable.ic_stop)
            } else {
                voiceManager.stop()
                waveformManager.stopAnimation()
                btnPlayMessage.setIconResource(R.drawable.ic_play)
            }
        }
    }

    /**
     * Carga la configuración inicial y configura el observador para la aplicación inmediata.
     */
    private fun loadVoiceConfiguration() {
        adminConfigViewModel.loadCurrentConfig()

        // ❌ El observador de currentConfig se ha movido dentro del VoiceConfigHelper
        // para garantizar que la configuración se aplique correctamente al VoiceManager.

        // MANTENER: El Snackbar de éxito
        adminConfigViewModel.saveSuccess.observe(this) { success ->
            if (success) {
                // voiceManager.applyTtsSettings() ya se llama desde el Helper al actualizar la config.
                UiUtils.showSnackbar(binding.root, "Configuración de voz actualizada correctamente.", isError = false)
            }
        }
    }

    // ----------------------------------------------------------
    // ⚙️ Configuración de Voz (Usando VoiceConfigHelper y UiUtils)
    // ----------------------------------------------------------
    private fun setupVoiceConfigurationSection() {
        voiceConfigHelper.setup(
            binding.iaAdminSection.spinnerIaVoz,
            binding.iaAdminSection.spinnerTonoVoz
        )
        adminConfigViewModel.saveStatus.observe(this) { message ->
            // Muestra Snackbar si la operación falló (éxito ya se maneja en saveSuccess)
            if (message.contains("Error")) {
                UiUtils.showSnackbar(binding.root, message, isError = true)
            }
        }
    }

    // ----------------------------------------------------------
    // 👥 Lógica de Usuarios (Refactorizada con UserViewModel)
    // ----------------------------------------------------------
    private fun setupUserSection() {
        setupRecyclerView()
        setupTabs()

        // Observar los datos del ViewModel
        lifecycleScope.launch {
            userViewModel.users.collect { users ->
                userAdapter.updateList(users)
                Log.d(TAG, "Usuarios cargados (ViewModel): ${users.size}")
            }
        }
    }

    private fun setupRecyclerView() {
        val recycler = binding.userAdminSection.recyclerViewUsuarios
        recycler.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(emptyList()) { user -> showUserDialog(user) }
        recycler.adapter = userAdapter
    }

    private fun setupTabs() {
        val tabLayout = binding.userAdminSection.tabLayoutUsuarios
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> userViewModel.loadAllUsers(estado = "activo")
                    1 -> userViewModel.loadAllUsers(estado = "bloqueado")
                    2 -> userViewModel.loadAllUsers(estado = "all")
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        userViewModel.loadAllUsers(estado = "activo")
    }

    private fun showUserDialog(user: UserResponse) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_card_user, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        val txtName = dialogView.findViewById<TextView>(R.id.textViewUserName)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.buttonCloseDialog)
        val btnBlock = dialogView.findViewById<MaterialButton>(R.id.buttonBlockUser)

        txtName.text = user.nombre
        btnBlock.text = if (user.estado == "activo") "Bloquear Usuario" else "Desbloquear Usuario"

        btnClose.setOnClickListener { dialog.dismiss() }
        btnBlock.setOnClickListener {
            val nuevoEstado = if (user.estado == "activo") "bloqueado" else "activo"

            userViewModel.updateUserEstado(user.id, nuevoEstado)

            val message = "Usuario ${user.nombre} ahora está $nuevoEstado"
            UiUtils.showSnackbar(binding.root, message, isError = nuevoEstado == "bloqueado")

            dialog.dismiss()
        }
        dialog.show()
    }


    // ----------------------------------------------------------
    // 📊 Lógica de Gráficas (Usando AdminChartManager)
    // ----------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupChartSection() {
        chartManager.loadInitialCharts(
            binding.graficasAdminSection.graphContainerTemp, binding.graficasAdminSection.btnChangeTemp,
            binding.graficasAdminSection.graphContainerPressure, binding.graficasAdminSection.btnChangePressure,
            binding.graficasAdminSection.graphContainerGas, binding.graficasAdminSection.btnChangeGas
        )

        binding.graficasAdminSection.btnChangeTemp.setOnClickListener {
            chartManager.showGraphTypeDialog("Temperatura", binding.graficasAdminSection.graphContainerTemp, binding.graficasAdminSection.btnChangeTemp, 2)
        }
        binding.graficasAdminSection.btnChangePressure.setOnClickListener {
            chartManager.showGraphTypeDialog("Presión", binding.graficasAdminSection.graphContainerPressure, binding.graficasAdminSection.btnChangePressure, 3)
        }
        binding.graficasAdminSection.btnChangeGas.setOnClickListener {
            chartManager.showGraphTypeDialog("Metano", binding.graficasAdminSection.graphContainerGas, binding.graficasAdminSection.btnChangeGas, 1)
        }
    }


    // ----------------------------------------------------------
    // 🧭 Lógica de Navegación (Usando NavigationManager)
    // ----------------------------------------------------------
    private fun setupNavigationDrawer() {
        headerBinding = HeaderLayoutAdminBinding.bind(binding.mainHeader.root)

        navigationManager = NavigationManager(
            context = this,
            drawerLayout = binding.adminDashboard,
            navigationView = binding.navView,
            currentActivity = "admin_dashboard",
            view = binding.root
        )

        headerBinding.settingsIcon.setOnClickListener {
            binding.adminDashboard.openDrawer(GravityCompat.START)
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            binding.adminDashboard.closeDrawers()

            navigationManager.handleAdminNavigation(menuItem.itemId, "admin_dashboard")

            if (menuItem.itemId == R.id.nav_logout) performLogout()
            true
        }
    }

    private fun handleScrollToSection() {
        val scrollTo = intent.getStringExtra("SCROLL_TO")
        when (scrollTo) {
            "graphs" -> showSection(graphs = true)
            "ai" -> showSection(ai = true)
            "users" -> {
                showSection(users = true)
                userViewModel.loadAllUsers()
            }
        }
    }

    private fun showSection(home: Boolean = false, graphs: Boolean = false, ai: Boolean = false, users: Boolean = false) {
        with(binding) {
            graficasAdminSection.root.visibility = if (graphs || home) View.VISIBLE else View.GONE
            iaAdminSection.root.visibility = if (ai || home) View.VISIBLE else View.GONE
            userAdminSection.root.visibility = if (users || home) View.VISIBLE else View.GONE
        }
    }

    private fun performLogout() {
        voiceManager.stop()
        waveformManager.stopAnimation()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showError(message: String) {
        UiUtils.showSnackbar(binding.root, message, isError = true)
        Log.e(TAG, message)
    }

    override fun onBackPressed() {
        if (binding.adminDashboard.isDrawerOpen(GravityCompat.START)) {
            binding.adminDashboard.closeDrawer(GravityCompat.START)
        } else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.stop()
        waveformManager.stopAnimation()
        UiUtils.hideLoading()
    }
}
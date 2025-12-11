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
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.user.UserResponse
import com.sena.monitoreo.data.repository.GraficasRepository
import com.sena.monitoreo.data.repository.ProcesoRepository
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
import com.sena.monitoreo.utils.UiUtils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.button.MaterialButton
import com.masoudss.lib.WaveformSeekBar
import com.sena.monitoreo.ui.admin.factory.AdminConfigViewModelFactory
import com.sena.monitoreo.ui.admin.factory.UserViewModelFactory
import com.sena.monitoreo.ui.admin.factory.ProcesoViewModelFactory
import com.sena.monitoreo.ui.base.BaseVoiceActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException

class AdminDashboardActivity : BaseVoiceActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var headerBinding: HeaderLayoutAdminBinding
    private lateinit var userAdapter: UserAdapter

    private val graficasRepo = GraficasRepository()
    private val userRepo = UserRepository()
    private val procesoRepo = ProcesoRepository(RetrofitClient.apiProceso)
    private val TAG = "AdminDashboard"

    // Managers / Helpers
    private lateinit var navigationManager: NavigationManager
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

    private val procesoViewModel: ProcesoViewModel by lazy {
        ViewModelProvider(this, ProcesoViewModelFactory(procesoRepo))
            .get(ProcesoViewModel::class.java)
    }

    // ----------------------------------------------------------
    // Variable y Constantes para el mensaje de voz por sección
    // ----------------------------------------------------------
    private var currentSectionMessage: String = ""

    companion object {
        private const val MSG_HOME = "Bienvenido al Panel de Control. Aquí puedes gestionar usuarios, gráficas y la configuración de voz."
        private const val MSG_GRAPHS = "Estás en la sección de Gráficas. Cambia el tipo de gráfica de los datos de temperatura, presión y metano."
        private const val MSG_USERS = "Estás en el Gestor de Usuarios. Aquí puedes ver y cambiar el estado de los usuarios activos y bloqueados."
        private const val MSG_AI = "Estás en la Configuración de Voz. Ajusta el tono y el género de la voz para la asistencia del sistema."
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeManagers()
        setupNavigationDrawer()
        setupVoiceConfigurationSection()
        setupUserSection()
        setupChartSection()

        loadVoiceConfiguration()
        observeLoadingStates()
        observeNetworkErrors()

        handleScrollToSection()
    }

    // ----------------------------------------------------------
    // Implementación de NetworkRetryListener (Heredado de BaseActivity)
    // ----------------------------------------------------------
    override fun onNetworkRetry() {

        chartManager.loadInitialCharts(
            binding.graficasAdminSection.graphContainerTemp, binding.graficasAdminSection.btnChangeTemp,
            binding.graficasAdminSection.graphContainerPressure, binding.graficasAdminSection.btnChangePressure,
            binding.graficasAdminSection.graphContainerGas, binding.graficasAdminSection.btnChangeGas
        )

        adminConfigViewModel.loadCurrentConfig()
        userViewModel.loadAllUsers(getCurrentUserFilter())
    }

    private fun getCurrentUserFilter(): String {
        return when (binding.userAdminSection.tabLayoutUsuarios.selectedTabPosition) {
            0 -> "activo"
            1 -> "bloqueado"
            else -> "all"
        }
    }

    private fun observeNetworkErrors() {
        lifecycleScope.launch {
            // Observar errores de UserViewModel
            userViewModel.networkError.collectLatest { errorMessage ->
                if (errorMessage != null) {
                    showNetworkError(errorMessage)
                    userViewModel.clearNetworkError()
                }
            }
        }

        // Observar errores de AdminConfigViewModel
        adminConfigViewModel.saveStatus.observe(this) { message ->
            if (message.contains("Error de red", ignoreCase = true) || message.contains("IOException", ignoreCase = true)) {
                showNetworkError(message)
            } else if (!message.contains("éxito", ignoreCase = true) && message.isNotEmpty()) {
                UiUtils.showSnackbar(binding.root, message, isError = true)
            } else if (message.contains("éxito", ignoreCase = true)) {
                UiUtils.showSnackbar(binding.root, message, isError = false)
            }
        }

        // Observar errores de carga inicial de AdminConfigViewModel
        adminConfigViewModel.loadError.observe(this) { message ->
            if (message.contains("Error de red", ignoreCase = true) || message.contains("IOException", ignoreCase = true)) {
                showNetworkError(message)
            } else if (message.isNotEmpty()) {
                UiUtils.showSnackbar(binding.root, message, isError = true)
            }
        }

        // Observar errores de ProcesoViewModel
        procesoViewModel.procesoStatus.observe(this) { message ->
            if (message.contains("Error de red", ignoreCase = true) || message.contains("IOException", ignoreCase = true)) {
                showNetworkError(message)
            } else if (message.isNotEmpty() && !message.contains("Proceso activo", ignoreCase = true) &&
                !message.contains("Proceso inactivo", ignoreCase = true)) {
                UiUtils.showSnackbar(binding.root, message, isError = true)
            }
        }
    }
    // Observar estados de carga (Loading)
    private fun observeLoadingStates() {
        adminConfigViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                UiUtils.showLoading(this, "Guardando configuración...")
            } else {
                UiUtils.hideLoading()
            }
        }

        lifecycleScope.launch {
            userViewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    UiUtils.showLoading(this@AdminDashboardActivity, "Cargando usuarios...")
                } else {
                    UiUtils.hideLoading()
                }
            }
        }

        procesoViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                UiUtils.showLoading(this, "Procesando...")
            } else {
                UiUtils.hideLoading()
            }
        }
    }
    // Inicialización de Managers, Voz y Waveform
    private fun initializeManagers() {
        headerBinding = HeaderLayoutAdminBinding.bind(binding.mainHeader.root)
        val waveFormSection = headerBinding.waveformSection.root
        val waveformSeekBar = waveFormSection.findViewById<WaveformSeekBar>(R.id.waveformSeekBar)
        val btnPlayMessage = waveFormSection.findViewById<MaterialButton>(R.id.btn_play_message)

        // Configurar componentes de voz heredados
        setupWaveformComponents(waveformSeekBar, btnPlayMessage)

        chartManager = AdminChartManager(this, graficasRepo, lifecycleScope)

        voiceConfigHelper = VoiceConfigHelper(
            context = this,
            viewModel = adminConfigViewModel,
            saveButton = binding.iaAdminSection.btnSaveVoiceConfig,
            voiceManager = voiceManager,
            lifecycleOwner = this
        )

        btnPlayMessage.setOnClickListener {
            if (!voiceManager.isSpeaking) {
                startSpeaking() // Reproduce currentSectionMessage
            } else {
                stopSpeaking() // Detiene la voz y la animación
            }
        }
    }
    // Reproducción de Mensaje de Voz Específico (Sobreescrito)
    override fun startSpeaking() {
        // La configuración de TTS ya se aplica en onVoiceInitialized y al cambiar en el helper
        super.startSpeaking()

        if (currentSectionMessage.isNotEmpty()) {
            Log.d(TAG, "🔊 Reproduciendo mensaje de sección: $currentSectionMessage")
            // Usamos el método de texto corto (speakWithWaveform) que maneja la sincronización.
            speakWithWaveform(currentSectionMessage)
        }
    }

    /**
     * Carga la configuración inicial y configura el observador para la aplicación inmediata.
     */
    private fun loadVoiceConfiguration() {
        adminConfigViewModel.loadCurrentConfig()

        adminConfigViewModel.saveSuccess.observe(this) {}
    }

    // Configuración de Voz (Usando VoiceConfigHelper y UiUtils)
    private fun setupVoiceConfigurationSection() {
        voiceConfigHelper.setup(
            binding.iaAdminSection.spinnerIaVoz,
            binding.iaAdminSection.spinnerTonoVoz
        )
    }
    // Lógica de Usuarios (Corregida la Carga Inicial)
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
            override fun onTabReselected(tab: TabLayout.Tab?) {
                onTabSelected(tab)
            }
        })
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
    // Lógica de Gráficas (Usando AdminChartManager)
    // ----------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupChartSection() {
        chartManager.loadInitialCharts(
            binding.graficasAdminSection.graphContainerTemp, binding.graficasAdminSection.btnChangeTemp,
            binding.graficasAdminSection.graphContainerPressure, binding.graficasAdminSection.btnChangePressure,
            binding.graficasAdminSection.graphContainerGas, binding.graficasAdminSection.btnChangeGas
        )

        binding.graficasAdminSection.btnChangeTemp.setOnClickListener {
            // ❌ CAMBIAR de ID 2 a ID 3 para Temperatura
            chartManager.showGraphTypeDialog("Temperatura", binding.graficasAdminSection.graphContainerTemp, binding.graficasAdminSection.btnChangeTemp, 3)
        }
        binding.graficasAdminSection.btnChangePressure.setOnClickListener {
            // ❌ CAMBIAR de ID 3 a ID 2 para Presión
            chartManager.showGraphTypeDialog("Presión", binding.graficasAdminSection.graphContainerPressure, binding.graficasAdminSection.btnChangePressure, 2)
        }
        binding.graficasAdminSection.btnChangeGas.setOnClickListener {
            // ✅ Gas Metano mantiene ID 1
            chartManager.showGraphTypeDialog("Metano", binding.graficasAdminSection.graphContainerGas, binding.graficasAdminSection.btnChangeGas, 1)
        }
    }
    // ----------------------------------------------------------
    // Lógica de Navegación (Solo cambia mensaje, NO habla automáticamente)
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

            // Navegación especial para AdminDashboardActivity (cambia de sección)
            when (menuItem.itemId) {
                R.id.nav_graphis -> showSection(graphs = true)
                R.id.nav_volumen -> showSection(ai = true)
                R.id.nav_users -> showSection(users = true)
                R.id.nav_logout -> performLogout()
                else -> navigationManager.handleAdminNavigation(menuItem.itemId) // Otras navegaciones
            }
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
                userViewModel.loadAllUsers(estado = getCurrentUserFilter())
            }
            else -> showSection(home = true) // Por defecto, mostrar todas
        }
    }

    /**
     * Controla la visibilidad de las secciones y define el mensaje de voz actual.
     */
    private fun showSection(home: Boolean = false, graphs: Boolean = false, ai: Boolean = false, users: Boolean = false) {
        with(binding) {
            // 1. Ocultar todas las secciones primero
            graficasAdminSection.root.visibility = View.GONE
            iaAdminSection.root.visibility = View.GONE
            userAdminSection.root.visibility = View.GONE

            // 2. Determinar la sección a mostrar y el mensaje
            if (home) {
                graficasAdminSection.root.visibility = View.VISIBLE
                iaAdminSection.root.visibility = View.VISIBLE
                userAdminSection.root.visibility = View.VISIBLE
                currentSectionMessage = MSG_HOME
            } else if (graphs) {
                graficasAdminSection.root.visibility = View.VISIBLE
                iaAdminSection.root.visibility = View.GONE
                userAdminSection.root.visibility = View.GONE
                currentSectionMessage = MSG_GRAPHS
            } else if (ai) {
                graficasAdminSection.root.visibility = View.GONE
                iaAdminSection.root.visibility = View.VISIBLE
                userAdminSection.root.visibility = View.GONE
                currentSectionMessage = MSG_AI
            } else if (users) {
                graficasAdminSection.root.visibility = View.GONE
                iaAdminSection.root.visibility = View.GONE
                userAdminSection.root.visibility = View.VISIBLE
                currentSectionMessage = MSG_USERS
            } else {
                currentSectionMessage = MSG_HOME // Fallback
            }

            // 3. Detener voz anterior si estaba activa (siempre se detiene al cambiar de sección)
            if (voiceManager.isSpeaking) {
                stopSpeaking()
            }

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
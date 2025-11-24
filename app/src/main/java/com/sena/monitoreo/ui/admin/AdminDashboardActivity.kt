package com.sena.monitoreo.ui.admin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.sena.monitoreo.utils.voice.VoiceManager
// import com.sena.monitoreo.utils.voice.WaveformManager // ❌ ELIMINADO para evitar conflicto
import com.sena.monitoreo.utils.UiUtils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.button.MaterialButton
import com.masoudss.lib.WaveformSeekBar
import com.sena.monitoreo.ui.admin.factory.AdminConfigViewModelFactory
import com.sena.monitoreo.ui.admin.factory.UserViewModelFactory
import com.sena.monitoreo.ui.admin.factory.ProcesoViewModelFactory
import com.sena.monitoreo.ui.base.BaseVoiceActivity
import com.sena.monitoreo.utils.NetworkRetryListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException

class AdminDashboardActivity : BaseVoiceActivity(), NetworkRetryListener {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var headerBinding: HeaderLayoutAdminBinding
    private lateinit var userAdapter: UserAdapter

    private val graficasRepo = GraficasRepository()
    private val userRepo = UserRepository()
    private val procesoRepo = ProcesoRepository(RetrofitClient.apiProceso)
    private val TAG = "AdminDashboard"

    // Managers / Helpers
    private lateinit var navigationManager: NavigationManager
    // private lateinit var waveformManager: WaveformManager // ❌ Eliminada la re-declaración. Usar la heredada.
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

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNetworkErrorHandling(binding.adminDashboard as ViewGroup, this)

        initializeManagers()
        setupNavigationDrawer()
        setupVoiceConfigurationSection()
        setupUserSection()
        setupChartSection()

        loadVoiceConfiguration()
        observeLoadingStates()
        observeNetworkErrors()

        handleScrollToSection()

        if (intent.getStringExtra("SCROLL_TO") == null) {
            showSection(home = true)
        }
    }

    // ----------------------------------------------------------
    // 💡 Implementación de NetworkRetryListener
    // ----------------------------------------------------------
    override fun onNetworkRetry() {
        // ❌ Reemplazar 'chartManager.reloadAllCharts()' con una llamada de recarga válida.
        // Asumiendo que loadInitialCharts puede forzar una recarga o hay un método específico.
        // Si tienes un método específico en AdminChartManager (ej. reloadData), úsalo.
        chartManager.loadInitialCharts(
            binding.graficasAdminSection.graphContainerTemp, binding.graficasAdminSection.btnChangeTemp,
            binding.graficasAdminSection.graphContainerPressure, binding.graficasAdminSection.btnChangePressure,
            binding.graficasAdminSection.graphContainerGas, binding.graficasAdminSection.btnChangeGas
        )

        adminConfigViewModel.loadCurrentConfig()
        userViewModel.loadAllUsers(getCurrentUserFilter())

        hideNetworkError()
    }

    private fun getCurrentUserFilter(): String {
        return when (binding.userAdminSection.tabLayoutUsuarios.selectedTabPosition) {
            0 -> "activo"
            1 -> "bloqueado"
            else -> "all"
        }
    }


    // ----------------------------------------------------------
    // ⚠️ Observar errores de red de ViewModels
    // ----------------------------------------------------------
    private fun observeNetworkErrors() {
        lifecycleScope.launch {
            // Observar errores de UserViewModel
            userViewModel.networkError.collectLatest { errorMessage ->
                if (errorMessage != null) {
                    showNetworkError(errorMessage)
                    userViewModel.clearNetworkError()
                } else {
                    hideNetworkError()
                }
            }
        }

        // Observar errores de AdminConfigViewModel
        adminConfigViewModel.saveStatus.observe(this) { message ->
            if (message.contains("Error de red", ignoreCase = true) || message.contains("IOException", ignoreCase = true)) {
                showNetworkError(message)
            } else if (!isNetworkErrorVisible()) {
                if (message.contains("éxito")) {
                    UiUtils.showSnackbar(binding.root, message, isError = false)
                }
            }
        }

        // Observar errores de carga inicial de AdminConfigViewModel (si falla al cargar)
        adminConfigViewModel.loadError.observe(this) { message ->
            if (message.contains("Error de red", ignoreCase = true) || message.contains("IOException", ignoreCase = true)) {
                showNetworkError(message)
            }
        }
    }


    // ----------------------------------------------------------
    // 🔄 Observar estados de carga (Loading)
    // ----------------------------------------------------------
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
            // Manejar loading específico del proceso si es necesario
        }
    }


    // ----------------------------------------------------------
    // 💡 Inicialización de Managers, Voz y Waveform
    // ----------------------------------------------------------
    private fun initializeManagers() {
        headerBinding = HeaderLayoutAdminBinding.bind(binding.mainHeader.root)
        val waveFormSection = headerBinding.waveformSection.root
        val waveformSeekBar = waveFormSection.findViewById<WaveformSeekBar>(R.id.waveformSeekBar)
        val btnPlayMessage = waveFormSection.findViewById<MaterialButton>(R.id.btn_play_message)

        // waveformManager y voiceManager son propiedades heredadas de BaseVoiceActivity
        // Solo necesitamos inicializarlas aquí si BaseVoiceActivity no lo hace automáticamente
        // o si necesitamos pasarles las vistas específicas.

        // ASUMO que BaseVoiceActivity ya maneja voiceManager.initialize() y la configuración básica.
        // Si no es el caso, esta inicialización debe estar en BaseVoiceActivity o BaseActivity.
        // Para solucionar el error: re-usamos la propiedad heredada.

        // waveformManager = WaveformManager(waveformSeekBar) // ❌ No se re-declara
        // voiceManager = VoiceManager(this) { btnPlayMessage.isEnabled = true } // ❌ No se re-declara

        // Para evitar el conflicto de re-declaración, aseguramos que la propiedad heredada
        // (si existe) se configure con las vistas, o que se use el método setupWaveformComponents
        // heredado de BaseVoiceActivity, que es lo correcto para las clases base que manejan vistas.
        setupWaveformComponents(waveformSeekBar, btnPlayMessage)

        chartManager = AdminChartManager(this, graficasRepo, lifecycleScope)

        voiceConfigHelper = VoiceConfigHelper(
            context = this,
            viewModel = adminConfigViewModel,
            saveButton = binding.iaAdminSection.btnSaveVoiceConfig,
            voiceManager = voiceManager,
            lifecycleOwner = this
        )

        // waveformManager.setupInitialSamples() // ❌ Usar la propiedad heredada
        waveformManager.setupInitialSamples() // ✅ Correcto, si está configurado en setupWaveformComponents
        // voiceManager.initialize() // ✅ Correcto, si está configurado en setupWaveformComponents

        btnPlayMessage.setOnClickListener {
            val message = "Bienvenido al panel del administrador. Aquí puedes gestionar usuarios, revisar las gráficas y acceder a las funciones de inteligencia artificial."

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

        adminConfigViewModel.saveSuccess.observe(this) {}
    }

    // ----------------------------------------------------------
    // ⚙️ Configuración de Voz (Usando VoiceConfigHelper y UiUtils)
    // ----------------------------------------------------------
    private fun setupVoiceConfigurationSection() {
        voiceConfigHelper.setup(
            binding.iaAdminSection.spinnerIaVoz,
            binding.iaAdminSection.spinnerTonoVoz
        )
    }

    // ----------------------------------------------------------
    // 👥 Lógica de Usuarios (Corregida la Carga Inicial)
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

        userViewModel.loadAllUsers(estado = "activo")
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
                hideNetworkError()
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
            if (!isNetworkErrorVisible()) {
                UiUtils.showSnackbar(binding.root, message, isError = nuevoEstado == "bloqueado")
            }

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
    // 🧭 Lógica de Navegación (Corregida la Visibilidad)
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
            navigationManager.handleAdminNavigation(menuItem.itemId)

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

    /**
     * Controla la visibilidad de las secciones.
     * Si home=true, muestra todas las secciones (Dashboard).
     */
    private fun showSection(home: Boolean = false, graphs: Boolean = false, ai: Boolean = false, users: Boolean = false) {
        with(binding) {
            if (home) {
                graficasAdminSection.root.visibility = View.VISIBLE
                iaAdminSection.root.visibility = View.VISIBLE
                userAdminSection.root.visibility = View.VISIBLE
            } else {
                graficasAdminSection.root.visibility = if (graphs) View.VISIBLE else View.GONE
                iaAdminSection.root.visibility = if (ai) View.VISIBLE else View.GONE
                userAdminSection.root.visibility = if (users) View.VISIBLE else View.GONE
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
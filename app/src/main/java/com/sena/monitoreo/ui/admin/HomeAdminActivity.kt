package com.sena.monitoreo.ui.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.repository.ProcesoRepository
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.databinding.ActivityHomeAdminBinding
import com.sena.monitoreo.ui.admin.viewmodel.ProcesoViewModel
import com.sena.monitoreo.ui.admin.factory.ProcesoViewModelFactory
import com.sena.monitoreo.ui.base.factory.VoiceConfigViewModelFactory
import com.sena.monitoreo.ui.base.viewmodel.VoiceConfigViewModel
import com.sena.monitoreo.ui.base.BaseVoiceActivity
import com.sena.monitoreo.utils.UiUtils
import com.sena.monitoreo.utils.navigation.NavigationManager
import com.sena.monitoreo.utils.proceso.ProcesoManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeAdminActivity : BaseVoiceActivity() {

    private lateinit var binding: ActivityHomeAdminBinding
    private lateinit var navigationManager: NavigationManager
    private lateinit var procesoManager: ProcesoManager

    private val voiceConfigViewModel: VoiceConfigViewModel by lazy {
        val repository = VoiceRepository(RetrofitClient.apiVoice)
        ViewModelProvider(this, VoiceConfigViewModelFactory(repository))
            .get(VoiceConfigViewModel::class.java)
    }

    private val procesoViewModel: ProcesoViewModel by lazy {
        val repository = ProcesoRepository(RetrofitClient.apiProceso)
        ViewModelProvider(this, ProcesoViewModelFactory(repository))
            .get(ProcesoViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()
        setupVoiceConfiguration()
        setupProcesoControl()

        // Cargar estado del proceso al iniciar
        lifecycleScope.launch {
            procesoViewModel.loadProcesoStatus()
        }
    }

    override fun onNetworkRetry() {
        lifecycleScope.launch {
            procesoViewModel.loadProcesoStatus()
            voiceConfigViewModel.loadCurrentConfig()
        }
    }

    private fun setupNavigation() {
        navigationManager = NavigationManager(
            context = this,
            drawerLayout = binding.homeAdmin,
            navigationView = binding.navView,
            currentActivity = "home_admin",
            view = binding.root
        )

        binding.mainHeader.settingsIcon.setOnClickListener {
            if (binding.homeAdmin.isDrawerOpen(GravityCompat.START)) {
                binding.homeAdmin.closeDrawer(GravityCompat.START)
            } else {
                binding.homeAdmin.openDrawer(GravityCompat.START)
            }
        }

        navigationManager.setupNavigation("home_admin")
    }

    private fun setupVoiceConfiguration() {
        setupWaveformComponents(
            binding.mainHeader.waveformSection.waveformSeekBar,
            binding.mainHeader.waveformSection.btnPlayMessage
        )

        voiceConfigViewModel.loadCurrentConfig()

        // Observar configuración de voz
        lifecycleScope.launch {
            voiceConfigViewModel.currentConfig.collectLatest { config ->
                voiceManager.currentPitch = config.voicePitch.toFloat()
                voiceManager.currentGender = config.voiceGender

                if (isVoiceInitialized) {
                    voiceManager.applyTtsSettings()
                }
            }
        }

        // Manejo de errores de voz
        lifecycleScope.launch {
            voiceConfigViewModel.uiState.collectLatest { state ->
                when (state) {
                    is VoiceConfigViewModel.VoiceConfigUiState.Error -> {
                        if (state.message.contains("Error de red", ignoreCase = true) ||
                            state.message.contains("IOException", ignoreCase = true)) {
                            showNetworkError(state.message)
                        } else {
                            // Mostrar otros errores como snackbar
                            UiUtils.showSnackbar(binding.root, state.message, isError = true)
                        }
                    }
                    VoiceConfigViewModel.VoiceConfigUiState.Success -> {
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setupProcesoControl() {
        val cardBinding = binding.cardProcesoControlInclude

        procesoManager = ProcesoManager(
            context = this,
            procesoViewModel = procesoViewModel,
            btnIniciar = cardBinding.btnIniciarProceso,
            btnFinalizar = cardBinding.btnFinalizarProceso,
            tvEstado = cardBinding.tvProcesoEstado,
            progressBar = cardBinding.procesoProgressBar,
            lifecycleOwner = this,
            onStatusUpdate = { message ->
                // Solo mostrar Snackbar y hablar si no es un error de red
                if (!message.contains("Error de red", ignoreCase = true) &&
                    !message.contains("IOException", ignoreCase = true)) {
                    UiUtils.showSnackbar(binding.root, message)
                    speakWithWaveform(message)
                }
            }
        )

        procesoManager.setupProcesoControl()

        // Observar el estado del proceso para manejar errores de red
        procesoViewModel.procesoStatus.observe(this) { mensaje ->
            if (mensaje.contains("Error de red", ignoreCase = true) ||
                mensaje.contains("IOException", ignoreCase = true)) {
                showNetworkError(mensaje)
            } else if (mensaje.isNotEmpty() &&
                !mensaje.contains("Proceso activo", ignoreCase = true) &&
                !mensaje.contains("Proceso inactivo", ignoreCase = true)) {
                // Mostrar otros mensajes del proceso como snackbar
                UiUtils.showSnackbar(binding.root, mensaje,
                    isError = mensaje.contains("Error", ignoreCase = true))
            }
        }

        // Observar errores de red específicos del proceso
        procesoViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                UiUtils.showLoading(this, "Procesando...")
            } else {
                UiUtils.hideLoading()
            }
        }
    }

    private fun navigateToAdminDashboard(section: String? = null) {
        val intent = Intent(this, AdminDashboardActivity::class.java)
        section?.let {
            intent.putExtra("SCROLL_TO", it)
        }
        startActivity(intent)
        finish()
    }

    override fun onVoiceInitialized() {

        voiceConfigViewModel.currentConfig.value.let { config ->
            voiceManager.currentPitch = config.voicePitch.toFloat()
            voiceManager.currentGender = config.voiceGender
            voiceManager.applyTtsSettings()
        }

        // Solo iniciar speaking si no hay error de red
        startSpeaking()
    }

    override fun startSpeaking() {
        super.startSpeaking()
        val message = getString(R.string.welcome_message)
        speakWithWaveform(message)
    }

    override fun onBackPressed() {
        if (binding.homeAdmin.isDrawerOpen(GravityCompat.START)) {
            binding.homeAdmin.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        procesoManager.cleanup()
    }

    companion object {
        private const val TAG = "HomeAdminActivity"
    }
}
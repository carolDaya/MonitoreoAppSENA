package com.sena.monitoreo.ui.user

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.databinding.ActivityHomeUserBinding
import com.sena.monitoreo.ui.base.BaseVoiceActivity
import com.sena.monitoreo.ui.base.factory.VoiceConfigViewModelFactory
import com.sena.monitoreo.ui.base.viewmodel.VoiceConfigViewModel
import com.sena.monitoreo.utils.UiUtils
import com.sena.monitoreo.utils.navigation.NavigationManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeUserActivity : BaseVoiceActivity() {

    private lateinit var binding: ActivityHomeUserBinding
    private lateinit var navigationManager: NavigationManager
    private val TAG = "HomeUserActivity"

    // Repositorio
    private val voiceRepo = VoiceRepository(RetrofitClient.apiVoice)

    // ViewModel (Usando VoiceConfigViewModel y su Factory)
    private val viewModel: VoiceConfigViewModel by lazy {
        val factory = VoiceConfigViewModelFactory(voiceRepo)
        ViewModelProvider(this, factory)[VoiceConfigViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupVoiceConfiguration()

        // Iniciar la carga de la configuración de voz
        viewModel.loadCurrentConfig()
    }

    override fun onNetworkRetry() {
        Log.d(TAG, "onNetworkRetry: Reintentando carga de configuración de voz.")
        // Reintentar la carga de la configuración de voz
        viewModel.loadCurrentConfig()
    }

    private fun setupNavigation() {
        navigationManager = NavigationManager(
            context = this,
            drawerLayout = binding.homeAdmin,
            navigationView = binding.navView,
            currentActivity = "home",
            view = binding.root
        )

        binding.mainHeader.settingsIcon.setOnClickListener {
            binding.homeAdmin.openDrawer(GravityCompat.START)
        }

        navigationManager.setupNavigation("home")
    }

    private fun setupVoiceConfiguration() {
        setupWaveformComponents(
            binding.mainHeader.waveformSection.waveformSeekBar,
            binding.mainHeader.waveformSection.btnPlayMessage
        )

        // Observar el estado de la UI (errores de red/lógicos)
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    VoiceConfigViewModel.VoiceConfigUiState.Loading -> {
                        // Opcional: mostrar un indicador de carga
                    }
                    is VoiceConfigViewModel.VoiceConfigUiState.Error -> {
                        Log.e(TAG, "Error de Config. Voz: ${state.message}")
                        // 💡 NUEVO ENFOQUE: Usar showNetworkError para errores de red
                        if (state.message.contains("Error de red", ignoreCase = true) ||
                            state.message.contains("IOException", ignoreCase = true)) {
                            showNetworkError(state.message)
                        } else {
                            // Mostrar otros errores como snackbar
                            UiUtils.showSnackbar(binding.root, state.message, isError = true)
                        }
                    }
                    VoiceConfigViewModel.VoiceConfigUiState.Success -> {
                        // Éxito en la carga - no necesita acción específica
                    }
                    VoiceConfigViewModel.VoiceConfigUiState.Idle -> {}
                }
            }
        }

        // Observar la configuración (datos)
        lifecycleScope.launch {
            viewModel.currentConfig.collectLatest { config ->
                voiceManager.currentPitch = config.voicePitch.toFloat()
                voiceManager.currentGender = config.voiceGender

                // Aplicar configuración solo si la Activity ya terminó de inicializar el TTS
                if (isVoiceInitialized) {
                    voiceManager.applyTtsSettings()
                }
            }
        }
    }

    override fun onVoiceInitialized() {
        // Se llama cuando el motor TTS de Android está listo.
        // Aplicamos la configuración que ya pudimos haber cargado del servidor.
        viewModel.currentConfig.value.let { config ->
            voiceManager.currentPitch = config.voicePitch.toFloat()
            voiceManager.currentGender = config.voiceGender
            voiceManager.applyTtsSettings()
        }

        // Reproducir mensaje de bienvenida
        startSpeaking()
    }

    override fun startSpeaking() {
        super.startSpeaking()
        val message = getString(R.string.welcome_message)
        // ✅ USANDO EL MÉTODO DE BASE
        speakWithWaveform(message)
    }

    override fun onBackPressed() {
        if (binding.homeAdmin.isDrawerOpen(GravityCompat.START)) {
            binding.homeAdmin.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
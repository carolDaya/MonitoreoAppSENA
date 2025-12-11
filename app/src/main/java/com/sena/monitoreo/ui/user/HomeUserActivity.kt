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

    // ViewModel
    private val viewModel: VoiceConfigViewModel by lazy {
        val factory = VoiceConfigViewModelFactory(voiceRepo)
        ViewModelProvider(this, factory)[VoiceConfigViewModel::class.java]
    }

    // Variables para control rápido
    private var isTtsReadyForImmediateUse = false
    private val welcomeMessage = "Bienvenido al sistema de monitoreo de biodigestores"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()

        // 1️⃣ CONFIGURACIÓN POR DEFECTO INMEDIATA (sin esperar red)
        voiceManager.currentPitch = 1.0f
        voiceManager.currentGender = "FEMALE"

        setupWaveformComponents(
            binding.mainHeader.waveformSection.waveformSeekBar,
            binding.mainHeader.waveformSection.btnPlayMessage
        )

        // 2️⃣ Cargar configuración en background (no bloqueante)
        viewModel.loadCurrentConfig()

        // 3️⃣ Configurar botón para respuesta ULTRA-RÁPIDA
        binding.mainHeader.waveformSection.btnPlayMessage.setOnClickListener {
            handlePlayButtonClick()
        }

        // 4️⃣ Observar cambios de configuración (en background)
        setupVoiceConfigurationAsync()

        // 5️⃣ Mostrar ayudita visual
        showHelpTooltip()
    }

    override fun onNetworkRetry() {
        Log.d(TAG, "onNetworkRetry: Reintentando carga de configuración de voz.")
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

    /**
     * Configuración ASINCRONA - No bloquea la UI principal
     */
    private fun setupVoiceConfigurationAsync() {
        lifecycleScope.launch {
            viewModel.currentConfig.collectLatest { config ->
                // Actualizar configuración cuando llegue
                voiceManager.currentPitch = config.voicePitch.toFloat()
                voiceManager.currentGender = config.voiceGender

                // Si TTS ya está listo, aplicar inmediatamente
                if (isVoiceInitialized) {
                    voiceManager.applyTtsSettings()
                    Log.d(TAG, "⚡ Configuración del servidor aplicada")
                }
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is VoiceConfigViewModel.VoiceConfigUiState.Error -> {
                        // Solo manejar errores críticos
                        if (state.message.contains("Error de red", ignoreCase = true)) {
                            Log.w(TAG, "Usando configuración por defecto por error de red")
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onVoiceInitialized() {
        Log.d(TAG, "✅ TTS INICIALIZADO - Listo para usar")

        // 1. Aplicar configuración actual inmediatamente
        voiceManager.applyTtsSettings()

        // 2. Marcar que TTS está listo para uso inmediato
        isTtsReadyForImmediateUse = true

        // 3. Pre-cargar mensaje (opcional, para mayor velocidad)
        lifecycleScope.launch {
            // Pequeño delay para asegurar que TTS esté completamente listo
            kotlinx.coroutines.delay(100)
            Log.d(TAG, "🔊 TTS completamente listo para respuesta instantánea")
        }
    }

    /**
     * Manejo ULTRA-RÁPIDO del click del botón
     */
    /**
     * Manejo ULTRA-RÁPIDO del click del botón
     */
    private fun handlePlayButtonClick() {
        if (!isVoiceInitialized) {
            Log.w(TAG, "⏳ TTS aún no inicializado, mostrando mensaje...")
            UiUtils.showSnackbar(binding.root, "Inicializando voz... por favor espera", false)
            return
        }

        if (voiceManager.isSpeaking) {
            // Si ya está hablando, detener
            Log.d(TAG, "⏸️ Deteniendo voz manualmente")
            stopSpeaking()
            // Asegurarse de detener waveform también
            waveformManager.stopAnimation()
            binding.mainHeader.waveformSection.btnPlayMessage.setIconResource(R.drawable.ic_play)
            return
        }

        Log.d(TAG, "🎯 Botón presionado - Respuesta INMEDIATA")

        // 1. Cambiar icono inmediatamente (feedback visual)
        binding.mainHeader.waveformSection.btnPlayMessage.setIconResource(R.drawable.ic_stop)

        // 2. Hablar SIN delays
        speakImmediately()
    }
    /**
     * Hablar inmediatamente sin procesos adicionales
     */
    /**
     * Hablar inmediatamente sin procesos adicionales
     */
    private fun speakImmediately() {
        try {
            // Mensaje corto y directo
            val message = getString(R.string.welcome_message)

            // Configurar callbacks para UI
            voiceManager.setOnUtteranceCompletedListener {
                lifecycleScope.launch {
                    kotlinx.coroutines.delay(200)
                    // 1. Cambiar icono
                    binding.mainHeader.waveformSection.btnPlayMessage.setIconResource(R.drawable.ic_play)
                    // 2. DETENER waveform cuando termine de hablar
                    waveformManager.stopAnimation()
                    Log.d(TAG, "🔇 Voz terminada - Waveform detenido")
                }
            }

            voiceManager.setOnSpeechStartedListener {
                lifecycleScope.launch {
                    // 3. Iniciar waveform cuando empiece a hablar
                    waveformManager.startContinuousAnimation(this) {
                        Log.d(TAG, "Waveform completado")
                    }
                    Log.d(TAG, "🔊 Voz iniciada - Waveform iniciado")
                }
            }

            // Hablar INMEDIATAMENTE
            voiceManager.speak(message)

            Log.d(TAG, "🔊 Hablando inmediatamente: ${message.length} caracteres")

        } catch (e: Exception) {
            Log.e(TAG, "Error al hablar: ${e.message}")
            binding.mainHeader.waveformSection.btnPlayMessage.setIconResource(R.drawable.ic_play)
            // Asegurarse de detener waveform en caso de error
            waveformManager.stopAnimation()
        }
    }

    // 🎈 Tooltip / Ayudita visual
    private fun showHelpTooltip() {
        UiUtils.showTooltip(
            anchor = binding.mainHeader.waveformSection.btnPlayMessage,
            message = "Presiona para escuchar el mensaje de bienvenida",
            isError = false
        )
    }

    override fun onBackPressed() {
        if (binding.homeAdmin.isDrawerOpen(GravityCompat.START)) {
            binding.homeAdmin.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()

        // Verificar si TTS está listo cuando se vuelve a la actividad
        if (isVoiceInitialized && !isTtsReadyForImmediateUse) {
            isTtsReadyForImmediateUse = true
            Log.d(TAG, "🔄 TTS re-activado para respuesta rápida")
        }
    }
}
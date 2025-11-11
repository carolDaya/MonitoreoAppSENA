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
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModel
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModelFactory
import com.sena.monitoreo.ui.admin.viewmodel.ProcesoViewModel
import com.sena.monitoreo.ui.admin.viewmodel.ProcesoViewModelFactory
import com.sena.monitoreo.ui.base.BaseVoiceActivity
import com.sena.monitoreo.utils.UiUtils
import com.sena.monitoreo.utils.navigation.NavigationManager
import com.sena.monitoreo.utils.proceso.ProcesoManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeAdminActivity : BaseVoiceActivity() {

    private lateinit var binding: ActivityHomeAdminBinding
    private lateinit var navigationManager: NavigationManager
    private lateinit var procesoManager: ProcesoManager


    // ViewModels
    private val viewModel: AdminConfigViewModel by lazy {
        val repository = VoiceRepository(RetrofitClient.apiVoice)
        ViewModelProvider(this, AdminConfigViewModelFactory(repository))
            .get(AdminConfigViewModel::class.java)
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

        setupNavigation() // ✅ PRIMERO CONFIGURAR NAVEGACIÓN
        setupVoiceConfiguration()
        setupProcesoControl()

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
            binding.homeAdmin.openDrawer(GravityCompat.START)
        }

        navigationManager.setupAdminNavigation("home_admin")
    }

    private fun setupVoiceConfiguration() {
        // Configurar waveform components usando el método de la clase base
        setupWaveformComponents(
            binding.mainHeader.waveformSection.waveformSeekBar,
            binding.mainHeader.waveformSection.btnPlayMessage
        )

        viewModel.loadCurrentConfig()
        viewModel.currentConfig.observe(this) { config ->
            voiceManager.currentPitch = config.pitch
            voiceManager.currentGender = config.gender
            if (isVoiceInitialized) {
                voiceManager.applyTtsSettings()

                // Reproducir mensaje de prueba cuando se actualiza la configuración
                lifecycleScope.launch {
                    delay(500)
                    speakWithWaveform("Configuración de voz actualizada")
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
                UiUtils.showSnackbar(binding.root, message)
                // Opcional: también reproducir por voz
                speakWithWaveform(message)
            }
        )

        procesoManager.setupProcesoControl()
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
        // Configuración adicional cuando la voz esté lista
        Log.d(TAG, "Voz inicializada en HomeAdmin")

        // Verificar si hay configuración de voz cargada
        viewModel.currentConfig.value?.let { config ->
            voiceManager.currentPitch = config.pitch
            voiceManager.currentGender = config.gender
            voiceManager.applyTtsSettings()
        }
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
// HomeUserActivity.kt (versión refactorizada)
package com.sena.monitoreo.ui.user

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.databinding.ActivityHomeUserBinding
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModel
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModelFactory
import com.sena.monitoreo.ui.base.BaseVoiceActivity
import com.sena.monitoreo.utils.navigation.NavigationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeUserActivity : BaseVoiceActivity() {

    private lateinit var binding: ActivityHomeUserBinding
    private lateinit var navigationManager: NavigationManager

    private val viewModel: AdminConfigViewModel by lazy {
        val repository = VoiceRepository(RetrofitClient.apiVoice)
        ViewModelProvider(this, AdminConfigViewModelFactory(repository))
            .get(AdminConfigViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupVoiceConfiguration()
    }

    private fun setupNavigation() {
        navigationManager = NavigationManager(
            context = this,
            drawerLayout = binding.homeAdmin,
            navigationView = binding.navView,
            currentActivity = "home"
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

        viewModel.loadCurrentConfig()
        viewModel.currentConfig.observe(this) { config ->
            voiceManager.currentPitch = config.pitch
            voiceManager.currentGender = config.gender

            if (isVoiceInitialized) {
                voiceManager.applyTtsSettings()
                lifecycleScope.launch {
                    delay(500)
                }
            }
        }
    }

    override fun onVoiceInitialized() {
        // Configuración adicional cuando la voz esté lista
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
}
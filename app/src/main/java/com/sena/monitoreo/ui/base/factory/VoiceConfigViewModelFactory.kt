package com.sena.monitoreo.ui.base.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.ui.base.viewmodel.VoiceConfigViewModel

/**
 * Factoría para inicializar VoiceConfigViewModel, inyectando VoiceRepository.
 * Esto permite que VoiceConfigViewModel obtenga la configuración de voz sin depender
 * directamente de la construcción manual de dependencias en la Activity/Fragment.
 */
class VoiceConfigViewModelFactory(
    private val voiceRepository: VoiceRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoiceConfigViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VoiceConfigViewModel(voiceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class requested")
    }
}
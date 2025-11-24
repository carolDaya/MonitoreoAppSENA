package com.sena.monitoreo.ui.admin.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModel

class AdminConfigViewModelFactory(private val repository: VoiceRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminConfigViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminConfigViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
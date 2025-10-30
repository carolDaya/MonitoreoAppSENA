package com.sena.monitoreo.ui.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sena.monitoreo.data.repository.VoiceRepository

class AdminConfigViewModelFactory(private val repository: VoiceRepository) : ViewModelProvider.Factory {

    // Sobrescribe el método para crear el ViewModel
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Verifica si la clase solicitada es AdminConfigViewModel
        if (modelClass.isAssignableFrom(AdminConfigViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Crea y devuelve la instancia pasando el repositorio
            return AdminConfigViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
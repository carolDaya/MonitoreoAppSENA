package com.sena.monitoreo.ui.admin.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sena.monitoreo.data.repository.ProcesoRepository
import com.sena.monitoreo.ui.admin.viewmodel.ProcesoViewModel

class ProcesoViewModelFactory(private val repository: ProcesoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProcesoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProcesoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
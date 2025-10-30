// AdminConfigViewModel.kt
package com.sena.monitoreo.ui.admin.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sena.monitoreo.data.model.ai.VoiceResponse
import com.sena.monitoreo.data.repository.VoiceRepository
import kotlinx.coroutines.launch

class AdminConfigViewModel(private val repository: VoiceRepository) : ViewModel() {

    // LiveData para monitorear el estado de la configuración actual
    private val _currentConfig = MutableLiveData<VoiceResponse>()
    val currentConfig: LiveData<VoiceResponse> = _currentConfig

    // LiveData para notificar el resultado de la operación de guardar
    private val _saveStatus = MutableLiveData<String>()
    val saveStatus: LiveData<String> = _saveStatus

    // LiveData para indicar si una operación está en curso (cargando)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Carga la configuración de voz actual desde el backend.
     * Usado al iniciar la Activity para precargar los Spinners.
     */
    fun loadCurrentConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val config = repository.getVoiceConfig()
                _currentConfig.postValue(config)
            } catch (e: Exception) {
                _saveStatus.postValue("Error al cargar la configuración actual: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Guarda la nueva configuración de voz en el backend.
     */
    fun saveConfiguration(gender: String, pitch: Float) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.saveVoiceConfig(gender, pitch)
                _saveStatus.postValue("Configuración de voz guardada con éxito.")
            } catch (e: Exception) {
                _saveStatus.postValue("Error al guardar: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
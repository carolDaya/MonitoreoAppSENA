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

    // LiveData para notificar el éxito del guardado
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    // LiveData para indicar si una operación está en curso (cargando)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Carga la configuración de voz actual desde el backend.
     */
    fun loadCurrentConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val config = repository.getVoiceConfig()
                _currentConfig.postValue(config)
            } catch (e: Exception) {
                // Aquí usamos Log, el Snackbar se maneja en la Activity
                // _saveStatus.postValue("Error al cargar la configuración actual: ${e.message}")
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
            _saveSuccess.value = false
            try {
                repository.saveVoiceConfig(gender, pitch)

                loadCurrentConfig()

                _saveStatus.postValue("Configuración de voz guardada con éxito.")
                _saveSuccess.postValue(true)

            } catch (e: Exception) {
                _saveStatus.postValue("Error al guardar: ${e.message}")
                _saveSuccess.postValue(false)
            } finally {
                // La Activity ocultará el loading observando _isLoading
            }
        }
    }
}
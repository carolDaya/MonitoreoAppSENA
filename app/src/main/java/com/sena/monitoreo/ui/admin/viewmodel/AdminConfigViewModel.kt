package com.sena.monitoreo.ui.admin.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sena.monitoreo.data.model.voice.VoiceConfigResponse
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.utils.ResultWrapper
import kotlinx.coroutines.launch
import java.io.IOException

class AdminConfigViewModel(private val repository: VoiceRepository) : ViewModel() {

    private val _currentConfig = MutableLiveData<VoiceConfigResponse>()
    val currentConfig: LiveData<VoiceConfigResponse> = _currentConfig
    private val _saveStatus = MutableLiveData<String>()
    val saveStatus: LiveData<String> = _saveStatus

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData para errores de carga inicial
    private val _loadError = MutableLiveData<String>()
    val loadError: LiveData<String> = _loadError

    /**
     * Carga la configuración de voz actual desde el backend.
     */
    fun loadCurrentConfig() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            _loadError.postValue("") // Limpiar error de carga

            try {
                when (val result = repository.getVoiceConfig()) {
                    is ResultWrapper.Success -> {
                        _currentConfig.postValue(result.data)
                        _saveStatus.postValue("Configuración de voz cargada.")
                    }
                    is ResultWrapper.Error -> {
                        _loadError.postValue("Error al cargar la configuración: ${result.message}")
                    }
                }
            } catch (e: IOException) {
                _loadError.postValue("Error de red: No se pudo cargar la configuración de voz.") // 💡 Error de red
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Guarda la nueva configuración de voz en el backend.
     */
    fun saveConfiguration(gender: String, pitch: Float) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            _saveSuccess.postValue(false)
            _saveStatus.postValue("") // Limpiar estado de guardado

            val newConfig = VoiceConfigResponse(
                voiceGender = gender,
                voicePitch = pitch.toDouble()
            )

            try {
                when (val result = repository.saveVoiceConfig(newConfig)) {
                    is ResultWrapper.Success -> {
                        _currentConfig.postValue(result.data)
                        _saveStatus.postValue("Configuración de voz guardada con éxito.")
                        _saveSuccess.postValue(true)
                    }
                    is ResultWrapper.Error -> {
                        _saveStatus.postValue("Error al guardar: ${result.message}")
                        _saveSuccess.postValue(false)
                    }
                }
            } catch (e: IOException) {
                _saveStatus.postValue("Error de red: No se pudo guardar la configuración de voz.") // 💡 Error de red
                _saveSuccess.postValue(false)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
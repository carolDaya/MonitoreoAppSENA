package com.sena.monitoreo.ui.base.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sena.monitoreo.data.model.voice.VoiceConfigResponse
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.utils.ResultWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * ViewModel genérico para cargar la configuración de voz (Pitch y Gender).
 */
class VoiceConfigViewModel(
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    // --- Definición del Estado de UI (Necesario para el manejo de errores de red) ---

    sealed interface VoiceConfigUiState {
        data object Idle : VoiceConfigUiState
        data object Loading : VoiceConfigUiState
        data object Success : VoiceConfigUiState
        // 💡 CLAVE: Incluir la clase Error para pasar el mensaje
        data class Error(val message: String) : VoiceConfigUiState
    }

    private val _uiState = MutableStateFlow<VoiceConfigUiState>(VoiceConfigUiState.Idle)
    val uiState: StateFlow<VoiceConfigUiState> = _uiState.asStateFlow()

    // --- Flujos de Datos Existentes ---

    private val _currentConfig = MutableStateFlow(
        VoiceConfigResponse(voicePitch = 1.0, voiceGender = "F")
    )
    val currentConfig = _currentConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadCurrentConfig() {
        _isLoading.value = true
        _uiState.value = VoiceConfigUiState.Loading // 💡 Actualizar estado de UI a Loading

        viewModelScope.launch {
            try {
                when (val result = voiceRepository.getVoiceConfig()) {
                    is ResultWrapper.Success -> {
                        _currentConfig.value = result.data
                        _uiState.value = VoiceConfigUiState.Success // 💡 Éxito
                    }
                    is ResultWrapper.Error -> {
                        // Si es un error de servidor o lógico, lo reportamos al UI
                        _uiState.value = VoiceConfigUiState.Error(result.message) // 💡 Error lógico
                    }
                }
            } catch (e: IOException) {
                // Error de red (Timeouts, no connection, etc.)
                _uiState.value = VoiceConfigUiState.Error("Error de red: No se pudo conectar al servidor.") // 💡 Error de red
            } catch (e: Exception) {
                // Error inesperado
                _uiState.value = VoiceConfigUiState.Error("Error inesperado al cargar la configuración: ${e.message}") // 💡 Error inesperado
            } finally {
                _isLoading.value = false
            }
        }
    }
}
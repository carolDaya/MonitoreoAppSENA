package com.sena.monitoreo.ui.base.viewmodel

import android.util.Log
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

    private val TAG = "VoiceConfigViewModel"

    // --- Definición del Estado de UI (Necesario para el manejo de errores de red) ---

    sealed interface VoiceConfigUiState {
        data object Idle : VoiceConfigUiState
        data object Loading : VoiceConfigUiState
        data object Success : VoiceConfigUiState
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
        loadCurrentConfig(forceRefresh = false)
    }

    /**
     * 🔄 MÉTODO ACTUALIZADO: Cargar configuración de voz
     * @param forceRefresh Si es true, ignora cualquier caché y fuerza recarga desde API
     */
    fun loadCurrentConfig(forceRefresh: Boolean = false) {
        _isLoading.value = true
        _uiState.value = VoiceConfigUiState.Loading

        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Cargando configuración de voz (forceRefresh: $forceRefresh)")

                when (val result = voiceRepository.getVoiceConfig(forceRefresh)) {
                    is ResultWrapper.Success -> {
                        _currentConfig.value = result.data
                        _uiState.value = VoiceConfigUiState.Success

                        if (forceRefresh) {
                            Log.d(TAG, "✅ Configuración de voz FORZADA recargada: " +
                                    "Pitch=${result.data.voicePitch}, Gender=${result.data.voiceGender}")
                        } else {
                            Log.d(TAG, "✅ Configuración de voz cargada: " +
                                    "Pitch=${result.data.voicePitch}, Gender=${result.data.voiceGender}")
                        }
                    }
                    is ResultWrapper.Error -> {
                        val errorMsg = if (forceRefresh) {
                            "Error forzando recarga de configuración: ${result.message}"
                        } else {
                            "Error cargando configuración: ${result.message}"
                        }
                        _uiState.value = VoiceConfigUiState.Error(errorMsg)
                        Log.e(TAG, "❌ $errorMsg")
                    }
                }
            } catch (e: IOException) {
                val errorMsg = if (forceRefresh) {
                    "Error de red forzando recarga: No se pudo conectar al servidor."
                } else {
                    "Error de red: No se pudo conectar al servidor."
                }
                _uiState.value = VoiceConfigUiState.Error(errorMsg)
                Log.e(TAG, "❌ $errorMsg", e)
            } catch (e: Exception) {
                val errorMsg = if (forceRefresh) {
                    "Error inesperado forzando recarga: ${e.message}"
                } else {
                    "Error inesperado al cargar la configuración: ${e.message}"
                }
                _uiState.value = VoiceConfigUiState.Error(errorMsg)
                Log.e(TAG, "❌ $errorMsg", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 🆕 MÉTODO ADICIONAL: Actualizar configuración localmente (para usar cuando cambia en otra pantalla)
     */
    fun updateLocalConfig(pitch: Double, gender: String) {
        _currentConfig.value = VoiceConfigResponse(
            voicePitch = pitch,
            voiceGender = gender
        )
        Log.d(TAG, "🔄 Configuración local actualizada: Pitch=$pitch, Gender=$gender")
    }
}
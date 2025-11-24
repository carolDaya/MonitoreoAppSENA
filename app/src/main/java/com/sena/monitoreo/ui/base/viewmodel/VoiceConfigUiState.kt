package com.sena.monitoreo.ui.base.viewmodel

sealed interface VoiceConfigUiState {
    data object Idle : VoiceConfigUiState
    data object Loading : VoiceConfigUiState
    data object Success : VoiceConfigUiState
    data class Error(val message: String) : VoiceConfigUiState
}
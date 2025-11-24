package com.sena.monitoreo.ui.auth.viewmodel.password_reset

sealed class ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState()
    object Loading : ForgotPasswordUiState()
    data class Success(val phone: String) : ForgotPasswordUiState()
    data class Error(val message: String) : ForgotPasswordUiState()
    data class ValidationError(val message: String) : ForgotPasswordUiState() // Para errores de validación local
}
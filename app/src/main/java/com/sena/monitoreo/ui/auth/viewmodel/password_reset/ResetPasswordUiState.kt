package com.sena.monitoreo.ui.auth.viewmodel.password_reset

sealed class ResetPasswordUiState {
    object Idle : ResetPasswordUiState()
    object Loading : ResetPasswordUiState()
    object Success : ResetPasswordUiState()
    data class Error(val message: String) : ResetPasswordUiState()
    data class ValidationError(val newPassError: String? = null, val confirmPassError: String? = null) : ResetPasswordUiState()
}
package com.sena.monitoreo.ui.auth.viewmodel.password_reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.utils.ResultWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun verifyPhoneNumber(phone: String) {
        // Limpiar estados previos
        _uiState.value = ForgotPasswordUiState.Idle

        when {
            phone.isEmpty() -> {
                _uiState.value = ForgotPasswordUiState.ValidationError("Ingresa tu número de teléfono")
                return
            }
            phone.length != 10 -> {
                _uiState.value = ForgotPasswordUiState.ValidationError("El teléfono debe tener 10 dígitos")
                return
            }
            !phone.all { it.isDigit() } -> {
                _uiState.value = ForgotPasswordUiState.ValidationError("Solo se permiten números")
                return
            }
        }

        _uiState.value = ForgotPasswordUiState.Loading

        viewModelScope.launch {
            try {
                when (val result = authRepository.requestPasswordReset(phone)) {
                    is ResultWrapper.Success -> {
                        _uiState.value = ForgotPasswordUiState.Success(phone)
                    }
                    is ResultWrapper.Error -> {
                        _uiState.value = ForgotPasswordUiState.Error(result.message)
                    }
                }
            } catch (e: IOException) {
                _uiState.value = ForgotPasswordUiState.Error("Error de red: verifica tu conexión.")
            } catch (e: Exception) {
                _uiState.value = ForgotPasswordUiState.Error("Error inesperado al verificar el teléfono.")
            }
        }
    }
}
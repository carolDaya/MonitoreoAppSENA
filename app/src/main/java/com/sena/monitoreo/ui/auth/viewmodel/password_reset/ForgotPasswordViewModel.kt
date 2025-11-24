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

    /**
     * Inicia el proceso de restablecimiento de contraseña verificando el número de teléfono.
     */
    fun verifyPhoneNumber(phone: String) {
        if (!isInputValid(phone)) return

        _uiState.value = ForgotPasswordUiState.Loading

        viewModelScope.launch {
            try {
                when (val result = authRepository.requestPasswordReset(phone)) {
                    is ResultWrapper.Success -> {
                        _uiState.value = ForgotPasswordUiState.Success(phone)
                    }
                    is ResultWrapper.Error -> {
                        // 💡 Error del servidor (ej. "Usuario no existe" o cualquier otro error de negocio)
                        _uiState.value = ForgotPasswordUiState.Error(result.message)
                    }
                }
            } catch (e: IOException) {
                // Error de conexión de bajo nivel
                _uiState.value = ForgotPasswordUiState.Error("Error de red: verifica tu conexión.")
            } catch (e: Exception) {
                // Error inesperado
                _uiState.value = ForgotPasswordUiState.Error("Error inesperado al verificar el teléfono.")
            }
        }
    }

    /**
     * Valida el formato y presencia del número de teléfono.
     */
    private fun isInputValid(phone: String): Boolean {
        val trimmedPhone = phone.trim()

        if (trimmedPhone.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error("El número de teléfono no puede estar vacío.")
            return false
        }

        if (!trimmedPhone.matches(Regex("^\\d{10}$"))) {
            // Mensaje que la Activity puede interpretar como "Validación de formato"
            _uiState.value = ForgotPasswordUiState.Error("Error de formato: El número debe tener exactamente 10 dígitos.")
            return false
        }

        return true
    }
}
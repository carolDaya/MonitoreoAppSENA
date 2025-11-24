package com.sena.monitoreo.ui.auth.viewmodel.password_reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.utils.ResultWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class ResetPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResetPasswordUiState>(ResetPasswordUiState.Idle)
    val uiState = _uiState.asStateFlow()

    /**
     * Valida y llama al repositorio para actualizar la contraseña.
     */
    fun updatePassword(phone: String, newPass: String, confirmPass: String) {
        if (phone.isBlank()) {
            _uiState.value = ResetPasswordUiState.Error("Error interno: Número de teléfono no válido.")
            return
        }

        // Validación. Si falla, isInputValid ya establece el estado Error.
        if (!isInputValid(newPass, confirmPass)) return

        _uiState.value = ResetPasswordUiState.Loading

        viewModelScope.launch {
            try {
                // El AuthRepository ya maneja la lógica de la llamada a la API y el ResultWrapper
                when (val result = authRepository.updatePassword(phone, newPass, confirmPass)) {
                    is ResultWrapper.Success -> {
                        _uiState.value = ResetPasswordUiState.Success
                    }
                    is ResultWrapper.Error -> {
                        _uiState.value = ResetPasswordUiState.Error(result.message)
                    }
                }
            } catch (e: IOException) {
                _uiState.value = ResetPasswordUiState.Error("Error de red: verifica tu conexión.")
            } catch (e: Exception) {
                _uiState.value = ResetPasswordUiState.Error("Error inesperado al actualizar la contraseña.")
            }
        }
    }

    /**
     * Valida la longitud y coincidencia de las contraseñas.
     * 💡 CORRECCIÓN: Ahora establece ResetPasswordUiState.Error directamente y retorna false si falla.
     */
    private fun isInputValid(newPass: String, confirmPass: String): Boolean {
        if (newPass.length < 6) {
            _uiState.value = ResetPasswordUiState.Error("Contraseña nueva: Debe tener al menos 6 caracteres.")
            return false
        }

        if (newPass != confirmPass) {
            _uiState.value = ResetPasswordUiState.Error("Confirmación de contraseña: Las contraseñas no coinciden.")
            return false
        }

        return true
    }
}
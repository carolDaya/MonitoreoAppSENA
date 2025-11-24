package com.sena.monitoreo.ui.auth.viewmodel.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sena.monitoreo.data.model.auth.LoginRequest
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.utils.ResultWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun login(phone: String, password: String) {
        if (phone.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Por favor completa todos los campos.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                // CORRECCIÓN: Usar ResultWrapper
                when (val result = authRepository.login(LoginRequest(phone, password))) {
                    is ResultWrapper.Success -> {
                        val role = result.data.rol
                        _uiState.value = LoginUiState.Success(role)
                    }

                    is ResultWrapper.Error -> {
                        _uiState.value = LoginUiState.Error(result.message)
                    }
                }
            } catch (e: IOException) {
                _uiState.value = LoginUiState.Error("Error de red: ${e.localizedMessage ?: "Verifica tu conexión."}")
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Error inesperado: ${e.localizedMessage ?: "Intenta nuevamente."}")
            }
        }
    }
}
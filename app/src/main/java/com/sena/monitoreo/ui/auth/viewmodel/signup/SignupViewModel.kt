package com.sena.monitoreo.ui.auth.viewmodel.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sena.monitoreo.R
import com.sena.monitoreo.data.model.auth.RegisterRequest
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.utils.ResultWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class SignupViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignupUiState>(SignupUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun register(
        name: String,
        phone: String,
        password: String,
        confirmPassword: String,
        getString: (Int) -> String
    ) {
        // Validación en el ViewModel
        if (!isFormValid(name, phone, password, confirmPassword, getString)) return

        viewModelScope.launch {
            _uiState.value = SignupUiState.Loading

            val request = RegisterRequest(name, phone, password, confirmPassword)

            try {
                when (val result = authRepository.register(request)) {
                    is ResultWrapper.Success -> {
                        _uiState.value = SignupUiState.Success
                    }
                    is ResultWrapper.Error -> {
                        _uiState.value = SignupUiState.Error(result.message)
                    }
                }
            } catch (e: IOException) {
                _uiState.value = SignupUiState.Error(getString(R.string.error_network))
            } catch (e: Exception) {
                _uiState.value = SignupUiState.Error(getString(R.string.error_unexpected))
            }
        }
    }

    private fun isFormValid(
        name: String,
        phone: String,
        password: String,
        confirmPassword: String,
        getString: (Int) -> String
    ): Boolean {
        if (name.isBlank()) {
            _uiState.value = SignupUiState.Error(getString(R.string.error_field_required_name))
            return false
        }
        if (phone.isBlank()) {
            _uiState.value = SignupUiState.Error(getString(R.string.error_field_required_phone))
            return false
        }
        if (!phone.matches(Regex("^\\d{10}$"))) {
            _uiState.value = SignupUiState.Error(getString(R.string.forbided_field_required))
            return false
        }
        if (password.isBlank()) {
            _uiState.value = SignupUiState.Error(getString(R.string.error_field_required_password))
            return false
        }
        if (confirmPassword.isBlank()) {
            _uiState.value = SignupUiState.Error(getString(R.string.error_field_required_confirm))
            return false
        }
        if (password != confirmPassword) {
            _uiState.value = SignupUiState.Error(getString(R.string.error_passwords_not_match))
            return false
        }
        return true
    }
}
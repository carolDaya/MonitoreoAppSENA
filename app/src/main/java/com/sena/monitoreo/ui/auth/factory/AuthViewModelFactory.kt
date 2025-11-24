package com.sena.monitoreo.ui.auth.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.ui.auth.viewmodel.login.LoginViewModel
import com.sena.monitoreo.ui.auth.viewmodel.signup.SignupViewModel
import com.sena.monitoreo.ui.auth.viewmodel.password_reset.ForgotPasswordViewModel
import com.sena.monitoreo.ui.auth.viewmodel.password_reset.ResetPasswordViewModel
/**
 * Factoría personalizada para instanciar LoginViewModel y cualquier otro ViewModelque dependa de AuthRepository.
 */
class AuthViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(authRepository) as T
        }
        if (modelClass.isAssignableFrom(SignupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SignupViewModel(authRepository) as T
        }
        if (modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ForgotPasswordViewModel(authRepository) as T
        }
        if (modelClass.isAssignableFrom(ResetPasswordViewModel::class.java)) { // <-- NUEVA CLASE
            @Suppress("UNCHECKED_CAST")
            return ResetPasswordViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
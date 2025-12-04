package com.sena.monitoreo.ui.auth.factory

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModel
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.ui.auth.factory.AuthViewModelFactory
import com.sena.monitoreo.ui.auth.viewmodel.login.LoginViewModel
import com.sena.monitoreo.ui.auth.viewmodel.signup.SignupViewModel
import com.sena.monitoreo.ui.auth.viewmodel.password_reset.ForgotPasswordViewModel
import com.sena.monitoreo.ui.auth.viewmodel.password_reset.ResetPasswordViewModel
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuthViewModelFactoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var authRepository: AuthRepository
    private lateinit var factory: AuthViewModelFactory

    @Before
    fun setUp() {
        authRepository = mockk()
        factory = AuthViewModelFactory(authRepository)
    }

    @Test
    fun `create should return LoginViewModel when modelClass is LoginViewModel`() {
        // When
        val viewModel = factory.create(LoginViewModel::class.java)

        // Then
        assertTrue(viewModel is LoginViewModel)
        assertNotNull(viewModel)
    }

    @Test
    fun `create should return SignupViewModel when modelClass is SignupViewModel`() {
        // When
        val viewModel = factory.create(SignupViewModel::class.java)

        // Then
        assertTrue(viewModel is SignupViewModel)
        assertNotNull(viewModel)
    }

    @Test
    fun `create should return ForgotPasswordViewModel when modelClass is ForgotPasswordViewModel`() {
        // When
        val viewModel = factory.create(ForgotPasswordViewModel::class.java)

        // Then
        assertTrue(viewModel is ForgotPasswordViewModel)
        assertNotNull(viewModel)
    }

    @Test
    fun `create should return ResetPasswordViewModel when modelClass is ResetPasswordViewModel`() {
        // When
        val viewModel = factory.create(ResetPasswordViewModel::class.java)

        // Then
        assertTrue(viewModel is ResetPasswordViewModel)
        assertNotNull(viewModel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `create should throw IllegalArgumentException for unknown ViewModel class`() {
        // When
        factory.create(TestViewModel::class.java)
    }

    // Clase de prueba para simular un ViewModel desconocido
    class TestViewModel : ViewModel()
}
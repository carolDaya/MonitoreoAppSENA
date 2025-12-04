package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.ApiService
import com.sena.monitoreo.data.model.auth.*
import com.sena.monitoreo.utils.ResultWrapper
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class AuthRepositoryTest {
    private val mockApi = mockk<ApiService>()
    private val repository = AuthRepository(mockApi)

    @Test
    fun `login should return success with valid credentials`() = runTest {
        // Arrange
        val loginRequest = LoginRequest("1234567890", "password")
        val expectedResponse = LoginResponse(
            id = 1,
            nombre = "Test User",
            telefono = "1234567890",
            rol = "USER",
            estado = "ACTIVO",
            conectado = true,
            ultimaConexion = "2024-01-01",
            message = "Login exitoso"
        )

        coEvery { mockApi.login(loginRequest) } returns Response.success(expectedResponse)

        // Act
        val result = repository.login(loginRequest)

        // Assert
        assertTrue(result is ResultWrapper.Success)
        val success = result as ResultWrapper.Success
        assertEquals("Test User", success.data.nombre)
        assertEquals("USER", success.data.rol)
    }

    @Test
    fun `register should return success with valid data`() = runTest {
        // Arrange
        val registerRequest = RegisterRequest(
            nombre = "New User",
            telefono = "0987654321",
            password = "password",
            confirmPassword = "password"
        )
        val expectedResponse = RegisterResponse(
            id = 2,
            nombre = "New User",
            telefono = "0987654321",
            rol = "USER",
            estado = "ACTIVO",
            conectado = false,
            ultimaConexion = null,
            message = "Usuario registrado exitosamente"
        )

        coEvery { mockApi.register(registerRequest) } returns Response.success(expectedResponse)

        // Act
        val result = repository.register(registerRequest)

        // Assert
        assertTrue(result is ResultWrapper.Success)
        assertEquals(2, (result as ResultWrapper.Success).data.id)
    }

    @Test
    fun `requestPasswordReset should return success with valid phone`() = runTest {
        // Arrange
        val phone = "1234567890"
        val expectedResponse = MessageResponse("Código enviado", phone)

        coEvery { mockApi.resetPasswordRequest(any()) } returns Response.success(expectedResponse)

        // Act
        val result = repository.requestPasswordReset(phone)

        // Assert
        assertTrue(result is ResultWrapper.Success)
        assertEquals("Código enviado", (result as ResultWrapper.Success).data.mensaje)
    }
}
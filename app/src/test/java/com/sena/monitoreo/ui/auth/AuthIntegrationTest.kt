package com.sena.monitoreo.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.auth.LoginRequest
import com.sena.monitoreo.data.model.auth.RegisterRequest
import com.sena.monitoreo.data.repository.AuthRepository
import com.sena.monitoreo.utils.ResultWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class AuthIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mockWebServer: MockWebServer
    private lateinit var authRepository: AuthRepository
    private val testPhone = "3001234567"
    private val testPassword = "password123"

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(com.sena.monitoreo.data.api.ApiService::class.java)
        authRepository = AuthRepository(apiService)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `login successful returns user data`() = runTest {
        // Arrange
        val jsonResponse = """
            {
                "id": 1,
                "nombre": "Test User",
                "telefono": "$testPhone",
                "rol": "user",
                "estado": "activo",
                "conectado": true,
                "ultima_conexion": "2024-01-01T10:00:00"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
        )

        // Act
        val result = authRepository.login(LoginRequest(testPhone, testPassword))

        // Assert
        assertTrue(result is ResultWrapper.Success)
        val success = result as ResultWrapper.Success
        assertEquals(1, success.data.id)
        assertEquals("Test User", success.data.nombre)
        assertEquals("user", success.data.rol)
    }

    @Test
    fun `login with invalid credentials returns error`() = runTest {
        // Arrange
        val errorResponse = """
            {
                "error": "Credenciales inválidas"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(errorResponse)
        )

        // Act
        val result = authRepository.login(LoginRequest("wrong", "wrong"))

        // Assert
        assertTrue(result is ResultWrapper.Error)
    }

    @Test
    fun `register new user successfully`() = runTest {
        // Arrange
        val jsonResponse = """
            {
                "id": 2,
                "nombre": "New User",
                "telefono": "3009876543",
                "rol": "user",
                "estado": "activo",
                "conectado": false,
                "ultima_conexion": null
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(jsonResponse)
        )

        // Act
        val request = RegisterRequest(
            "New User",
            "3009876543",
            "password123",
            "password123"
        )
        val result = authRepository.register(request)

        // Assert
        assertTrue(result is ResultWrapper.Success)
        val success = result as ResultWrapper.Success
        assertEquals(2, success.data.id)
        assertEquals("New User", success.data.nombre)
    }

    @Test
    fun `reset password request for existing user`() = runTest {
        // Arrange
        val jsonResponse = """
            {
                "mensaje": "SMS enviado a $testPhone",
                "telefono": "$testPhone"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(jsonResponse)
        )

        // Act
        val result = authRepository.requestPasswordReset(testPhone)

        // Assert
        assertTrue(result is ResultWrapper.Success)
        val success = result as ResultWrapper.Success
        assertTrue(success.data.mensaje.contains("SMS enviado"))
        assertEquals(testPhone, success.data.telefono)
    }

    @Test
    fun `network error returns appropriate wrapper`() = runTest {
        // Arrange
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        // Act
        val result = authRepository.login(LoginRequest(testPhone, testPassword))

        // Assert
        assertTrue(result is ResultWrapper.Error)
        val error = result as ResultWrapper.Error
        assertTrue(error.message.contains("Error"))
    }
}
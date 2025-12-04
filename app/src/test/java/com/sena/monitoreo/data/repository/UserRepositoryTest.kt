package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.ApiUser
import com.sena.monitoreo.data.model.user.UpdateEstadoRequest
import com.sena.monitoreo.data.model.user.UserResponse
import com.sena.monitoreo.utils.ResultWrapper
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class UserRepositoryTest {
    private val mockApi = mockk<ApiUser>()
    private val repository = UserRepository(mockApi)

    @Test
    fun `getAllUsers should return list of users`() = runTest {
        // Arrange
        val expectedUsers = listOf(
            UserResponse(
                id = 1,
                nombre = "User 1",
                telefono = "123",
                rol = "ADMIN",
                estado = "ACTIVO",
                conectado = true,
                ultima_conexion = "2024-01-01"
            ),
            UserResponse(
                id = 2,
                nombre = "User 2",
                telefono = "456",
                rol = "USER",
                estado = "BLOQUEADO",
                conectado = false,
                ultima_conexion = "2024-01-01"
            )
        )

        coEvery { mockApi.getAllUsers() } returns Response.success(expectedUsers)

        // Act
        val result = repository.getAllUsers()

        // Assert
        assertTrue(result is ResultWrapper.Success)
        assertEquals(2, (result as ResultWrapper.Success).data.size)
        assertEquals("ADMIN", result.data[0].rol)
    }

    @Test
    fun `updateEstado should return success on valid update`() = runTest {
        // Arrange
        val userId = 1
        val nuevoEstado = "BLOQUEADO"

        coEvery { mockApi.updateEstado(userId, any()) } returns Response.success(Unit)

        // Act
        val result = repository.updateEstado(userId, nuevoEstado)

        // Assert
        assertTrue(result is ResultWrapper.Success)
    }
}
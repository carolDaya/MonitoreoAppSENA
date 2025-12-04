package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.ApiAiService
import com.sena.monitoreo.data.model.ai.AnalisisResponse
import com.sena.monitoreo.utils.ResultWrapper
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.test.runTest
import retrofit2.Response

class AnalisisRepositoryTest {
    private val mockApi = mockk<ApiAiService>()
    private val repository = AnalisisRepository(mockApi)

    @Test
    @JvmName("analizarLecturaSuccess")
    fun `analizarLectura should return success when API call succeeds`() = runTest {
        // Arrange
        val expectedResponse = AnalisisResponse(
            alerta_ia = 1,
            dia_proceso = 5,
            mensaje_lectura = "Normal",
            recomendacion = "Continuar monitoreo",
            tipo_alerta_modelo = "BAJA",
            tipo_estado = "ESTABLE"
        )
        val mockResponse = Response.success(expectedResponse)

        coEvery { mockApi.analizarDatos() } returns mockResponse

        // Act
        val result = repository.analizarLectura()

        // Assert
        assertTrue(result is ResultWrapper.Success)
        assertEquals(expectedResponse, (result as ResultWrapper.Success).data)
    }

    @Test
    fun `analizarLectura should return error when API call fails`() = runTest {
        // Arrange
        coEvery { mockApi.analizarDatos() } throws Exception("Network error")

        // Act
        val result = repository.analizarLectura()

        // Assert
        assertTrue(result is ResultWrapper.Error)
    }
}
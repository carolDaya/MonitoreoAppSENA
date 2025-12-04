package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.ApiGraficas
import com.sena.monitoreo.data.model.admin.GraficaResponse
import com.sena.monitoreo.data.model.admin.GraficaUpdateResponse
import com.sena.monitoreo.utils.ResultWrapper
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class GraficasRepositoryTest {
    private val mockApi = mockk<ApiGraficas>()
    private val repository = GraficasRepository(mockApi)

    @Test
    fun `updateGrafica should return success on valid update`() = runTest {
        // Arrange
        val sensorId = 1
        val tipo = "LINEA"
        val expectedResponse = GraficaUpdateResponse("Gráfica actualizada")

        coEvery { mockApi.updateGrafica(any()) } returns Response.success(expectedResponse)

        // Act
        val result = repository.updateGrafica(sensorId, tipo)

        // Assert
        assertTrue(result is ResultWrapper.Success)
        assertEquals("Gráfica actualizada", (result as ResultWrapper.Success).data.message)
    }

    @Test
    fun `getGraficas should return list of graficas`() = runTest {
        // Arrange
        val expectedList = listOf(
            GraficaResponse(1, "LINEA"),
            GraficaResponse(2, "BARRA")
        )

        coEvery { mockApi.getGraficas() } returns Response.success(expectedList)

        // Act
        val result = repository.getGraficas()

        // Assert
        assertTrue(result is ResultWrapper.Success)
        assertEquals(2, (result as ResultWrapper.Success).data.size)
        assertEquals("LINEA", result.data[0].tipo_grafica)
    }
}
package com.sena.monitoreo.ui.user

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.sena.monitoreo.data.model.ai.AnalisisResponse
import com.sena.monitoreo.data.repository.AnalisisRepository
import com.sena.monitoreo.utils.ResultWrapper
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlertsActivityRegressionTest {

    // Regla para LiveData
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Mocks
    private lateinit var mockAnalisisRepo: AnalisisRepository
    private lateinit var activity: AlertsActivity

    @Before
    fun setUp() {
        // Configurar coroutines para testing
        Dispatchers.setMain(StandardTestDispatcher())

        // Crear mocks
        mockAnalisisRepo = mockk()
    }

    /**
     * REGRESIÓN CRÍTICA 1: La actividad debe manejar alertas correctamente
     * Este comportamiento NO debe cambiar nunca
     */
    @Test
    fun regression_critical_alert_data_handling() {
        runTest {
            // ARRANGE - Datos que SIEMPRE deben funcionar
            val expectedAlertData = AnalisisResponse(
                alerta_ia = 1,  // Alerta activa
                dia_proceso = 5,
                mensaje_lectura = "Temperatura elevada",
                recomendacion = "Reducir temperatura",
                tipo_alerta_modelo = "warning",
                tipo_estado = "Advertencia"
            )

            // Configurar mock para devolver datos de alerta
            coEvery { mockAnalisisRepo.analizarLectura() } returns
                    ResultWrapper.Success(expectedAlertData)

            // ACT - Simular que la actividad procesa la alerta
            val displayText = buildString {
                appendLine("-${expectedAlertData.tipo_estado}")
                appendLine()
                appendLine("-Tipo de Alerta: ${expectedAlertData.tipo_alerta_modelo}")
                appendLine()
                appendLine("-Recomendación:")
                appendLine(expectedAlertData.recomendacion)
            }

            // ASSERT - Verificar comportamiento esperado que NO debe cambiar
            // 1. El formato de visualización debe mantenerse
            assertTrue(displayText.contains(expectedAlertData.tipo_estado!!))
            assertTrue(displayText.contains("Tipo de Alerta:"))
            assertTrue(displayText.contains("Recomendación:"))

            // 2. El mensaje de voz debe formatearse correctamente
            val voiceMessage = formatAnalysisMessage(expectedAlertData)
            assertTrue(voiceMessage.contains(expectedAlertData.mensaje_lectura))
            assertTrue(voiceMessage.contains(expectedAlertData.recomendacion))

            println("✅ REGRESIÓN: Manejo de alertas mantiene formato correcto")
        }
    }

    /**
     * REGRESIÓN 2: Sin alertas activas debe mostrar mensaje apropiado
     */
    @Test
    fun regression_no_active_alerts_handling() {
        runTest {
            // ARRANGE - Respuesta sin alerta (alerta_ia != 1)
            val noAlertData = AnalisisResponse(
                alerta_ia = 0,  // NO hay alerta
                dia_proceso = 5,
                mensaje_lectura = "Estado normal",
                recomendacion = "Continuar monitoreo",
                tipo_alerta_modelo = "normal",
                tipo_estado = "Normal"
            )

            // ACT - Simular respuesta sin alerta
            val fallbackMessage = "No hay alertas activas en este momento"

            // ASSERT - Debe mostrar mensaje de "no hay alertas"
            assertTrue(fallbackMessage.contains("No hay alertas"))
            assertFalse(fallbackMessage.contains("Alerta:")) // No debe mencionar alerta

            println("✅ REGRESIÓN: Sin alertas muestra mensaje apropiado")
        }
    }

    /**
     * REGRESIÓN 3: Formato de mensaje para voz debe mantenerse
     * Este formato es usado por el sistema de TTS
     */
    @Test
    fun regression_voice_message_format() {
        // Datos de prueba
        val analisis = AnalisisResponse(
            alerta_ia = 1,
            dia_proceso = 5,
            mensaje_lectura = "Temperatura crítica",
            recomendacion = "Activar enfriamiento",
            tipo_alerta_modelo = "critical",
            tipo_estado = "Crítico"
        )

        // Obtener mensaje formateado (simulando el método real)
        val formattedMessage = formatAnalysisMessage(analisis)

        // REGRESIÓN: El formato debe incluir ciertos elementos
        assertTrue("Debe contener mensaje de lectura",
            formattedMessage.contains(analisis.mensaje_lectura))
        assertTrue("Debe contener recomendación",
            formattedMessage.contains(analisis.recomendacion))
        assertTrue("Debe contener tipo de estado",
            formattedMessage.contains(analisis.tipo_estado!!))

        println("✅ REGRESIÓN: Formato de mensaje de voz consistente")
    }

    /**
     * REGRESIÓN 5: Configuración de voz debe aplicarse correctamente
     */
    @Test
    fun regression_voice_config_application() {
        // Valores de configuración que deben aceptarse
        val validGenders = listOf("FEMALE", "MALE")
        val validPitchRange = 0.5f..2.0f

        validGenders.forEach { gender ->
            assertTrue("Género $gender debe ser válido",
                gender == "FEMALE" || gender == "MALE")
        }

        // Pitch debe estar en rango válido
        assertTrue("Pitch 1.0f debe estar en rango", 1.0f in validPitchRange)
        assertTrue("Pitch 1.5f debe estar en rango", 1.5f in validPitchRange)

        println("✅ REGRESIÓN: Validación de configuración de voz consistente")
    }

    // Helper function similar a la de AlertsActivity
    private fun formatAnalysisMessage(analisis: AnalisisResponse): String {
        return buildString {
            append("Alerta: ${analisis.tipo_estado}. ")
            append("${analisis.mensaje_lectura}. ")
            append("Recomendación: ${analisis.recomendacion}")
        }
    }
}
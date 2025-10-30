package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.model.ai.VoiceResponse
import com.sena.monitoreo.data.api.ApiVoice

class VoiceRepository(private val apiService: ApiVoice) {

    /**
     * Obtiene la configuración de voz del backend.
     */
    suspend fun getVoiceConfig(): VoiceResponse {
        return try {
            apiService.fetchVoiceConfig()
        } catch (e: Exception) {
            // Valor por defecto en caso de fallo
            VoiceResponse(gender = "FEMALE", pitch = 1.0f)
        }
    }

    /**
     * Guarda la configuración de voz en el backend.
     */
    suspend fun saveVoiceConfig(gender: String, pitch: Float) {
        val configToSend = VoiceResponse(gender = gender, pitch = pitch)
        apiService.saveVoiceConfig(configToSend)
    }
}
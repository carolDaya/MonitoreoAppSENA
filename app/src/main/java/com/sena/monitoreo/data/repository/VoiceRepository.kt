package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.model.ai.VoiceResponse
import com.sena.monitoreo.data.api.ApiVoice

class VoiceRepository(private val apiService: ApiVoice) {

    suspend fun getVoiceConfig(): VoiceResponse {
        return try {
            apiService.fetchVoiceConfig()
        } catch (e: Exception) {
            // Valor por defecto en caso de error
            VoiceResponse(gender = "FEMALE", pitch = 1.0f)
        }
    }

    suspend fun saveVoiceConfig(gender: String, pitch: Float) {
        val config = VoiceResponse(gender = gender, pitch = pitch)
        apiService.saveVoiceConfig(config)
    }
}

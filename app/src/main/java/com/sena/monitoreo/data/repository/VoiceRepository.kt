package com.sena.monitoreo.data.repository

import com.sena.monitoreo.data.api.ApiVoice
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.voice.VoiceConfigResponse
import com.sena.monitoreo.utils.ResultWrapper
import com.sena.monitoreo.utils.safeApiCall

/**
 * Repositorio para la gestión de la configuración de voz
 */
class VoiceRepository(
    private val apiVoice: ApiVoice = RetrofitClient.apiVoice
) {

    /**
     * Obtiene la configuración de voz actual.
     */
    suspend fun getVoiceConfig(): ResultWrapper<VoiceConfigResponse> {
        return safeApiCall {
            apiVoice.getVoiceConfig()
        }
    }

    /**
     * Guarda o actualiza la configuración de voz.
     *
     * @param config El objeto que contiene el gender y el pitch.
     */
    suspend fun saveVoiceConfig(config: VoiceConfigResponse): ResultWrapper<VoiceConfigResponse> {
        return safeApiCall {
            apiVoice.saveVoiceConfig(config)
        }
    }
}
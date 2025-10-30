package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.ai.VoiceResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiVoice {

    /**
     * Endpoint GET: Obtener la configuración actual.
     */
    @GET("voice")
    suspend fun fetchVoiceConfig(): VoiceResponse // Usa el data class

    /**
     * Endpoint POST: Guardar/Actualizar la configuración.
     */
    @POST("voice")
    suspend fun saveVoiceConfig(@Body config: VoiceResponse): VoiceResponse // Usa el data class
}
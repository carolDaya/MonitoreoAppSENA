package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.voice.VoiceConfigResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiVoice {

    /**
     * Obtiene la configuración de voz almacenada.
     * @return Response con la configuración actual de voz.
     */
    @GET("voice")
    suspend fun getVoiceConfig(): Response<VoiceConfigResponse>

    /**
     * Guarda o actualiza la configuración de voz.
     * @param config Datos de configuración de voz a guardar.
     * @return Response con la configuración guardada.
     */
    @POST("voice")
    suspend fun saveVoiceConfig(@Body config: VoiceConfigResponse): Response<VoiceConfigResponse>

}

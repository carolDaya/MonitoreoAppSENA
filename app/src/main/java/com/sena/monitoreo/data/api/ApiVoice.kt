package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.voice.VoiceConfigResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiVoice {

    @GET("voice")
    suspend fun getVoiceConfig(): Response<VoiceConfigResponse>

    @POST("voice")
    suspend fun saveVoiceConfig(@Body config: VoiceConfigResponse): Response<VoiceConfigResponse>

}
package com.sena.monitoreo.data.model.voice

import com.google.gson.annotations.SerializedName

/**
 * DTO para la respuesta y la solicitud de configuración de voz.
 */
data class VoiceConfigResponse(
    @SerializedName("voice_gender")
    val voiceGender: String, // e.g., "FEMALE", "MALE"

    @SerializedName("voice_pitch")
    val voicePitch: Double // e.g., 1.0
)

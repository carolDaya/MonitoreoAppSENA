package com.sena.monitoreo.data.model.ai

import com.google.gson.annotations.SerializedName

/**
 * Data class que representa la configuración de voz recibida/enviada desde el backend.
 */
data class VoiceResponse(

    // Mapea 'voice_gender' del JSON (Python) a 'gender' (Kotlin)
    @SerializedName("voice_gender")
    val gender: String, // Valores: "FEMALE", "MALE", "ROBOTIC"

    // Mapea 'voice_pitch' del JSON (Python) a 'pitch' (Kotlin)
    @SerializedName("voice_pitch")
    val pitch: Float    //Valores: 0.8 (Grave), 1.0 (Normal), 1.3 (Aguda)
)
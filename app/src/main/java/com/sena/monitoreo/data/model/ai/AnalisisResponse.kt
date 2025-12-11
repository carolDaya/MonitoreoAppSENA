package com.sena.monitoreo.data.model.ai

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// Asegúrate de que todas las propiedades sean val
data class AnalisisResponse(
    @SerializedName("ale    rta_ia") val alerta_ia: Int,
    @SerializedName("dia_proceso") val dia_proceso: Int? = null,
    @SerializedName("mensaje_lectura") val mensaje_lectura: String,
    @SerializedName("recomendacion") val recomendacion: String,
    @SerializedName("tipo_alerta_modelo") val tipo_alerta_modelo: String,
    @SerializedName("tipo_estado") val tipo_estado: String
) : Serializable { // ✅ Añade esto

    // Constructor secundario para compatibilidad si es necesario
    constructor() : this(0, null, "", "", "", "")

    // También puedes añadir toString para debugging
    override fun toString(): String {
        return "AnalisisResponse(alerta_ia=$alerta_ia, mensaje='$mensaje_lectura', recomendacion='$recomendacion')"
    }
}
package com.sena.monitoreo.data.repository

import android.util.Log
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.ai.AnalisisResponse

class AnalisisRepository {

    private val apiService = RetrofitClient.apiAi
    private val TAG = "AnalisisRepository"

    suspend fun analizarLectura(): AnalisisResponse? {

        try {
            // Llamada simple al servidor sin parámetros
            val response = apiService.analizarDatos()

            if (response.isSuccessful && response.body() != null) {
                return response.body()
            } else {
                Log.e(TAG, "Error en la respuesta de la IA: ${response.code()}. Mensaje: ${response.errorBody()?.string()}")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error de red al llamar a la IA: ${e.message}")
            return null
        }
    }
}
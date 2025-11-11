package com.sena.monitoreo.data.repository

import android.util.Log
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.sensor.LecturaResponse

class LecturaRepository {
    private val api = RetrofitClient.apiLecturas
    private val TAG = "LecturaRepository"

    /**
     * Obtiene las lecturas de un sensor.
     * El backend (Flask) se encarga de FILTRAR automáticamente por el proceso activo.
     */
    suspend fun getLecturas(sensorId: Int): List<LecturaResponse> {
        return try {
            // Llama al endpoint /lecturas/{sensorId} que ahora es filtrado por proceso activo
            val response = api.getLecturasPorSensor(sensorId)
            if (response.isSuccessful) {
                val lecturas = response.body() ?: emptyList()

                // Si lecturas es vacía, significa: A) Proceso inactivo O B) Proceso activo sin datos aún.
                Log.d(TAG, "✅ Lecturas obtenidas para sensor $sensorId: ${lecturas.size}")
                lecturas
            } else {
                Log.e(TAG, "❌ Error al obtener lecturas: ${response.code()}. Cuerpo: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción al obtener lecturas para sensor $sensorId", e)
            emptyList()
        }
    }
}
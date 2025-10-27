package com.sena.monitoreo.data.repository

import android.util.Log
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.sensor.LecturaResponse // Importamos el modelo

class LecturaRepository {
    private val api = RetrofitClient.apiLecturas // Usa el cliente nuevo
    private val TAG = "LecturaRepository"

    // 👇 FUNCIÓN REQUERIDA EN SensorDataActivity
    suspend fun getLecturas(sensorId: Int): List<LecturaResponse> {
        return try {
            val response = api.getLecturasPorSensor(sensorId)
            if (response.isSuccessful) {
                val lecturas = response.body() ?: emptyList()
                Log.d(TAG, "✅ Lecturas obtenidas para sensor $sensorId: ${lecturas.size}")
                lecturas
            } else {
                Log.e(TAG, "❌ Error al obtener lecturas: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción al obtener lecturas para sensor $sensorId", e)
            emptyList()
        }
    }
}
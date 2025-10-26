package com.sena.monitoreo.data.repository


import android.util.Log
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.admin.GraficaResponse
import com.sena.monitoreo.data.model.admin.GraficaUpdateRequest
import com.sena.monitoreo.data.model.admin.GraficaUpdateResponse
import retrofit2.http.GET

class GraficasRepository {
    private val api = RetrofitClient.apiGraficas
    private val TAG = "GraficasRepository"

    suspend fun updateGrafica(sensorId: Int, tipo: String): GraficaUpdateResponse? {
        return try {
            val response = api.updateGrafica(GraficaUpdateRequest(sensorId, tipo))
            if (response.isSuccessful) {
                Log.d(TAG, "✅ Gráfica actualizada: sensor=$sensorId, tipo=$tipo")
                response.body()
            } else {
                Log.e(TAG, "❌ Error al actualizar: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción al actualizar gráfica", e)
            null
        }
    }

    suspend fun getGraficas(): List<GraficaResponse> {
        return try {
            val response = api.getGraficas()
            if (response.isSuccessful) {
                val graficas = response.body() ?: emptyList()
                Log.d(TAG, "✅ Gráficas obtenidas: ${graficas.size}")
                graficas
            } else {
                Log.e(TAG, "❌ Error al obtener gráficas: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción al obtener gráficas", e)
            emptyList()
        }
    }


}
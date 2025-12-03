// Crear archivo: utils/cache/SensorCache.kt
package com.sena.monitoreo.utils.cache

import com.sena.monitoreo.data.model.sensor.LecturaResponse
import android.util.Log

object SensorCache {
    private const val TAG = "SensorCache"
    private const val CACHE_DURATION = 25_000L // 25 segundos (menos que tu refresh de 30s)

    data class CachedData(
        val lecturas: List<LecturaResponse>,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val cache = mutableMapOf<Int, CachedData>()

    /**
     * Obtener lecturas desde caché si están frescas
     */
    fun get(sensorId: Int): List<LecturaResponse>? {
        val cached = cache[sensorId] ?: return null
        val age = System.currentTimeMillis() - cached.timestamp

        return if (age < CACHE_DURATION) {
            Log.d(TAG, "✅ Cache HIT para sensor $sensorId (edad: ${age}ms)")
            cached.lecturas
        } else {
            Log.d(TAG, "⏰ Cache EXPIRED para sensor $sensorId (edad: ${age}ms)")
            cache.remove(sensorId)
            null
        }
    }

    /**
     * Guardar lecturas en caché
     */
    fun put(sensorId: Int, lecturas: List<LecturaResponse>) {
        cache[sensorId] = CachedData(lecturas)
        Log.d(TAG, "💾 Cache SAVED para sensor $sensorId (${lecturas.size} lecturas)")
    }

    /**
     * Limpiar caché de un sensor específico
     */
    fun invalidate(sensorId: Int) {
        cache.remove(sensorId)
        Log.d(TAG, "🗑️ Cache INVALIDATED para sensor $sensorId")
    }

    /**
     * Limpiar todo el caché
     */
    fun clear() {
        cache.clear()
        Log.d(TAG, "🗑️ Cache CLEARED")
    }

    /**
     * Verificar si el caché está fresco
     */
    fun isFresh(sensorId: Int): Boolean {
        val cached = cache[sensorId] ?: return false
        val age = System.currentTimeMillis() - cached.timestamp
        return age < CACHE_DURATION
    }
}
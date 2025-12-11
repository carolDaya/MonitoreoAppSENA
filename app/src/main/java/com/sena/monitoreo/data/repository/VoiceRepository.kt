package com.sena.monitoreo.data.repository

import android.util.Log
import com.sena.monitoreo.data.api.ApiVoice
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.voice.VoiceConfigResponse
import com.sena.monitoreo.utils.ResultWrapper
import com.sena.monitoreo.utils.safeApiCall

/**
 * Repositorio para la gestión de la configuración de voz
 */
class VoiceRepository(
    private val apiVoice: ApiVoice = RetrofitClient.apiVoice
) {
    private val TAG = "VoiceRepository"

    // Cache simple en memoria
    private var cachedConfig: VoiceConfigResponse? = null
    private var lastCacheTime: Long = 0
    private val CACHE_DURATION = 5 * 60 * 1000L // 5 minutos de caché

    /**
     * Obtiene la configuración de voz actual.
     * @param forceRefresh Si es true, ignora el caché y llama a la API
     */
    suspend fun getVoiceConfig(forceRefresh: Boolean = false): ResultWrapper<VoiceConfigResponse> {
        // Si se fuerza refresh, limpiar caché y llamar directamente a API
        if (forceRefresh) {
            Log.d(TAG, "🔄 Forzando recarga de configuración de voz (ignorando caché)")
            cachedConfig = null
            lastCacheTime = 0
        }

        // Verificar si el caché es fresco
        val cacheAge = System.currentTimeMillis() - lastCacheTime
        if (cachedConfig != null && cacheAge < CACHE_DURATION && !forceRefresh) {
            Log.d(TAG, "⚡ Usando configuración de voz desde caché (${cacheAge}ms old)")
            return ResultWrapper.Success(cachedConfig!!)
        }

        // Si no hay caché, está expirado, o se fuerza refresh, llamar a la API
        Log.d(TAG, "📡 Obteniendo configuración de voz desde API...")
        return safeApiCall {
            apiVoice.getVoiceConfig()
        }.also { result ->
            // Cachear resultado exitoso
            if (result is ResultWrapper.Success) {
                cachedConfig = result.data
                lastCacheTime = System.currentTimeMillis()
                Log.d(TAG, "✅ Configuración de voz obtenida y cacheada: " +
                        "Pitch=${result.data.voicePitch}, Gender=${result.data.voiceGender}")
            } else if (result is ResultWrapper.Error) {
                Log.e(TAG, "❌ Error obteniendo configuración de voz: ${result.message}")
            }
        }
    }

    /**
     * Guarda o actualiza la configuración de voz.
     * @param config El objeto que contiene el gender y el pitch.
     */
    suspend fun saveVoiceConfig(config: VoiceConfigResponse): ResultWrapper<VoiceConfigResponse> {
        return safeApiCall {
            apiVoice.saveVoiceConfig(config)
        }.also { result ->
            // Si se guarda exitosamente, actualizar caché
            if (result is ResultWrapper.Success) {
                cachedConfig = result.data
                lastCacheTime = System.currentTimeMillis()
                Log.d(TAG, "💾 Configuración de voz guardada y cacheada: " +
                        "Pitch=${result.data.voicePitch}, Gender=${result.data.voiceGender}")
            } else if (result is ResultWrapper.Error) {
                Log.e(TAG, "❌ Error guardando configuración de voz: ${result.message}")
            }
        }
    }

    /**
     * 🔄 Limpiar caché (para recargas forzadas)
     */
    fun clearCache() {
        cachedConfig = null
        lastCacheTime = 0
        Log.d(TAG, "🧹 Cache de voz limpiado")
    }

    /**
     * 📥 Obtener configuración cacheada (para verificaciones)
     */
    fun getCachedConfig(): VoiceConfigResponse? {
        val cacheAge = System.currentTimeMillis() - lastCacheTime
        return if (cachedConfig != null && cacheAge < CACHE_DURATION) {
            cachedConfig
        } else {
            null // Cache expirado o no existe
        }
    }

    /**
     * 🔍 Verificar si hay caché fresco disponible
     */
    fun hasFreshCache(): Boolean {
        val cacheAge = System.currentTimeMillis() - lastCacheTime
        return cachedConfig != null && cacheAge < CACHE_DURATION
    }

    /**
     * ⏱️ Obtener edad del caché en milisegundos
     */
    fun getCacheAge(): Long {
        return System.currentTimeMillis() - lastCacheTime
    }
}
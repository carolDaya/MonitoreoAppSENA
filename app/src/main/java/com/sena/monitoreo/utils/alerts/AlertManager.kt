package com.sena.monitoreo.utils.alerts

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.sena.monitoreo.data.exception.ApiException.NetworkError
import com.sena.monitoreo.data.model.ai.AnalisisResponse
import com.sena.monitoreo.data.repository.AnalisisRepository
import com.sena.monitoreo.utils.ResultWrapper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlertManager(
    private val context: Context,
    private val analisisRepo: AnalisisRepository,
    private val onAlertDetected: (AnalisisResponse) -> Unit,
    private val onError: (String) -> Unit
) {
    private val TAG = "AlertManager"
    private val PREFS_NAME = "AlertaPrefs"
    private val KEY_LAST_ALERT_TIME = "last_alert_time"
    private val ALERT_COOLDOWN_HOURS = 2L
    private val ALERT_CHECK_INTERVAL = 10 * 60 * 1000L

    private var alertCheckJob: Job? = null
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun startPeriodicAlertCheck(scope: LifecycleCoroutineScope) {
        alertCheckJob = scope.launch {
            while (true) {
                Log.d(TAG, "Verificación periódica de alertas...")
                checkAndHandleAlert()
                delay(ALERT_CHECK_INTERVAL)
            }
        }
    }

    fun stopPeriodicAlertCheck() {
        alertCheckJob?.cancel()
        alertCheckJob = null
        Log.d(TAG, "Verificación periódica de alertas detenida.")
    }

    suspend fun checkAndHandleAlert() {
        val lastAlertTimeMillis = prefs.getLong(KEY_LAST_ALERT_TIME, 0L)
        val currentTimeMillis = System.currentTimeMillis()

        val hoursSinceLastAlert = (currentTimeMillis - lastAlertTimeMillis) / (1000.0 * 60 * 60)
        val shouldCheck = lastAlertTimeMillis == 0L || hoursSinceLastAlert >= ALERT_COOLDOWN_HOURS

        if (shouldCheck) {
            Log.d(TAG, "Consultando backend para análisis...")

            try {
                when (val analisisResult = analisisRepo.analizarLectura()) {
                    is ResultWrapper.Success -> {
                        val analisis = analisisResult.data

                        // DEBUG COMPLETO DE LA RESPUESTA
                        Log.d(TAG, "📊 RESPUESTA DEL BACKEND:")
                        Log.d(TAG, "  alerta_ia: ${analisis.alerta_ia}")
                        Log.d(TAG, "  tipo: ${analisis.alerta_ia?.javaClass?.simpleName}")
                        Log.d(TAG, "  mensaje_lectura: ${analisis.mensaje_lectura}")
                        Log.d(TAG, "  recomendacion: ${analisis.recomendacion}")
                        Log.d(TAG, "  tipo_alerta_modelo: ${analisis.tipo_alerta_modelo}")
                        Log.d(TAG, "  tipo_estado: ${analisis.tipo_estado}")

                        // VERIFICACIÓN MÁS FLEXIBLE
                        val isAlert = when {
                            // Caso 1: alerta_ia como Int
                            analisis.alerta_ia == 1 -> {
                                Log.d(TAG, "✅ alerta_ia == 1 (Int)")
                                true
                            }
                            // Caso 3: Si tiene mensaje de alerta en tipo_estado
                            analisis.tipo_estado?.contains("Alerta", ignoreCase = true) == true -> {
                                Log.d(TAG, "✅ tipo_estado contiene 'Alerta'")
                                true
                            }
                            // Caso 4: Si tiene "Anormal" en tipo_alerta_modelo
                            analisis.tipo_alerta_modelo?.contains("Anormal", ignoreCase = true) == true -> {
                                Log.d(TAG, "✅ tipo_alerta_modelo contiene 'Anormal'")
                                true
                            }
                            // Caso 5: Si hay recomendación (indicativo de alerta)
                            !analisis.recomendacion.isNullOrEmpty() &&
                                    analisis.recomendacion != "Sin recomendaciones" -> {
                                Log.d(TAG, "✅ Tiene recomendación: ${analisis.recomendacion}")
                                true
                            }
                            else -> {
                                Log.d(TAG, "❌ No cumple criterios de alerta")
                                false
                            }
                        }

                        if (isAlert) {
                            Log.d(TAG, "🚨 ALERTA DETECTADA! Enviando notificación...")

                            // Guardar el tiempo para iniciar el cooldown
                            prefs.edit().putLong(KEY_LAST_ALERT_TIME, currentTimeMillis).apply()

                            // Enviar alerta
                            onAlertDetected(analisis)
                        } else {
                            Log.d(TAG, "✅ Sistema normal. alerta_ia=${analisis.alerta_ia}")
                        }
                    }
                    is ResultWrapper.Error -> {
                        val errorType = if (analisisResult.exception is NetworkError) "RED" else "API"
                        Log.e(TAG, "❌ Error de $errorType: ${analisisResult.message}")
                        onError(analisisResult.message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 Excepción en checkAndHandleAlert: ${e.message}", e)
                onError("Error interno: ${e.message}")
            }
        } else {
            val remainingHours = ALERT_COOLDOWN_HOURS - hoursSinceLastAlert
            Log.d(TAG, "⏳ Cooldown activo. Faltan ${"%.2f".format(remainingHours)} horas.")
        }
    }

    fun resetCooldown() {
        prefs.edit().remove(KEY_LAST_ALERT_TIME).apply()
        Log.d(TAG, "Cooldown reseteado")
    }

    fun forceAlertCheck(scope: LifecycleCoroutineScope) {
        scope.launch {
            resetCooldown()
            checkAndHandleAlert()
        }
    }
}
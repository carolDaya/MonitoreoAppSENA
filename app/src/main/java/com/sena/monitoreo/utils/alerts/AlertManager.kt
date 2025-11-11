// AlertManager.kt
package com.sena.monitoreo.utils.alerts

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.data.model.ai.AnalisisResponse
import com.sena.monitoreo.data.repository.AnalisisRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

class AlertManager(
    private val context: Context,
    private val analisisRepo: AnalisisRepository,
    private val onAlertDetected: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val TAG = "AlertManager"
    private val PREFS_NAME = "AlertaPrefs"
    private val KEY_LAST_ALERT_TIME = "last_alert_time"
    private val ALERT_COOLDOWN_HOURS = 2L
    private val ALERT_CHECK_INTERVAL = 10 * 60 * 1000L // 10 minutos

    private var alertCheckJob: Job? = null
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun startPeriodicAlertCheck(scope: androidx.lifecycle.LifecycleCoroutineScope) {
        alertCheckJob = scope.launch {
            while (true) {
                delay(ALERT_CHECK_INTERVAL)
                Log.d(TAG, "🔄 Verificación periódica de alertas...")
                checkAndHandleAlert()
            }
        }
    }

    fun stopPeriodicAlertCheck() {
        alertCheckJob?.cancel()
        alertCheckJob = null
    }

    suspend fun checkAndHandleAlert() {
        val lastAlertTimeMillis = prefs.getLong(KEY_LAST_ALERT_TIME, 0L)
        val currentTimeMillis = System.currentTimeMillis()
        val hoursSinceLastAlert = (currentTimeMillis - lastAlertTimeMillis) / (1000 * 60 * 60).toDouble()
        val shouldCheck = lastAlertTimeMillis == 0L || hoursSinceLastAlert >= ALERT_COOLDOWN_HOURS

        if (shouldCheck) {
            Log.d(TAG, "✅ Consultando backend para análisis...")
            val analisisResult = analisisRepo.analizarLectura()
            if (analisisResult.success != null) {
                val analisis = analisisResult.success
                if (analisis.alerta_ia == 1) {
                    Log.d(TAG, "⚠️ Alerta activa detectada: ${analisis.mensaje_lectura}")
                    prefs.edit().putLong(KEY_LAST_ALERT_TIME, currentTimeMillis).apply()
                    onAlertDetected(analisis.mensaje_lectura)
                } else {
                    Log.d(TAG, "✅ Sistema normal. Sin alertas activas.")
                }
            } else {
                val errorMessage = analisisResult.errorMessage ?: "Error desconocido al verificar alertas."
                Log.e(TAG, "❌ Error: No se pudo obtener análisis: $errorMessage")
                onError(errorMessage)
            }
        } else {
            Log.d(TAG, "⏸️ Cooldown activo. Faltan ${ALERT_COOLDOWN_HOURS - hoursSinceLastAlert} horas.")
        }
    }

    fun resetCooldown() {
        prefs.edit().remove(KEY_LAST_ALERT_TIME).apply()
        Log.d(TAG, "🧪 Cooldown reseteado")
    }

    fun forceAlertCheck(scope: androidx.lifecycle.LifecycleCoroutineScope) {
        scope.launch {
            resetCooldown()
            checkAndHandleAlert()
        }
    }
}
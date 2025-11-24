package com.sena.monitoreo.utils.alerts

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.sena.monitoreo.data.exception.ApiException.NetworkError
import com.sena.monitoreo.data.repository.AnalisisRepository
import com.sena.monitoreo.utils.ResultWrapper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    private val ALERT_CHECK_INTERVAL = 10 * 60 * 1000L

    private var alertCheckJob: Job? = null
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Inicia la corrutina que verifica periódicamente si se debe lanzar una alerta.
     * Utiliza un ámbito de Lifecycle para detenerse automáticamente si el dueño del ciclo de vida se destruye.
     */
    fun startPeriodicAlertCheck(scope: LifecycleCoroutineScope) {
        alertCheckJob = scope.launch {
            while (true) {
                Log.d(TAG, "🔄 Verificación periódica de alertas...")
                checkAndHandleAlert()
                delay(ALERT_CHECK_INTERVAL)
            }
        }
    }

    /**
     * Detiene la verificación periódica de alertas cancelando el Job de la corrutina.
     */
    fun stopPeriodicAlertCheck() {
        alertCheckJob?.cancel()
        alertCheckJob = null
        Log.d(TAG, "❌ Verificación periódica de alertas detenida.")
    }

    /**
     * Realiza la lógica de verificación: respeta el Cooldown y consulta la API si es necesario.
     */
    suspend fun checkAndHandleAlert() {
        val lastAlertTimeMillis = prefs.getLong(KEY_LAST_ALERT_TIME, 0L)
        val currentTimeMillis = System.currentTimeMillis()

        // Convertir la diferencia de tiempo a horas (en double para precisión)
        val hoursSinceLastAlert = (currentTimeMillis - lastAlertTimeMillis) / (1000.0 * 60 * 60)

        // Solo verificar si nunca se ha hecho o si el tiempo de cooldown ha pasado
        val shouldCheck = lastAlertTimeMillis == 0L || hoursSinceLastAlert >= ALERT_COOLDOWN_HOURS

        if (shouldCheck) {
            Log.d(TAG, "✅ Consultando backend para análisis...")

            when (val analisisResult = analisisRepo.analizarLectura()) {
                is ResultWrapper.Success -> {
                    val analisis = analisisResult.data

                    // Verificar si la IA detectó una alerta (asumiendo 1 = Alerta activa)
                    if (analisis.alerta_ia == 1) {
                        Log.d(TAG, "⚠️ Alerta activa detectada: ${analisis.mensaje_lectura}")

                        // Guardar el tiempo para iniciar el cooldown
                        prefs.edit().putLong(KEY_LAST_ALERT_TIME, currentTimeMillis).apply()
                        onAlertDetected(analisis.mensaje_lectura)
                    } else {
                        Log.d(TAG, "✅ Sistema normal. Sin alertas activas.")
                    }
                }
                is ResultWrapper.Error -> {
                    // Usar la jerarquía de ApiException para determinar el tipo de error
                    val errorType = if (analisisResult.exception is NetworkError) "RED/DESCONEXIÓN" else "API"
                    Log.e(TAG, "❌ Error de $errorType: ${analisisResult.message}")
                    onError(analisisResult.message)
                }
            }
        } else {
            val remainingHours = ALERT_COOLDOWN_HOURS - hoursSinceLastAlert
            Log.d(TAG, "⏸️ Cooldown activo. Faltan ${"%.2f".format(remainingHours)} horas.")
        }
    }

    /**
     * Elimina el registro de la última alerta para forzar la siguiente verificación.
     */
    fun resetCooldown() {
        prefs.edit().remove(KEY_LAST_ALERT_TIME).apply()
        Log.d(TAG, "🧪 Cooldown reseteado")
    }

    /**
     * Fuerza una verificación de alerta inmediatamente, reseteando el cooldown.
     */
    fun forceAlertCheck(scope: LifecycleCoroutineScope) {
        scope.launch {
            resetCooldown()
            checkAndHandleAlert()
        }
    }
}
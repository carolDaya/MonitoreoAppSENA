package com.sena.monitoreo.ui.base

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.masoudss.lib.WaveformSeekBar
import com.sena.monitoreo.R
import com.sena.monitoreo.data.model.ai.AnalisisResponse
import com.sena.monitoreo.utils.NetworkRetryListener // Importar la interfaz
import com.sena.monitoreo.utils.voice.VoiceManager
import com.sena.monitoreo.utils.voice.WaveformManager
import kotlinx.coroutines.launch

/**
 * Clase base para Activities que requieren inicialización y gestión del motor de voz (TTS).
 * Hereda de BaseActivity para obtener el manejo de errores de red.
 */
abstract class BaseVoiceActivity : BaseActivity() {

    protected lateinit var voiceManager: VoiceManager
    protected lateinit var waveformManager: WaveformManager
    protected lateinit var btnPlay: MaterialButton
    protected lateinit var waveformSeekBar: WaveformSeekBar

    var isVoiceInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeVoiceComponents()
    }

    private fun initializeVoiceComponents() {
        voiceManager = VoiceManager(this) {
            isVoiceInitialized = true
            onVoiceInitialized()
        }
        voiceManager.initialize()
    }

    protected fun setupWaveformComponents(waveformSeekBar: WaveformSeekBar, playButton: MaterialButton) {
        this.waveformSeekBar = waveformSeekBar
        this.btnPlay = playButton

        waveformManager = WaveformManager(waveformSeekBar)
        waveformManager.setupInitialSamples()

        btnPlay.setOnClickListener {
            if (!voiceManager.isSpeaking) {
                startSpeaking()
            } else {
                stopSpeaking()
            }
        }
    }

    protected open fun onVoiceInitialized() {
        // Para ser sobreescrito por actividades hijas
    }

    protected open fun startSpeaking() {
        btnPlay.setIconResource(R.drawable.ic_stop)
    }

    protected open fun stopSpeaking() {
        voiceManager.stop()
        waveformManager.stopAnimation()
        btnPlay.setIconResource(R.drawable.ic_play)
    }

    protected fun speakWithWaveform(text: String) {
        if (!isVoiceInitialized) return

        voiceManager.speak(text)
        lifecycleScope.launch {
            waveformManager.startAnimation(text.length, this) {
                stopSpeaking()
            }
        }
    }

    protected fun speakLongTextWithWaveform(fullText: String) {
        if (!isVoiceInitialized) return

        // Calcular duración estimada (más tiempo para textos largos)
        val estimatedDurationMs = (fullText.length * 100L).coerceAtLeast(5000L)

        voiceManager.speakLongText(fullText)
        lifecycleScope.launch {
            waveformManager.startLongAnimation(estimatedDurationMs, this) {
                // Verificar si aún está hablando
                if (!voiceManager.isCurrentlySpeaking()) {
                    stopSpeaking()
                }
            }
        }
    }

    protected fun speakWithPausesAndWaveform(text: String, pauseBetweenSentences: Long = 800) {
        if (!isVoiceInitialized) return

        val estimatedDurationMs = (text.length * 120L).coerceAtLeast(6000L)

        voiceManager.speakWithPauses(text, pauseBetweenSentences)
        lifecycleScope.launch {
            waveformManager.startLongAnimation(estimatedDurationMs, this) {
                if (!voiceManager.isCurrentlySpeaking()) {
                    stopSpeaking()
                }
            }
        }
    }

    protected fun speakLongTextWithContinuousWaveform(fullText: String) {
        if (!isVoiceInitialized) return

        Log.d("BaseVoiceActivity", "🔊 Iniciando reproducción larga con waveform continuo")

        // Configurar callback para cuando termine la voz
        voiceManager.setOnUtteranceCompletedListener {
            Log.d("BaseVoiceActivity", "🔊 Callback: Voz terminó, deteniendo waveform")
            lifecycleScope.launch {
                // Pequeño delay para asegurar que realmente terminó
                kotlinx.coroutines.delay(300)
                stopSpeaking()
            }
        }

        // Iniciar animación CONTINUA (no basada en tiempo)
        lifecycleScope.launch {
            waveformManager.startContinuousAnimation(this) {
                Log.d("BaseVoiceActivity", "🔊 Waveform continuo terminó")
            }
        }

        // Reproducir texto largo con callback
        voiceManager.speakLongTextWithCallback(fullText)
    }

    protected fun speakLongTextWithTimedWaveform(fullText: String) {
        if (!isVoiceInitialized) return

        // Calcular duración estimada (más conservadora)
        val estimatedDurationMs = (fullText.length * 120L).coerceAtLeast(8000L)

        // Iniciar animación por TIEMPO
        lifecycleScope.launch {
            waveformManager.startTimedAnimation(estimatedDurationMs, this) {
                // Verificar si la voz sigue hablando
                if (voiceManager.isCurrentlySpeaking()) {
                    // Si sigue hablando, continuar la animación
                    Log.d("BaseVoiceActivity", "🔊 Tiempo agotado pero voz sigue, continuando animación")
                    waveformManager.startContinuousAnimation(this)
                } else {
                    stopSpeaking()
                }
            }
        }

        // Reproducir texto largo
        voiceManager.speakLongText(fullText)
    }

    protected fun formatAnalysisMessage(analisis: AnalisisResponse): String {
        return buildString {
            append(analisis.mensaje_lectura.replace("|", " y "))
            append(". ")
            append("Recomendación del sistema: ")
            append(analisis.recomendacion)
            append(". ")
            append("Este es un: ${analisis.tipo_alerta_modelo}")
            append(". ")
            append("Estado general: ${analisis.tipo_estado}")
            append(". ")
            append("Por favor, tome las medidas necesarias.")
        }
    }

    protected fun formatShortAnalysisMessage(analisis: AnalisisResponse): String {
        return buildString {
            append("Alerta: ${analisis.tipo_estado}. ")
            append(analisis.mensaje_lectura.replace("|", " y "))
            append(". Recomendación: ${analisis.recomendacion}")
        }
    }

    fun isVoiceReady(): Boolean = isVoiceInitialized

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.release()
        waveformManager.stopAnimation()
    }
}
package com.sena.monitoreo.ui.base

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.masoudss.lib.WaveformSeekBar
import com.sena.monitoreo.R
import com.sena.monitoreo.data.model.ai.AnalisisResponse
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
    private val TAG = "BaseVoiceActivity"

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
        // Asegurarse de limpiar los listeners después de detener
        voiceManager.setOnUtteranceCompletedListener { }
        voiceManager.setOnSpeechStartedListener { } // ✅ Limpiar el nuevo listener
    }

    // MÉTODO ÚNICO PARA TEXTO CORTO
    protected fun speakWithWaveform(text: String) {
        if (!isVoiceInitialized) return
        startSpeakingAction(text, false)
    }

    // MÉTODO ÚNICO PARA TEXTO LARGO
    protected fun speakWithPausesAndWaveform(text: String, pauseBetweenSentences: Long = 800) {
        if (!isVoiceInitialized) return
        startSpeakingAction(text, true, pauseBetweenSentences)
    }

    // ✅ Lógica centralizada para iniciar la voz y el waveform continuo
    private fun startSpeakingAction(text: String, isLongText: Boolean, pause: Long = 800) {
        // 1. Configurar el callback para cuando termine la voz (detiene la animación)
        voiceManager.setOnUtteranceCompletedListener {
            Log.d(TAG, "🔊 Callback: Voz terminó, deteniendo waveform")
            lifecycleScope.launch {
                kotlinx.coroutines.delay(200)
                stopSpeaking()
            }
        }

        // 2. Configurar el callback para cuando la voz COMIENCE (inicia la animación)
        voiceManager.setOnSpeechStartedListener {
            Log.d(TAG, "▶️ TTS empezó a hablar, iniciando waveform continuo.")
            lifecycleScope.launch {
                // ✅ INICIAR animación CONTINUA SOLO CUANDO el TTS haya iniciado el sonido
                waveformManager.startContinuousAnimation(this) {
                    Log.d(TAG, "🔊 Waveform continuo terminó")
                }
            }
        }

        // 3. Reproducir texto (esto inicia el proceso en el motor TTS, pero la animación espera)
        if (isLongText) {
            voiceManager.speakLongText(text, pause)
        } else {
            voiceManager.speak(text)
        }
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
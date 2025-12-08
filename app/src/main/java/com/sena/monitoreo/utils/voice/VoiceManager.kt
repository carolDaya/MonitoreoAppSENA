package com.sena.monitoreo.utils.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

class VoiceManager(
    private val context: Context,
    private val onInitComplete: (() -> Unit)? = null
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceManager"
        private const val UTTERANCE_ID_PREFIX = "TTS_ID_"
        private const val SENTENCE_ID_PREFIX = "SENTENCE_"
    }

    private var tts: TextToSpeech? = null
    var isReady = false
        private set

    var isSpeaking: Boolean = false
        private set

    // Configuración
    var currentPitch: Float = 1.0f
    var currentGender: String = "FEMALE"
    var currentRate: Float = 1.0f

    private val preferredMaleVoices = listOf("male", "hombre", "masculino", "man", "mfb")
    private val preferredFemaleVoices = listOf("female", "mujer", "femenino", "woman", "efb")

    private var onUtteranceCompleted: (() -> Unit)? = null
    // ✅ NUEVO: Callback para notificar cuando el TTS realmente empieza a hablar
    private var onSpeechStarted: (() -> Unit)? = null

    fun initialize() {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            applyTtsSettings()
            onInitComplete?.invoke()
            Log.d(TAG, "TTS inicializado correctamente")
        } else {
            Log.e(TAG, "Error al inicializar TTS: $status")
        }
    }

    fun setOnUtteranceCompletedListener(listener: () -> Unit) {
        this.onUtteranceCompleted = listener
    }

    // ✅ NUEVO: Setter para el listener de inicio de voz
    fun setOnSpeechStartedListener(listener: () -> Unit) {
        this.onSpeechStarted = listener
    }

    fun applyTtsSettings() {
        if (tts == null || !isReady) {
            Log.w(TAG, "TTS no está listo para aplicar settings")
            return
        }

        Log.d(TAG, "🎯 Aplicando configuración: $currentGender, pitch: $currentPitch")

        val locale = Locale("es", "ES")
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Idioma TTS no compatible")
            return
        }

        when (currentGender.uppercase()) {
            "MALE" -> {
                setupMaleVoice(locale)
                Log.d(TAG, "🔊 Voz masculina configurada")
            }
            "FEMALE" -> {
                setupFemaleVoice(locale)
                Log.d(TAG, "🔊 Voz femenina configurada")
            }
            "ROBOTIC" -> {
                setupRoboticVoice()
                Log.d(TAG, "🔊 Voz robótica configurada")
            }
            else -> {
                setupFemaleVoice(locale)
                Log.w(TAG, "⚠️ Género no reconocido, usando femenino por defecto")
            }
        }

        tts?.setPitch(mapPitchValue(currentPitch))
        tts?.setSpeechRate(mapSpeechRateValue(currentRate))

        setupProgressListener()
        Log.d(TAG, "✅ Configuración de voz aplicada exitosamente")
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                onSpeechStarted?.invoke() // ✅ Llamar aquí para iniciar el waveform
            }
            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                onUtteranceCompleted?.invoke()
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
                Log.e(TAG, "Error in: $utteranceId")
                onUtteranceCompleted?.invoke()
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                isSpeaking = false
            }
        })
    }

    private fun setupMaleVoice(locale: Locale) {
        tts?.voices?.find {
            it.locale.language == locale.language &&
                    preferredMaleVoices.any { pref -> it.name.contains(pref, true) }
        }?.let {
            tts?.voice = it
        }
    }

    private fun setupFemaleVoice(locale: Locale) {
        tts?.voices?.find {
            it.locale.language == locale.language &&
                    preferredFemaleVoices.any { pref -> it.name.contains(pref, true) }
        }?.let {
            tts?.voice = it
        }
    }

    private fun setupRoboticVoice() {
        val locale = Locale("es", "ES")
        tts?.voices?.find { it.locale.language == locale.language }?.let { tts?.voice = it }
        tts?.setPitch(0.3f)
        tts?.setSpeechRate(0.75f)
    }

    private fun mapPitchValue(value: Float): Float = value.coerceIn(0.5f, 2.0f)

    private fun mapSpeechRateValue(value: Float): Float = value.coerceIn(0.1f, 2.0f)


    fun speak(text: String) {
        if (!isReady) {
            Log.w(TAG, "TTS no está listo")
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "$UTTERANCE_ID_PREFIX${System.currentTimeMillis()}")
    }

    fun speakLongText(fullText: String, pauseBetweenSentences: Long = 800) {
        speakWithPauses(fullText, pauseBetweenSentences)
    }

    fun speakWithPauses(text: String, pauseBetweenSentences: Long = 500) {
        if (!isReady) return

        val sentences = text.split(Regex("(?<=[.!?])\\s*")).map { it.trim() }.filter { it.isNotEmpty() }

        if (sentences.size == 1) {
            tts?.speak(sentences[0], TextToSpeech.QUEUE_FLUSH, null, "$SENTENCE_ID_PREFIX${System.currentTimeMillis()}")
            return
        }

        sentences.forEachIndexed { index, sentence ->
            val utteranceId = "$SENTENCE_ID_PREFIX$index"
            if (index == 0) {
                tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } else {
                tts?.playSilentUtterance(pauseBetweenSentences, TextToSpeech.QUEUE_ADD, null)
                tts?.speak(sentence, TextToSpeech.QUEUE_ADD, null, utteranceId)
            }
        }
    }

    fun isCurrentlySpeaking(): Boolean { return isSpeaking }

    fun stop() {
        tts?.stop()
        isSpeaking = false
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        isReady = false
        isSpeaking = false
        onUtteranceCompleted = null
        onSpeechStarted = null // Limpiar el nuevo listener también
    }
}
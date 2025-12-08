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

    private var totalSentencesToSpeak = 0
    private var sentencesSpoken = 0

    fun speakWithPauses(text: String, pauseBetweenSentences: Long = 500) {
        if (!isReady) {
            Log.w(TAG, "⚠️ TTS no está listo")
            return
        }

        Log.d(TAG, "🔤 Texto recibido para dividir: $text")

        // Regex mejorado que NO divide en puntos decimales
        val sentences = text
            .split(Regex("(?<=[.!?])\\s+(?=[A-ZÁÉÍÓÚÑ])"))
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "." }

        Log.d(TAG, "📝 Oraciones detectadas: ${sentences.size}")
        sentences.forEachIndexed { index, sentence ->
            Log.d(TAG, "   [$index]: $sentence")
        }

        if (sentences.isEmpty()) {
            Log.w(TAG, "⚠️ No se detectaron oraciones válidas")
            return
        }

        // ✅ NUEVO: Resetear contadores
        totalSentencesToSpeak = sentences.size
        sentencesSpoken = 0
        Log.d(TAG, "🎯 Preparando para reproducir $totalSentencesToSpeak oraciones")

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

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // ✅ Solo marcar como "hablando" en la primera oración
                if (utteranceId?.startsWith(SENTENCE_ID_PREFIX) == true) {
                    val sentenceIndex = utteranceId.removePrefix(SENTENCE_ID_PREFIX).toIntOrNull()
                    if (sentenceIndex == 0 || !isSpeaking) {
                        isSpeaking = true
                        onSpeechStarted?.invoke()
                        Log.d(TAG, "🎤 TTS iniciado en oración #$sentenceIndex")
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                // ✅ NUEVO: Solo llamar al callback cuando TODAS las oraciones terminen
                if (utteranceId?.startsWith(SENTENCE_ID_PREFIX) == true) {
                    sentencesSpoken++
                    Log.d(TAG, "✅ Oración completada: $sentencesSpoken/$totalSentencesToSpeak")

                    if (sentencesSpoken >= totalSentencesToSpeak) {
                        isSpeaking = false
                        Log.d(TAG, "🎉 TODAS las oraciones completadas")
                        onUtteranceCompleted?.invoke()
                    }
                } else {
                    // Para utterances normales (no divididos en oraciones)
                    isSpeaking = false
                    onUtteranceCompleted?.invoke()
                }
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
                Log.e(TAG, "❌ Error en: $utteranceId")
                onUtteranceCompleted?.invoke()
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                isSpeaking = false
                sentencesSpoken = 0
                totalSentencesToSpeak = 0
            }
        })
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
        sentencesSpoken = 0
        totalSentencesToSpeak = 0
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
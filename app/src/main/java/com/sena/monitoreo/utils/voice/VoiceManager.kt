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

    private var tts: TextToSpeech? = null
    var isReady = false
        private set
    var isSpeaking = false
        private set

    // Configuración
    var currentPitch: Float = 1.0f
    var currentGender: String = "FEMALE"

    // Voces preferidas
    private val preferredMaleVoices = listOf("male", "hombre", "masculino", "man", "mfb")
    private val preferredFemaleVoices = listOf("female", "mujer", "femenino", "woman", "efb")

    // Callback para cuando termine la reproducción
    private var onUtteranceCompleted: (() -> Unit)? = null

    fun initialize() {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            applyTtsSettings()
            onInitComplete?.invoke()
            Log.d("VoiceManager", "TTS inicializado correctamente")
        } else {
            Log.e("VoiceManager", "Error al inicializar TTS: $status")
        }
    }

    fun setOnUtteranceCompletedListener(listener: () -> Unit) {
        this.onUtteranceCompleted = listener
    }

    fun applyTtsSettings() {
        if (tts == null || !isReady) return

        val locale = Locale("es", "ES")
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("VoiceManager", "Idioma TTS no compatible")
            return
        }

        when (currentGender.uppercase()) {
            "MALE" -> setupMaleVoice(locale)
            "FEMALE" -> setupFemaleVoice(locale)
            "ROBOTIC" -> setupRoboticVoice()
            else -> setupFemaleVoice(locale)
        }

        tts?.setPitch(mapPitchValue(currentPitch))
    }

    private fun setupMaleVoice(locale: Locale) {
        tts?.voices?.find {
            it.locale.language == "es" &&
                    preferredMaleVoices.any { pref -> it.name.contains(pref, true) }
        }?.let {
            tts?.voice = it
            return
        }
        tts?.setPitch(0.8f)
        tts?.setSpeechRate(0.9f)
    }

    private fun setupFemaleVoice(locale: Locale) {
        tts?.voices?.find {
            it.locale.language == "es" &&
                    preferredFemaleVoices.any { pref -> it.name.contains(pref, true) }
        }?.let {
            tts?.voice = it
            return
        }
        tts?.setPitch(1.1f)
        tts?.setSpeechRate(1.0f)
    }

    private fun setupRoboticVoice() {
        val locale = Locale("es", "ES")
        tts?.voices?.find { it.locale.language == "es" }?.let { tts?.voice = it }
        tts?.setPitch(0.3f)
        tts?.setSpeechRate(0.75f)
    }

    private fun mapPitchValue(value: Float): Float = when (value) {
        0.8f -> 0.6f
        1.0f -> 1.0f
        1.3f -> 1.6f
        else -> value.coerceIn(0.5f, 2.0f)
    }

    fun speak(text: String) {
        if (!isReady) {
            Log.w("VoiceManager", "TTS no está listo")
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
        isSpeaking = true
    }

    fun speakLongText(fullText: String, chunkSize: Int = 200) {
        if (!isReady) {
            Log.w("VoiceManager", "TTS no está listo")
            return
        }

        // Dividir el texto en chunks
        val chunks = fullText.chunked(chunkSize)

        // Hablar el primer chunk
        tts?.speak(chunks[0], TextToSpeech.QUEUE_FLUSH, null, "TTS_CHUNK_0")

        // Encolar los chunks restantes
        for (i in 1 until chunks.size) {
            tts?.speak(chunks[i], TextToSpeech.QUEUE_ADD, null, "TTS_CHUNK_$i")
        }

        isSpeaking = true
    }

    fun speakLongTextWithCallback(fullText: String, chunkSize: Int = 200) {
        if (!isReady) {
            Log.w("VoiceManager", "TTS no está listo")
            return
        }

        // Configurar listener de finalización
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d("VoiceManager", "Started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d("VoiceManager", "Completed: $utteranceId")
                // Verificar si es el último chunk
                if (utteranceId?.startsWith("TTS_CHUNK_") == true) {
                    val chunkIndex = utteranceId.removePrefix("TTS_CHUNK_").toIntOrNull()
                    val totalChunks = fullText.chunked(chunkSize).size
                    if (chunkIndex == totalChunks - 1) {
                        // Es el último chunk
                        Log.d("VoiceManager", "Último chunk completado, llamando callback")
                        onUtteranceCompleted?.invoke()
                    }
                } else if (utteranceId == "TTS_ID") {
                    // Para speak normal
                    onUtteranceCompleted?.invoke()
                }
            }

            override fun onError(utteranceId: String?) {
                Log.e("VoiceManager", "Error in: $utteranceId")
                onUtteranceCompleted?.invoke() // Llamar callback incluso en error
            }
        })

        // Dividir el texto en chunks
        val chunks = fullText.chunked(chunkSize)

        // Hablar el primer chunk
        tts?.speak(chunks[0], TextToSpeech.QUEUE_FLUSH, null, "TTS_CHUNK_0")

        // Encolar los chunks restantes
        for (i in 1 until chunks.size) {
            tts?.speak(chunks[i], TextToSpeech.QUEUE_ADD, null, "TTS_CHUNK_$i")
        }

        isSpeaking = true
    }

    fun speakWithPauses(text: String, pauseBetweenSentences: Long = 500) {
        if (!isReady) return

        // Dividir por oraciones
        val sentences = text.split(". ", "! ", "? ").map { it.trim() }

        sentences.forEachIndexed { index, sentence ->
            val utteranceId = "SENTENCE_$index"
            if (index == 0) {
                tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } else {
                // Agregar pausa pequeña entre oraciones
                tts?.playSilentUtterance(pauseBetweenSentences, TextToSpeech.QUEUE_ADD, null)
                tts?.speak(sentence, TextToSpeech.QUEUE_ADD, null, utteranceId)
            }
        }

        isSpeaking = true
    }

    fun isCurrentlySpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
        onUtteranceCompleted = null // Limpiar callback
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        isReady = false
        isSpeaking = false
        onUtteranceCompleted = null
    }
}
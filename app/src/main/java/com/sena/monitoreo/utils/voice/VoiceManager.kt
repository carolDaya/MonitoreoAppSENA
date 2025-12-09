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
        private const val MIN_SENTENCE_LENGTH = 5
    }

    private var tts: TextToSpeech? = null
    var isReady = false
        private set

    var isSpeaking: Boolean = false
        private set

    var currentPitch: Float = 1.0f
    var currentGender: String = "FEMALE"
    var currentRate: Float = 1.0f

    private val preferredMaleVoices = listOf("male", "hombre", "masculino", "man", "mfb")
    private val preferredFemaleVoices = listOf("female", "mujer", "femenino", "woman", "efb")

    private var onUtteranceCompleted: (() -> Unit)? = null
    private var onSpeechStarted: (() -> Unit)? = null

    private var totalSentencesToSpeak = 0
    private var sentencesSpoken = 0

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

    fun setOnSpeechStartedListener(listener: () -> Unit) {
        this.onSpeechStarted = listener
    }

    fun applyTtsSettings() {
        if (tts == null || !isReady) {
            Log.w(TAG, "TTS no está listo")
            return
        }

        val locale = Locale("es", "ES")
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Idioma no soportado")
            return
        }

        when (currentGender.uppercase()) {
            "MALE" -> setupMaleVoice(locale)
            "FEMALE" -> setupFemaleVoice(locale)
            "ROBOTIC" -> setupRoboticVoice()
            else -> setupFemaleVoice(locale)
        }

        tts?.setPitch(mapPitchValue(currentPitch))
        tts?.setSpeechRate(mapSpeechRateValue(currentRate))
        setupProgressListener()
    }

    private fun setupMaleVoice(locale: Locale) {
        tts?.voices?.find {
            it.locale.language == locale.language &&
                    preferredMaleVoices.any { pref -> it.name.contains(pref, true) }
        }?.let { tts?.voice = it }
    }

    private fun setupFemaleVoice(locale: Locale) {
        tts?.voices?.find {
            it.locale.language == locale.language &&
                    preferredFemaleVoices.any { pref -> it.name.contains(pref, true) }
        }?.let { tts?.voice = it }
    }

    private fun setupRoboticVoice() {
        val locale = Locale("es", "ES")
        tts?.voices?.find { it.locale.language == locale.language }?.let { tts?.voice = it }
        tts?.setPitch(0.3f)
        tts?.setSpeechRate(0.75f)
    }

    private fun mapPitchValue(value: Float): Float = value.coerceIn(0.5f, 2.0f)
    private fun mapSpeechRateValue(value: Float): Float = value.coerceIn(0.1f, 2.0f)

    // MÉTODO OPTIMIZADO: Hablar texto corto inmediatamente
    fun speak(text: String) {
        if (!isReady) {
            Log.w(TAG, "TTS no está listo")
            return
        }

        val cleanText = normalizeText(text)
        val utteranceId = "$UTTERANCE_ID_PREFIX${System.currentTimeMillis()}"

        Log.d(TAG, "🎤 Hablando texto corto: $cleanText")
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    // División inteligente de texto largo
    fun speakLongText(fullText: String, pauseBetweenSentences: Long = 800) {
        if (!isReady) {
            Log.w(TAG, "TTS no está listo")
            return
        }

        val normalizedText = normalizeText(fullText)
        val sentences = splitIntoSentences(normalizedText)

        Log.d(TAG, "Dividido en ${sentences.size} oraciones")

        // Si es texto corto, hablar directo
        if (sentences.size <= 1 || normalizedText.length < 150) {
            speak(normalizedText)
            return
        }

        // Para textos largos, usar pausas
        speakWithPauses(sentences, pauseBetweenSentences)
    }

    // Normalizar texto antes de hablar
    private fun normalizeText(text: String): String {
        return text
            .replace("\\u00b0", " grados ")
            .replace("°", " grados ")
            .replace("|", " y ")
            .replace(Regex("\\.{2,}"), ".")  // .. -> .
            .replace(Regex("\\s+"), " ")      // Múltiples espacios -> 1
            .trim()
    }

    //  División mejorada de oraciones
    private fun splitIntoSentences(text: String): List<String> {
        // División simple y robusta
        return text
            .split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.length >= MIN_SENTENCE_LENGTH }
            .also { sentences ->
                sentences.forEachIndexed { i, s ->
                    Log.d(TAG, "  [$i]: ${s.take(50)}${if (s.length > 50) "..." else ""}")
                }
            }
    }

    private fun speakWithPauses(sentences: List<String>, pauseBetweenSentences: Long) {
        totalSentencesToSpeak = sentences.size
        sentencesSpoken = 0

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
                if (!isSpeaking) {
                    isSpeaking = true
                    onSpeechStarted?.invoke()
                    Log.d(TAG, "TTS iniciado")
                }
            }

            override fun onDone(utteranceId: String?) {
                when {
                    utteranceId?.startsWith(SENTENCE_ID_PREFIX) == true -> {
                        sentencesSpoken++
                        Log.d(TAG, "Oración $sentencesSpoken/$totalSentencesToSpeak")

                        if (sentencesSpoken >= totalSentencesToSpeak) {
                            isSpeaking = false
                            onUtteranceCompleted?.invoke()
                        }
                    }
                    else -> {
                        isSpeaking = false
                        onUtteranceCompleted?.invoke()
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
                Log.e(TAG, "Error TTS: $utteranceId")
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
        onSpeechStarted = null
    }
}
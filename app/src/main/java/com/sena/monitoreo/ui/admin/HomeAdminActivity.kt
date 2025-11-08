package com.sena.monitoreo.ui.admin

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.masoudss.lib.WaveformSeekBar
import com.sena.monitoreo.R
import android.view.View
import com.sena.monitoreo.data.api.ApiProceso
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.repository.ProcesoRepository
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.databinding.ActivityHomeAdminBinding
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModel
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModelFactory
import com.sena.monitoreo.ui.admin.viewmodel.ProcesoViewModel
import com.sena.monitoreo.ui.admin.viewmodel.ProcesoViewModelFactory
import com.sena.monitoreo.ui.auth.LoginActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

class HomeAdminActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityHomeAdminBinding
    private var tts: TextToSpeech? = null
    private var isSpeaking = false
    private lateinit var waveformSeekBar: WaveformSeekBar
    private lateinit var btnPlay: MaterialButton
    private val SPLASH_TIME_OUT: Long = 5000 // 5 segundos

    // Variables de configuración de voz
    private var currentPitch: Float = 1.0f
    private var currentGender: String = "FEMALE"
    private var ttsReady: Boolean = false // Bandera para saber si el motor TTS está listo

    // Listas de voces preferidas por género (basadas en nombres comunes)
    private val preferredMaleVoices = listOf(
        "male", "hombre", "mexicano", "masculino", "man", "mfb", "macho"
    )

    private val preferredFemaleVoices = listOf(
        "female", "mujer", "femenino", "woman", "efb", "hembra", "chica"
    )

    // Inicialización Lazy del ViewModel
    private val viewModel: AdminConfigViewModel by lazy {
        val repository = VoiceRepository(RetrofitClient.apiVoice)
        ViewModelProvider(this, AdminConfigViewModelFactory(repository))
            .get(AdminConfigViewModel::class.java)
    }
    private val procesoViewModel: ProcesoViewModel by lazy {
        val repository = ProcesoRepository(RetrofitClient.apiProceso)
        ViewModelProvider(this, ProcesoViewModelFactory(repository))
            .get(ProcesoViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // **1. Cargar la configuración de voz al iniciar**
        loadVoiceConfiguration()

        // Inicializar TextToSpeech
        tts = TextToSpeech(this, this)

        initWaveformViews()

        // Lógica de Splash
        val fromMenu = intent.getBooleanExtra("FROM_MENU", false)
        if (!fromMenu) {
            lifecycleScope.launch {
                delay(SPLASH_TIME_OUT)
                val intent = Intent(this@HomeAdminActivity, AdminDashboardActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        // --- Menú lateral ---
        binding.mainHeader.settingsIcon.setOnClickListener {
            binding.homeAdmin.openDrawer(GravityCompat.START)
        }

        setupNavigationView()
        setupProcesoControl()
    }

    // Función para cargar la configuración de voz desde el backend
    private fun loadVoiceConfiguration() {
        viewModel.loadCurrentConfig()

        viewModel.currentConfig.observe(this) { config ->
            // Almacenar los valores cargados
            currentPitch = config.pitch
            currentGender = config.gender

            // Si el TTS ya está listo, aplica la nueva configuración inmediatamente
            if (ttsReady) {
                applyTtsSettings()
                // Reproducir mensaje de prueba cuando se actualiza la configuración
                lifecycleScope.launch {
                    delay(500)
                    speakText("Configuración de voz actualizada")
                }
            }
        }
    }

    /**
     * Verifica si hay voces en español disponibles, si no, intenta instalarlas
     */
    private fun checkAndInstallSpanishVoices() {
        val spanishVoices = tts?.voices?.filter { voice ->
            voice.locale.language == "es"
        }

        if (spanishVoices.isNullOrEmpty()) {
            // No hay voces en español, intentar instalarlas
            Toast.makeText(this, "No hay voces en español. Instálelas desde ajustes.", Toast.LENGTH_LONG).show()
            println("❌ No hay voces en español disponibles")
        } else {
            println("✅ Voces en español disponibles: ${spanishVoices.size}")
            spanishVoices.forEach { voice ->
                println(" - ${voice.name} (${voice.locale})")
            }
        }
    }

    /**
     * Convierte el pitch del backend a valores más efectivos para TTS
     */
    private fun mapPitchValue(backendPitch: Float): Float {
        return when (backendPitch) {
            0.8f -> 0.6f  // Más grave para mejor diferenciación
            1.0f -> 1.0f  // Normal
            1.3f -> 1.6f  // Más agudo para mejor diferenciación
            else -> backendPitch.coerceIn(0.5f, 2.0f)
        }
    }

    // Configuración del TTS: Aplica Pitch, Rate, y selecciona la Voz.
    private fun applyTtsSettings() {
        if (tts == null || !ttsReady) return

        val locale = Locale("es", "ES")
        val langResult = tts?.setLanguage(locale)

        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "Idioma español no disponible. Instale voces en español.", Toast.LENGTH_LONG).show()
            btnPlay.isEnabled = false
            return
        }

        when (currentGender.uppercase()) {
            "ROBOTIC" -> {
                setupRoboticVoice()
                Toast.makeText(this, "Modo Voz Robótica activado", Toast.LENGTH_SHORT).show()
            }
            "MALE" -> {
                setupMaleVoice(locale)
            }
            "FEMALE" -> {
                setupFemaleVoice(locale)
            }
            else -> {
                setupFemaleVoice(locale)
            }
        }

        // Aplicar el pitch específico después de configurar la voz base
        val effectivePitch = mapPitchValue(currentPitch)
        tts?.setPitch(effectivePitch)

        btnPlay.isEnabled = true
    }

    /**
     * Configura voz masculina con efecto de pitch más grave
     */
    private fun setupMaleVoice(locale: Locale) {
        // Prioridad 1: Buscar voces en español que suenen masculinas
        tts?.voices?.find { voice ->
            voice.locale.language == "es" &&
                    preferredMaleVoices.any { preferred ->
                        voice.name.contains(preferred, ignoreCase = true)
                    }
        }?.let { voice ->
            tts?.voice = voice
            tts?.setSpeechRate(1.0f)
            Toast.makeText(this, "Voz masculina en español activada", Toast.LENGTH_SHORT).show()
            return
        }

        // Prioridad 2: Buscar cualquier voz en español
        tts?.voices?.find { voice ->
            voice.locale.language == "es"
        }?.let { voice ->
            tts?.voice = voice
            // Aplicar pitch más grave para simular voz masculina
            tts?.setPitch(0.7f)
            tts?.setSpeechRate(0.9f)
            Toast.makeText(this, "Voz masculina simulada con pitch grave", Toast.LENGTH_SHORT).show()
            return
        }

        // Prioridad 3: Buscar voces en inglés masculinas
        tts?.voices?.find { voice ->
            voice.locale.language == "en" &&
                    preferredMaleVoices.any { preferred ->
                        voice.name.contains(preferred, ignoreCase = true)
                    }
        }?.let { voice ->
            tts?.voice = voice
            tts?.setPitch(0.8f)
            Toast.makeText(this, "Usando voz masculina en inglés", Toast.LENGTH_SHORT).show()
            return
        }

        // Prioridad 4: Cualquier voz en inglés
        tts?.voices?.find { voice ->
            voice.locale.language == "en"
        }?.let { voice ->
            tts?.voice = voice
            tts?.setPitch(0.7f) // Pitch grave para masculino
            tts?.setSpeechRate(0.9f)
            Toast.makeText(this, "Usando voz en inglés con efecto masculino", Toast.LENGTH_SHORT).show()
            return
        }

        // Último fallback: cualquier voz disponible
        tts?.voices?.firstOrNull()?.let { voice ->
            tts?.voice = voice
            tts?.setPitch(0.7f) // Pitch grave para masculino
            tts?.setSpeechRate(0.9f)
            Toast.makeText(this, "Usando voz disponible con efecto masculino", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "No se encontró voz masculina adecuada", Toast.LENGTH_SHORT).show()
    }

    /**
     * Configura voz femenina
     */
    private fun setupFemaleVoice(locale: Locale) {
        // Prioridad 1: Buscar voces en español que suenen femeninas
        tts?.voices?.find { voice ->
            voice.locale.language == "es" &&
                    preferredFemaleVoices.any { preferred ->
                        voice.name.contains(preferred, ignoreCase = true)
                    }
        }?.let { voice ->
            tts?.voice = voice
            tts?.setSpeechRate(1.0f)
            Toast.makeText(this, "Voz femenina en español activada", Toast.LENGTH_SHORT).show()
            return
        }

        // Prioridad 2: Buscar cualquier voz en español
        tts?.voices?.find { voice ->
            voice.locale.language == "es"
        }?.let { voice ->
            tts?.voice = voice
            tts?.setPitch(1.1f) // Pitch más agudo para femenino
            tts?.setSpeechRate(1.0f)
            Toast.makeText(this, "Voz femenina en español", Toast.LENGTH_SHORT).show()
            return
        }

        // Prioridad 3: Buscar voces en inglés femeninas
        tts?.voices?.find { voice ->
            voice.locale.language == "en" &&
                    preferredFemaleVoices.any { preferred ->
                        voice.name.contains(preferred, ignoreCase = true)
                    }
        }?.let { voice ->
            tts?.voice = voice
            tts?.setPitch(1.2f)
            Toast.makeText(this, "Usando voz femenina en inglés", Toast.LENGTH_SHORT).show()
            return
        }

        // Prioridad 4: Cualquier voz en inglés
        tts?.voices?.find { voice ->
            voice.locale.language == "en"
        }?.let { voice ->
            tts?.voice = voice
            tts?.setPitch(1.1f) // Pitch más agudo para femenino
            tts?.setSpeechRate(1.0f)
            Toast.makeText(this, "Usando voz en inglés con efecto femenino", Toast.LENGTH_SHORT).show()
            return
        }

        // Último fallback
        tts?.voices?.firstOrNull()?.let { voice ->
            tts?.voice = voice
            tts?.setPitch(1.1f) // Pitch más agudo para femenino
            tts?.setSpeechRate(1.0f)
            Toast.makeText(this, "Usando voz disponible con efecto femenino", Toast.LENGTH_SHORT).show()
            return
        }
    }

    /**
     * Configura efecto de voz robótica
     */
    private fun setupRoboticVoice() {
        // Para efecto robótico, usar una voz neutra y aplicar efectos extremos
        val locale = Locale("es", "ES")

        // Primero intentar con voz en español
        tts?.voices?.find { voice ->
            voice.locale.language == "es"
        }?.let { voice ->
            tts?.voice = voice
        } ?: run {
            // Si no hay español, usar cualquier voz disponible
            tts?.voices?.firstOrNull()?.let { voice ->
                tts?.voice = voice
            }
        }

        // Configuración extrema para efecto robótico
        tts?.setPitch(0.3f) // Pitch muy grave
        tts?.setSpeechRate(0.75f) // Velocidad lenta y mecánica
    }

    /**
     * Debug: muestra las voces disponibles en el log
     */
    private fun logAvailableVoices() {
        println("=== VOCES TTS DISPONIBLES ===")
        tts?.voices?.forEach { voice ->
            println("Nombre: ${voice.name}")
            println(" - Idioma: ${voice.locale}")
            println(" - Características: ${voice.features}")
            println("---")
        }
        println("=============================")

        // Mostrar estadísticas de idiomas disponibles
        val voicesByLanguage = tts?.voices?.groupBy { it.locale.language }
        println("=== ESTADÍSTICAS DE IDIOMAS ===")
        voicesByLanguage?.forEach { (language, voices) ->
            println("$language: ${voices.size} voces")
        }
        println("=============================")
    }

    // onInit se llama cuando el motor TTS termina de inicializarse.
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true // El motor está listo

            // Verificar voces en español
            checkAndInstallSpanishVoices()

            // Debug: mostrar voces disponibles
            logAvailableVoices()

            // Aplicar configuración cargada
            applyTtsSettings()

            Toast.makeText(this, "Motor TTS inicializado correctamente", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error al inicializar TextToSpeech", Toast.LENGTH_SHORT).show()
            btnPlay.isEnabled = false
        }
    }

    private fun setupNavigationView() {
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_graphis -> {
                    val intent = Intent(this, AdminDashboardActivity::class.java)
                    intent.putExtra("SCROLL_TO", "graphs")
                    startActivity(intent)
                }
                R.id.nav_volumen -> {
                    val intent = Intent(this, AdminDashboardActivity::class.java)
                    intent.putExtra("SCROLL_TO", "ai")
                    startActivity(intent)
                }
                R.id.nav_datos_gas -> {
                    Toast.makeText(this, "Datos de Gas", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_datos_tem -> {
                    Toast.makeText(this, "Datos de Temperatura", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_datos_presion -> {
                    Toast.makeText(this, "Datos de Presión", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_users -> {
                    val intent = Intent(this, AdminDashboardActivity::class.java)
                    intent.putExtra("SCROLL_TO", "users")
                    startActivity(intent)
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Configuración Admin", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_logout -> {
                    stopSpeaking()
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                }
            }
            binding.homeAdmin.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun initWaveformViews() {
        val waveformBinding = binding.mainHeader.waveformSection
        waveformSeekBar = waveformBinding.waveformSeekBar
        btnPlay = waveformBinding.btnPlayMessage
        setupWaveformSamples()
        btnPlay.setOnClickListener {
            if (!isSpeaking) startSpeaking() else stopSpeaking()
        }
    }

    private fun setupWaveformSamples() {
        val samples = IntArray(100) {
            Random.nextInt(10, 100)
        }
        waveformSeekBar.setSampleFrom(samples)
        waveformSeekBar.progress = 0f
    }

    private fun startSpeaking() {
        val message = getString(R.string.welcome_message)
        speakText(message)
        isSpeaking = true
        btnPlay.setIconResource(R.drawable.ic_stop)

        startWaveformAnimation()
    }

    private fun stopSpeaking() {
        tts?.stop()
        isSpeaking = false
        btnPlay.setIconResource(R.drawable.ic_play)
        waveformSeekBar.progress = 0f
    }

    private fun startWaveformAnimation() {
        lifecycleScope.launch {
            var progress = 0f
            val maxProgress = waveformSeekBar.maxProgress
            val duration = 5000L

            while (isSpeaking && progress < maxProgress && tts?.isSpeaking == true) {
                progress += (maxProgress / (duration / 50)).toFloat()
                waveformSeekBar.progress = progress.coerceAtMost(maxProgress)

                val dynamicSamples = IntArray(100) {
                    Random.nextInt(5, 95)
                }
                waveformSeekBar.setSampleFrom(dynamicSamples)

                delay(50L)
            }

            if (isSpeaking) {
                isSpeaking = false
                btnPlay.setIconResource(R.drawable.ic_play)
                waveformSeekBar.progress = 0f
            }
        }
    }

    // Reproducir texto
    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
    }

    override fun onBackPressed() {
        if (binding.homeAdmin.isDrawerOpen(GravityCompat.START)) {
            binding.homeAdmin.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // Liberar recursos
    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }
    private fun setupProcesoControl() {

        val cardBinding = binding.cardProcesoControlInclude

        val btnIniciar = cardBinding.btnIniciarProceso
        val btnFinalizar = cardBinding.btnFinalizarProceso
        val tvEstado = cardBinding.tvProcesoEstado
        val progressBar = cardBinding.procesoProgressBar

        // 1. Listeners de los botones
        btnIniciar.setOnClickListener {
            procesoViewModel.iniciarProceso()
        }

        btnFinalizar.setOnClickListener {
            procesoViewModel.finalizarProceso()
        }

        // 2. Observar el estado de Carga (Loading)
        procesoViewModel.isLoading.observe(this) { isLoading ->
            // Deshabilitar ambos botones durante la operación para evitar doble click
            btnIniciar.isEnabled = !isLoading && (procesoViewModel.isProcesoActivo.value == false)
            btnFinalizar.isEnabled = !isLoading && (procesoViewModel.isProcesoActivo.value == true)

            // Mostrar u ocultar la barra de progreso
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 3. Observar si hay Proceso Activo para actualizar la UI
        procesoViewModel.isProcesoActivo.observe(this) { isActive ->
            tvEstado.text = if (isActive) "Estado: 🟢 Activo (Monitoreando)" else "Estado: 🔴 Inactivo (Se requiere iniciar proceso)"

            // Habilitar/Deshabilitar botones basado en el estado
            val isLoading = procesoViewModel.isLoading.value ?: false
            btnIniciar.isEnabled = !isActive && !isLoading
            btnFinalizar.isEnabled = isActive && !isLoading
        }

        // 4. Observar el mensaje de Estado (Éxito/Error)
        procesoViewModel.procesoStatus.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                // Hablar el mensaje (como lo pide el flujo)
                speakText(message)

                // Limpiar el mensaje después de mostrarlo (opcional, para evitar repeticiones)
                // procesoViewModel.clearStatus()
            }
        }

        // 5. Verificar el estado inicial del proceso al cargar la actividad
        // 💡 Esto es CRÍTICO: Necesitas llamar a tu ViewModel para saber si hay un proceso activo al inicio
        // Asumiendo que agregaste 'verificarEstadoProceso()' a tu ProcesoViewModel y Backend
        procesoViewModel.verificarEstadoProceso()
    }
}
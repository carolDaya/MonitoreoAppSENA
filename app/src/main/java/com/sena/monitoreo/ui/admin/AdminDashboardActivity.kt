package com.sena.monitoreo.ui.admin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.*
import com.github.mikephil.charting.data.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.masoudss.lib.WaveformSeekBar
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.user.UserResponse
import com.sena.monitoreo.data.repository.GraficasRepository
import com.sena.monitoreo.data.repository.UserRepository
import com.sena.monitoreo.data.repository.VoiceRepository
import com.sena.monitoreo.databinding.ActivityAdminDashboardBinding
import com.sena.monitoreo.databinding.HeaderLayoutAdminBinding
import com.sena.monitoreo.ui.admin.adapter.UserAdapter
import com.sena.monitoreo.ui.auth.LoginActivity
import com.sena.monitoreo.ui.admin.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class AdminDashboardActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var headerBinding: HeaderLayoutAdminBinding
    private lateinit var userAdapter: UserAdapter
    private val graficasRepo = GraficasRepository()
    private val TAG = "AdminDashboard"

    // 🔊 Variables para Text-to-Speech y Waveform
    private lateinit var waveformSeekBar: WaveformSeekBar
    private lateinit var btnPlayMessage: MaterialButton
    private var tts: TextToSpeech? = null
    private var isSpeaking = false
    private var ttsReady: Boolean = false

    // Variables de configuración de voz
    private var currentPitch: Float = 1.0f
    private var currentGender: String = "FEMALE"

    // Listas de voces preferidas por género
    private val preferredMaleVoices = listOf(
        "male", "hombre", "mexicano", "masculino", "man", "mfb", "macho"
    )

    private val preferredFemaleVoices = listOf(
        "female", "mujer", "femenino", "woman", "efb", "hembra", "chica"
    )

    // 🆕 ViewModel para configuración de voz
    private val viewModel: AdminConfigViewModel by lazy {
        val repository = VoiceRepository(RetrofitClient.apiVoice)
        ViewModelProvider(this, AdminConfigViewModelFactory(repository))
            .get(AdminConfigViewModel::class.java)
    }

    // 🆕 Mapeos para convertir la selección del Spinner a valores de la API
    private val genderValueMap = mapOf(
        "Femenina" to "FEMALE",
        "Masculina" to "MALE",
        "Robótica" to "ROBOTIC"
    )

    private val pitchValueMap = mapOf(
        "Grave" to 0.8f,
        "Normal" to 1.0f,
        "Aguda" to 1.3f
    )

    // 🆕 Variables para almacenar la configuración seleccionada temporalmente
    private var selectedGender: String = "FEMALE"
    private var selectedPitch: Float = 1.0f

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigationDrawer()
        setupRecyclerView()
        setupTabs()
        cargarGraficasGuardadas()
        setupChartClickListeners()
        setupVoiceConfiguration() // 🆕 Configuración de voz
        setupTextToSpeech() // 🔊 Configuración de TTS y Waveform
        loadVoiceConfiguration() // Cargar configuración desde backend

        // Manejar el scroll automático si viene desde el menú
        handleScrollToSection()

        // Por defecto mostrar todo
        showSection(home = true)
    }

    // ----------------------------------------------------------
    // 🔊 Configuración de Text-to-Speech y Waveform
    // ----------------------------------------------------------
    private fun setupTextToSpeech() {
        // 🔹 Acceso al header incluido
        val headerView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main_header)
        val waveFormSection = headerView.findViewById<View>(R.id.waveform_section)

        // 🔹 Referencias internas del include (wave_form.xml)
        waveformSeekBar = waveFormSection.findViewById(R.id.waveformSeekBar)
        btnPlayMessage = waveFormSection.findViewById(R.id.btn_play_message)

        // Configurar samples iniciales del waveform
        setupWaveformSamples()

        // Inicializar TTS
        tts = TextToSpeech(this, this)

        btnPlayMessage.setOnClickListener {
            if (!isSpeaking) {
                startSpeaking()
            } else {
                stopSpeaking()
            }
        }
    }

    // Configurar samples aleatorios para el waveform
    private fun setupWaveformSamples() {
        val samples = IntArray(100) {
            Random.nextInt(10, 100)
        }
        waveformSeekBar.setSampleFrom(samples)
        waveformSeekBar.progress = 0f
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
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            applyTtsSettings()
            Toast.makeText(this, "Motor TTS inicializado", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Error al inicializar TextToSpeech", Toast.LENGTH_SHORT).show()
            btnPlayMessage.isEnabled = false
        }
    }

    // Configuración del TTS: Aplica Pitch, Rate, y selecciona la Voz
    private fun applyTtsSettings() {
        if (tts == null || !ttsReady) return

        val locale = Locale("es", "ES")
        val langResult = tts?.setLanguage(locale)

        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "Idioma español no disponible", Toast.LENGTH_SHORT).show()
            btnPlayMessage.isEnabled = false
            return
        }

        when (currentGender.uppercase()) {
            "ROBOTIC" -> setupRoboticVoice()
            "MALE" -> setupMaleVoice(locale)
            "FEMALE" -> setupFemaleVoice(locale)
            else -> setupFemaleVoice(locale)
        }

        val effectivePitch = mapPitchValue(currentPitch)
        tts?.setPitch(effectivePitch)

        btnPlayMessage.isEnabled = true
    }

    private fun mapPitchValue(backendPitch: Float): Float {
        return when (backendPitch) {
            0.8f -> 0.6f
            1.0f -> 1.0f
            1.3f -> 1.6f
            else -> backendPitch.coerceIn(0.5f, 2.0f)
        }
    }

    private fun setupMaleVoice(locale: Locale) {
        tts?.voices?.find { voice ->
            voice.locale.language == "es" &&
                    preferredMaleVoices.any { preferred ->
                        voice.name.contains(preferred, ignoreCase = true)
                    }
        }?.let { voice ->
            tts?.voice = voice
            tts?.setSpeechRate(1.0f)
            return
        }

        tts?.voices?.find { voice ->
            voice.locale.language == "es"
        }?.let { voice ->
            tts?.voice = voice
            tts?.setPitch(0.7f)
            tts?.setSpeechRate(0.9f)
            return
        }

        tts?.voices?.firstOrNull()?.let { voice ->
            tts?.voice = voice
            tts?.setPitch(0.7f)
            tts?.setSpeechRate(0.9f)
        }
    }

    private fun setupFemaleVoice(locale: Locale) {
        tts?.voices?.find { voice ->
            voice.locale.language == "es" &&
                    preferredFemaleVoices.any { preferred ->
                        voice.name.contains(preferred, ignoreCase = true)
                    }
        }?.let { voice ->
            tts?.voice = voice
            tts?.setSpeechRate(1.0f)
            return
        }

        tts?.voices?.find { voice ->
            voice.locale.language == "es"
        }?.let { voice ->
            tts?.voice = voice
            tts?.setPitch(1.1f)
            tts?.setSpeechRate(1.0f)
            return
        }

        tts?.voices?.firstOrNull()?.let { voice ->
            tts?.voice = voice
            tts?.setPitch(1.1f)
            tts?.setSpeechRate(1.0f)
        }
    }

    private fun setupRoboticVoice() {
        tts?.voices?.find { voice ->
            voice.locale.language == "es"
        }?.let { voice ->
            tts?.voice = voice
        } ?: run {
            tts?.voices?.firstOrNull()?.let { voice ->
                tts?.voice = voice
            }
        }

        tts?.setPitch(0.3f)
        tts?.setSpeechRate(0.75f)
    }

    private fun startSpeaking() {
        val message = "Bienvenido al panel del administrador. Aquí puedes gestionar usuarios, revisar las gráficas y acceder a las funciones de inteligencia artificial."
        speakText(message)
        isSpeaking = true
        btnPlayMessage.setIconResource(R.drawable.ic_stop)
        startWaveformAnimation()
    }

    private fun stopSpeaking() {
        tts?.stop()
        isSpeaking = false
        btnPlayMessage.setIconResource(R.drawable.ic_play)
        waveformSeekBar.progress = 0f
    }

    private fun startWaveformAnimation() {
        lifecycleScope.launch {
            var progress = 0f
            val maxProgress = waveformSeekBar.maxProgress
            val duration = 8000L // Duración aproximada del mensaje

            while (isSpeaking && progress < maxProgress && tts?.isSpeaking == true) {
                progress += (maxProgress / (duration / 50)).toFloat()
                waveformSeekBar.progress = progress.coerceAtMost(maxProgress)

                // Actualizar samples dinámicamente para efecto de onda
                val dynamicSamples = IntArray(100) {
                    Random.nextInt(5, 95)
                }
                waveformSeekBar.setSampleFrom(dynamicSamples)

                delay(50L)
            }

            if (isSpeaking) {
                isSpeaking = false
                btnPlayMessage.setIconResource(R.drawable.ic_play)
                waveformSeekBar.progress = 0f
            }
        }
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
    }

    // ----------------------------------------------------------
    // 🆕 Configuración de Voz con ViewModel
    // ----------------------------------------------------------
    private fun setupVoiceConfiguration() {
        // Cargar la configuración actual desde el backend al iniciar
        viewModel.loadCurrentConfig()

        // Configurar Listeners y Adaptadores para los Spinners
        val spinnerVoz = binding.iaAdminSection.spinnerIaVoz
        val spinnerTono = binding.iaAdminSection.spinnerTonoVoz

        // Adapter de Voz (Gender)
        ArrayAdapter.createFromResource(
            this, R.array.tipos_de_voz, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerVoz.adapter = adapter
        }

        spinnerVoz.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedKey = parent.getItemAtPosition(position).toString()
                selectedGender = genderValueMap[selectedKey] ?: "FEMALE"
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Adapter de Tono (Pitch)
        ArrayAdapter.createFromResource(
            this, R.array.tonos_de_voz, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTono.adapter = adapter
        }

        spinnerTono.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedKey = parent.getItemAtPosition(position).toString()
                selectedPitch = pitchValueMap[selectedKey] ?: 1.0f
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Observar y Precargar la configuración actual
        viewModel.currentConfig.observe(this) { config ->
            setSpinnerSelection(spinnerVoz, genderValueMap.entries, config.gender)
            setSpinnerSelection(spinnerTono, pitchValueMap.entries, config.pitch)
        }

        // Observar el estado de guardado (para el Toast)
        viewModel.saveStatus.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Configurar el botón de guardar
        binding.iaAdminSection.btnSaveVoiceConfig.setOnClickListener {
            saveConfiguration()
        }
    }

    // 🆕 Función genérica para precargar el valor guardado en el Spinner
    private fun <T> setSpinnerSelection(spinner: Spinner, mapEntries: Set<Map.Entry<String, T>>, value: T) {
        val keyToSelect = mapEntries.find { it.value == value }?.key
        if (keyToSelect != null) {
            val adapter = spinner.adapter as? ArrayAdapter<String>
            adapter?.let {
                val position = it.getPosition(keyToSelect)
                spinner.setSelection(position)
            }
        }
    }

    // 🆕 Función que llama al ViewModel para guardar
    private fun saveConfiguration() {
        viewModel.saveConfiguration(selectedGender, selectedPitch)
    }

    // ----------------------------------------------------------
    // 🆕 Manejar scroll automático a secciones específicas
    // ----------------------------------------------------------
    private fun handleScrollToSection() {
        val scrollTo = intent.getStringExtra("SCROLL_TO")
        when (scrollTo) {
            "graphs" -> showSection(graphs = true)
            "ai" -> showSection(ai = true)
            "users" -> {
                showSection(users = true)
                loadUsers("all")
            }
        }
    }

    // ----------------------------------------------------------
    // Configuración del menú lateral
    // ----------------------------------------------------------
    private fun setupNavigationDrawer() {
        headerBinding = HeaderLayoutAdminBinding.bind(binding.mainHeader.root)

        headerBinding.settingsIcon.setOnClickListener {
            binding.adminDashboard.openDrawer(GravityCompat.START)
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            binding.adminDashboard.closeDrawers()

            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeAdminActivity::class.java)
                    intent.putExtra("FROM_MENU", true)
                    startActivity(intent)
                    finish()
                }
                R.id.nav_graphis -> showSection(graphs = true)
                R.id.nav_volumen -> showSection(ai = true)
                R.id.nav_users -> {
                    showSection(users = true)
                    loadUsers("all")
                }
                R.id.nav_logout -> performLogout()
            }
            true
        }
    }

    // ----------------------------------------------------------
    // Mostrar secciones dinámicamente
    // ----------------------------------------------------------
    private fun showSection(
        home: Boolean = false,
        graphs: Boolean = false,
        ai: Boolean = false,
        users: Boolean = false
    ) {
        with(binding) {
            graficasAdminSection.root.visibility = View.GONE
            iaAdminSection.root.visibility = View.GONE
            userAdminSection.root.visibility = View.GONE

            when {
                home -> {
                    graficasAdminSection.root.visibility = View.VISIBLE
                    iaAdminSection.root.visibility = View.VISIBLE
                    userAdminSection.root.visibility = View.VISIBLE
                }
                graphs -> graficasAdminSection.root.visibility = View.VISIBLE
                ai -> iaAdminSection.root.visibility = View.VISIBLE
                users -> userAdminSection.root.visibility = View.VISIBLE
            }
        }
    }

    private fun performLogout() {
        stopSpeaking()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // ----------------------------------------------------------
    // 🔹 Configuración del RecyclerView de Usuarios
    // ----------------------------------------------------------
    private fun setupRecyclerView() {
        val recycler = binding.userAdminSection.recyclerViewUsuarios
        recycler.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(emptyList()) { user ->
            showUserDialog(user)
        }
        recycler.adapter = userAdapter
    }

    // ----------------------------------------------------------
    // 🔹 Configuración de Tabs para filtrar usuarios
    // ----------------------------------------------------------
    private fun setupTabs() {
        val tabLayout = binding.userAdminSection.tabLayoutUsuarios

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadUsers("active")
                    1 -> loadUsers("blocked")
                    2 -> loadUsers("all")
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    // ----------------------------------------------------------
    // 🔹 Cargar usuarios desde el backend
    // ----------------------------------------------------------
    private fun loadUsers(type: String) {
        lifecycleScope.launch {
            try {
                val response = when (type) {
                    "active" -> RetrofitClient.apiUser.getActiveUsers()
                    "blocked" -> RetrofitClient.apiUser.getBlockedUsers()
                    else -> RetrofitClient.apiUser.getAllUsers()
                }

                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    userAdapter.updateList(users)
                    Log.d(TAG, "$type -> ${users.size} usuarios cargados")
                } else {
                    showError("Error al obtener usuarios (${response.code()})")
                }

            } catch (e: Exception) {
                showError("Error de conexión: ${e.message}")
            }
        }
    }

    // ----------------------------------------------------------
    // 🔹 Diálogo de detalles del usuario con bloqueo/desbloqueo
    // ----------------------------------------------------------
    private fun showUserDialog(user: UserResponse) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_admin_card_user, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val txtName = dialogView.findViewById<TextView>(R.id.textViewUserName)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.buttonCloseDialog)
        val btnBlock = dialogView.findViewById<MaterialButton>(R.id.buttonBlockUser)

        txtName.text = user.nombre

        btnBlock.text = if (user.estado == "activo") "Bloquear Usuario" else "Desbloquear Usuario"

        btnClose.setOnClickListener { dialog.dismiss() }

        btnBlock.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val nuevoEstado = if (user.estado == "activo") "bloqueado" else "activo"
                    val repo = UserRepository()
                    val success = repo.updateEstado(user.id, nuevoEstado)

                    if (success) {
                        Toast.makeText(
                            this@AdminDashboardActivity,
                            "Usuario ${user.nombre} ahora está $nuevoEstado",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadUsers("all")
                    } else {
                        Toast.makeText(
                            this@AdminDashboardActivity,
                            "Error al actualizar estado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    // ----------------------------------------------------------
    // Cargar configuraciones de gráficas desde backend
    // ----------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.M)
    private fun cargarGraficasGuardadas() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔄 Solicitando configuración de gráficas al servidor...")
                val configuraciones = graficasRepo.getGraficas()

                if (configuraciones.isEmpty()) {
                    Log.w(TAG, "⚠️ No hay configuraciones guardadas, usando valores por defecto")
                    setupChartsDefault()
                    return@launch
                }

                configuraciones.forEach { config ->
                    val tipo = config.tipo_grafica
                    val sensorId = config.sensor_id

                    when (sensorId) {
                        2 -> updateChart(
                            binding.graficasAdminSection.graphContainerTemp,
                            tipo,
                            "Temperatura",
                            binding.graficasAdminSection.btnChangeTemp,
                            sensorId
                        )
                        3 -> updateChart(
                            binding.graficasAdminSection.graphContainerPressure,
                            tipo,
                            "Presión",
                            binding.graficasAdminSection.btnChangePressure,
                            sensorId
                        )
                        1 -> updateChart(
                            binding.graficasAdminSection.graphContainerGas,
                            tipo,
                            "Metano",
                            binding.graficasAdminSection.btnChangeGas,
                            sensorId
                        )
                    }
                }

                Toast.makeText(this@AdminDashboardActivity, "Configuraciones cargadas", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al cargar configuraciones", e)
                Toast.makeText(this@AdminDashboardActivity, "Error al cargar configuraciones", Toast.LENGTH_SHORT).show()
                setupChartsDefault()
            }
        }
    }

    // ----------------------------------------------------------
    // Gráficas por defecto
    // ----------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupChartsDefault() {
        Log.d(TAG, "📊 Configurando gráficas por defecto (línea)")
        updateChart(binding.graficasAdminSection.graphContainerTemp, "line", "Temperatura", binding.graficasAdminSection.btnChangeTemp, 2)
        updateChart(binding.graficasAdminSection.graphContainerPressure, "line", "Presión", binding.graficasAdminSection.btnChangePressure, 3)
        updateChart(binding.graficasAdminSection.graphContainerGas, "line", "Metano", binding.graficasAdminSection.btnChangeGas, 1)
    }

    // ----------------------------------------------------------
    // Botones para cambiar tipo de gráfica
    // ----------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.M)
    private fun setupChartClickListeners() {
        binding.graficasAdminSection.btnChangeTemp.setOnClickListener {
            showGraphTypeDialog("Temperatura", binding.graficasAdminSection.graphContainerTemp, binding.graficasAdminSection.btnChangeTemp, 2)
        }
        binding.graficasAdminSection.btnChangePressure.setOnClickListener {
            showGraphTypeDialog("Presión", binding.graficasAdminSection.graphContainerPressure, binding.graficasAdminSection.btnChangePressure, 3)
        }
        binding.graficasAdminSection.btnChangeGas.setOnClickListener {
            showGraphTypeDialog("Metano", binding.graficasAdminSection.graphContainerGas, binding.graficasAdminSection.btnChangeGas, 1)
        }
    }

    // ----------------------------------------------------------
    // Diálogo para cambiar tipo de gráfica y guardar backend
    // ----------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.M)
    private fun showGraphTypeDialog(label: String, container: FrameLayout, button: ImageView, sensorId: Int) {
        val graphTypes = arrayOf("Gráfica de Barra", "Gráfica de Línea", "Gráfica Circular")
        MaterialAlertDialogBuilder(this)
            .setTitle("Seleccionar Tipo de Gráfica")
            .setItems(graphTypes) { _, which ->
                val type = when (which) {
                    0 -> "bar"
                    1 -> "line"
                    2 -> "pie"
                    else -> "line"
                }

                updateChart(container, type, label, button, sensorId)

                lifecycleScope.launch {
                    try {
                        Log.d(TAG, "💾 Guardando configuración: sensor=$sensorId, tipo=$type")
                        val result = graficasRepo.updateGrafica(sensorId, type)
                        if (result != null) {
                            Log.d(TAG, "✅ Configuración guardada exitosamente")
                            Toast.makeText(this@AdminDashboardActivity, "Gráfica de $label cambiada a ${graphTypes[which]}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@AdminDashboardActivity, "Error guardando en servidor", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Excepción al guardar", e)
                        Toast.makeText(this@AdminDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    // ----------------------------------------------------------
    // Dibujar gráficas dinámicamente según tipo
    // ----------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateChart(container: FrameLayout, type: String, label: String, button: ImageView, sensorId: Int) {
        container.removeAllViews()

        val chart: Chart<*> = when (type) {
            "bar" -> BarChart(this)
            "line" -> LineChart(this)
            "pie" -> PieChart(this)
            else -> LineChart(this)
        }

        val chartColor = when (sensorId) {
            1 -> resources.getColor(R.color.temp_color, null)
            2 -> resources.getColor(R.color.pressure_color, null)
            3 -> resources.getColor(R.color.gas_color, null)
            else -> resources.getColor(R.color.teal_700, null)
        }

        when (chart) {
            is BarChart -> {
                val entries = listOf(
                    BarEntry(1f, 25f),
                    BarEntry(2f, 28f),
                    BarEntry(3f, 32f)
                )
                val dataSet = BarDataSet(entries, label).apply { color = chartColor }
                chart.data = BarData(dataSet)
                chart.xAxis.isEnabled = false
            }
            is LineChart -> {
                val entries = listOf(
                    Entry(1f, 24.5f),
                    Entry(2f, 26.0f),
                    Entry(3f, 25.8f)
                )
                val dataSet = LineDataSet(entries, label).apply {
                    color = chartColor
                    lineWidth = 2f
                    setDrawCircles(true)
                    setDrawValues(true)
                }
                chart.data = LineData(dataSet)
                chart.xAxis.isEnabled = false
            }
            is PieChart -> {
                val entries = listOf(
                    PieEntry(60f, "Normal"),
                    PieEntry(30f, "Alta"),
                    PieEntry(10f, "Baja")
                )
                val dataSet = PieDataSet(entries, label).apply {
                    colors = listOf(
                        chartColor,
                        resources.getColor(R.color.teal_200, null),
                        resources.getColor(R.color.teal_700, null)
                    )
                }
                chart.data = PieData(dataSet)
                chart.setDrawEntryLabels(false)
            }
        }

        chart.description.isEnabled = false
        chart.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        chart.invalidate()

        container.removeAllViews()
        container.addView(chart)
        container.addView(button)
    }

    // ----------------------------------------------------------
    // Manejo de errores
    // ----------------------------------------------------------
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e(TAG, message)
    }

    // ----------------------------------------------------------
    // Manejar botón de retroceso
    // ----------------------------------------------------------
    override fun onBackPressed() {
        if (binding.adminDashboard.isDrawerOpen(GravityCompat.START)) {
            binding.adminDashboard.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // ----------------------------------------------------------
    // Limpiar recursos al destruir la Activity
    // ----------------------------------------------------------
    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }
}
package com.sena.monitoreo.utils.voice

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.lifecycle.LifecycleOwner
import com.sena.monitoreo.R
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModel // <-- USAMOS EL VM REAL

/**
 * Clase auxiliar para gestionar la configuración de la voz de TTS (Text-to-Speech)
 * en la interfaz de usuario, vinculando los Spinners al ViewModel y al VoiceManager.
 */
class VoiceConfigHelper(
    private val context: Context,
    // AHORA ESPERAMOS EL TIPO REAL USADO EN LA ACTIVITY
    private val viewModel: AdminConfigViewModel,
    private val saveButton: View,
    private val voiceManager: VoiceManager,
    // RECIBIMOS EL CICLO DE VIDA AQUÍ PARA USARLO EN EL OBSERVER
    private val lifecycleOwner: LifecycleOwner // <-- AÑADIDO AL CONSTRUCTOR
) {
    private lateinit var voiceSpinner: Spinner
    private lateinit var pitchSpinner: Spinner

    private var selectedGender: String = "FEMALE"
    private var selectedPitch: Float = 1.0f

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

    /**
     * Configura los Spinners, la observación del ViewModel y el listener del botón de guardar.
     */
    fun setup(spinnerVoz: Spinner, spinnerTono: Spinner) {
        // 1. Almacenar referencias de las vistas
        this.voiceSpinner = spinnerVoz
        this.pitchSpinner = spinnerTono

        // 2. Configurar Spinners
        setupGenderSpinner(voiceSpinner)
        setupPitchSpinner(pitchSpinner)

        // 3. Observar configuración guardada
        setupConfigObserver()

        // 4. Configurar Guardar (LLAMANDO AL VM CORRECTO)
        saveButton.setOnClickListener {
            // El VM es AdminConfigViewModel y tiene saveConfiguration
            viewModel.saveConfiguration(selectedGender, selectedPitch)
        }
    }

    private fun setupGenderSpinner(spinner: Spinner) {
        setupSpinner(spinner, R.array.tipos_de_voz) { parent, position ->
            selectedGender = genderValueMap[parent.getItemAtPosition(position).toString()] ?: "FEMALE"
        }
    }
    private fun setupPitchSpinner(spinner: Spinner) {
        setupSpinner(spinner, R.array.tonos_de_voz) { parent, position ->
            selectedPitch = pitchValueMap[parent.getItemAtPosition(position).toString()] ?: 1.0f
        }
    }

    // (Método setupSpinner se mantiene igual)
    private fun setupSpinner(
        spinner: Spinner,
        arrayId: Int,
        onItemSelected: (parent: AdapterView<*>, position: Int) -> Unit
    ) {
        ArrayAdapter.createFromResource(
            context, arrayId, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                onItemSelected(parent, position)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupConfigObserver() {
        // Usamos el lifecycleOwner del constructor
        viewModel.currentConfig.observe(lifecycleOwner) { config ->
            // 1. Seleccionar Spinners
            // Asumiendo que las propiedades del VoiceConfigResponse son 'voiceGender' y 'voicePitch'
            setSpinnerSelection(voiceSpinner, mapEntries = genderValueMap.entries, value = config.voiceGender)
            setSpinnerSelection(pitchSpinner, mapEntries = pitchValueMap.entries, value = config.voicePitch.toFloat())

            // 2. Sincronizar VoiceManager
            voiceManager.currentPitch = config.voicePitch.toFloat()
            voiceManager.currentGender = config.voiceGender
            voiceManager.applyTtsSettings()
        }
    }

    private fun <T> setSpinnerSelection(spinner: Spinner, mapEntries: Set<Map.Entry<String, T>>, value: T) {
        val keyToSelect = mapEntries.find { it.value == value }?.key

        keyToSelect?.let { key ->
            val adapter = spinner.adapter as? ArrayAdapter<String>
            adapter?.let {
                val position = it.getPosition(key)
                if (position >= 0) {
                    spinner.setSelection(position)
                }
            }
        }
    }
}
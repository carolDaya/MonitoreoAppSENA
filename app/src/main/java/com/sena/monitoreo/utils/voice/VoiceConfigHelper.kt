package com.sena.monitoreo.utils.voice

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.sena.monitoreo.R
import com.sena.monitoreo.ui.admin.viewmodel.AdminConfigViewModel

class VoiceConfigHelper(
    private val context: Context,
    private val viewModel: AdminConfigViewModel,
    private val saveButton: View,
    // 💡 NUEVO: Referencia al VoiceManager
    private val voiceManager: VoiceManager
) {
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

    fun setup(spinnerVoz: Spinner, spinnerTono: Spinner) {
        setupSpinner(spinnerVoz, R.array.tipos_de_voz) { parent, _, position, _ ->
            selectedGender = genderValueMap[parent.getItemAtPosition(position).toString()] ?: "FEMALE"
        }

        setupSpinner(spinnerTono, R.array.tonos_de_voz) { parent, _, position, _ ->
            selectedPitch = pitchValueMap[parent.getItemAtPosition(position).toString()] ?: 1.0f
        }

        viewModel.currentConfig.observe(context as androidx.lifecycle.LifecycleOwner) { config ->
            setSpinnerSelection(spinnerVoz, genderValueMap.entries, config.gender)
            setSpinnerSelection(spinnerTono, pitchValueMap.entries, config.pitch)

            // 💡 CLAVE: Sincronizar el VoiceManager con la nueva configuración
            voiceManager.currentPitch = config.pitch
            voiceManager.currentGender = config.gender
            voiceManager.applyTtsSettings() // Aplicar la configuración inmediatamente
        }

        saveButton.setOnClickListener {
            viewModel.saveConfiguration(selectedGender, selectedPitch)
        }
    }

    private fun setupSpinner(spinner: Spinner, arrayId: Int, listener: (AdapterView<*>, View?, Int, Long) -> Unit) {
        ArrayAdapter.createFromResource(
            context, arrayId, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                listener(parent, view, position, id)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
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
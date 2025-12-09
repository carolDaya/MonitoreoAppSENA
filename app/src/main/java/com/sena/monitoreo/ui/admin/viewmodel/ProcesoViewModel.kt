package com.sena.monitoreo.ui.admin.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sena.monitoreo.data.repository.ProcesoRepository
import com.sena.monitoreo.utils.ResultWrapper
import kotlinx.coroutines.launch
import android.util.Log
import java.io.IOException

class ProcesoViewModel(private val repository: ProcesoRepository) : ViewModel() {

    private val _isProcesoActivo = MutableLiveData<Boolean?>(null)
    val isProcesoActivo: LiveData<Boolean?> = _isProcesoActivo
    private val _procesoStatus = MutableLiveData<String>()
    val procesoStatus: LiveData<String> = _procesoStatus
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Inicia el proceso de monitoreo.
     */
    fun iniciarProceso() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                when (val result = repository.iniciarProceso()) {
                    is ResultWrapper.Success -> {
                        val mensajeFinal = result.data.mensaje ?: "Proceso iniciado correctamente"
                        _procesoStatus.postValue(mensajeFinal)
                        _isProcesoActivo.postValue(true)
                    }
                    is ResultWrapper.Error -> {
                        val mensajeError = result.message ?: "Error desconocido al iniciar el proceso"
                        _procesoStatus.postValue(mensajeError)
                    }
                }
            } catch (e: IOException) {
                val errorMsg = "Error de red: No se pudo conectar al servidor."
                _procesoStatus.postValue(errorMsg)
            } catch (e: Exception) {
                val errorMsg = "Error inesperado: ${e.message}"
                _procesoStatus.postValue(errorMsg)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Finaliza el proceso de monitoreo.
     */
    fun finalizarProceso() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                when (val result = repository.finalizarProceso()) {
                    is ResultWrapper.Success -> {
                        val mensajeFinal = result.data.mensaje ?: "Proceso finalizado correctamente"
                        _procesoStatus.postValue(mensajeFinal)
                        _isProcesoActivo.postValue(false)
                    }
                    is ResultWrapper.Error -> {
                        val mensajeError = result.message ?: "Error desconocido al finalizar el proceso"
                        _procesoStatus.postValue(mensajeError)
                    }
                }
            } catch (e: IOException) {
                val errorMsg = "Error de red: No se pudo conectar al servidor."
                _procesoStatus.postValue(errorMsg)
            } catch (e: Exception) {
                val errorMsg = "Error inesperado: ${e.message}"
                _procesoStatus.postValue(errorMsg)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Método para cargar el estado
     */
    fun loadProcesoStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val result = repository.verificarEstado()) {
                    is ResultWrapper.Success -> {
                        val nuevoEstado = result.data.proceso_activo
                        val estadoAnterior = _isProcesoActivo.value

                        if (estadoAnterior != nuevoEstado) {
                            _isProcesoActivo.value = nuevoEstado
                        } else {
                            Log.d("ProcesoVM", "⚡ Estado MANTENIDO: $nuevoEstado (sin cambios)")
                        }

                        _procesoStatus.value = if (nuevoEstado) "Proceso activo" else "Proceso inactivo"
                    }
                    is ResultWrapper.Error -> {
                        val mensajeError = result.message ?: "Error al verificar estado"
                        _procesoStatus.value = mensajeError
                    }
                }
            } catch (e: IOException) {
                val errorMsg = "Error de red al verificar estado"
                _procesoStatus.value = errorMsg
            } catch (e: Exception) {
                val errorMsg = "Error inesperado: ${e.message}"
                _procesoStatus.value = errorMsg
            } finally {
                _isLoading.value = false
            }
        }
    }
}
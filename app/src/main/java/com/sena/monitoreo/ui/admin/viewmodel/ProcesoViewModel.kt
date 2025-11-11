package com.sena.monitoreo.ui.admin.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sena.monitoreo.data.repository.ProcesoRepository
import kotlinx.coroutines.launch

class ProcesoViewModel(val repository: ProcesoRepository) : ViewModel() {

    // LiveData para el mensaje de estado (éxito o error)
    private val _procesoStatus = MutableLiveData<String>()
    val procesoStatus: LiveData<String> = _procesoStatus

    // LiveData para manejar el estado de carga
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData para saber si hay proceso activo
    private val _isProcesoActivo = MutableLiveData<Boolean>(false)
    val isProcesoActivo: LiveData<Boolean> = _isProcesoActivo

    // Función para iniciar el proceso
    fun iniciarProceso() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.iniciarProceso()
                if (response.isSuccessful) {
                    val mensajeBackend = response.body()?.mensaje
                    val mensajeFinal = if (mensajeBackend.isNullOrEmpty()) {
                        "Proceso iniciado correctamente"
                    } else {
                        mensajeBackend
                    }
                    _procesoStatus.postValue(mensajeFinal)
                    _isProcesoActivo.postValue(true)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val mensajeError = if (errorBody.isNullOrEmpty()) {
                        "Error ${response.code()}: No se pudo iniciar el proceso"
                    } else {
                        errorBody
                    }
                    _procesoStatus.postValue(mensajeError)
                }
            } catch (e: Exception) {
                _procesoStatus.postValue("Error de conexión: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Función para finalizar el proceso
    fun finalizarProceso() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.finalizarProceso()
                if (response.isSuccessful) {
                    val mensajeBackend = response.body()?.mensaje
                    val mensajeFinal = if (mensajeBackend.isNullOrEmpty()) {
                        "Proceso finalizado correctamente"
                    } else {
                        mensajeBackend
                    }
                    _procesoStatus.postValue(mensajeFinal)
                    _isProcesoActivo.postValue(false)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val mensajeError = if (errorBody.isNullOrEmpty()) {
                        "Error ${response.code()}: No se pudo finalizar el proceso"
                    } else {
                        errorBody
                    }
                    _procesoStatus.postValue(mensajeError)
                }
            } catch (e: Exception) {
                _procesoStatus.postValue("Error de conexión: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Verificar estado del proceso
    fun verificarEstadoProceso() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.verificarEstado()
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    _isProcesoActivo.value = responseBody.proceso_activo
                } else {
                    _isProcesoActivo.value = false
                }
            } catch (e: Exception) {
                _isProcesoActivo.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}
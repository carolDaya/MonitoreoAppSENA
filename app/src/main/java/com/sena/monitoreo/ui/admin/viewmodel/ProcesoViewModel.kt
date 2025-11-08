package com.sena.monitoreo.ui.admin.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sena.monitoreo.data.repository.ProcesoRepository
import kotlinx.coroutines.launch

class ProcesoViewModel(private val repository: ProcesoRepository) : ViewModel() {

    // LiveData para el mensaje de estado (éxito o error)
    private val _procesoStatus = MutableLiveData<String>()
    val procesoStatus: LiveData<String> = _procesoStatus

    // LiveData para manejar el estado de carga
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData para saber si hay proceso activo (requiere un endpoint GET en el backend)
    private val _isProcesoActivo = MutableLiveData<Boolean>(false)
    val isProcesoActivo: LiveData<Boolean> = _isProcesoActivo

    // Función para iniciar el proceso
    fun iniciarProceso() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = repository.iniciarProceso()
                if (response.isSuccessful) {
                    // Éxito: "Proceso iniciado correctamente."
                    _procesoStatus.postValue(response.body()?.mensaje ?: "Proceso iniciado (mensaje vacío).")
                    _isProcesoActivo.postValue(true) // Actualizar estado
                } else {
                    // Error: "Ya existe un proceso activo." o error del servidor
                    val errorBody = response.errorBody()?.string()
                    // Intenta parsear el error para mostrar el mensaje del backend
                    _procesoStatus.postValue(errorBody ?: "Error ${response.code()}: No se pudo iniciar el proceso.")
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
                    // Éxito: "Proceso finalizado correctamente."
                    _procesoStatus.postValue(response.body()?.mensaje ?: "Proceso finalizado (mensaje vacío).")
                    _isProcesoActivo.postValue(false) // Actualizar estado
                } else {
                    // Error: "No hay procesos activos para finalizar."
                    val errorBody = response.errorBody()?.string()
                    _procesoStatus.postValue(errorBody ?: "Error ${response.code()}: No se pudo finalizar el proceso.")
                }
            } catch (e: Exception) {
                _procesoStatus.postValue("Error de conexión: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // 💡 IMPORTANTE: Si tienes el endpoint hay_proceso_activo() en el backend, úsalo aquí
    fun verificarEstadoProceso() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.verificarEstado()

                // 💡 Asegúrate de que response.body() no sea nulo antes de acceder a sus propiedades
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!

                    // ✅ CORRECCIÓN CLAVE: Usar el booleano 'proceso_activo' directamente
                    _isProcesoActivo.value = responseBody.proceso_activo

                    // Usar el mensaje para la notificación (Toast)
                    _procesoStatus.value = "Estado actual verificado: ${responseBody.mensaje}"

                } else {
                    _procesoStatus.value = "Error ${response.code()} al verificar estado."
                    _isProcesoActivo.value = false
                }
            } catch (e: Exception) {
                _procesoStatus.value = "Error de conexión al verificar estado: ${e.message}"
                _isProcesoActivo.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}

package com.sena.monitoreo.ui.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sena.monitoreo.data.repository.UserRepository
import com.sena.monitoreo.data.model.user.UserResponse
import com.sena.monitoreo.utils.ResultWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import java.io.IOException

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _users = MutableStateFlow<List<UserResponse>>(emptyList())
    val users: StateFlow<List<UserResponse>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 💡 Nuevo StateFlow para errores de red
    private val _networkError = MutableStateFlow<String?>(null)
    val networkError: StateFlow<String?> = _networkError.asStateFlow()

    private val TAG = "UserViewModel"


    /**
     * Carga los usuarios filtrados por estado (activo, bloqueado, all).
     */
    fun loadAllUsers(estado: String = "all") {
        viewModelScope.launch {
            _isLoading.value = true
            _networkError.value = null // Limpiar error antes de iniciar

            try {
                // Seleccionar la llamada al repositorio según el estado solicitado
                val resultWrapper = when (estado.lowercase()) {
                    "activo" -> repository.getActiveUsers()
                    "bloqueado" -> repository.getBlockedUsers()
                    else -> repository.getAllUsers()
                }

                if (resultWrapper is ResultWrapper.Success) {
                    _users.value = resultWrapper.data
                    Log.d(TAG, "Cargando usuarios ($estado): ${_users.value.size} resultados")
                } else if (resultWrapper is ResultWrapper.Error) {
                    Log.e(TAG, "Error al cargar usuarios por estado ($estado): ${resultWrapper.message}")
                    _users.value = emptyList()
                    // No reportamos error de red aquí a menos que sea una IOException
                }

            } catch (e: IOException) {
                val errorMessage = "Error de red al cargar usuarios: No se pudo conectar al servidor."
                Log.e(TAG, errorMessage, e)
                _networkError.value = errorMessage // 💡 Reportar error de red
                _users.value = emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al cargar usuarios: ${e.message}", e)
                _users.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualiza el estado de un usuario (bloquear/activar) y recarga la lista.
     */
    fun updateUserEstado(userId: Int, nuevoEstado: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _networkError.value = null // Limpiar error antes de iniciar
            try {
                val resultWrapper = repository.updateEstado(userId, nuevoEstado)

                if (resultWrapper is ResultWrapper.Success) {
                    Log.d(TAG, "Estado de usuario $userId actualizado a $nuevoEstado con éxito.")
                    // Recargar la lista completa para reflejar el cambio en la vista actual
                    loadAllUsers("all")

                } else if (resultWrapper is ResultWrapper.Error) {
                    Log.e(TAG, "Error al actualizar estado del usuario $userId: ${resultWrapper.message}")
                    // No reportamos error de red aquí a menos que sea una IOException
                }

            } catch (e: IOException) {
                val errorMessage = "Error de red: No se pudo actualizar el estado del usuario $userId."
                Log.e(TAG, errorMessage, e)
                _networkError.value = errorMessage // 💡 Reportar error de red
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al actualizar estado del usuario $userId: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 💡 Función para que la actividad limpie el error después de mostrarlo
    fun clearNetworkError() {
        _networkError.value = null
    }
}
package com.sena.monitoreo.ui.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sena.monitoreo.data.repository.UserRepository
import com.sena.monitoreo.data.model.user.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _users = MutableStateFlow<List<UserResponse>>(emptyList())
    val users: StateFlow<List<UserResponse>> = _users

    // 💡 NUEVO: Variable para gestionar el estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val TAG = "UserViewModel"


    /**
     * Carga los usuarios filtrados por estado (activo, bloqueado, all).
     * @param estado El estado del usuario a cargar. Por defecto, carga todos.
     */
    fun loadAllUsers(estado: String = "all") {
        viewModelScope.launch {
            _isLoading.value = true // 💡 Inicia la carga
            try {
                val result = when (estado.lowercase()) {
                    "activo" -> repository.getActiveUsers()
                    "bloqueado" -> repository.getBlockedUsers()
                    else -> repository.getAllUsers()
                }
                _users.value = result ?: emptyList()
                Log.d(TAG, "Cargando usuarios ($estado): ${_users.value.size} resultados")
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar usuarios por estado ($estado): ${e.message}", e)
                _users.value = emptyList()
            } finally {
                _isLoading.value = false // 💡 Finaliza la carga
            }
        }
    }

    fun updateUserEstado(userId: Int, nuevoEstado: String) {
        viewModelScope.launch {
            _isLoading.value = true // 💡 Inicia la carga para la actualización
            try {
                repository.updateEstado(userId, nuevoEstado)

                // Recarga la lista para reflejar el cambio.
                // Usamos loadAllUsers(estado actual) si deseas recargar solo la pestaña visible.
                // Mantendremos "all" para asegurar que el cambio se procese correctamente.
                loadAllUsers("all")

            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar estado del usuario $userId: ${e.message}")
            } finally {
                // NOTA: Si loadAllUsers() se ejecuta sin error, él mismo pondrá _isLoading.value = false.
                // Si la actualización falla ANTES de llamar a loadAllUsers, debe cerrarse aquí:
                if (_isLoading.value) {
                    _isLoading.value = false
                }
            }
        }
    }
}
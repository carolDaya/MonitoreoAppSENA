package com.sena.monitoreo.ui.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sena.monitoreo.data.repository.UserRepository
import com.sena.monitoreo.data.model.user.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.Math.log
import kotlin.math.log

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _users = MutableStateFlow<List<UserResponse>>(emptyList())
    val users: StateFlow<List<UserResponse>> = _users

    fun loadAllUsers() {
        viewModelScope.launch {
            try {
                val result = repository.getAllUsers()
                _users.value = result ?: emptyList()
            } catch (e: Exception) {
                _users.value = emptyList()
            }
        }
    }

    fun updateUserEstado(userId: Int, nuevoEstado: String) {
        viewModelScope.launch {
            try {
                val success = repository.updateEstado(userId, nuevoEstado)
                if (success) {
                    loadAllUsers()
                }
            } catch (e: Exception) {
            }
        }
    }
}

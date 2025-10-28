package com.sena.monitoreo.ui.user

import android.util.Log
import androidx.lifecycle.*
import com.sena.monitoreo.data.model.user.UserResponse
import com.sena.monitoreo.data.repository.UserRepository
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UserRepository) : ViewModel() {
    private val _users = MutableLiveData<List<UserResponse>>()
    val users: LiveData<List<UserResponse>> = _users

    fun loadUsers(type: String) {
        viewModelScope.launch {
            try {
                val response = when (type) {
                    "active" -> repository.getActiveUsers()
                    "blocked" -> repository.getBlockedUsers()
                    else -> repository.getAllUsers()
                }

                if (response.isSuccessful) {
                    _users.value = response.body() ?: emptyList()
                } else {
                    Log.e("UserViewModel", "Error ${response.code()}: ${response.errorBody()?.string()}")
                    _users.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Exception loading users", e)
                _users.value = emptyList()
            }
        }
    }
}
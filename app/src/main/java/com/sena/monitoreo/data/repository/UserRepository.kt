package com.sena.monitoreo.data.repository
import com.sena.monitoreo.data.api.ApiUser
import com.sena.monitoreo.data.model.user.UserResponse
import retrofit2.Response

class UserRepository(private val userApi: ApiUser) {
    suspend fun getAllUsers(): Response<List<UserResponse>> = userApi.getAllUsers().execute()
    suspend fun getActiveUsers(): Response<List<UserResponse>> = userApi.getActiveUsers().execute()
    suspend fun getBlockedUsers(): Response<List<UserResponse>> = userApi.getBlockedUsers().execute()
}
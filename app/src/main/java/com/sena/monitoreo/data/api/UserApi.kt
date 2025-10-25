package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.UserResponse
import retrofit2.Call
import retrofit2.http.GET

interface UserApi {

    @GET("users")
    fun getAllUsers(): Call<List<UserResponse>>

    @GET("users/active")
    fun getActiveUsers(): Call<List<UserResponse>>

    @GET("users/blocked")
    fun getBlockedUsers(): Call<List<UserResponse>>
}

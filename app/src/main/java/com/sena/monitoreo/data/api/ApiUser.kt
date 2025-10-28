package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.user.UpdateEstadoRequest
import com.sena.monitoreo.data.model.user.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiUser {

    @GET("users")
    suspend fun getAllUsers(): Response<List<UserResponse>>

    @GET("users/active")
    suspend fun getActiveUsers(): Response<List<UserResponse>>

    @GET("users/blocked")
    suspend fun getBlockedUsers(): Response<List<UserResponse>>

    @PUT("users/{id}/estado")
    suspend fun updateEstado(
        @Path("id") id: Int,
        @Body request: UpdateEstadoRequest
    ): Response<Unit>



}

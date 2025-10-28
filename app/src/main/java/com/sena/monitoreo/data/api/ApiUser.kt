package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.user.*
import retrofit2.Call
import retrofit2.http.GET

/**
 * Define los endpoints para la gestión de usuarios (consulta de listas activas, bloqueadas y todas).
 * Las rutas se mapean directamente a las definidas en el Blueprint 'users' de Flask.
 */
interface ApiUser {

    /**
     * Obtiene todos los usuarios, independientemente de su estado.
     * Mapea a: GET /users
     */
    @GET("users")
    fun getAllUsers(): Call<List<UserResponse>>

    /**
     * Obtiene solo los usuarios con estado 'activo'.
     * Mapea a: GET /users/active
     */
    @GET("users/active")
    fun getActiveUsers(): Call<List<UserResponse>>

    /**
     * Obtiene solo los usuarios con estado 'bloqueado'.
     * Mapea a: GET /users/blocked
     */
    @GET("users/blocked")
    fun getBlockedUsers(): Call<List<UserResponse>>
}

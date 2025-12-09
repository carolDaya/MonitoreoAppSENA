package com.sena.monitoreo.data.api

import com.sena.monitoreo.data.model.user.UpdateEstadoRequest
import com.sena.monitoreo.data.model.user.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiUser {

    /**
     * Obtiene todos los usuarios.
     * @return Response con la lista completa de usuarios.
     */
    @GET("users")
    suspend fun getAllUsers(): Response<List<UserResponse>>

    /**
     * Obtiene únicamente los usuarios activos.
     * @return Response con la lista de usuarios activos.
     */
    @GET("users/active")
    suspend fun getActiveUsers(): Response<List<UserResponse>>

    /**
     * Obtiene únicamente los usuarios bloqueados.
     * @return Response con la lista de usuarios bloqueados.
     */
    @GET("users/blocked")
    suspend fun getBlockedUsers(): Response<List<UserResponse>>

    /**
     * Actualiza el estado de un usuario (activo/bloqueado).
     * @param id ID del usuario.
     * @param request Objeto con el nuevo estado.
     * @return Response vacío indicando éxito o error.
     */
    @PUT("users/{id}/estado")
    suspend fun updateEstado(
        @Path("id") id: Int,
        @Body request: UpdateEstadoRequest
    ): Response<Unit>
}

package com.sena.monitoreo.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sena.monitoreo.R
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.model.UserResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ActiveUsersFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    // Asegúrate de que esta clase exista y esté importada
    private lateinit var userAdapter: UserListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla el layout del RecyclerView
        return inflater.inflate(R.layout.fragment_active_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Asegúrate de que este ID coincida con fragment_active_users.xml
        recyclerView = view.findViewById(R.id.recycler_active_users)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Inicializa un adaptador vacío
        userAdapter = UserListAdapter(emptyList())
        recyclerView.adapter = userAdapter

        fetchActiveUsers()
    }

    private fun fetchActiveUsers() {
        // ***************************************************************
        // LÍNEA CORREGIDA: Accede a la instancia de la API directamente
        // ***************************************************************
        val apiService = RetrofitClient.userApi

        // La llamada a la función ya es correcta
        apiService.getActiveUsers().enqueue(object : Callback<List<UserResponse>> {
            override fun onResponse(
                call: Call<List<UserResponse>>,
                response: Response<List<UserResponse>>
            ) {
                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    // Si el backend no filtra, filtramos aquí. Si el backend filtra,
                    // simplemente usamos 'users'.
                    val activeUsers = users.filter { it.estado.equals("activo", ignoreCase = true) }

                    // El método updateUsers() está en tu UserListAdapter.kt
                    userAdapter.updateUsers(activeUsers)

                } else {
                    Log.e("ActiveUsersFragment", "Error en la respuesta: ${response.code()}")
                    Toast.makeText(context, "Error al cargar usuarios: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                Log.e("ActiveUsersFragment", "Fallo de red: ${t.message}")
                Toast.makeText(context, "Fallo de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
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
import com.sena.monitoreo.data.api.RetrofitClient // Acceso al objeto RetrofitClient
import com.sena.monitoreo.data.model.UserResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AllUsersFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Asegúrate de que este layout exista: fragment_all_users.xml
        return inflater.inflate(R.layout.fragment_all_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Asegúrate de que este ID coincida con fragment_all_users.xml
        recyclerView = view.findViewById(R.id.recycler_all_users)
        recyclerView.layoutManager = LinearLayoutManager(context)

        userAdapter = UserListAdapter(emptyList())
        recyclerView.adapter = userAdapter

        fetchAllUsers()
    }

    private fun fetchAllUsers() {
        // ***************************************************************
        // LÍNEA CORREGIDA: Accede a la instancia de la API directamente
        // ***************************************************************
        val apiService = RetrofitClient.userApi

        // ¡Se utiliza getAllUsers! Esto elimina la última advertencia de la API.
        apiService.getAllUsers().enqueue(object : Callback<List<UserResponse>> {
            override fun onResponse(
                call: Call<List<UserResponse>>,
                response: Response<List<UserResponse>>
            ) {
                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    // Muestra la lista completa sin filtrar
                    userAdapter.updateUsers(users)

                } else {
                    Log.e("AllUsersFragment", "Error en la respuesta: ${response.code()}")
                    Toast.makeText(context, "Error al cargar todos los usuarios: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                Log.e("AllUsersFragment", "Fallo de red: ${t.message}")
                Toast.makeText(context, "Fallo de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
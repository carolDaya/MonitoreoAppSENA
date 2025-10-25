package com.sena.monitoreo.ui.admin

// ... Importaciones (iguales que ActiveUsersFragment) ...
import android.os.Bundle
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

class BlockedUsersFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Necesitas un layout XML separado, por ejemplo, fragment_blocked_users.xml
        return inflater.inflate(R.layout.fragment_blocked_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Asegúrate de usar el ID correcto para este RecyclerView
        recyclerView = view.findViewById(R.id.recycler_blocked_users)
        recyclerView.layoutManager = LinearLayoutManager(context)

        userAdapter = UserListAdapter(emptyList())
        recyclerView.adapter = userAdapter

        fetchBlockedUsers()
    }

    private fun fetchBlockedUsers() {
        val apiService = RetrofitClient.userApi // Acceso limpio

        // ¡Se utiliza getBlockedUsers! (Advertencia eliminada)
        apiService.getBlockedUsers().enqueue(object : Callback<List<UserResponse>> {
            override fun onResponse(call: Call<List<UserResponse>>, response: Response<List<UserResponse>>) {
                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    userAdapter.updateUsers(users)
                } else {
                    Toast.makeText(context, "Error al cargar bloqueados: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                Toast.makeText(context, "Fallo de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
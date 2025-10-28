package com.sena.monitoreo.ui.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.sena.monitoreo.data.api.RetrofitClient
import com.sena.monitoreo.data.repository.UserRepository
import com.sena.monitoreo.databinding.IncludeUserAdminBinding // Debe coincidir con el nombre de tu archivo include
import com.sena.monitoreo.R // Asegúrate de que R se importe correctamente

class UserListFragment : Fragment() {

    private var _binding: IncludeUserAdminBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: UserViewModel
    private lateinit var userAdapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = IncludeUserAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicialización de ViewModel
        val userApi = RetrofitClient.apiUsers
        val repository = UserRepository(userApi)
        val viewModelFactory = UserViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory).get(UserViewModel::class.java)

        // 2. Configuración del RecyclerView
        userAdapter = UserAdapter(emptyList())
        binding.recyclerViewUsuarios.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = userAdapter
        }

        // 3. Configuración del TabLayout
        setupTabLayout()

        // 4. Observadores (LiveData)
        setupObservers()

        // 5. Carga inicial: Usuarios Activos
        viewModel.loadUsers("active")
    }

    private fun setupTabLayout() {
        binding.tabLayoutUsuarios.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // Determina qué tipo de usuario cargar basado en la posición de la pestaña
                val userType = when (tab.position) {
                    0 -> "active"   // Primera pestaña: Usuarios activos
                    1 -> "blocked"  // Segunda pestaña: Usuarios bloqueados
                    2 -> "all"      // Tercera pestaña: Todos los usuarios
                    else -> "active"
                }
                Log.d("UserListFragment", "Cargando usuarios: $userType")
                viewModel.loadUsers(userType)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupObservers() {
        // Observa la lista de usuarios y actualiza el adaptador
        viewModel.users.observe(viewLifecycleOwner) { userList ->
            Log.d("UserListFragment", "Recibidos ${userList.size} usuarios.")
            userAdapter.updateUsers(userList)

            // Si el RecyclerView está dentro de un NestedScrollView, forzamos la actualización.
            // Si no lo haces, podría no mostrarse correctamente.
            binding.recyclerViewUsuarios.requestLayout()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
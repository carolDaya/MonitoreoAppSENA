package com.sena.monitoreo.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
// Usaremos ViewBinding para acceder a los elementos del layout incluido
import com.sena.monitoreo.databinding.IncludeUserAdminBinding // Asume que se generó este binding

class UserManagementFragment : Fragment() {

    // Usamos el ViewBinding generado para include_user_admin.xml
    private var _binding: IncludeUserAdminBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Asume que el nombre del binding generado es IncludeUserAdminBinding
        _binding = IncludeUserAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Configurar el adaptador para el ViewPager2
        val viewPager = binding.viewPagerUsers

        // El adaptador maneja la lógica de qué fragmento mostrar en cada pestaña
        // Se usa childFragmentManager porque está dentro de otro Fragment (AdminDashboardActivity)
        val adapter = UserPagerAdapter(this)
        viewPager.adapter = adapter

        // 2. Vincular el TabLayout con el ViewPager2
        val tabLayout = binding.tabLayoutUsuarios

        // Define los nombres de las pestañas
        val tabTitles = listOf("Activos", "Bloqueados", "Todos")

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar la referencia del binding para evitar fugas de memoria
        _binding = null
    }
}
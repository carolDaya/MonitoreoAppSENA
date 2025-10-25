package com.sena.monitoreo.ui.admin

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

// Recibe el fragmento contenedor (UserManagementFragment)
class UserPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    // Son 3 fragments: Activos, Bloqueados, Todos
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            // El orden aquí DEBE coincidir con el orden que establezcas en el TabLayoutMediator
            // del UserManagementFragment
            0 -> ActiveUsersFragment()
            1 -> BlockedUsersFragment()
            2 -> AllUsersFragment()
            else -> ActiveUsersFragment()
        }
    }
}
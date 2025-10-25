// UserListAdapter.kt
package com.sena.monitoreo.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sena.monitoreo.R
import com.sena.monitoreo.data.model.UserResponse
// Asegúrate de importar el modelo de datos

// Necesitarás definir la vista de un solo ítem, asumimos sec_item_user_admin
class UserListAdapter(
    private var users: List<UserResponse>
) : RecyclerView.Adapter<UserListAdapter.UserViewHolder>() {

    // Define un ViewHolder interno para mantener las vistas del ítem
    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Aquí deberías inicializar tus TextViews, ImageViews, etc.
        // Ejemplo: val userName: TextView = view.findViewById(R.id.user_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // Infla el layout de un solo ítem de usuario
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sec_item_user_admin, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        // Aquí debes asignar los datos del 'user' a las vistas del 'holder'
        // Ejemplo: holder.userName.text = user.nombre
    }

    override fun getItemCount(): Int = users.size

    // Método que el Fragmento llama para actualizar la lista de usuarios
    fun updateUsers(newUsers: List<UserResponse>) {
        users = newUsers
        // Notifica al RecyclerView que los datos han cambiado
        notifyDataSetChanged()
    }
}
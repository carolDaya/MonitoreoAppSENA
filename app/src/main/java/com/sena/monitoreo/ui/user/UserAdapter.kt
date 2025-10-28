package com.sena.monitoreo.ui.user

// com.sena.monitoreo.ui.users.UserAdapter.kt
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sena.monitoreo.R
import com.sena.monitoreo.data.model.user.UserResponse

// Importante: Asumo que tienes el layout sec_item_user_admin.xml creado
class UserAdapter(private var users: List<UserResponse>) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    fun updateUsers(newUsers: List<UserResponse>) {
        this.users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sec_item_user_admin, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Asumo que estos IDs son los que realmente usas en tu sec_item_user_admin.xml
        private val nameTextView: TextView = itemView.findViewById(R.id.text_nombre_usuario)
        // Ya que tu item solo muestra Nombre, el resto lo podrías cargar en un detalle
        // Pero para el ejercicio, usaremos solo el nombre como lo indica tu último layout.

        fun bind(user: UserResponse) {
            // Muestra solo el nombre y el estado
            nameTextView.text = user.nombre

            // Aquí podrías cambiar el icono o el fondo según el estado
            val colorResId = if (user.estado == "activo") {
                R.color.teal_deep // Asume color para activo
            } else {
                R.color.red // Asume color para bloqueado
            }
            // Ejemplo: cambiar color del texto del nombre
            nameTextView.setTextColor(ContextCompat.getColor(itemView.context, colorResId))
        }
    }
}
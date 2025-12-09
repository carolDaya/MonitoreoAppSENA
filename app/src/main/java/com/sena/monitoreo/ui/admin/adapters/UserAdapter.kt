package com.sena.monitoreo.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.sena.monitoreo.R
import com.sena.monitoreo.data.model.user.UserResponse

class UserAdapter(
    private var userList: List<UserResponse>,
    private val onViewClick: (UserResponse) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgEstado: ImageView = itemView.findViewById(R.id.img_indicador_estado)
        val txtNombre: TextView = itemView.findViewById(R.id.text_nombre_usuario)
        val btnVer: MaterialButton = itemView.findViewById(R.id.btn_ver_usuario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sec_item_user_admin, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.txtNombre.text = user.nombre

        //  Mostrar ícono según estado
        val iconRes = when (user.estado?.lowercase()) {
            "activo" -> R.drawable.ic_admin_active_user
            "bloqueado" -> R.drawable.ic_admin_blocked_user
            else -> R.drawable.ic_admin_unknown_user // opcional si quieres manejar nulos
        }
        holder.imgEstado.setImageResource(iconRes)

        //  Acción al pulsar el botón "Ver"
        holder.btnVer.setOnClickListener {
            onViewClick(user)
        }
    }

    override fun getItemCount(): Int = userList.size

    // Método para actualizar la lista
    fun updateList(newList: List<UserResponse>) {
        userList = newList
        notifyDataSetChanged()
    }
}

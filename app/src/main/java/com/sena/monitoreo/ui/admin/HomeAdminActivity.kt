package com.sena.monitoreo.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.R
import com.sena.monitoreo.databinding.ActivityHomeAdminBinding
import com.sena.monitoreo.ui.auth.LoginActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeAdminBinding
    private val SPLASH_TIME_OUT: Long = 2000 // 2 segundos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verificamos si viene desde el menú o desde launcher
        val fromMenu = intent.getBooleanExtra("FROM_MENU", false)

        if (!fromMenu) {
            // 👇 Solo aplica splash si vienes desde el launcher
            lifecycleScope.launch {
                delay(SPLASH_TIME_OUT)
                val intent = Intent(this@HomeAdminActivity, AdminDashboardActivity::class.java)
                startActivity(intent)
                finish() // cerramos el splash
            }
        }

        // --- Menú lateral ---
        // Conecta el icono de ajustes para abrir el menú
        binding.mainHeader.settingsIcon.setOnClickListener {
            binding.homeAdmin.openDrawer(GravityCompat.START)
        }

        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_graphis -> {
                    val intent = Intent(this, AdminDashboardActivity::class.java)
                    intent.putExtra("SCROLL_TO", "graphs")
                    startActivity(intent)
                }
                R.id.nav_volumen -> {
                    val intent = Intent(this, AdminDashboardActivity::class.java)
                    intent.putExtra("SCROLL_TO", "ai")
                    startActivity(intent)
                }
                R.id.nav_users -> {
                    val intent = Intent(this, AdminDashboardActivity::class.java)
                    intent.putExtra("SCROLL_TO", "users")
                    startActivity(intent)
                }
                R.id.nav_settings -> {
                    // Acción para la configuración
                    Toast.makeText(this, "Configuración Admin", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_logout -> {
                    // Lógica para cerrar sesión
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().clear().apply()

                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                    Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                }
            }
            binding.homeAdmin.closeDrawer(GravityCompat.START)
            true
        }
    }
}
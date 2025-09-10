package com.sena.monitoreo.ui.user

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.R
import com.sena.monitoreo.databinding.ActivityHomeUserBinding
import com.sena.monitoreo.ui.auth.LoginActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeUserBinding
    private val SPLASH_TIME_OUT: Long = 2000 // 3 segundos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verificamos si viene desde el menú o desde launcher
        val fromMenu = intent.getBooleanExtra("FROM_MENU", false)

        if (!fromMenu) {
            // 👇 Solo aplica splash si vienes desde el launcher
            lifecycleScope.launch {
                delay(SPLASH_TIME_OUT)
                val intent = Intent(this@HomeUserActivity, SensorDataActivity::class.java)
                startActivity(intent)
                finish() // cerramos el splash
            }
        }

        // --- Menú lateral ---
        binding.mainHeaderUser.menuIcon.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_datos_gas -> {
                    val intent = Intent(this, SensorDataActivity::class.java)
                    intent.putExtra("SCROLL_TO", "gas")
                    startActivity(intent)
                }
                R.id.nav_datos_tem -> {
                    val intent = Intent(this, SensorDataActivity::class.java)
                    intent.putExtra("SCROLL_TO", "temp")
                    startActivity(intent)
                }
                R.id.nav_datos_presion -> {
                    val intent = Intent(this, SensorDataActivity::class.java)
                    intent.putExtra("SCROLL_TO", "pressure")
                    startActivity(intent)
                }
                else -> {
                    // Limpia los datos de sesión si los guardas en SharedPreferences
                    val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    prefs.edit().clear().apply()

                    // Ir al LoginActivity y limpiar el back stack
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                    Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
}

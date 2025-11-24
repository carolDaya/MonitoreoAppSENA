package com.sena.monitoreo.utils.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.sena.monitoreo.R
import com.sena.monitoreo.ui.admin.AdminDashboardActivity
import com.sena.monitoreo.ui.admin.HomeAdminActivity
import com.sena.monitoreo.ui.auth.LoginActivity
import com.sena.monitoreo.ui.user.HomeUserActivity
import com.sena.monitoreo.ui.user.SensorDataActivity
import com.sena.monitoreo.utils.UiUtils

/**
 * Gestiona la navegación a través del NavigationView para usuarios y administradores.
 */
class NavigationManager(
    private val context: Context,
    private val drawerLayout: DrawerLayout,
    private val navigationView: NavigationView,
    private val currentActivity: String = "",
    private val view: View? = null
) {
    companion object {
        private const val PREFS_NAME = "app_prefs"
    }

    /**
     * Configura el listener de navegación basándose en el rol proporcionado.
     * Esto resuelve el 'Unresolved reference: setupNavigation' en HomeUserActivity.
     */
    fun setupNavigation(role: String) {
        // La actividad de inicio del usuario (HomeUserActivity) llama a setupNavigation("home").
        // La actividad de inicio del admin (HomeAdminActivity) podría llamar a setupNavigation("admin").

        if (role.contains("user", ignoreCase = true) || currentActivity == "home") {
            setupUserNavigation()
        } else if (role.contains("admin", ignoreCase = true) || currentActivity == "home_admin") {
            setupAdminNavigation()
        } else {
            // Si no se especifica rol, y no estamos en una actividad conocida, por defecto usamos User
            setupUserNavigation()
        }
    }


    /**
     * Configura el listener para el menú de navegación del usuario estándar.
     */
    fun setupUserNavigation() {
        navigationView.setNavigationItemSelectedListener { item ->
            handleUserNavigation(item.itemId)
            drawerLayout.closeDrawers()
            true
        }
    }

    /**
     * Configura el listener para el menú de navegación del administrador.
     */
    fun setupAdminNavigation() {
        navigationView.setNavigationItemSelectedListener { item ->
            handleAdminNavigation(item.itemId)
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun handleUserNavigation(itemId: Int) {
        when (itemId) {
            R.id.nav_home -> {
                if (currentActivity != "home") {
                    UiUtils.navigateTo(context, HomeUserActivity::class.java, true)
                    finishCurrentActivity()
                } else {
                    showSnackbar("Ya estás en la pantalla principal.")
                }
            }
            R.id.nav_datos_gas -> {
                if (currentActivity == "sensor_data") {
                    // 💡 CORRECCIÓN: Llamada directa al método público
                    val sensorActivity = context as? SensorDataActivity
                    sensorActivity?.navigateToCard("GAS")
                } else {
                    val intent = Intent(context, SensorDataActivity::class.java).apply {
                        putExtra("SENSOR_TYPE", "GAS")
                    }
                    context.startActivity(intent)
                }
            }
            R.id.nav_datos_tem -> {
                if (currentActivity == "sensor_data") {
                    val sensorActivity = context as? SensorDataActivity
                    sensorActivity?.navigateToCard("TEMP")
                } else {
                    val intent = Intent(context, SensorDataActivity::class.java).apply {
                        putExtra("SENSOR_TYPE", "TEMP")
                    }
                    context.startActivity(intent)
                }
            }
            R.id.nav_datos_presion -> {
                if (currentActivity == "sensor_data") {
                    val sensorActivity = context as? SensorDataActivity
                    sensorActivity?.navigateToCard("PRESSURE")
                } else {
                    val intent = Intent(context, SensorDataActivity::class.java).apply {
                        putExtra("SENSOR_TYPE", "PRESSURE")
                    }
                    context.startActivity(intent)
                }
            }
            R.id.nav_settings -> showSnackbar("Configuración de Usuario.")
            R.id.nav_logout -> logout()
        }
    }

    fun handleAdminNavigation(itemId: Int) {
        when (itemId) {
            R.id.nav_home -> {
                if (currentActivity != "home_admin") {
                    UiUtils.navigateTo(context, HomeAdminActivity::class.java, true)
                    finishCurrentActivity()
                } else {
                    showSnackbar("Ya estás en el Panel de Administración.")
                }
            }
            // Navegar al Dashboard con diferentes secciones de desplazamiento
            R.id.nav_graphis -> navigateToAdminDashboard("graphs")
            R.id.nav_volumen -> navigateToAdminDashboard("ai")
            R.id.nav_users -> navigateToAdminDashboard("users")

            // Mensajes para ítems que aún no navegan
            R.id.nav_datos_gas -> showSnackbar("Datos de Gas (Próximamente).")
            R.id.nav_datos_tem -> showSnackbar("Datos de Temperatura (Próximamente).")
            R.id.nav_datos_presion -> showSnackbar("Datos de Presión (Próximamente).")

            R.id.nav_settings -> showSnackbar("Configuración de Administrador.")
            R.id.nav_logout -> logout()
        }
    }

    /**
     * Navega al Dashboard de Administración con un marcador para desplazar la vista.
     */
    private fun navigateToAdminDashboard(section: String) {
        val intent = Intent(context, AdminDashboardActivity::class.java).apply {
            putExtra("SCROLL_TO", section)
        }
        context.startActivity(intent)
    }

    /**
     * Realiza el proceso de cierre de sesión: limpia preferencias y navega al Login.
     */
    private fun logout() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)

        finishCurrentActivity()
    }

    /**
     * Finaliza la actividad actual si el contexto es una Activity.
     */
    private fun finishCurrentActivity() {
        if (context is Activity) {
            context.finish()
        }
    }

    /**
     * Muestra un Snackbar, utilizando la View proporcionada o la View raíz de la Actividad.
     */
    private fun showSnackbar(message: String) {
        val targetView = view ?: if (context is Activity) {
            context.findViewById(android.R.id.content)
        } else {
            null
        }

        targetView?.let {
            UiUtils.showSnackbar(it, message)
        }
    }
}
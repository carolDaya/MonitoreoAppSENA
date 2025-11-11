package com.sena.monitoreo.utils.navigation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.sena.monitoreo.R
import com.sena.monitoreo.ui.admin.AdminDashboardActivity
import com.sena.monitoreo.ui.admin.HomeAdminActivity
import com.sena.monitoreo.ui.auth.LoginActivity
import com.sena.monitoreo.ui.user.HomeUserActivity
import com.sena.monitoreo.ui.user.SensorDataActivity
import com.sena.monitoreo.utils.UiUtils

class NavigationManager(
    private val context: Context,
    private val drawerLayout: DrawerLayout,
    private val navigationView: NavigationView,
    private val currentActivity: String = "",
    private val view: android.view.View? = null
) {

    fun setupNavigation(currentScreen: String = "") {
        navigationView.setNavigationItemSelectedListener { item ->
            handleUserNavigation(item.itemId, currentScreen)
            drawerLayout.closeDrawers()
            true
        }
    }

    fun setupAdminNavigation(currentScreen: String = "") {
        navigationView.setNavigationItemSelectedListener { item ->
            handleAdminNavigation(item.itemId, currentScreen)
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun handleUserNavigation(itemId: Int, currentScreen: String) {
        when (itemId) {
            R.id.nav_home -> {
                if (currentScreen != "home") {
                    navigateTo(HomeUserActivity::class.java, true)
                } else {
                    showSnackbar("Ya estás en Inicio")
                }
            }
            R.id.nav_datos_gas, R.id.nav_datos_tem, R.id.nav_datos_presion -> {
                if (currentScreen != "sensor_data") {
                    navigateTo(SensorDataActivity::class.java, false)
                } else {
                    showSnackbar("Ya estás en la pantalla de datos")
                }
            }
            R.id.nav_settings -> {
                showSnackbar("Configuración Usuario")
            }
            R.id.nav_logout -> logout()
        }
    }

    fun handleAdminNavigation(itemId: Int, currentScreen: String) {
        when (itemId) {
            R.id.nav_home -> {
                if (currentScreen != "home_admin") {
                    navigateTo(HomeAdminActivity::class.java, true)
                } else {
                    showSnackbar("Ya estás en Inicio")
                }
            }
            R.id.nav_graphis -> {
                navigateToAdminDashboard("graphs")
            }
            R.id.nav_volumen -> {
                navigateToAdminDashboard("ai")
            }
            R.id.nav_datos_gas -> {
                showSnackbar("Datos de Gas")
            }
            R.id.nav_datos_tem -> {
                showSnackbar("Datos de Temperatura")
            }
            R.id.nav_datos_presion -> {
                showSnackbar("Datos de Presión")
            }
            R.id.nav_users -> {
                navigateToAdminDashboard("users")
            }
            R.id.nav_settings -> {
                showSnackbar("Configuración Admin")
            }
            R.id.nav_logout -> logout()
        }
    }

    private fun navigateToAdminDashboard(section: String) {
        val intent = Intent(context, AdminDashboardActivity::class.java)
        intent.putExtra("SCROLL_TO", section)
        context.startActivity(intent)
    }

    private fun navigateTo(destination: Class<*>, finishCurrent: Boolean = false) {
        val intent = Intent(context, destination)
        context.startActivity(intent)
        if (finishCurrent && context is android.app.Activity) {
            context.finish()
        }
    }

    private fun logout() {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)

        if (context is android.app.Activity) {
            context.finish()
        }
    }

    private fun showSnackbar(message: String) {
        view?.let {
            UiUtils.showSnackbar(it, message)
        } ?: run {
            // Fallback si no hay view disponible
            if (context is android.app.Activity) {
                UiUtils.showSnackbar((context as android.app.Activity).findViewById(android.R.id.content), message)
            }
        }
    }
}
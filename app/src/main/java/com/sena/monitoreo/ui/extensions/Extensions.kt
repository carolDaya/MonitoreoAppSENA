package com.sena.monitoreo.ui.extensions

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.sena.monitoreo.R
import com.sena.monitoreo.ui.auth.LoginActivity
import com.sena.monitoreo.utils.UiUtils

fun AppCompatActivity.setupNavigationView(navView: NavigationView, drawer: DrawerLayout) {
    navView.setNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_home -> UiUtils.navigateTo(this, this::class.java, finishCurrent = true)
            R.id.nav_datos_gas, R.id.nav_datos_tem, R.id.nav_datos_presion -> UiUtils.showSnackbar(drawer, "Ya estás en la pantalla de datos")
            R.id.nav_settings -> UiUtils.showSnackbar(drawer, "Configuración Usuario")
            R.id.nav_logout -> {
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
        }
        drawer.closeDrawer(GravityCompat.START)
        true
    }
}

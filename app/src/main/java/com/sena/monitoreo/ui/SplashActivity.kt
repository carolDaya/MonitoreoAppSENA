package com.sena.monitoreo.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sena.monitoreo.R
import com.sena.monitoreo.ui.auth.LoginActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_TIMEOUT = 4000L // 4 segundos
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Esperar SPLASH_TIMEOUT y abrir LoginActivity
        lifecycleScope.launch {
            delay(SPLASH_TIMEOUT)
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}

package com.sena.monitoreo.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.sena.monitoreo.R
import com.sena.monitoreo.ui.auth.LoginActivity
import com.sena.monitoreo.ui.user.HomeUserActivity

class SplashActivity : AppCompatActivity() {

    // Define the delay time for the splash screen
    private val SPLASH_TIME_OUT: Long = 4000 // 4 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Get SharedPreferences to check if it's the first time the app is launched
        val sharedPref = getSharedPreferences("app_status", MODE_PRIVATE)
        val isFirstTime = sharedPref.getBoolean("is_first_time", true)

        if (isFirstTime) {
            // If it's the first time, show the splash screen with a delay
            Handler(Looper.getMainLooper()).postDelayed({
                // Navigate to the user's main screen (HomeUserActivity) after the delay
                val intent = Intent(this, HomeUserActivity::class.java)
                startActivity(intent)
                finish() // Close the splash activity
            }, SPLASH_TIME_OUT)

            // Update the flag so the splash screen won't show on subsequent launches
            with(sharedPref.edit()) {
                putBoolean("is_first_time", false)
                apply()
            }
        } else {
            // If it's not the first time, navigate directly to the login screen
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
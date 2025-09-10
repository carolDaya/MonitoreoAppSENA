package com.sena.monitoreo.ui.user

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.sena.monitoreo.R
import com.sena.monitoreo.databinding.ActivitySensorDataBinding
import com.sena.monitoreo.ui.auth.LoginActivity

class SensorDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySensorDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySensorDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ========= MENU LATERAL =========
        binding.headerUser.menuIcon.setOnClickListener {
            binding.containerSensor.openDrawer(GravityCompat.START)
        }

        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeUserActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                R.id.nav_datos_gas -> {
                    scrollToView(binding.cardMq4.root)
                }
                R.id.nav_datos_tem -> {
                    scrollToView(binding.cardTemperatura.root)
                }
                R.id.nav_datos_presion -> {
                    scrollToView(binding.cardPresion.root)
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_logout -> {
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
            // 👉 Cierra el Drawer después de seleccionar
            binding.containerSensor.closeDrawer(GravityCompat.START)
            true
        }

        // ========= TARJETAS Y GRAFICOS =========
        setupCards()

        // 👉 Comprobar si hay un extra y hacer scroll al lugar correcto
        val scrollToSection = intent.getStringExtra("SCROLL_TO")
        if (scrollToSection != null) {
            val targetView: View? = when (scrollToSection) {
                "gas" -> binding.cardMq4.root
                "temp" -> binding.cardTemperatura.root
                "pressure" -> binding.cardPresion.root
                else -> null
            }
            targetView?.let {
                scrollToView(it)
            }
        }
    }

    private fun setupCards() {
        // Gas
        binding.cardMq4.cardTitle.text = getString(R.string.gas_sensor_title)
        binding.cardMq4.cardEstado.text = getString(R.string.status_active)
        binding.cardMq4.cardIcon.setImageResource(R.drawable.ic_graph_metano_green)
        setupChart(binding.cardMq4.cardLineChart, "Gas", Color.RED)

        // Temperatura
        binding.cardTemperatura.cardTitle.text = getString(R.string.temperature_sensor_title)
        binding.cardTemperatura.cardEstado.text = getString(R.string.status_active)
        binding.cardTemperatura.cardIcon.setImageResource(R.drawable.ic_graph_tem_green)
        setupChart(binding.cardTemperatura.cardLineChart, "Temperatura", Color.BLUE)

        // Presión
        binding.cardPresion.cardTitle.text = getString(R.string.pressure_sensor_title)
        binding.cardPresion.cardEstado.text = getString(R.string.status_active)
        binding.cardPresion.cardIcon.setImageResource(R.drawable.ic_graph_presion_green)
        setupChart(binding.cardPresion.cardLineChart, "Presión", Color.GREEN)
    }

    /**
     * Configura y llena un LineChart con datos de ejemplo
     */
    private fun setupChart(chart: LineChart, label: String, color: Int) {
        val entries = listOf(
            Entry(1f, 10f),
            Entry(2f, 20f),
            Entry(3f, 15f),
            Entry(4f, 25f),
            Entry(5f, 18f)
        )

        val dataSet = LineDataSet(entries, label)
        dataSet.color = color
        dataSet.valueTextColor = Color.BLACK
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setCircleColor(color)

        val lineData = LineData(dataSet)
        chart.data = lineData

        chart.description.isEnabled = false
        chart.legend.isEnabled = true
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)

        chart.invalidate()

        // 👉 Verificar el último valor simulado
        val lastValue = entries.last().y
        checkSensorData(lastValue, label)
    }

    /**
     * Verifica si un valor está fuera de rango y lanza la alerta
     */
    private fun checkSensorData(value: Float, type: String) {
        var min = 0f
        var max = 100f

        when (type) {
            "Gas" -> { min = 0f; max = 50f }
            "Temperatura" -> { min = 15f; max = 30f }
            "Presión" -> { min = 80f; max = 120f }
        }

        if (value < min || value > max) {
            // 🚨 Ir a la pantalla de alerta
            val intent = Intent(this, AlertsActivity::class.java)
            intent.putExtra("SENSOR_TYPE", type)
            intent.putExtra("VALUE", value)
            startActivity(intent)
        }
    }

    /**
     * Scroll suave hasta una vista específica dentro del NestedScrollView
     */
    private fun scrollToView(targetView: android.view.View) {
        binding.mainScroll.post {
            binding.mainScroll.smoothScrollTo(0, targetView.top)
        }
    }
}

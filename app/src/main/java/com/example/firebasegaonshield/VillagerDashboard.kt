package com.example.firebasegaonshield

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class VillagerDashboard : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_villager_dashboard)

        setupBottomNavigation()
        setupChatbotButton()

        if (savedInstanceState == null) {
            loadFragment(VillDashboardFragment())
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(VillDashboardFragment())
                    true
                }
                R.id.nav_health -> {
                    loadFragment(HealthReportSubmitFragment())
                    true
                }
                R.id.nav_map -> {
                    loadFragment(GoogleMapFragment())
                    true
                }
                R.id.nav_alerts -> {
                    loadFragment(AlertsFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun setupChatbotButton() {
        val chatbotFab = findViewById<FloatingActionButton>(R.id.fab_chatbot)
        chatbotFab.setOnClickListener {
            val intent = Intent(this, ChatbotActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
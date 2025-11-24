package com.example.firebasegaonshield

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CommunityDashboard : AppCompatActivity(), CommunityHomeFragment.CommunityHomeListener {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var welcomeText: TextView
    private lateinit var tvFirstLetter: TextView
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_community_dashboard)

        initializeFirebase()
        initializeViews()
        setupEdgeToEdge()
        setupBottomNavigation()
        loadUserData()

        if (savedInstanceState == null) {
            loadFragment(CommunityHomeFragment())
        }
    }

    private fun initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    private fun initializeViews() {
        welcomeText = findViewById(R.id.welcomeText)
        tvFirstLetter = findViewById(R.id.tvFirstLetter)
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            try {
                when (item.itemId) {
                    R.id.nav_home -> {
                        loadFragment(CommunityHomeFragment())
                        true
                    }
                    R.id.nav_alerts -> {
                        loadFragment(CommunityAlertsFragment())
                        true
                    }
                    R.id.nav_reports -> {
                        loadFragment(CommunityReportsFragment())
                        true
                    }
                    R.id.nav_profile -> {
                        loadFragment(ProfileFragment())
                        true
                    }
                    else -> false
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
                false
            }
        }
        bottomNavigation.selectedItemId = R.id.nav_home
    }

    private fun loadUserData() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            fetchUserName(currentUser.uid)
        } else {
            setDefaultWelcomeMessage()
        }
    }

    private fun fetchUserName(userId: String) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val fullName = document.getString("fullName") ?: "ASHA Worker"
                    displayUserName(fullName)
                } else {
                    setDefaultWelcomeMessage()
                }
            }
            .addOnFailureListener {
                setDefaultWelcomeMessage()
            }
    }

    private fun displayUserName(fullName: String) {
        val firstName = fullName.split(" ").first()
        val firstLetter = firstName.first().uppercase()

        welcomeText.text = "Welcome, $firstName"
        tvFirstLetter.text = firstLetter
    }

    private fun setDefaultWelcomeMessage() {
        welcomeText.text = "Community Dashboard"
        tvFirstLetter.text = "A"
    }

    private fun loadFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading fragment: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        if (fragmentManager.backStackEntryCount > 1) {
            fragmentManager.popBackStack()
            updateBottomNavigation()
        } else {
            super.onBackPressed()
        }
    }

    private fun updateBottomNavigation() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when (currentFragment) {
            is CommunityHomeFragment -> bottomNavigation.selectedItemId = R.id.nav_home
            is CommunityAlertsFragment -> bottomNavigation.selectedItemId = R.id.nav_alerts
            is CommunityReportsFragment -> bottomNavigation.selectedItemId = R.id.nav_reports
            is ProfileFragment -> bottomNavigation.selectedItemId = R.id.nav_profile
        }
    }

    override fun onNavigateToReports() {
        loadFragment(CommunityReportsFragment())
        bottomNavigation.selectedItemId = R.id.nav_reports
    }

    override fun onShowCreateAlert() {
        try {
            val dialog = CreateAlertDialogFragment()
            dialog.show(supportFragmentManager, "CreateAlertDialog")
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot create alert: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onShowVillagerManagement() {
        Toast.makeText(this, "Villager Management - Coming Soon", Toast.LENGTH_SHORT).show()
    }
}
package com.example.firebasegaonshield

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        initializeViews()
        setupClickListeners()
        checkCurrentUser()
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        progressBar = findViewById(R.id.progressBar)

        // Initialize Firebase
        auth = Firebase.auth
        firestore = Firebase.firestore
    }

    private fun setupClickListeners() {
        tvRegister.setOnClickListener {
            startActivity(Intent(this, SignActivity::class.java))
        }

        btnLogin.setOnClickListener {
            loginUser()
        }
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            fetchUserRole(currentUser.uid)
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showToast("Please enter email and password")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email address")
            return
        }

        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        fetchUserRole(userId)
                    } else {
                        showLoading(false)
                        showToast("User ID not found")
                    }
                } else {
                    showLoading(false)
                    showToast("Login failed: ${task.exception?.message ?: "Unknown error"}")
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                showToast("Login error: ${exception.message}")
            }
    }

    private fun fetchUserRole(userId: String) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                if (document.exists()) {
                    val role = document.getString("role") ?: "villager"
                    val fullName = document.getString("fullName") ?: "User"
                    goToDashboard(role, fullName)
                } else {
                    createDefaultUserData(userId)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showToast("Failed to fetch user data: ${e.message}")
            }
    }

    private fun createDefaultUserData(userId: String) {
        val userData = hashMapOf(
            "userId" to userId,
            "fullName" to "User",
            "email" to (auth.currentUser?.email ?: ""),
            "role" to "villager",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                goToDashboard("villager", "User")
            }
            .addOnFailureListener { e ->
                showToast("Failed to create user data: ${e.message}")
            }
    }

    private fun goToDashboard(role: String, fullName: String) {
        val intent = when (role.lowercase()) {
            "villager" -> Intent(this, VillagerDashboard::class.java)
            "asha", "ashaworker" -> Intent(this, CommunityDashboard::class.java)
            else -> Intent(this, VillagerDashboard::class.java)
        }

        intent.putExtra("USER_ROLE", role)
        intent.putExtra("FULL_NAME", fullName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnLogin.text = if (show) "Signing In..." else "Sign In"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
package com.example.firebasegaonshield

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var roleGroup: RadioGroup
    private lateinit var btnSignup: Button
    private lateinit var tvLogin: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        roleGroup = findViewById(R.id.roleGroup)
        btnSignup = findViewById(R.id.btnSignup)
        tvLogin = findViewById(R.id.tvLogin)
        progressBar = findViewById(R.id.progressBar)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // No automatic selection - let user choose
    }

    private fun setupClickListeners() {
        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnSignup.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val name = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (name.isEmpty()) {
            showToast("Full name is required")
            etFullName.requestFocus()
            return
        }

        if (email.isEmpty()) {
            showToast("Email is required")
            etEmail.requestFocus()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please enter a valid email address")
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            showToast("Password is required")
            etPassword.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            showToast("Please confirm password")
            etConfirmPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            showToast("Passwords do not match")
            etConfirmPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            showToast("Password must be at least 6 characters")
            etPassword.requestFocus()
            return
        }

        // Check if a role is selected
        val selectedRoleId = roleGroup.checkedRadioButtonId
        if (selectedRoleId == -1) {
            showToast("Please select a role")
            return
        }

        val role = if (selectedRoleId == R.id.rbAsha) "asha" else "villager"
        showLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        saveUserToFirestore(userId, name, email, role)
                    } else {
                        showLoading(false)
                        showToast("User ID not found")
                    }
                } else {
                    showLoading(false)
                    showToast("Registration failed: ${task.exception?.message ?: "Unknown error"}")
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                showToast("Registration error: ${exception.message}")
            }
    }

    private fun saveUserToFirestore(userId: String, name: String, email: String, role: String) {
        val userData = hashMapOf(
            "userId" to userId,
            "fullName" to name,
            "email" to email,
            "role" to role,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                showToast("Registration Successful!")
                goToDashboard(role, name)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showToast("Failed to save user data: ${e.message}")
                // Delete the created user if Firestore save fails
                auth.currentUser?.delete()?.addOnCompleteListener {
                    showToast("Registration rolled back due to error")
                }
            }
    }

    private fun goToDashboard(role: String, fullName: String) {
        val intent = when (role.lowercase()) {
            "villager" -> Intent(this, VillagerDashboard::class.java)
            "asha" -> Intent(this, CommunityDashboard::class.java)
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
        btnSignup.isEnabled = !show
        btnSignup.text = if (show) "Creating Account..." else "Create Account"

        // Disable other inputs during loading
        etFullName.isEnabled = !show
        etEmail.isEnabled = !show
        etPassword.isEnabled = !show
        etConfirmPassword.isEnabled = !show
        roleGroup.isEnabled = !show
        tvLogin.isEnabled = !show
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
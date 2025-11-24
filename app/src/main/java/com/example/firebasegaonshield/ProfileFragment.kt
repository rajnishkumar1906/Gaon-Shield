package com.example.firebasegaonshield

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment(), EditProfileDialogFragment.ProfileUpdateListener {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var tvUserName: TextView
    private lateinit var tvUserInitial: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvAge: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvVillage: TextView
    private lateinit var tvDistrict: TextView
    private lateinit var tvState: TextView

    private lateinit var cardAshaInfo: CardView
    private lateinit var cardVillagerInfo: CardView
    private lateinit var tvAshaId: TextView
    private lateinit var tvAreaCovered: TextView
    private lateinit var tvFamilyMembers: TextView
    private lateinit var tvLandArea: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupFirebase()
        loadUserProfile()
    }

    private fun initializeViews(view: View) {
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserInitial = view.findViewById(R.id.tvUserInitial)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvRole = view.findViewById(R.id.tvRole)
        tvPhone = view.findViewById(R.id.tvPhone)
        tvAge = view.findViewById(R.id.tvAge)
        tvGender = view.findViewById(R.id.tvGender)
        tvVillage = view.findViewById(R.id.tvVillage)
        tvDistrict = view.findViewById(R.id.tvDistrict)
        tvState = view.findViewById(R.id.tvState)

        cardAshaInfo = view.findViewById(R.id.cardAshaInfo)
        cardVillagerInfo = view.findViewById(R.id.cardVillagerInfo)
        tvAshaId = view.findViewById(R.id.tvAshaId)
        tvAreaCovered = view.findViewById(R.id.tvAreaCovered)
        tvFamilyMembers = view.findViewById(R.id.tvFamilyMembers)
        tvLandArea = view.findViewById(R.id.tvLandArea)

        view.findViewById<CardView>(R.id.cardEditProfile).setOnClickListener {
            showEditProfileDialog()
        }

        view.findViewById<CardView>(R.id.cardLogout).setOnClickListener {
            logoutUser()
        }
    }

    private fun setupFirebase() {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showErrorMessage("User not logged in")
            return
        }

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    displayUserProfile(document)
                } else {
                    createDefaultProfile(currentUser.uid)
                }
            }
            .addOnFailureListener { e ->
                showErrorMessage("Failed to load profile: ${e.message}")
            }
    }

    private fun displayUserProfile(document: com.google.firebase.firestore.DocumentSnapshot) {
        tvUserName.text = document.getString("fullName") ?: "User"
        tvUserInitial.text = getInitials(document.getString("fullName") ?: "U")
        tvEmail.text = document.getString("email") ?: "No email"
        tvRole.text = when (val role = document.getString("role") ?: "villager") {
            "asha", "ashaworker" -> "ASHA Worker"
            "villager" -> "Villager"
            else -> role
        }
        tvPhone.text = document.getString("phone") ?: "Not set"
        tvAge.text = document.getLong("age")?.let { "$it years" } ?: "Not set"
        tvGender.text = document.getString("gender") ?: "Not set"
        tvVillage.text = document.getString("village") ?: "Not set"
        tvDistrict.text = document.getString("district") ?: "Not set"
        tvState.text = document.getString("state") ?: "Not set"

        val role = document.getString("role") ?: "villager"
        when (role.lowercase()) {
            "asha", "ashaworker" -> {
                cardAshaInfo.visibility = View.VISIBLE
                cardVillagerInfo.visibility = View.GONE
                displayAshaInfo(document)
            }
            "villager" -> {
                cardAshaInfo.visibility = View.GONE
                cardVillagerInfo.visibility = View.VISIBLE
                displayVillagerInfo(document)
            }
            else -> {
                cardAshaInfo.visibility = View.GONE
                cardVillagerInfo.visibility = View.GONE
            }
        }
    }

    private fun displayAshaInfo(document: com.google.firebase.firestore.DocumentSnapshot) {
        val userId = auth.currentUser?.uid ?: ""
        tvAshaId.text = "ASH${userId.takeLast(6).uppercase()}"
        tvAreaCovered.text = document.getString("village") ?: "Unknown Village"
    }

    private fun displayVillagerInfo(document: com.google.firebase.firestore.DocumentSnapshot) {
        // Load additional villager info if needed
        tvFamilyMembers.text = "Not set"
        tvLandArea.text = "Not set"
    }

    private fun getInitials(fullName: String): String {
        return fullName.split(" ")
            .take(2)
            .joinToString("") { it.firstOrNull()?.toString() ?: "" }
            .uppercase()
    }

    private fun createDefaultProfile(userId: String) {
        val currentUser = auth.currentUser
        val userData = hashMapOf<String, Any>(
            "userId" to userId,
            "fullName" to (currentUser?.displayName ?: "User"),
            "email" to (currentUser?.email ?: ""),
            "role" to "villager",
            "village" to "Unknown",
            "district" to "Unknown",
            "state" to "Unknown"
        )

        firestore.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                loadUserProfile()
            }
            .addOnFailureListener { e ->
                showErrorMessage("Failed to create profile: ${e.message}")
            }
    }

    private fun showEditProfileDialog() {
        val dialog = EditProfileDialogFragment()
        dialog.setProfileUpdateListener(this)
        dialog.show(parentFragmentManager, "EditProfileDialog")
    }

    private fun logoutUser() {
        auth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onProfileUpdated() {
        loadUserProfile()
    }

    companion object {
        fun newInstance(): ProfileFragment {
            return ProfileFragment()
        }
    }
}
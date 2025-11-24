package com.example.firebasegaonshield

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VillDashboardFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var tvWelcome: TextView
    private lateinit var tvAlertsCount: TextView
    private lateinit var tvReportsCount: TextView
    private lateinit var tvRiskLevel: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vill_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initializeViews(view)
        loadDashboardData()
    }

    private fun initializeViews(view: View) {
        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvAlertsCount = view.findViewById(R.id.tvAlertsCount)
        tvReportsCount = view.findViewById(R.id.tvReportsCount)
        tvRiskLevel = view.findViewById(R.id.tvRiskLevel)
    }

    private fun loadDashboardData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Load user name
            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    val fullName = document.getString("fullName") ?: "Villager"
                    tvWelcome.text = "Welcome, $fullName"
                }

            // Load alerts count
            firestore.collection("alerts")
                .get()
                .addOnSuccessListener { documents ->
                    tvAlertsCount.text = documents.size().toString()
                }

            // Load user's reports count
            firestore.collection("health_reports")
                .whereEqualTo("reportedBy", currentUser.uid)
                .get()
                .addOnSuccessListener { documents ->
                    tvReportsCount.text = documents.size().toString()
                }

            // Load risk level (simplified)
            tvRiskLevel.text = "Low"
        }
    }
}
package com.example.firebasegaonshield

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CommunityHomeFragment : Fragment() {

    interface CommunityHomeListener {
        fun onNavigateToReports()
        fun onShowCreateAlert()
        fun onShowVillagerManagement()
    }

    // Action Cards
    private lateinit var cardCreateAlert: CardView
    private lateinit var cardViewReports: CardView
        private lateinit var cardVillagerManagement: CardView

    // Quick Stats
    private lateinit var tvVillagerCount: TextView
    private lateinit var tvReportsCount: TextView
    private lateinit var tvAlertsCount: TextView
    private lateinit var tvActiveCases: TextView
    private lateinit var tvHighRiskCount: TextView
    private lateinit var tvOutbreakRisk: TextView

    // Disease Distribution
    private lateinit var tvCholeraCount: TextView
    private lateinit var tvTyphoidCount: TextView
    private lateinit var tvDysenteryCount: TextView
    private lateinit var tvGastroCount: TextView
    private lateinit var tvHepatitisCount: TextView
    private lateinit var tvMalariaCount: TextView

    // Progress Bars for Disease Trends
    private lateinit var progressCholera: ProgressBar
    private lateinit var progressTyphoid: ProgressBar
    private lateinit var progressDysentery: ProgressBar
    private lateinit var progressGastro: ProgressBar
    private lateinit var progressHepatitis: ProgressBar
    private lateinit var progressMalaria: ProgressBar

    // High Risk Cases List
    private lateinit var rvHighRiskCases: RecyclerView
    private lateinit var highRiskAdapter: HighRiskCasesAdapter
    private lateinit var tvNoHighRisk: TextView

    private var listener: CommunityHomeListener? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var reportsListener: ListenerRegistration? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CommunityHomeListener) {
            listener = context
        } else {
            throw ClassCastException("$context must implement CommunityHomeListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        initializeViews(view)
        setupClickListeners()
        setupRecyclerView()
        loadDashboardData()
    }

    private fun initializeViews(view: View) {
        // Action cards
        cardCreateAlert = view.findViewById(R.id.cardCreateAlert)
        cardViewReports = view.findViewById(R.id.cardViewReports)
        cardVillagerManagement = view.findViewById(R.id.cardVillagerManagement)

        // Quick Stats
        tvVillagerCount = view.findViewById(R.id.tvVillagerCount)
        tvReportsCount = view.findViewById(R.id.tvReportsCount)
        tvAlertsCount = view.findViewById(R.id.tvAlertsCount)
        tvActiveCases = view.findViewById(R.id.tvActiveCases)
        tvHighRiskCount = view.findViewById(R.id.tvHighRiskCount)
        tvOutbreakRisk = view.findViewById(R.id.tvOutbreakRisk)

        // Disease Counts
        tvCholeraCount = view.findViewById(R.id.tvCholeraCount)
        tvTyphoidCount = view.findViewById(R.id.tvTyphoidCount)
        tvDysenteryCount = view.findViewById(R.id.tvDysenteryCount)
        tvGastroCount = view.findViewById(R.id.tvGastroCount)
        tvHepatitisCount = view.findViewById(R.id.tvHepatitisCount)
        tvMalariaCount = view.findViewById(R.id.tvMalariaCount)

        // Progress Bars
        progressCholera = view.findViewById(R.id.progressCholera)
        progressTyphoid = view.findViewById(R.id.progressTyphoid)
        progressDysentery = view.findViewById(R.id.progressDysentery)
        progressGastro = view.findViewById(R.id.progressGastro)
        progressHepatitis = view.findViewById(R.id.progressHepatitis)
        progressMalaria = view.findViewById(R.id.progressMalaria)

        // High Risk Cases
        rvHighRiskCases = view.findViewById(R.id.rvHighRiskCases)
        tvNoHighRisk = view.findViewById(R.id.tvNoHighRisk)
    }

    private fun setupRecyclerView() {
        highRiskAdapter = HighRiskCasesAdapter(emptyList()) { report ->
            showHighRiskCaseDetails(report)
        }
        rvHighRiskCases.layoutManager = LinearLayoutManager(requireContext())
        rvHighRiskCases.adapter = highRiskAdapter
    }

    private fun setupClickListeners() {
        cardCreateAlert.setOnClickListener {
            listener?.onShowCreateAlert()
        }

        cardViewReports.setOnClickListener {
            listener?.onNavigateToReports()
        }

        cardVillagerManagement.setOnClickListener {
            listener?.onShowVillagerManagement()
        }
    }

    private fun loadDashboardData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { userDoc ->
                    val userVillage = userDoc.getString("village") ?: "Unknown"
                    loadVillagerCount(userVillage)
                    loadAlertsCount(currentUser.uid)
                    setupRealTimeReportsListener(userVillage)
                }
                .addOnFailureListener {
                    setupRealTimeReportsListener("Unknown")
                }
        } else {
            setupRealTimeReportsListener("Unknown")
        }
    }

    private fun setupRealTimeReportsListener(village: String) {
        reportsListener?.remove()

        reportsListener = firestore.collection("health_reports")
            .whereEqualTo("village", village)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val reports = snapshot?.toObjects(HealthReport::class.java) ?: emptyList()
                updateDashboardWithReports(reports)
            }
    }

    private fun updateDashboardWithReports(reports: List<HealthReport>) {
        // Update basic counts
        tvReportsCount.text = reports.size.toString()

        // Calculate active cases and high risk
        val activeCases = reports.count { it.severity > 0 }
        val highRiskCases = reports.filter { it.severity >= 7 }

        tvActiveCases.text = activeCases.toString()
        tvHighRiskCount.text = highRiskCases.size.toString()

        // Update high risk cases list
        updateHighRiskCases(highRiskCases)

        // Calculate disease distribution
        calculateDiseaseDistribution(reports)

        // Calculate outbreak risk
        calculateOutbreakRisk(reports, highRiskCases.size)
    }

    private fun updateHighRiskCases(highRiskCases: List<HealthReport>) {
        if (highRiskCases.isEmpty()) {
            rvHighRiskCases.visibility = View.GONE
            tvNoHighRisk.visibility = View.VISIBLE
        } else {
            rvHighRiskCases.visibility = View.VISIBLE
            tvNoHighRisk.visibility = View.GONE
            highRiskAdapter.updateData(highRiskCases.take(5))
        }
    }

    private fun calculateDiseaseDistribution(reports: List<HealthReport>) {
        val diseaseCounts = mapOf(
            "Cholera" to reports.count { it.predictedDisease.contains("Cholera", ignoreCase = true) },
            "Typhoid" to reports.count { it.predictedDisease.contains("Typhoid", ignoreCase = true) },
            "Dysentery" to reports.count { it.predictedDisease.contains("Dysentery", ignoreCase = true) },
            "Gastroenteritis" to reports.count { it.predictedDisease.contains("Gastroenteritis", ignoreCase = true) },
            "Hepatitis" to reports.count { it.predictedDisease.contains("Hepatitis", ignoreCase = true) },
            "Malaria" to reports.count { it.predictedDisease.contains("Malaria", ignoreCase = true) }
        )

        // Update disease counts
        tvCholeraCount.text = diseaseCounts["Cholera"].toString()
        tvTyphoidCount.text = diseaseCounts["Typhoid"].toString()
        tvDysenteryCount.text = diseaseCounts["Dysentery"].toString()
        tvGastroCount.text = diseaseCounts["Gastroenteritis"].toString()
        tvHepatitisCount.text = diseaseCounts["Hepatitis"].toString()
        tvMalariaCount.text = diseaseCounts["Malaria"].toString()

        // Update progress bars
        val totalCases = reports.size
        if (totalCases > 0) {
            progressCholera.progress = (diseaseCounts["Cholera"]!! * 100 / totalCases)
            progressTyphoid.progress = (diseaseCounts["Typhoid"]!! * 100 / totalCases)
            progressDysentery.progress = (diseaseCounts["Dysentery"]!! * 100 / totalCases)
            progressGastro.progress = (diseaseCounts["Gastroenteritis"]!! * 100 / totalCases)
            progressHepatitis.progress = (diseaseCounts["Hepatitis"]!! * 100 / totalCases)
            progressMalaria.progress = (diseaseCounts["Malaria"]!! * 100 / totalCases)
        } else {
            // Reset progress bars if no cases
            progressCholera.progress = 0
            progressTyphoid.progress = 0
            progressDysentery.progress = 0
            progressGastro.progress = 0
            progressHepatitis.progress = 0
            progressMalaria.progress = 0
        }
    }

    private fun calculateOutbreakRisk(reports: List<HealthReport>, highRiskCount: Int) {
        val outbreakRisk = when {
            highRiskCount >= 10 -> "HIGH 🔴"
            highRiskCount >= 5 -> "MEDIUM 🟡"
            highRiskCount >= 2 -> "LOW 🟢"
            else -> "NONE ✅"
        }

        tvOutbreakRisk.text = outbreakRisk
    }

    private fun loadVillagerCount(village: String) {
        firestore.collection("users")
            .whereEqualTo("role", "villager")
            .whereEqualTo("village", village)
            .get()
            .addOnSuccessListener { documents ->
                tvVillagerCount.text = documents.size().toString()
            }
            .addOnFailureListener {
                tvVillagerCount.text = "0"
            }
    }

    private fun loadAlertsCount(userId: String) {
        firestore.collection("alerts")
            .whereEqualTo("senderId", userId)
            .get()
            .addOnSuccessListener { documents ->
                tvAlertsCount.text = documents.size().toString()
            }
            .addOnFailureListener {
                tvAlertsCount.text = "0"
            }
    }

    private fun showHighRiskCaseDetails(report: HealthReport) {
        Toast.makeText(
            requireContext(),
            "High risk case: ${report.patientName} - ${report.predictedDisease}",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        reportsListener?.remove()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        fun newInstance(): CommunityHomeFragment {
            return CommunityHomeFragment()
        }
    }
}
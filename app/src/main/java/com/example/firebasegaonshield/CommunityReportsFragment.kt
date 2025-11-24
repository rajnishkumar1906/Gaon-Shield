package com.example.firebasegaonshield

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class CommunityReportsFragment : Fragment() {

    private lateinit var rvReports: RecyclerView
    private lateinit var emptyStateReports: View
    private lateinit var tvEmptyStateMessage: TextView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var reportsAdapter: HealthReportsAdapter

    private val TAG = "CommunityReportsFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "Fragment onViewCreated called")

        try {
            initializeViews(view)
            setupRecyclerView()
            loadReports()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing reports: ${e.message}", e)
            Toast.makeText(requireContext(), "Error initializing reports: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeViews(view: View) {
        rvReports = view.findViewById(R.id.rvReports)
        emptyStateReports = view.findViewById(R.id.emptyStateReports)
        tvEmptyStateMessage = view.findViewById(R.id.tvEmptyStateMessage)
        firestore = FirebaseFirestore.getInstance()
        Log.d(TAG, "Views initialized")
    }

    private fun setupRecyclerView() {
        reportsAdapter = HealthReportsAdapter(emptyList()) { report ->
            showReportDetails(report)
        }
        rvReports.layoutManager = LinearLayoutManager(requireContext())
        rvReports.adapter = reportsAdapter
        Log.d(TAG, "RecyclerView setup completed")
    }

    private fun loadReports() {
        Log.d(TAG, "Loading reports from Firestore...")

        firestore.collection("health_reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error loading reports: ${error.message}")
                    Toast.makeText(requireContext(), "Error loading reports: ${error.message}", Toast.LENGTH_SHORT).show()
                    showEmptyState("Error loading reports")
                    return@addSnapshotListener
                }

                Log.d(TAG, "Snapshot received: ${snapshot?.size()} documents")

                val reports = mutableListOf<HealthReport>()
                snapshot?.documents?.forEach { document ->
                    try {
                        val report = document.toHealthReport()
                        reports.add(report)
                        Log.d(TAG, "Loaded report: ${report.patientName} - ${report.predictedDisease}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing document ${document.id}: ${e.message}")
                    }
                }

                Log.d(TAG, "Successfully parsed ${reports.size} reports")

                if (reports.isEmpty()) {
                    showEmptyState("No health reports found")
                    Log.d(TAG, "Showing empty state")
                } else {
                    hideEmptyState()
                    Log.d(TAG, "Showing ${reports.size} reports")
                }

                reportsAdapter.updateData(reports)
            }
    }

    private fun showEmptyState(message: String = "No reports available") {
        emptyStateReports.visibility = View.VISIBLE
        rvReports.visibility = View.GONE
        tvEmptyStateMessage.text = message
    }

    private fun hideEmptyState() {
        emptyStateReports.visibility = View.GONE
        rvReports.visibility = View.VISIBLE
    }

    private fun showReportDetails(report: HealthReport) {
        Toast.makeText(
            requireContext(),
            "Patient: ${report.patientName}\nDisease: ${report.predictedDisease}\nSeverity: ${report.severity}/10\nVillage: ${report.village}",
            Toast.LENGTH_LONG
        ).show()
    }
}

// Extension function to convert DocumentSnapshot to HealthReport
fun com.google.firebase.firestore.DocumentSnapshot.toHealthReport(): HealthReport {
    return HealthReport(
        reportId = getString("reportId") ?: id,
        patientName = getString("patientName") ?: "Unknown Patient",
        patientAge = getLong("patientAge")?.toInt() ?: 0,
        patientGender = getString("patientGender") ?: "",
        village = getString("village") ?: "Unknown Village",
        community = getString("community") ?: "",
        region = getString("region") ?: "",
        reportedBy = getString("reportedBy") ?: "",
        reportedByName = getString("reportedByName") ?: "",
        symptoms = get("symptoms") as? List<String> ?: emptyList(),
        symptomDuration = getLong("symptomDuration")?.toInt() ?: 0,
        severity = getLong("severity")?.toInt() ?: 1,
        waterSource = try {
            HealthReport.WaterSource.valueOf(getString("waterSource") ?: "TAP")
        } catch (e: Exception) {
            HealthReport.WaterSource.TAP
        },
        predictedDisease = getString("predictedDisease") ?: "Not Predicted",
        confidence = (getDouble("confidence") ?: 0.0).toFloat(),
        status = try {
            HealthReport.ReportStatus.valueOf(getString("status") ?: "PENDING")
        } catch (e: Exception) {
            HealthReport.ReportStatus.PENDING
        },
        assignedAshaWorker = getString("assignedAshaWorker") ?: "Not Assigned",
        notes = getString("notes") ?: "",
        timestamp = getTimestamp("timestamp") ?: Timestamp.now(),
        isEmergency = getBoolean("isEmergency") ?: false
    )
}

// HealthReportsAdapter class - Fixed version
class HealthReportsAdapter(
    private var reports: List<HealthReport>,
    private val onReportClick: (HealthReport) -> Unit
) : RecyclerView.Adapter<HealthReportsAdapter.ReportViewHolder>() {

    private val TAG = "HealthReportsAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_health_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        try {
            val report = reports[position]
            Log.d(TAG, "Binding report at position $position: ${report.patientName}")
            holder.bind(report)
        } catch (e: Exception) {
            Log.e(TAG, "Error binding report at position $position: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = reports.size

    fun updateData(newReports: List<HealthReport>) {
        Log.d(TAG, "Updating adapter with ${newReports.size} reports")
        this.reports = newReports
        notifyDataSetChanged()
    }

    inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvSymptoms: TextView = itemView.findViewById(R.id.tvSymptoms)
        private val tvPrediction: TextView = itemView.findViewById(R.id.tvPrediction)
        private val tvVillage: TextView = itemView.findViewById(R.id.tvVillage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvSeverity: TextView = itemView.findViewById(R.id.tvSeverity)
        private val tvEmergency: TextView = itemView.findViewById(R.id.tvEmergency)

        fun bind(report: HealthReport) {
            try {
                Log.d(TAG, "Binding report: ${report.patientName}")

                // Set basic information
                tvPatientName.text = report.patientName.ifEmpty { "Unknown Patient" }
                tvStatus.text = report.status.name.replace("_", " ").uppercase()
                tvPrediction.text = "Predicted: ${report.predictedDisease}"
                tvVillage.text = "Village: ${report.village}"
                tvSeverity.text = "Severity: ${report.severity}/10"

                // Handle symptoms - safely handle null or empty lists
                val symptomsText = if (report.symptoms.isNotEmpty()) {
                    report.symptoms.joinToString(", ").take(50) + if (report.symptoms.joinToString(", ").length > 50) "..." else ""
                } else {
                    "No symptoms reported"
                }
                tvSymptoms.text = "Symptoms: $symptomsText"

                // Handle emergency indicator
                if (report.isEmergency) {
                    tvEmergency.visibility = View.VISIBLE
                    tvEmergency.text = "🚨 EMERGENCY"
                } else {
                    tvEmergency.visibility = View.GONE
                }

                // Format timestamp
                tvTime.text = formatTimestamp(report.timestamp)

                // Set status color
                val statusColor = when (report.status) {
                    HealthReport.ReportStatus.PENDING -> android.R.color.holo_orange_light
                    HealthReport.ReportStatus.UNDER_REVIEW -> android.R.color.holo_blue_light
                    HealthReport.ReportStatus.RESOLVED -> android.R.color.holo_green_light
                    HealthReport.ReportStatus.CRITICAL -> android.R.color.holo_red_light
                    else -> android.R.color.darker_gray
                }
                tvStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, statusColor))

                itemView.setOnClickListener {
                    onReportClick(report)
                }

                Log.d(TAG, "Successfully bound report: ${report.patientName}")

            } catch (e: Exception) {
                Log.e(TAG, "Error binding report UI: ${e.message}")
                e.printStackTrace()
                // Fallback display
                tvPatientName.text = "Error loading report"
                tvStatus.text = "ERROR"
                tvStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark))
            }
        }

        private fun formatTimestamp(timestamp: Timestamp?): String {
            return try {
                val date = timestamp?.toDate() ?: Date()
                val now = Date()
                val diff = now.time - date.time

                when {
                    diff < 60000 -> "Just now"
                    diff < 3600000 -> "${diff / 60000}m ago"
                    diff < 86400000 -> "${diff / 3600000}h ago"
                    else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {
                "Unknown time"
            }
        }
    }
}
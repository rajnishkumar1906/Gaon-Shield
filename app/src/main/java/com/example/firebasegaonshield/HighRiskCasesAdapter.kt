package com.example.firebasegaonshield

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HighRiskCasesAdapter(
    private var highRiskCases: List<HealthReport>,
    private val onCaseClick: (HealthReport) -> Unit
) : RecyclerView.Adapter<HighRiskCasesAdapter.HighRiskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HighRiskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_high_risk_case, parent, false)
        return HighRiskViewHolder(view)
    }

    override fun onBindViewHolder(holder: HighRiskViewHolder, position: Int) {
        holder.bind(highRiskCases[position])
    }

    override fun getItemCount(): Int = highRiskCases.size

    fun updateData(newCases: List<HealthReport>) {
        this.highRiskCases = newCases
        notifyDataSetChanged()
    }

    inner class HighRiskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPatientName: TextView = itemView.findViewById(R.id.tvPatientName)
        private val tvDisease: TextView = itemView.findViewById(R.id.tvDisease)
        private val tvSeverity: TextView = itemView.findViewById(R.id.tvSeverity)
        private val tvSymptoms: TextView = itemView.findViewById(R.id.tvSymptoms)

        fun bind(report: HealthReport) {
            tvPatientName.text = report.patientName.ifEmpty { "Unknown Patient" }
            tvDisease.text = "Predicted: ${report.predictedDisease}"
            tvSeverity.text = "Severity: ${report.severity}/10"
            tvSymptoms.text = "Symptoms: ${report.symptoms.take(3).joinToString(", ")}"

            itemView.setOnClickListener {
                onCaseClick(report)
            }
        }
    }
}
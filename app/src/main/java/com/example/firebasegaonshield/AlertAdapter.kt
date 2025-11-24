package com.example.firebasegaonshield

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class AlertsAdapter(
    private val alerts: List<Alert>,
    private val onAlertClick: (Alert) -> Unit
) : RecyclerView.Adapter<AlertsAdapter.AlertViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        holder.bind(alerts[position])
    }

    override fun getItemCount(): Int = alerts.size

    inner class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardAlert: MaterialCardView = itemView.findViewById(R.id.cardAlert)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvAlertTitle)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvAlertMessage)
        private val tvSender: TextView = itemView.findViewById(R.id.tvAlertSender)
        private val tvTime: TextView = itemView.findViewById(R.id.tvAlertTime)
        private val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)

        fun bind(alert: Alert) {
            tvTitle.text = alert.title
            tvMessage.text = alert.message
            tvSender.text = "From: ${alert.senderName} (${alert.senderRole})"
            tvTime.text = formatTimestamp(alert.timestamp)

            // Set priority color
            // Set priority color
            val (priorityText, backgroundRes) = when (alert.priority) {
                Alert.Priority.LOW -> Pair("Low", R.drawable.priority_background_green)
                Alert.Priority.MEDIUM -> Pair("Medium", R.drawable.priority_background_orange)
                Alert.Priority.HIGH -> Pair("High", R.drawable.priority_background_red)
                Alert.Priority.URGENT -> Pair("Urgent", R.drawable.priority_background_red)
            }
            tvPriority.text = priorityText
            tvPriority.setBackgroundResource(backgroundRes)

            // Change background if not read
            if (!alert.isRead) {
                cardAlert.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.unread_alert_bg))
            } else {
                cardAlert.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.white))
            }

            itemView.setOnClickListener {
                onAlertClick(alert)
            }
        }

        private fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
            val date = timestamp.toDate()
            val now = Date()
            val diff = now.time - date.time

            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000}m ago"
                diff < 86400000 -> "${diff / 3600000}h ago"
                else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
            }
        }
    }
}
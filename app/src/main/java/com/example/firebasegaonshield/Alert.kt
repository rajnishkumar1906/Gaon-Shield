package com.example.firebasegaonshield

import com.google.firebase.Timestamp

data class Alert(
    val alertId: String = "",
    val title: String = "",
    val message: String = "",
    val type: AlertType = AlertType.COMMUNITY,
    val priority: Priority = Priority.LOW,
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val targetAudience: TargetAudience = TargetAudience.ALL,
    val villageIds: List<String> = emptyList(),
    val communityIds: List<String> = emptyList(),
    val regionIds: List<String> = emptyList(),
    val timestamp: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp? = null,
    val isRead: Boolean = false,
    val actionRequired: Boolean = false,
    val relatedReportId: String? = null
) {
    enum class AlertType {
        DISEASE_OUTBREAK,
        WATER_QUALITY,
        WEATHER,
        COMMUNITY,
        HEALTH_ADVISORY,
        SAFETY_UPDATE
    }

    enum class Priority {
        LOW, MEDIUM, HIGH, URGENT
    }

    enum class TargetAudience {
        ALL, VILLAGE, COMMUNITY, REGION, SPECIFIC_USERS
    }
}
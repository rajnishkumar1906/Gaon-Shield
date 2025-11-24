package com.example.firebasegaonshield

import com.google.firebase.Timestamp

data class SafetyZone(
    val zoneId: String = "",
    val villageId: String = "",
    val communityId: String = "",
    val centerLatitude: Double = 0.0,
    val centerLongitude: Double = 0.0,
    val radius: Double = 2000.0,
    val safetyStatus: SafetyStatus = SafetyStatus.SAFE,
    val patientCount: Int = 0,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val lastUpdated: Timestamp = Timestamp.now(),
    val affectedDiseases: List<String> = emptyList()
) {
    enum class SafetyStatus {
        SAFE, MODERATE, UNSAFE, CRITICAL
    }

    enum class RiskLevel {
        LOW, MEDIUM, HIGH, VERY_HIGH
    }
}
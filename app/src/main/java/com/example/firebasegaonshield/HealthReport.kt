package com.example.firebasegaonshield

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint as FirestoreGeoPoint

data class HealthReport(
    val reportId: String = "",
    val patientName: String = "",
    val patientAge: Int = 0,
    val patientGender: String = "",
    val village: String = "",
    val community: String = "",
    val region: String = "",
    val reportedBy: String = "",
    val reportedByName: String = "",
    val symptoms: List<String> = emptyList(),
    val symptomDuration: Int = 0,
    val severity: Int = 1,
    val waterSource: WaterSource = WaterSource.TAP,
    val predictedDisease: String = "Not Predicted",
    val confidence: Float = 0f,
    val status: ReportStatus = ReportStatus.PENDING,
    val assignedAshaWorker: String = "Not Assigned",
    val notes: String = "",
    val location: FirestoreGeoPoint? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val isEmergency: Boolean = false
) {
    enum class WaterSource {
        WELL, TAP, RIVER, POND, OTHER
    }

    enum class ReportStatus {
        PENDING, UNDER_REVIEW, RESOLVED, CRITICAL
    }

    // Helper function to convert to Map for Firestore
    fun toMap(): Map<String, Any> {
        return mapOf(
            "reportId" to reportId,
            "patientName" to patientName,
            "patientAge" to patientAge,
            "patientGender" to patientGender,
            "village" to village,
            "community" to community,
            "region" to region,
            "reportedBy" to reportedBy,
            "reportedByName" to reportedByName,
            "symptoms" to symptoms,
            "symptomDuration" to symptomDuration,
            "severity" to severity,
            "waterSource" to waterSource.name,
            "predictedDisease" to predictedDisease,
            "confidence" to confidence,
            "status" to status.name,
            "assignedAshaWorker" to assignedAshaWorker,
            "notes" to notes,
            "timestamp" to timestamp,
            "isEmergency" to isEmergency
        ).apply {
            if (location != null) {
                plus("location" to location)
            }
        }
    }
}
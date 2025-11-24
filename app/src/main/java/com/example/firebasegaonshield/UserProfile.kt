package com.example.firebasegaonshield

import com.google.firebase.Timestamp

data class UserProfile(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "",
    val phone: String = "",
    val age: Int = 0,
    val gender: String = "",
    val village: String = "",
    val district: String = "",
    val state: String = "",
    val profileImage: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
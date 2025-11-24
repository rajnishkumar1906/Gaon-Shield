package com.example.firebasegaonshield

data class Region(
    val regionId: String = "",
    val name: String = "",
    val state: String = "",
    val district: String = "",
    val communityIds: List<String> = emptyList(),
    val totalVillages: Int = 0,
    val totalPopulation: Int = 0,
    val currentPatientCount: Int = 0
)
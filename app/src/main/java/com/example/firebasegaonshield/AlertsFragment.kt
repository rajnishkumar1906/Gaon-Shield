package com.example.firebasegaonshield

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import android.widget.Toast

class AlertsFragment : Fragment() {

    private lateinit var rvAlerts: RecyclerView
    private lateinit var emptyState: View
    private lateinit var btnRefresh: MaterialButton
    private lateinit var chipGroup: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipDisease: Chip
    private lateinit var chipWater: Chip
    private lateinit var chipCommunity: Chip

    private lateinit var alertsAdapter: AlertsAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var alertsListener: ListenerRegistration? = null

    private val allAlerts = mutableListOf<Alert>()
    private val filteredAlerts = mutableListOf<Alert>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_alerts, container, false)
        initializeViews(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFirebase()
        setupRecyclerView()
        setupClickListeners()
        loadAlertsFromFirestore()
    }

    private fun initializeViews(view: View) {
        rvAlerts = view.findViewById(R.id.rvAlerts)
        emptyState = view.findViewById(R.id.emptyState)
        btnRefresh = view.findViewById(R.id.btnRefresh)
        chipGroup = view.findViewById(R.id.chipGroup)
        chipAll = view.findViewById(R.id.chipAll)
        chipDisease = view.findViewById(R.id.chipDisease)
        chipWater = view.findViewById(R.id.chipWater)
        chipCommunity = view.findViewById(R.id.chipCommunity)

        // Remove FAB reference - villagers can't create alerts
    }

    private fun setupFirebase() {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    private fun setupRecyclerView() {
        alertsAdapter = AlertsAdapter(filteredAlerts) { alert ->
            markAlertAsRead(alert.alertId)
        }
        rvAlerts.layoutManager = LinearLayoutManager(requireContext())
        rvAlerts.adapter = alertsAdapter
    }

    private fun setupClickListeners() {
        btnRefresh.setOnClickListener {
            loadAlertsFromFirestore()
        }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            filterAlerts()
        }
    }

    private fun loadAlertsFromFirestore() {
        alertsListener?.remove()

        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(currentUserId).get()
            .addOnSuccessListener { userDocument ->
                val userVillage = userDocument.getString("village") ?: "Unknown"

                alertsListener = firestore.collection("alerts")
                    .whereArrayContains("targetVillages", userVillage) // Show alerts for villager's village only
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            showEmptyState("Error loading alerts")
                            return@addSnapshotListener
                        }

                        allAlerts.clear()
                        snapshot?.documents?.forEach { document ->
                            val alert = document.toObject(Alert::class.java)
                            alert?.let {
                                val alertWithId = if (it.alertId.isEmpty()) {
                                    it.copy(alertId = document.id)
                                } else {
                                    it
                                }
                                allAlerts.add(alertWithId)
                            }
                        }

                        filterAlerts()
                    }
            }
            .addOnFailureListener {
                showEmptyState("Failed to load your village information")
            }
    }

    private fun filterAlerts() {
        filteredAlerts.clear()

        when {
            chipAll.isChecked || chipGroup.checkedChipId == View.NO_ID -> {
                filteredAlerts.addAll(allAlerts)
            }
            chipDisease.isChecked -> {
                filteredAlerts.addAll(allAlerts.filter { it.type == Alert.AlertType.DISEASE_OUTBREAK })
            }
            chipWater.isChecked -> {
                filteredAlerts.addAll(allAlerts.filter { it.type == Alert.AlertType.WATER_QUALITY })
            }
            chipCommunity.isChecked -> {
                filteredAlerts.addAll(allAlerts.filter { it.type == Alert.AlertType.COMMUNITY })
            }
        }

        updateUI()
    }

    private fun markAlertAsRead(alertId: String) {
        if (alertId.isNotEmpty()) {
            firestore.collection("alerts").document(alertId)
                .update("isRead", true)
                .addOnSuccessListener {
                    // Update local data
                    val index = allAlerts.indexOfFirst { it.alertId == alertId }
                    if (index != -1) {
                        val updatedAlert = allAlerts[index].copy(isRead = true)
                        allAlerts[index] = updatedAlert

                        val filteredIndex = filteredAlerts.indexOfFirst { it.alertId == alertId }
                        if (filteredIndex != -1) {
                            filteredAlerts[filteredIndex] = updatedAlert
                        }

                        alertsAdapter.notifyDataSetChanged()
                    }
                }
        }
    }

    private fun updateUI() {
        if (filteredAlerts.isEmpty()) {
            showEmptyState("No alerts for your village")
        } else {
            hideEmptyState()
        }
        alertsAdapter.notifyDataSetChanged()
    }

    private fun showEmptyState(message: String = "No alerts available") {
        emptyState.visibility = View.VISIBLE
        rvAlerts.visibility = View.GONE
        // You might want to update the empty state text here
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        rvAlerts.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        alertsListener?.remove()
    }
}
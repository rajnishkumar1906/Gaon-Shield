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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import android.widget.Toast

class CommunityAlertsFragment : Fragment() {

    private lateinit var rvAlerts: RecyclerView
    private lateinit var emptyState: View
    private lateinit var btnRefresh: MaterialButton
    private lateinit var fabNewAlert: FloatingActionButton
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
        return inflater.inflate(R.layout.fragment_community_alerts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupFirebase()
        setupRecyclerView()
        setupClickListeners()
        loadAllAlerts() // Load ALL alerts for management
    }

    private fun initializeViews(view: View) {
        rvAlerts = view.findViewById(R.id.rvAlerts)
        emptyState = view.findViewById(R.id.emptyState)
        btnRefresh = view.findViewById(R.id.btnRefresh)
        fabNewAlert = view.findViewById(R.id.fabNewAlert)
        chipGroup = view.findViewById(R.id.chipGroup)
        chipAll = view.findViewById(R.id.chipAll)
        chipDisease = view.findViewById(R.id.chipDisease)
        chipWater = view.findViewById(R.id.chipWater)
        chipCommunity = view.findViewById(R.id.chipCommunity)

        // FAB should be visible for ASHA workers
        fabNewAlert.visibility = View.VISIBLE
    }

    private fun setupFirebase() {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    private fun setupRecyclerView() {
        alertsAdapter = AlertsAdapter(filteredAlerts) { alert ->
            // For community dashboard, show more options (edit, delete, etc.)
            showAlertOptions(alert)
        }
        rvAlerts.layoutManager = LinearLayoutManager(requireContext())
        rvAlerts.adapter = alertsAdapter
    }

    private fun setupClickListeners() {
        btnRefresh.setOnClickListener {
            loadAllAlerts()
        }

        fabNewAlert.setOnClickListener {
            // Only ASHA workers can create alerts
            if (isAshaWorker()) {
                val dialog = CreateAlertDialogFragment()
                dialog.show(parentFragmentManager, "CreateAlertDialog")
            } else {
                Toast.makeText(requireContext(), "Only ASHA workers can create alerts", Toast.LENGTH_SHORT).show()
            }
        }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            filterAlerts()
        }
    }

    private fun isAshaWorker(): Boolean {
        // Check if current user is ASHA worker
        // You might want to implement proper role checking
        return true // Temporary - implement proper role check
    }

    private fun loadAllAlerts() {
        alertsListener?.remove()

        // Load ALL alerts for management view
        alertsListener = firestore.collection("alerts")
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

    private fun showAlertOptions(alert: Alert) {
        // Show options like edit, delete, view details for community dashboard
        Toast.makeText(
            requireContext(),
            "Alert: ${alert.title}\nClick to manage options",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateUI() {
        if (filteredAlerts.isEmpty()) {
            showEmptyState("No alerts created yet")
        } else {
            hideEmptyState()
        }
        alertsAdapter.notifyDataSetChanged()
    }

    private fun showEmptyState(message: String = "No alerts available") {
        emptyState.visibility = View.VISIBLE
        rvAlerts.visibility = View.GONE
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
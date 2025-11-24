package com.example.firebasegaonshield

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class CreateAlertDialogFragment : DialogFragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_create_alert, null)

        val spinnerType: Spinner = view.findViewById(R.id.spinnerAlertType)
        val spinnerPriority: Spinner = view.findViewById(R.id.spinnerPriority)
        val etTitle: EditText = view.findViewById(R.id.etAlertTitle)
        val etMessage: EditText = view.findViewById(R.id.etAlertMessage)

        // Setup alert type spinner
        val alertTypes = Alert.AlertType.values().map { it.name }
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, alertTypes)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = typeAdapter

        // Setup priority spinner
        val priorities = Alert.Priority.values().map { it.name }
        val priorityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, priorities)
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = priorityAdapter

        builder.setView(view)
            .setTitle("Create New Alert")
            .setPositiveButton("Send Alert") { dialog, which ->
                val title = etTitle.text.toString().trim()
                val message = etMessage.text.toString().trim()
                val type = Alert.AlertType.valueOf(spinnerType.selectedItem.toString())
                val priority = Alert.Priority.valueOf(spinnerPriority.selectedItem.toString())

                if (title.isEmpty() || message.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                createNewAlert(title, message, type, priority)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

        return builder.create()
    }

    private fun createNewAlert(title: String, message: String, type: Alert.AlertType, priority: Alert.Priority) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val userName = document.getString("fullName") ?: "Unknown"
                val userRole = document.getString("role") ?: "ASHA Worker"

                val newAlert = Alert(
                    alertId = firestore.collection("alerts").document().id,
                    title = title,
                    message = message,
                    type = type,
                    priority = priority,
                    senderId = currentUser.uid,
                    senderName = userName,
                    senderRole = userRole,
                    timestamp = Timestamp.now(),
                    isRead = false,
                    actionRequired = priority == Alert.Priority.HIGH || priority == Alert.Priority.URGENT
                )

                firestore.collection("alerts")
                    .add(newAlert)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Alert sent successfully", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to send alert: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to get user data", Toast.LENGTH_SHORT).show()
            }
    }
}
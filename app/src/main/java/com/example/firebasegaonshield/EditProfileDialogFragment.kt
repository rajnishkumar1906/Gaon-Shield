package com.example.firebasegaonshield

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileDialogFragment : DialogFragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var profileUpdateListener: ProfileUpdateListener? = null

    interface ProfileUpdateListener {
        fun onProfileUpdated()
    }

    fun setProfileUpdateListener(listener: ProfileUpdateListener) {
        this.profileUpdateListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_profile, null)

        val etFullName: EditText = view.findViewById(R.id.etFullName)
        val etPhone: EditText = view.findViewById(R.id.etPhone)
        val etAge: EditText = view.findViewById(R.id.etAge)
        val etGender: EditText = view.findViewById(R.id.etGender)
        val etVillage: EditText = view.findViewById(R.id.etVillage)
        val etDistrict: EditText = view.findViewById(R.id.etDistrict)
        val etState: EditText = view.findViewById(R.id.etState)

        loadCurrentProfile(etFullName, etPhone, etAge, etGender, etVillage, etDistrict, etState)

        builder.setView(view)
            .setTitle("Edit Profile")
            .setPositiveButton("Save") { dialog, which ->
                saveProfile(
                    etFullName.text.toString(),
                    etPhone.text.toString(),
                    etAge.text.toString(),
                    etGender.text.toString(),
                    etVillage.text.toString(),
                    etDistrict.text.toString(),
                    etState.text.toString()
                )
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }

        return builder.create()
    }

    private fun loadCurrentProfile(
        etFullName: EditText, etPhone: EditText, etAge: EditText,
        etGender: EditText, etVillage: EditText, etDistrict: EditText, etState: EditText
    ) {
        val currentUser = auth.currentUser ?: return

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    etFullName.setText(document.getString("fullName") ?: "")
                    etPhone.setText(document.getString("phone") ?: "")
                    etAge.setText(document.getLong("age")?.toString() ?: "")
                    etGender.setText(document.getString("gender") ?: "")
                    etVillage.setText(document.getString("village") ?: "")
                    etDistrict.setText(document.getString("district") ?: "")
                    etState.setText(document.getString("state") ?: "")
                }
            }
    }

    private fun saveProfile(
        fullName: String, phone: String, age: String,
        gender: String, village: String, district: String, state: String
    ) {
        val currentUser = auth.currentUser ?: return

        val updates = hashMapOf<String, Any>(
            "fullName" to fullName,
            "phone" to phone,
            "gender" to gender,
            "village" to village,
            "district" to district,
            "state" to state
        )

        age.toIntOrNull()?.let {
            updates["age"] = it
        }

        firestore.collection("users").document(currentUser.uid)
            .update(updates)
            .addOnSuccessListener {
                profileUpdateListener?.onProfileUpdated()
                showMessage("Profile updated successfully")
                dismiss()
            }
            .addOnFailureListener { e ->
                showMessage("Failed to update profile: ${e.message}")
            }
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
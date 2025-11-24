package com.example.firebasegaonshield

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HealthReportSubmitFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var diseasePredictor: DiseasePredictor

    // Views
    private lateinit var etPatientName: EditText
    private lateinit var etAge: EditText
    private lateinit var etGender: EditText
    private lateinit var etSymptomDuration: EditText
    private lateinit var etSeverity: EditText
    private lateinit var cbFever: CheckBox
    private lateinit var cbDiarrhea: CheckBox
    private lateinit var cbVomiting: CheckBox
    private lateinit var cbAbdominalPain: CheckBox
    private lateinit var cbDehydration: CheckBox
    private lateinit var cbBloodInStool: CheckBox
    private lateinit var cbFatigue: CheckBox
    private lateinit var cbNausea: CheckBox
    private lateinit var cbMuscleCramps: CheckBox
    private lateinit var cbHeadache: CheckBox
    private lateinit var rgWaterSource: RadioGroup
    private lateinit var btnPredict: Button
    private lateinit var btnSubmit: Button
    private lateinit var btnClearForm: Button
    private lateinit var tvPredictionResult: TextView
    private lateinit var tvConfidence: TextView
    private lateinit var cardPredictionResult: androidx.cardview.widget.CardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_health_report_submit, container, false)
        initializeViews(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        diseasePredictor = DiseasePredictor(requireContext())

        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        etPatientName = view.findViewById(R.id.etPatientName)
        etAge = view.findViewById(R.id.etAge)
        etGender = view.findViewById(R.id.etGender)
        etSymptomDuration = view.findViewById(R.id.etSymptomDuration)
        etSeverity = view.findViewById(R.id.etSeverity)
        cbFever = view.findViewById(R.id.cbFever)
        cbDiarrhea = view.findViewById(R.id.cbDiarrhea)
        cbVomiting = view.findViewById(R.id.cbVomiting)
        cbAbdominalPain = view.findViewById(R.id.cbAbdominalPain)
        cbDehydration = view.findViewById(R.id.cbDehydration)
        cbBloodInStool = view.findViewById(R.id.cbBloodInStool)
        cbFatigue = view.findViewById(R.id.cbFatigue)
        cbNausea = view.findViewById(R.id.cbNausea)
        cbMuscleCramps = view.findViewById(R.id.cbMuscleCramps)
        cbHeadache = view.findViewById(R.id.cbHeadache)
        rgWaterSource = view.findViewById(R.id.rgWaterSource)
        btnPredict = view.findViewById(R.id.btnPredict)
        btnSubmit = view.findViewById(R.id.btnSubmitReport)
        btnClearForm = view.findViewById(R.id.btnClearForm)
        tvPredictionResult = view.findViewById(R.id.tvPredictionResult)
        tvConfidence = view.findViewById(R.id.tvConfidence)
        cardPredictionResult = view.findViewById(R.id.cardPredictionResult)
    }

    private fun setupClickListeners() {
        btnPredict.setOnClickListener {
            predictDisease()
        }

        btnSubmit.setOnClickListener {
            submitHealthReport()
        }

        btnClearForm.setOnClickListener {
            clearForm()
        }
    }

    private fun predictDisease() {
        // Validate required fields
        if (!validateInput()) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val healthData = collectHealthData()
        val prediction = diseasePredictor.predict(healthData)

        prediction?.let {
            // Show the prediction card
            cardPredictionResult.visibility = View.VISIBLE
            tvPredictionResult.text = "Predicted: ${it.disease}"
            tvConfidence.text = "Confidence: ${"%.2f".format(it.confidence * 100)}%"

            // Change color based on confidence
            val confidenceColor = when {
                it.confidence > 0.7 -> "#4CAF50" // Green for high confidence
                it.confidence > 0.4 -> "#FF9800" // Orange for medium confidence
                else -> "#F44336" // Red for low confidence
            }
            tvConfidence.setTextColor(android.graphics.Color.parseColor(confidenceColor))

        } ?: run {
            cardPredictionResult.visibility = View.VISIBLE
            tvPredictionResult.text = "Prediction failed"
            tvConfidence.text = "Please try again"
            tvConfidence.setTextColor(android.graphics.Color.parseColor("#F44336"))
        }
    }

    private fun validateInput(): Boolean {
        return etAge.text.toString().isNotEmpty() &&
                etSymptomDuration.text.toString().isNotEmpty() &&
                etSeverity.text.toString().isNotEmpty() &&
                rgWaterSource.checkedRadioButtonId != -1
    }

    private fun submitHealthReport() {
        if (!validateInput()) {
            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val healthData = collectHealthData()
        val currentUser = auth.currentUser ?: return

        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val userName = document.getString("fullName") ?: "Unknown"
                val userVillage = document.getString("village") ?: "Unknown"
                val userCommunity = document.getString("community") ?: "Unknown"
                val userRegion = document.getString("region") ?: "Unknown"

                val predictedDisease = if (cardPredictionResult.visibility == View.VISIBLE) {
                    tvPredictionResult.text.toString().replace("Predicted: ", "").split(" ")[0]
                } else {
                    "Not Predicted"
                }

                val confidence = if (cardPredictionResult.visibility == View.VISIBLE) {
                    tvConfidence.text.toString().replace("Confidence: ", "").replace("%", "").toFloatOrNull() ?: 0f
                } else {
                    0f
                }

                val healthReport = HealthReport(
                    reportId = firestore.collection("health_reports").document().id,
                    patientName = etPatientName.text.toString(),
                    patientAge = etAge.text.toString().toIntOrNull() ?: 0,
                    patientGender = etGender.text.toString(),
                    village = userVillage,
                    community = userCommunity,
                    region = userRegion,
                    reportedBy = currentUser.uid,
                    reportedByName = userName,
                    symptoms = getSelectedSymptoms(),
                    symptomDuration = etSymptomDuration.text.toString().toIntOrNull() ?: 0,
                    severity = etSeverity.text.toString().toIntOrNull() ?: 1,
                    waterSource = getSelectedWaterSource(),
                    predictedDisease = predictedDisease,
                    confidence = confidence / 100f,
                    timestamp = Timestamp.now(),
                    isEmergency = (etSeverity.text.toString().toIntOrNull() ?: 1) >= 8
                )

                // Use toMap() for better Firestore compatibility
                firestore.collection("health_reports")
                    .document(healthReport.reportId)
                    .set(healthReport.toMap())
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Health report submitted successfully", Toast.LENGTH_SHORT).show()
                        clearForm()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to submit report: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun collectHealthData(): HealthData {
        return HealthData(
            age = etAge.text.toString().toIntOrNull() ?: 0,
            symptomDuration = etSymptomDuration.text.toString().toIntOrNull() ?: 0,
            fever = cbFever.isChecked,
            diarrhea = cbDiarrhea.isChecked,
            vomiting = cbVomiting.isChecked,
            abdominalPain = cbAbdominalPain.isChecked,
            dehydration = cbDehydration.isChecked,
            bloodInStool = cbBloodInStool.isChecked,
            fatigue = cbFatigue.isChecked,
            nausea = cbNausea.isChecked,
            muscleCramps = cbMuscleCramps.isChecked,
            headache = cbHeadache.isChecked,
            waterSource = getWaterSourceValue(),
            severity = etSeverity.text.toString().toIntOrNull() ?: 1
        )
    }

    private fun getSelectedSymptoms(): List<String> {
        val symptoms = mutableListOf<String>()
        if (cbFever.isChecked) symptoms.add("Fever")
        if (cbDiarrhea.isChecked) symptoms.add("Diarrhea")
        if (cbVomiting.isChecked) symptoms.add("Vomiting")
        if (cbAbdominalPain.isChecked) symptoms.add("Abdominal Pain")
        if (cbDehydration.isChecked) symptoms.add("Dehydration")
        if (cbBloodInStool.isChecked) symptoms.add("Blood in Stool")
        if (cbFatigue.isChecked) symptoms.add("Fatigue")
        if (cbNausea.isChecked) symptoms.add("Nausea")
        if (cbMuscleCramps.isChecked) symptoms.add("Muscle Cramps")
        if (cbHeadache.isChecked) symptoms.add("Headache")
        return symptoms
    }

    private fun getSelectedWaterSource(): HealthReport.WaterSource {
        return when (rgWaterSource.checkedRadioButtonId) {
            R.id.rbWell -> HealthReport.WaterSource.WELL
            R.id.rbTap -> HealthReport.WaterSource.TAP
            R.id.rbRiver -> HealthReport.WaterSource.RIVER
            R.id.rbPond -> HealthReport.WaterSource.POND
            else -> HealthReport.WaterSource.OTHER
        }
    }

    private fun getWaterSourceValue(): Int {
        return when (rgWaterSource.checkedRadioButtonId) {
            R.id.rbWell -> 0
            R.id.rbTap -> 1
            R.id.rbRiver -> 2
            R.id.rbPond -> 3
            else -> 4
        }
    }

    private fun clearForm() {
        etPatientName.text.clear()
        etAge.text.clear()
        etGender.text.clear()
        etSymptomDuration.text.clear()
        etSeverity.text.clear()
        rgWaterSource.clearCheck()

        val checkboxes = listOf(
            cbFever, cbDiarrhea, cbVomiting, cbAbdominalPain, cbDehydration,
            cbBloodInStool, cbFatigue, cbNausea, cbMuscleCramps, cbHeadache
        )
        checkboxes.forEach { it.isChecked = false }

        cardPredictionResult.visibility = View.GONE
        tvPredictionResult.text = "No prediction yet"
        tvConfidence.text = ""
    }

    override fun onDestroy() {
        super.onDestroy()
        diseasePredictor.close()
    }
}
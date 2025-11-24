package com.example.firebasegaonshield

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DiseasePredictor(context: Context) {

    private var interpreter: Interpreter? = null

    companion object {
        private const val TAG = "DiseasePredictor"
        private const val MODEL_FILE = "disease_predictor.tflite"
    }

    init {
        loadModel(context)
    }

    private fun loadModel(context: Context) {
        try {
            val model = loadModelFile(context)
            interpreter = Interpreter(model)
            Log.d(TAG, "Model loaded successfully")

            // Test if interpreter is working
            testInterpreter()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${e.message}", e)
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun testInterpreter() {
        try {
            // Test with dummy data to verify the model works
            val testInput = FloatArray(14) { 0.5f }
            val inputBuffer = ByteBuffer.allocateDirect(14 * 4)
                .order(ByteOrder.nativeOrder())

            testInput.forEach { value ->
                inputBuffer.putFloat(value)
            }

            val outputArray = Array(1) { FloatArray(6) }
            interpreter?.run(inputBuffer, outputArray)
            Log.d(TAG, "Interpreter test successful")
        } catch (e: Exception) {
            Log.e(TAG, "Interpreter test failed: ${e.message}", e)
        }
    }

    data class PredictionResult(
        val disease: String,
        val confidence: Float
    )

    fun predict(healthData: HealthData): PredictionResult? {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter not loaded")
            return null
        }

        return try {
            val inputArray = prepareInputArray(healthData)

            // Create input buffer with correct size
            val inputBuffer = ByteBuffer.allocateDirect(14 * 4) // 14 features * 4 bytes per float
                .order(ByteOrder.nativeOrder())

            // Put all input values into the buffer
            inputArray.forEach { value ->
                inputBuffer.putFloat(value)
            }

            // Reset buffer position to start for reading
            inputBuffer.rewind()

            // Output array for 6 classes
            val outputArray = Array(1) { FloatArray(6) }

            // Run inference
            interpreter!!.run(inputBuffer, outputArray)

            // Process results
            val probabilities = outputArray[0]
            Log.d(TAG, "Raw probabilities: ${probabilities.joinToString()}")

            val predictedClass = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
            val confidence = probabilities[predictedClass]

            val diseaseName = when (predictedClass) {
                0 -> "Cholera"
                1 -> "Typhoid"
                2 -> "Dysentery"
                3 -> "Gastroenteritis"
                4 -> "Hepatitis"
                5 -> "Malaria"
                else -> "Unknown"
            }

            Log.d(TAG, "Prediction: $diseaseName with confidence: $confidence")
            PredictionResult(diseaseName, confidence)
        } catch (e: Exception) {
            Log.e(TAG, "Prediction failed: ${e.message}", e)
            null
        }
    }

    private fun prepareInputArray(healthData: HealthData): FloatArray {
        return floatArrayOf(
            healthData.age.toFloat(),
            healthData.symptomDuration.toFloat(),
            if (healthData.fever) 1f else 0f,
            if (healthData.diarrhea) 1f else 0f,
            if (healthData.vomiting) 1f else 0f,
            if (healthData.abdominalPain) 1f else 0f,
            if (healthData.dehydration) 1f else 0f,
            if (healthData.bloodInStool) 1f else 0f,
            if (healthData.fatigue) 1f else 0f,
            if (healthData.nausea) 1f else 0f,
            if (healthData.muscleCramps) 1f else 0f,
            if (healthData.headache) 1f else 0f,
            healthData.waterSource.toFloat(),
            healthData.severity.toFloat()
        )
    }

    fun close() {
        interpreter?.close()
    }
}

data class HealthData(
    val age: Int,
    val symptomDuration: Int,
    val fever: Boolean,
    val diarrhea: Boolean,
    val vomiting: Boolean,
    val abdominalPain: Boolean,
    val dehydration: Boolean,
    val bloodInStool: Boolean,
    val fatigue: Boolean,
    val nausea: Boolean,
    val muscleCramps: Boolean,
    val headache: Boolean,
    val waterSource: Int,
    val severity: Int
)
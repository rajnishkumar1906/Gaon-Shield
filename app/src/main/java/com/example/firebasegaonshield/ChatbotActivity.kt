package com.example.firebasegaonshield

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatbotActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTitle: TextView
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()

    private var generativeModel: GenerativeModel? = null
    private val TAG = "ChatbotActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_bot)
        Log.d(TAG, "ChatbotActivity created")

        initializeViews()
        setupChatRecyclerView()
        initializeGemini()
        setupClickListeners()
    }

    private fun initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        backButton = findViewById(R.id.backButton)
        progressBar = findViewById(R.id.progressBar)
        tvTitle = findViewById(R.id.tvTitle)

        tvTitle.text = "ArogyaSathi Assistant"
        Log.d(TAG, "Views initialized")
    }

    private fun setupChatRecyclerView() {
        chatAdapter = ChatAdapter(chatMessages)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter

        addWelcomeMessage()
        Log.d(TAG, "RecyclerView setup completed")
    }

    private fun initializeGemini() {
        try {
            val apiKey = "AIzaSyDHhzuoEojVaIIJ2xZ_TrtdWWDxOZ60cmQ"

            // Using the free model gemini-1.5-flash
            generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash", // Free model
                apiKey = apiKey
            )

            Log.d(TAG, "Gemini AI initialized successfully with free model")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Gemini AI: ${e.message}", e)
            showToast("AI service initialized in offline mode")
            addAIMessage("Note: I'm running in basic mode but can still help with health questions.")
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            Log.d(TAG, "Back button clicked")
            onBackPressed()
        }

        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                Log.d(TAG, "Sending message: $message")
                sendMessage(message)
                messageInput.text.clear()
            } else {
                showToast("Please enter a message")
            }
        }

        messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                val message = messageInput.text.toString().trim()
                if (message.isNotEmpty()) {
                    sendMessage(message)
                    messageInput.text.clear()
                }
                true
            } else {
                false
            }
        }
    }

    private fun addWelcomeMessage() {
        val welcomeMessage = ChatMessage(
            message = "नमस्ते! I'm ArogyaSathi, your health companion. मैं आपके स्वास्थ्य की देखभाल में आपकी सहायता के लिए यहाँ हूँ!\n\n" +
                    "I can help you with:\n\n" +
                    "• Health-related questions 🏥\n" +
                    "• Disease information & symptoms\n" +
                    "• First aid guidance\n" +
                    "• Preventive healthcare tips\n" +
                    "• Medicine information 💊\n" +
                    "• Healthy lifestyle advice\n" +
                    "• Emergency care guidance\n\n" +
                    "Note: For serious medical emergencies, please contact your nearest health center immediately.\n\n" +
                    "How can I help you today?",
            isUser = false,
            timestamp = System.currentTimeMillis()
        )
        chatMessages.add(welcomeMessage)
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        chatRecyclerView.scrollToPosition(chatMessages.size - 1)
    }

    private fun addAIMessage(message: String) {
        val aiMessage = ChatMessage(
            message = message,
            isUser = false,
            timestamp = System.currentTimeMillis()
        )
        chatMessages.add(aiMessage)
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        chatRecyclerView.scrollToPosition(chatMessages.size - 1)
    }

    private fun sendMessage(message: String) {
        // Add user message to chat
        val userMessage = ChatMessage(
            message = message,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        chatMessages.add(userMessage)
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        chatRecyclerView.scrollToPosition(chatMessages.size - 1)

        showLoading(true)

        // Get AI response
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = getAIResponse(message)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    val aiMessage = ChatMessage(
                        message = response,
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    chatMessages.add(aiMessage)
                    chatAdapter.notifyItemInserted(chatMessages.size - 1)
                    chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                    Log.d(TAG, "AI response received successfully")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Log.e(TAG, "Error getting AI response: ${e.message}", e)
                    // Use fallback responses when AI fails
                    val fallbackResponse = getFallbackResponse(message)
                    val errorMessage = ChatMessage(
                        message = fallbackResponse,
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    chatMessages.add(errorMessage)
                    chatAdapter.notifyItemInserted(chatMessages.size - 1)
                    chatRecyclerView.scrollToPosition(chatMessages.size - 1)
                }
            }
        }
    }

    private suspend fun getAIResponse(userMessage: String): String {
        return try {
            val model = generativeModel
            if (model != null) {
                val prompt = """You are ArogyaSathi, a helpful health assistant for Indian village communities. 
                    |Provide clear, simple health advice in English or Hinglish. 
                    |Focus on practical, culturally appropriate guidance for rural healthcare. 
                    |Always emphasize visiting doctors for serious conditions. 
                    |Be empathetic and supportive in your responses. Keep responses under 500 characters.
                    |
                    |User: $userMessage
                    |
                    |ArogyaSathi:""".trimMargin()

                Log.d(TAG, "Sending request to Gemini API with free model...")
                val response = model.generateContent(prompt)
                val responseText = response.text ?: "I couldn't generate a response. Please try again."
                Log.d(TAG, "Received response from Gemini API: ${responseText.take(50)}...")
                responseText
            } else {
                getFallbackResponse(userMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getAIResponse: ${e.message}", e)
            getFallbackResponse(userMessage)
        }
    }

    private fun getFallbackResponse(userMessage: String): String {
        return when {
            userMessage.contains("fever", ignoreCase = true) ->
                "For fever: Rest, drink fluids, take paracetamol. If fever >3 days or very high, see doctor. 🏥"

            userMessage.contains("headache", ignoreCase = true) ->
                "For headache: Rest in quiet room, cold compress, stay hydrated. If severe or frequent, consult doctor."

            userMessage.contains("cold", ignoreCase = true) || userMessage.contains("cough", ignoreCase = true) ->
                "For cold/cough: Drink warm ginger tea, rest. If breathing difficulty or worsening, see doctor immediately."

            userMessage.contains("stomach", ignoreCase = true) || userMessage.contains("diarrhea", ignoreCase = true) ->
                "For stomach issues: Drink ORS, eat light foods (banana, rice). If severe pain/vomiting, visit health center."

            userMessage.contains("pain", ignoreCase = true) ->
                "For pain: Rest affected area, simple pain relief. If severe/persistent/after injury, see doctor."

            userMessage.contains("hello", ignoreCase = true) || userMessage.contains("hi", ignoreCase = true) ->
                "नमस्ते! How can I help you with your health today? 🏥"

            userMessage.contains("thank", ignoreCase = true) ->
                "You're welcome! Stay healthy and don't hesitate to ask if you need more health guidance. 💙"

            else ->
                "I understand you're asking about health. For detailed medical advice, please consult your local health center. I can help with general health tips for fever, headache, cold, stomach issues, etc."
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        sendButton.isEnabled = !show
        messageInput.isEnabled = !show
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
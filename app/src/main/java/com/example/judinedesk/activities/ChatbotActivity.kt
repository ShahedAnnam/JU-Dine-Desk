package com.example.judinedesk.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.judinedesk.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import com.example.judinedesk.BuildConfig

class ChatbotActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatbotActivity"
    }

    // API Configuration - Use the latest Gemini API
    private val apiKey = BuildConfig.GEMINI_API_KEY
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Firebase
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // UI Components
    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var outputTextView: TextView

    // Chat History - Updated structure for Gemini 1.5
    private val chatHistory = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        Log.d(TAG, "ChatbotActivity started")
        Log.d(TAG, "API Key available: ${!apiKey.isNullOrEmpty()}")

        setupViews()
        setupClickListeners()

        // Initialize with system prompt
        addSystemPrompt()

        // Load previous chat history from Firebase
        loadChatHistoryFromFirebase { previousChat ->
            if (previousChat.isNotEmpty()) {
                chatHistory.addAll(previousChat)
                outputTextView.text = "Welcome back! How can I help you today?"
                Log.d(TAG, "Loaded ${previousChat.size} previous messages")
            } else {
                outputTextView.text = "Hello! I'm your AI assistant. How can I help you today?"
                Log.d(TAG, "No previous chat history found")
            }
        }
    }

    private fun addSystemPrompt() {
        val systemPrompt = mapOf(
            "role" to "user",
            "parts" to listOf(
                mapOf(
                    "text" to """
                    You are a helpful AI assistant for a university meal coupon system called Judine Desk.
                    You help students with:
                    1. Buying meal coupons
                    2. Checking available meals
                    3. Viewing QR codes
                    4. Managing their account
                    5. Answering questions about the system
                    
                    Keep your responses concise, helpful, and friendly.
                    If you don't know something, say so honestly.
                    """
                )
            )
        )

        // Add as initial context if not already present
        if (chatHistory.none { it["role"] == "user" &&
                    (it["parts"] as? List<*>)?.firstOrNull()?.toString()?.contains("system") == true }) {
            chatHistory.add(0, systemPrompt)
        }
    }

    private fun setupViews() {
        inputEditText = findViewById(R.id.inputEditText)
        sendButton = findViewById(R.id.sendButton)
        outputTextView = findViewById(R.id.outputTextView)
    }

    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            val userMessage = inputEditText.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                // Disable button while processing
                sendButton.isEnabled = false
                sendButton.text = "Sending..."

                inputEditText.text.clear()
                handleUserMessage(userMessage)
                saveMessageToFirebase("user", userMessage)
            }
        }
    }

    private fun handleUserMessage(message: String) {
        Log.d(TAG, "User message: $message")

        // Add to chat history
        val userMessage = mapOf(
            "role" to "user",
            "parts" to listOf(mapOf("text" to message))
        )
        chatHistory.add(userMessage)

        // Show in UI
        runOnUiThread {
            outputTextView.append("\n\nYou: $message")
            outputTextView.append("\nAI: Thinking...")
        }

        // Send to AI
        askGemini()
    }

    private fun askGemini() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Use the latest Gemini API endpoint
                val url = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=$apiKey"

                // Prepare the request body according to Gemini API format
                val requestBody = createRequestBody()

                Log.d(TAG, "Sending request to Gemini API")
                Log.d(TAG, "Request body: ${JSONObject(requestBody).toString(2)}")

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                handleApiResponse(response)

            } catch (e: Exception) {
                Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
                showError("Network error: ${e.message}")
                runOnUiThread {
                    sendButton.isEnabled = true
                    sendButton.text = "Send"
                }
            }
        }
    }

    private fun createRequestBody(): String {
        val contents = JSONArray()

        // Add all messages from chat history
        chatHistory.forEach { message ->
            val content = JSONObject().apply {
                put("role", message["role"])

                val parts = JSONArray()
                (message["parts"] as? List<Map<String, String>>)?.forEach { part ->
                    parts.put(JSONObject().apply {
                        put("text", part["text"])
                    })
                }
                put("parts", parts)
            }
            contents.put(content)
        }

        val requestBody = JSONObject().apply {
            put("contents", contents)

            // Optional: Add generation config for better responses
            val generationConfig = JSONObject().apply {
                put("temperature", 0.7)
                put("topK", 1)
                put("topP", 0.95)
                put("maxOutputTokens", 1024)
            }
            put("generationConfig", generationConfig)

            // Optional: Add safety settings
            val safetySettings = JSONArray().apply {
                put(JSONObject().apply {
                    put("category", "HARM_CATEGORY_HARASSMENT")
                    put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                })
                put(JSONObject().apply {
                    put("category", "HARM_CATEGORY_HATE_SPEECH")
                    put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                })
                put(JSONObject().apply {
                    put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT")
                    put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                })
                put(JSONObject().apply {
                    put("category", "HARM_CATEGORY_DANGEROUS_CONTENT")
                    put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                })
            }
            put("safetySettings", safetySettings)
        }

        return requestBody.toString()
    }

    private fun handleApiResponse(response: Response) {
        runOnUiThread {
            sendButton.isEnabled = true
            sendButton.text = "Send"
        }

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "No error body"
            Log.e(TAG, "API Response Error: ${response.code} - $errorBody")
            showError("API Error ${response.code}: ${response.message}")
            return
        }

        try {
            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "API Response: $responseBody")

            val json = JSONObject(responseBody)

            // Check for API errors
            if (json.has("error")) {
                val error = json.getJSONObject("error")
                val message = error.getString("message")
                val code = error.getInt("code")
                Log.e(TAG, "Gemini API Error: $code - $message")
                showError("Gemini API Error: $message")
                return
            }

            // Extract response text
            val candidates = json.optJSONArray("candidates")

            if (candidates == null || candidates.length() == 0) {
                Log.e(TAG, "No candidates in response")
                showError("No response from AI. The model might be blocked by safety filters.")
                return
            }

            val candidate = candidates.getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")

            if (parts.length() == 0) {
                Log.e(TAG, "No parts in response")
                showError("AI response was empty.")
                return
            }

            val text = parts.getJSONObject(0).getString("text").trim()

            // Check if response was blocked
            if (candidate.has("finishReason") &&
                candidate.getString("finishReason") == "SAFETY") {
                Log.w(TAG, "Response blocked by safety filters")
                handleAiResponse("I apologize, but I cannot provide a response to that question due to safety guidelines.")
                return
            }

            Log.d(TAG, "AI Response received: ${text.take(100)}...")
            handleAiResponse(text)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing API response: ${e.message}", e)
            showError("Error parsing response: ${e.message}")
        }
    }

    private fun handleAiResponse(text: String) {
        // Save to history
        val aiMessage = mapOf(
            "role" to "model",
            "parts" to listOf(mapOf("text" to text))
        )
        chatHistory.add(aiMessage)

        // Save to Firebase
        saveMessageToFirebase("model", text)

        // Show in UI
        runOnUiThread {
            // Remove "Thinking..." and add actual response
            val currentText = outputTextView.text.toString()
            val lastLineIndex = currentText.lastIndexOf("\nAI: Thinking...")

            if (lastLineIndex != -1) {
                outputTextView.text = currentText.substring(0, lastLineIndex) + "\nAI: $text"
            } else {
                outputTextView.append("\nAI: $text")
            }

            // Scroll to bottom
            val scrollAmount = outputTextView.layout.getLineTop(outputTextView.lineCount) - outputTextView.height
            if (scrollAmount > 0) {
                outputTextView.scrollTo(0, scrollAmount)
            }
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, message)
        runOnUiThread {
            outputTextView.append("\n\n[Error: $message]")
        }
    }

    // ================= Firebase Methods =================
    private fun saveMessageToFirebase(role: String, text: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "No user logged in, skipping Firebase save")
            return
        }

        val message = hashMapOf(
            "userId" to currentUser.uid,
            "role" to role,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("chats")
            .add(message)
            .addOnSuccessListener {
                Log.d(TAG, "Message saved to Firebase")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving message to Firebase", e)
            }
    }

    private fun loadChatHistoryFromFirebase(callback: (List<Map<String, Any>>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.w(TAG, "No user logged in, cannot load chat history")
            callback(emptyList())
            return
        }

        db.collection("chats")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp")
            .limit(20) // Load last 20 messages
            .get()
            .addOnSuccessListener { result ->
                val previousChat = mutableListOf<Map<String, Any>>()

                for (doc in result.documents) {
                    val role = doc.getString("role") ?: continue
                    val text = doc.getString("text") ?: continue

                    previousChat.add(
                        mapOf(
                            "role" to role,
                            "parts" to listOf(mapOf("text" to text))
                        )
                    )
                }

                Log.d(TAG, "Loaded ${previousChat.size} messages from Firebase")
                callback(previousChat)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load chat from Firebase", e)
                callback(emptyList())
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.dispatcher.executorService.shutdown()
        Log.d(TAG, "ChatbotActivity destroyed")
    }
}
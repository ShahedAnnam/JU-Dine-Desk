package com.example.judinedesk.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.judinedesk.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ChatbotActivity : AppCompatActivity() {

    // API Configuration
    private val apiKey = "You know what"
    private val client = OkHttpClient()

    // Firebase
    private val db = FirebaseFirestore.getInstance()

    // UI Components
    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var outputTextView: TextView

    // Chat History
    private val chatHistory = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        setupViews()
        setupClickListeners()

        // Load previous chat history from Firebase
        loadChatHistoryFromFirebase { previousChat ->
            if (previousChat.isNotEmpty()) {
                chatHistory.addAll(previousChat)
                askGemini() // AI analyzes previous messages silently
            }
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
                inputEditText.text.clear()
                handleUserMessage(userMessage)
                saveMessageToFirebase("user", userMessage)
            }
        }
    }

    private fun handleUserMessage(message: String) {
        // Add to chat history
        chatHistory.add(
            mapOf(
                "role" to "user",
                "parts" to listOf(mapOf("text" to message))
            )
        )

        // Show in UI
        outputTextView.append("\n\nYou: $message")

        // Send to AI
        askGemini()
    }

    private fun askGemini() {
        lifecycleScope.launch(Dispatchers.IO) {
            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val jsonBody = JSONObject(mapOf("contents" to chatHistory)).toString()
            val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showError("Error: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    handleApiResponse(response)
                }
            })
        }
    }

    private fun handleApiResponse(response: Response) {
        if (!response.isSuccessful) {
            showError("Unexpected response: $response")
            return
        }

        try {
            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")

            if (candidates != null && candidates.length() > 0) {
                val text = extractAiResponse(candidates)
                handleAiResponse(text)
                saveMessageToFirebase("model", text)
            } else {
                showError("No response from AI.")
            }
        } catch (e: Exception) {
            showError("Parsing error: ${e.message}")
        }
    }

    private fun extractAiResponse(candidates: org.json.JSONArray): String {
        return candidates.getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .replace(Regex("[*_]{1,2}"), "") // Remove markdown
    }

    private fun handleAiResponse(text: String) {
        // Save to history
        chatHistory.add(
            mapOf(
                "role" to "model",
                "parts" to listOf(mapOf("text" to text))
            )
        )

        // Show in UI
        runOnUiThread {
            outputTextView.append("\n\nAI: $text")
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            outputTextView.append("\n\n$message")
        }
    }

    // ================= Firebase Methods =================
    private fun saveMessageToFirebase(role: String, text: String) {
        val message = hashMapOf(
            "role" to role,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("chats")
            .add(message)
            .addOnSuccessListener { Log.d("Firebase", "Message saved") }
            .addOnFailureListener { e -> Log.e("Firebase", "Error saving message", e) }
    }

    private fun loadChatHistoryFromFirebase(callback: (List<Map<String, Any>>) -> Unit) {
        db.collection("chats")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                val previousChat = result.map { doc ->
                    mapOf(
                        "role" to doc.getString("role")!!,
                        "parts" to listOf(mapOf("text" to doc.getString("text")!!))
                    )
                }
                callback(previousChat)
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Failed to load chat", e)
                callback(emptyList())
            }
    }
}

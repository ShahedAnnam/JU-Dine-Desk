package com.example.judinedesk.activities

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.judinedesk.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class ChatbotActivity : AppCompatActivity() {

    // API Configuration
    private val apiKey = "Secrete Bro.."
    private val client = OkHttpClient()

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
                handleUserMessage(userMessage)
            }
        }
    }

    private fun handleUserMessage(message: String) {
        // Add to chat history
        chatHistory.add(mapOf(
            "role" to "user",
            "parts" to listOf(mapOf("text" to message))
        ))

        // Show in UI
        outputTextView.append("\n\nYou: $message")

        // Send to AI
        askGemini()

        // Clear input
        inputEditText.text.clear()
    }

    private fun askGemini() {
        lifecycleScope.launch(Dispatchers.IO) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
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
        chatHistory.add(mapOf(
            "role" to "model",
            "parts" to listOf(mapOf("text" to text))
        ))

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
}
package com.example.judinedesk

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    // TODO: Replace with your actual Gemini API key
    private val apiKey = "YOUR_API_KEY_HERE"

    private val client = OkHttpClient()

    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var outputTextView: TextView

    // Chat history storing user and AI messages
    private val chatHistory = mutableListOf<Map<String, Any>>()

    // Firebase Analytics instance
    private lateinit var analytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputEditText = findViewById(R.id.inputEditText)
        sendButton = findViewById(R.id.sendButton)
        outputTextView = findViewById(R.id.outputTextView)



        // --- Firebase Connection Check ---
        analytics = FirebaseAnalytics.getInstance(this)

        val connectionBundle = Bundle()
        connectionBundle.putString("status", "connected")
        analytics.logEvent("connection_test", connectionBundle)
        Toast.makeText(this, "Firebase is connected!", Toast.LENGTH_LONG).show()
        // Log app opened event
        val appOpenBundle = Bundle()
        analytics.logEvent("app_opened", appOpenBundle)

        sendButton.setOnClickListener {
            val userPrompt = inputEditText.text.toString().trim()
            if (userPrompt.isNotEmpty()) {
                // Save user message to chat history
                chatHistory.add(
                    mapOf("role" to "user", "parts" to listOf(mapOf("text" to userPrompt)))
                )

                // Show user message in UI
                outputTextView.append("\n\nYou: $userPrompt")

                // Log user message
                val userBundle = Bundle()
                userBundle.putString("message_text", userPrompt)
                analytics.logEvent("user_message", userBundle)

                // Ask Gemini API
                askGemini()

                inputEditText.text.clear()
            }
        }
    }

    private fun askGemini() {
        lifecycleScope.launch(Dispatchers.IO) {
            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            // Convert full chat history into JSON
            val jsonBody = JSONObject(mapOf("contents" to chatHistory)).toString()
            val mediaType = "application/json".toMediaTypeOrNull()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        outputTextView.append("\n\nError: ${e.message}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            runOnUiThread {
                                outputTextView.append("\n\nUnexpected response: $response")
                            }
                            return
                        }

                        val responseBody = response.body?.string()
                        Log.d("GeminiAPI", "Response: $responseBody")

                        try {
                            val json = JSONObject(responseBody ?: "")
                            val candidates = json.optJSONArray("candidates")

                            if (candidates != null && candidates.length() > 0) {
                                val text = candidates.getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text")
                                    .replace(Regex("[*_]{1,2}"), "") // clean Markdown

                                // Save AI response to history
                                chatHistory.add(
                                    mapOf("role" to "model", "parts" to listOf(mapOf("text" to text)))
                                )

                                // Log AI response
                                val aiBundle = Bundle()
                                aiBundle.putString("response_text", text)
                                analytics.logEvent("ai_response", aiBundle)

                                runOnUiThread {
                                    outputTextView.append("\n\nAI: $text")
                                }
                            } else {
                                runOnUiThread {
                                    outputTextView.append("\n\nNo response from AI.")
                                }
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                outputTextView.append("\n\nParsing error: ${e.message}")
                            }
                        }
                    }
                }
            })
        }
    }
}

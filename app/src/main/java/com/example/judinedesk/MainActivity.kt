package com.example.judinedesk

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private val apiKey = "never share here publicly"


    private val client = OkHttpClient()

    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var outputTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputEditText = findViewById(R.id.inputEditText)
        sendButton = findViewById(R.id.sendButton)
        outputTextView = findViewById(R.id.outputTextView)

        sendButton.setOnClickListener {
            val userPrompt = inputEditText.text.toString().trim()
            if (userPrompt.isNotEmpty()) {
                askGemini(userPrompt)
            }
        }
    }

    private fun askGemini(prompt: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val jsonBody = """
                {
                  "contents": [
                    {
                      "parts": [
                        {"text": "$prompt"}
                      ]
                    }
                  ]
                }
            """.trimIndent()

            val mediaType = "application/json".toMediaTypeOrNull()
            val requestBody = RequestBody.create(mediaType, jsonBody)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        outputTextView.text = "Error: ${e.message}"
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) {
                            runOnUiThread {
                                outputTextView.text = "Unexpected code: $response"
                            }
                            return
                        }

                        val responseBody = response.body?.string()
                        Log.d("GeminiAPI", "✅ Response: $responseBody")

                        try {
                            val json = JSONObject(responseBody ?: "")
                            var text = json.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")

                            // ✅ Remove Markdown symbols like **, __, *, _, etc.
                            text = text.replace(Regex("[*_]{1,2}"), "")

                            runOnUiThread {
                                outputTextView.text = text
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                outputTextView.text = "Parsing error: ${e.message}"
                            }
                        }
                    }
                }
            })
        }
    }
}

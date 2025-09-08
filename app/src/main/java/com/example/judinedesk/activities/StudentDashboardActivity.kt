package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.utils.AuthHelper
import android.widget.Toast


class StudentDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is logged in, if not redirect to login
        if (!AuthHelper.isLoggedIn()) {
            redirectToLogin()
            return
        }

        setContentView(R.layout.activity_student_dashboard)
        setupChatButton()
        setupLogoutButton()
        setupFeedbackButton()
    }
    private fun setupFeedbackButton() {
        val btnFeedback = findViewById<Button>(R.id.btn_feedback)
        btnFeedback.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }
    }



    private fun setupChatButton() {
        val btnChatAI = findViewById<Button>(R.id.btn_ai_chat)
        btnChatAI.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close this activity
    }

    // In StudentDashboardActivity.kt
    private fun setupLogoutButton() {
        val btnLogout = findViewById<Button>(R.id.btn_logout) // Add this button to your XML
        btnLogout.setOnClickListener {
            AuthHelper.logout()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            redirectToLogin()
        }
    }
}
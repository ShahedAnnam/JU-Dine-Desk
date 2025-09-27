package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.utils.AuthHelper

class StudentDashboardActivity : AppCompatActivity() {

    // Buttons at the top
    private var btnProfile: Button? = null
    private var btnAIChat: Button? = null
    private var btnLogout: Button? = null

    // Feature cards
    private var feedbackLayout: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check login
        if (!AuthHelper.isLoggedIn()) {
            redirectToLogin()
            return
        }

        setContentView(R.layout.activity_student_dashboard)

        // Initialize views
        setupViews()

        // Set click listeners
        setupClickListeners()
    }

    private fun setupViews() {
        btnProfile = findViewById(R.id.btn_profile)
        btnAIChat = findViewById(R.id.btn_ai_chat)
        btnLogout = findViewById(R.id.btn_logout)

        feedbackLayout = findViewById(R.id.feedback_layout)
    }

    private fun setupClickListeners() {
        btnProfile?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnAIChat?.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }

        btnLogout?.setOnClickListener {
            AuthHelper.logout()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            redirectToLogin()
        }

        feedbackLayout?.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

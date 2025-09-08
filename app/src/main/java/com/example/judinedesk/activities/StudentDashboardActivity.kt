package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.utils.AuthHelper


class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var btnProfile: Button
    private lateinit var btnChatAI: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check login
        if (!AuthHelper.isLoggedIn()) {
            redirectToLogin()
            return
        }

        setContentView(R.layout.activity_student_dashboard)
<<<<<<< HEAD
        setupChatButton()
        setupLogoutButton()
        setupFeedbackButton()
    }
    private fun setupFeedbackButton() {
        val btnFeedback = findViewById<Button>(R.id.btn_feedback)
        btnFeedback.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }
=======
        setupViews()
        setupClickListeners()
>>>>>>> origin/shahed
    }

    private fun setupViews() {
        btnProfile = findViewById(R.id.btn_profile)
        btnChatAI = findViewById(R.id.btn_ai_chat)
        btnLogout = findViewById(R.id.btn_logout)
    }

    private fun setupClickListeners() {
        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnChatAI.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }

        btnLogout.setOnClickListener {
            AuthHelper.logout()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            redirectToLogin()
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

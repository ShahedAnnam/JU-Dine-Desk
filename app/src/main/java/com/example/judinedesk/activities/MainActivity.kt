package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.utils.AuthHelper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in - if yes, go directly to dashboard
        if (AuthHelper.isLoggedIn()) {
            redirectToDashboard()
            return
        }

        setContentView(R.layout.activity_main)
        setupNavigation()
    }

    private fun setupNavigation() {
        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnGetStarted.setOnClickListener {
            // Only allow access if user is logged in
            if (AuthHelper.isLoggedIn()) {
                startActivity(Intent(this, StudentDashboardActivity::class.java))
            } else {
                Toast.makeText(this, "Please login first to access the dashboard", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun redirectToDashboard() {
        val intent = Intent(this, StudentDashboardActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity so user can't go back
    }


    override fun onStart() {
        super.onStart()
        // Optional: Check if user logged out from other activities
        if (AuthHelper.isLoggedIn() && !isTaskRoot) {
            redirectToDashboard()
        }
    }
}
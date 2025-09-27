package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.models.Manager
import com.example.judinedesk.models.Student
import com.example.judinedesk.models.Staff
import com.example.judinedesk.utils.AuthHelper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If user is logged in → send to specific dashboard
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
            if (AuthHelper.isLoggedIn()) {
                redirectToDashboard()
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
        val user = AuthHelper.currentUserData


        val intent = when (user) {
            is Student -> Intent(this, StudentDashboardActivity::class.java)
            is Manager -> Intent(this, ManagerDashboardActivity::class.java)
            is Staff -> Intent(this, StaffDashboardActivity::class.java)
            else -> {
                Toast.makeText(this, "Role not found, please login again", Toast.LENGTH_SHORT).show()
                Intent(this, LoginActivity::class.java)
            }
        }

        startActivity(intent)
        finish() // Prevent going back to MainActivity
    }


    override fun onStart() {
        super.onStart()
        if (AuthHelper.isLoggedIn() && !isTaskRoot) {
            redirectToDashboard()
        }
    }
}

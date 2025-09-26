package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.models.Manager
import com.example.judinedesk.models.Student
import com.example.judinedesk.models.Staff
import com.example.judinedesk.utils.AuthHelper
import android.util.Log

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setupViews()
        setupClickListeners()
    }

    private fun setupViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        tvRegister.setOnClickListener {
            goToRegister()
        }
    }

    private fun loginUser(email: String, password: String) {
        btnLogin.isEnabled = false

        AuthHelper.loginUser(email, password) { success, message, user ->
            btnLogin.isEnabled = true

            if (success) {
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                when (user) {
                    is Student -> goToStudentDashboard(user)
                    is Manager -> goToManagerDashboard(user)
                    is Staff -> goToStaffDashboard(user)
                    else -> Toast.makeText(this, "Unknown user role", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(this, "Login failed: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToStudentDashboard(student: Student) {
        val intent =Intent(this, StudentDashboardActivity::class.java)
        intent.putExtra("student", student)
        startActivity(intent)
        finish()
    }

    private fun goToManagerDashboard(manager: Manager) {
        val intent =Intent(this, ManagerDashboardActivity::class.java)
        intent.putExtra("manager", manager)
        startActivity(intent)
        finish()
    }

    private fun goToStaffDashboard(staff: Staff) {
        startActivity(Intent(this, StaffDashboardActivity::class.java))
        finish()
    }

    private fun goToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }
}

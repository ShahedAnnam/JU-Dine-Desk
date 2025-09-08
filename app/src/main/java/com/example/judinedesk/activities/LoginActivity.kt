package com.example.judinedesk.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.example.judinedesk.models.Student
import com.example.judinedesk.utils.AuthHelper

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

            loginStudent(email, password)
        }

        tvRegister.setOnClickListener {
            goToRegister()
        }
    }

    private fun loginStudent(email: String, password: String) {
        btnLogin.isEnabled = false

        // Updated login to fetch full Student object
        AuthHelper.loginStudent(email, password) { success, message, student ->
            btnLogin.isEnabled = true

            if (success && student != null) {
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                goToDashboard(student)
            } else {
                Toast.makeText(this, "Login failed: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToDashboard(student: Student) {
        val intent = Intent(this, StudentDashboardActivity::class.java)
        //intent.putExtra("student_data", student) // Pass the full student object
        startActivity(intent)
        finish()
    }

    private fun goToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}
